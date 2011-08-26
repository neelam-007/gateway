package com.l7tech.external.assertions.ssh.server;

import com.jscape.inet.scp.Scp;
import com.jscape.inet.scp.ScpException;
import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpException;
import com.jscape.inet.ssh.util.SshHostKeys;
import com.jscape.inet.ssh.util.SshParameters;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.ParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server side implementation of the SshRouteAssertion.
 *
 * @see com.l7tech.external.assertions.ssh.SshRouteAssertion
 */
public class ServerSshRouteAssertion extends ServerRoutingAssertion<SshRouteAssertion> {

    private SecurePasswordManager securePasswordManager;
    private ClusterPropertyCache clusterPropertyCache;

    public ServerSshRouteAssertion(SshRouteAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion, context);

        securePasswordManager = context.getBean("securePasswordManager", SecurePasswordManager.class);
        clusterPropertyCache = context.getBean("clusterPropertyCache", ClusterPropertyCache.class);
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {

        Message request;
        try {
            request = context.getTargetMessage(assertion.getRequestTarget());
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }

        final MimeKnob mimeKnob = request.getKnob(MimeKnob.class);
        if (mimeKnob == null) {
            // Uninitialized request
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Request is not initialized; nothing to route");
            return AssertionStatus.BAD_REQUEST;
        }

        // DELETE CURRENT SECURITY HEADER IF NECESSARY
        try {
            handleProcessedSecurityHeader(request);
        } catch(SAXException se) {
            logger.log(Level.INFO, "Error processing security header, request XML invalid ''{0}''", se.getMessage());
        }

        // determine username and password based on if they were pass-through or specified
        String username = null;
        String password = null;
        if (assertion.isCredentialsSourceSpecified()) {
            username = ExpandVariables.process(assertion.getUsername(), context.getVariableMap(Syntax.getReferencedNames(assertion.getUsername()), getAudit()), getAudit());
            if(assertion.getPasswordOid() != null) {
                try {
                    password = new String(securePasswordManager.decryptPassword(securePasswordManager.findByPrimaryKey(assertion.getPasswordOid()).getEncodedPassword()));
                } catch(FindException fe) {
                    return AssertionStatus.FAILED;
                } catch(ParseException pe) {
                    return AssertionStatus.FAILED;
                }
            }
        } else {
            final LoginCredentials credentials = context.getDefaultAuthenticationContext().getLastCredentials();
            if (credentials != null) {
                username = credentials.getName();
                password = new String(credentials.getCredentials());
            }
            if (username == null) {
                logAndAudit(AssertionMessages.SSH_ROUTING_PASSTHRU_NO_USERNAME);
                return AssertionStatus.FAILED;
            }
        }

        String host = ExpandVariables.process(assertion.getHost(), context.getVariableMap(Syntax.getReferencedNames(assertion.getHost()), getAudit()), getAudit());
        int port;
        try{
            port = Integer.parseInt(ExpandVariables.process(assertion.getPort(), context.getVariableMap(Syntax.getReferencedNames(assertion.getPort()), getAudit()), getAudit()));
        } catch (Exception e){
            //use default port
            port = 22;
        }

        ServerSshRouteClient sshClient = null;
        try {
            SshParameters sshParams = new SshParameters(host, port, username, password);

            if (assertion.isUsePublicKey() && assertion.getSshPublicKey() != null){
                String publicKey = ExpandVariables.process(assertion.getSshPublicKey().trim(), context.getVariableMap(
                        Syntax.getReferencedNames(assertion.getSshPublicKey().trim()), getAudit()), getAudit());

                // validate Public Key Data to cover context var scenario
                Pair<Boolean, String> publicIsValid = validatePublicKeyData(publicKey);
                if(!publicIsValid.left){
                    logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, SshAssertionMessages.SFTP_INVALID_CERT_EXCEPTION);
                    return AssertionStatus.FAILED;
                }
                String hostPublicKey = publicIsValid.right;
                SshHostKeys sshHostKeys = new SshHostKeys();
                sshHostKeys.addKey(InetAddress.getByName(host), hostPublicKey);
                sshParams.setHostKeys(sshHostKeys, false);
            }

            if(assertion.isUsePrivateKey()) {
                String privateKeyText = ExpandVariables.process(assertion.getPrivateKey(), context.getVariableMap(Syntax.getReferencedNames(assertion.getPrivateKey()), getAudit()), getAudit());
                sshParams.setSshPassword(null);
                if(password == null) {
                    sshParams.setPrivateKey(privateKeyText);
                } else {
                    sshParams.setPrivateKey(privateKeyText, password);
                }
            } else {
                sshParams = new SshParameters(host, port, username, password);
            }

            if (assertion.isScpProtocol()) {
                sshClient = new ServerSshRouteClient(new Scp(sshParams));
            } else {
                sshClient = new ServerSshRouteClient(new Sftp(sshParams));
            }

            sshClient.setTimeout(assertion.getConnectTimeout());   // connect timeout value from assertion UI
            sshClient.connect();

            if(!sshClient.isConnected()) {
                sshClient.disconnect();
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, "Failed to authenticate with the remote server.");
                return AssertionStatus.FAILED;
            }

            // upload the file
            try {
                sshClient.upload(mimeKnob.getEntireMessageBodyAsInputStream(), expandVariables(context, assertion.getDirectory()),  expandVariables(context, assertion.getFileName()));
            } catch (NoSuchPartException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {SshAssertionMessages.SFTP_NO_SUCH_PART_ERROR + ",server:" + getHostName(context, assertion)+ ",error:" + e.getMessage()}, e);
                return AssertionStatus.FAILED;
            } catch (ScpException e) {
                if (ExceptionUtils.getMessage(e).contains("SSH_FX_NO_SUCH_FILE")){
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SshAssertionMessages.SFTP_DIR_DOESNT_EXIST_ERROR+ ",server:" + getHostName(context, assertion)+ ",error:" + e.getMessage()}, e);
                } else{
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SshAssertionMessages.SFTP_EXCEPTION_ERROR + ",server:" + getHostName(context, assertion)+ ",error:"  + e.getMessage()}, e);
                }
                return AssertionStatus.FAILED;
            } catch (SftpException e) {
                if (ExceptionUtils.getMessage(e).contains("SSH_FX_NO_SUCH_FILE")){
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SshAssertionMessages.SFTP_DIR_DOESNT_EXIST_ERROR+ ",server:" + getHostName(context, assertion)+ ",error:" + e.getMessage()}, e);
                } else{
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SshAssertionMessages.SFTP_EXCEPTION_ERROR + ",server:" + getHostName(context, assertion)+ ",error:"  + e.getMessage()}, e);
                }
                return AssertionStatus.FAILED;
            } catch (IOException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SshAssertionMessages.SFTP_IO_EXCEPTION + ",server:" + getHostName(context, assertion) + ",error:" + e.getMessage()}, e);
                logger.log(Level.WARNING, "SFTP Route Assertion IO error: " + e, ExceptionUtils.getDebugException(e));
                return AssertionStatus.FAILED;
            } finally {
                if (sshClient != null){
                    sshClient.disconnect();
                }
            }

            return AssertionStatus.NONE;
        } catch(IOException ioe) {
            if (ExceptionUtils.getMessage(ioe).startsWith("Malformed SSH")){
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {SshAssertionMessages.SFTP_CERT_ISSUE_EXCEPTION, ioe.getMessage()}, ioe);
                logger.log(Level.WARNING, SshAssertionMessages.SFTP_CERT_ISSUE_EXCEPTION);
            } else if ( ioe instanceof SocketException ){
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Socket Exception for SFTP connection. Ensure the timeout entered is valid" + ioe.getMessage()}, ioe);
                logger.log(Level.WARNING, SshAssertionMessages.SFTP_SOCKET_EXCEPTION +ioe,new String[] {host, String.valueOf(port), username});
            } else {
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"IO Exception... SFTP connection establishment failed. Ensure the server trusted cert is valid" + ioe.getMessage()}, ioe);
                logger.log(Level.WARNING, SshAssertionMessages.SFTP_CONNECTION_EXCEPTION +ioe,new String[] {host, String.valueOf(port), username});
            }
            return AssertionStatus.FAILED;
        } catch(Exception e) {
            logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {e.getMessage()}, e);
            logger.log(Level.WARNING, SshAssertionMessages.SFTP_CONNECTION_EXCEPTION, new String[] {host, String.valueOf(port), username});
            logger.log(Level.WARNING, "SFTP Route Assertion error: " + e, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        } finally {
            if(sshClient != null) {
                sshClient.disconnect();
            }
        }
    }

    private String expandVariables(PolicyEnforcementContext context, String pattern) {
        final String[] variablesUsed = Syntax.getReferencedNames(pattern);
        final Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());
        return ExpandVariables.process(pattern, vars, getAudit());
    }

    private String getHostName(PolicyEnforcementContext context, SshRouteAssertion assertion) {
        return expandVariables(context, assertion.getHost());
    }

     public Pair<Boolean, String> validatePublicKeyData(String chardata) {
        boolean isValid;
        String keyString = "";
        try {
            Pattern p = Pattern.compile("(.*)\\s?(ssh-(dss|rsa))\\s+([a-zA-Z0-9+/]+={0,2})(?: .*|$)");
            Matcher m = p.matcher(chardata.trim());
            if(m.matches()) {
                String keyType = m.group(2);
                String keyText = m.group(4);
                byte[] key = HexUtils.decodeBase64(keyText, true);

                String decodedAlgorithmDesc = new String(key, 4, 7, "ISO8859_1");
                if (keyType.compareTo(decodedAlgorithmDesc) == 0){
                       keyString = keyType+ " "+keyText;
                       isValid = true;
                } else {
                       isValid = false;
                       logger.log(Level.WARNING, SshAssertionMessages.SFTP_ALGO_NOT_SUPPORTED_EXCEPTION, keyType);
                }

            } else {
               isValid = false;
               logger.log(Level.WARNING, SshAssertionMessages.SFTP_WRONG_FORMAT_SUPPORTED_EXCEPTION);
            }
        } catch (IOException e) {
            isValid = false;
            logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, e.toString(), "IO Exception... SFTP server trusted cert is INVALID");
            logger.log(Level.WARNING, SshAssertionMessages.SFTP_INVALID_CERT_EXCEPTION);
        }

        return new Pair<Boolean, String>(isValid, keyString);
    }
}

