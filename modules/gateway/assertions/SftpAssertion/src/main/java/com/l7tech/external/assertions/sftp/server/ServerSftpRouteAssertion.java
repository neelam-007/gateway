package com.l7tech.external.assertions.sftp.server;

import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpException;
import com.jscape.inet.ssh.util.SshHostKeys;
import com.jscape.inet.ssh.util.SshParameters;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.sftp.SftpRouteAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
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
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server side implementation of the SftpRouteAssertion.
 *
 * @see com.l7tech.external.assertions.sftp.SftpRouteAssertion
 */
public class ServerSftpRouteAssertion extends ServerRoutingAssertion<SftpRouteAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSftpRouteAssertion.class.getName());

    private final SftpRouteAssertion assertion;
    private final Auditor auditor;

    private SecurePasswordManager securePasswordManager;
    private ClusterPropertyCache clusterPropertyCache;

    public ServerSftpRouteAssertion(SftpRouteAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion, context, logger);

        this.assertion = assertion;
        this.auditor = context != null ? new Auditor(this, context, logger) : new LogOnlyAuditor(logger);

        securePasswordManager = (SecurePasswordManager)context.getBean("securePasswordManager");
        clusterPropertyCache = (ClusterPropertyCache)context.getBean("clusterPropertyCache");
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
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Request is not initialized; nothing to route");
            return AssertionStatus.BAD_REQUEST;
        }

        // DELETE CURRENT SECURITY HEADER IF NECESSARY
        try {
            handleProcessedSecurityHeader(request);
        } catch(SAXException se) {
            logger.log(Level.INFO, "Error processing security header, request XML invalid ''{0}''", se.getMessage());
        }

        String username = ExpandVariables.process(assertion.getUsername(), context.getVariableMap(Syntax.getReferencedNames(assertion.getUsername()), auditor), auditor);
        String host = ExpandVariables.process(assertion.getHost(), context.getVariableMap(Syntax.getReferencedNames(assertion.getHost()), auditor), auditor);
        int port = 0;
        try{
            port = Integer.parseInt(ExpandVariables.process(assertion.getPort(), context.getVariableMap(Syntax.getReferencedNames(assertion.getPort()), auditor), auditor));
        } catch (Exception e){
            //use default port
            port = 22;
        }
        String password = null;
        if(assertion.getPasswordOid() != null) {
            try {
                password = new String(securePasswordManager.decryptPassword(securePasswordManager.findByPrimaryKey(assertion.getPasswordOid()).getEncodedPassword()));
            } catch(FindException fe) {
                return AssertionStatus.FAILED;
            } catch(ParseException pe) {
                return AssertionStatus.FAILED;
            }
        }

        String fileName = null;
        String dir = null;
        if (assertion.getFileNameSource() == FtpFileNameSource.AUTO) {
            fileName = context.getRequestId().toString();
        } else if (assertion.getFileNameSource() == FtpFileNameSource.PATTERN) {
            fileName = expandVariables(context, assertion.getFileNamePattern());
        }

        if ((assertion.getDirectory() != null) && (!assertion.getDirectory().equalsIgnoreCase(""))){
            dir = expandVariables(context, assertion.getDirectory());
        }

        Sftp sftpClient = null;
        try {
            SshParameters sshParams = new SshParameters(host, port, username, password);

            if (assertion.isUsePublicKey() && assertion.getSshPublicKey() != null){
                String publicKey = ExpandVariables.process(assertion.getSshPublicKey().trim(), context.getVariableMap(
                        Syntax.getReferencedNames(assertion.getSshPublicKey().trim()), auditor), auditor);

                // validate Public Key Data to cover context var scenario
                Pair<Boolean, String> publicIsValid = validatePublicKeyData(publicKey);
                if(!publicIsValid.left.booleanValue()){
                    auditor.logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, SftpAssertionMessages.SFTP_INVALID_CERT_EXCEPTION);
                    return AssertionStatus.FAILED;
                }
                String hostPublicKey = publicIsValid.right.toString();
                SshHostKeys sshHostKeys = new SshHostKeys();
                sshHostKeys.addKey(InetAddress.getByName(host), hostPublicKey);
                sshParams.setHostKeys(sshHostKeys, false);
            }

            if(assertion.isUsePrivateKey()) {
                String privateKeyText = ExpandVariables.process(assertion.getPrivateKey(), context.getVariableMap(Syntax.getReferencedNames(assertion.getPrivateKey()), auditor), auditor);
                sshParams.setSshPassword(null);
                if(password == null) {
                    sshParams.setPrivateKey(privateKeyText);
                } else {
                    sshParams.setPrivateKey(privateKeyText, password);
                }
            } else  {
                sshParams = new SshParameters(host, port, username, password);
            }

            sftpClient = new Sftp(sshParams);
            sftpClient.setTimeout(assertion.getTimeout());   // connect timeout value from assertion UI
            sftpClient.connect();

            if(!sftpClient.isConnected()) {
                sftpClient.disconnect();
                auditor.logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, "Failed to authenticate with the remote server.");
                return AssertionStatus.FAILED;
            }

            // upload the file
            try {
                sftpClient.setDir(dir);
                sftpClient.upload(mimeKnob.getEntireMessageBodyAsInputStream(), fileName);
            } catch (NoSuchPartException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {SftpAssertionMessages.SFTP_NO_SUCH_PART_ERROR + ",server:"+getHostName(context, assertion)+ ",error:"+e.getMessage()}, e);
                return AssertionStatus.FAILED;
            } catch (SftpException e) {
                if (e.getMessage().toString().contains("SSH_FX_NO_SUCH_FILE")){
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SftpAssertionMessages.SFTP_DIR_DOESNT_EXIST_ERROR+ ",server:" +getHostName(context, assertion)+ ",error:"+e.getMessage()}, e);
                } else{
                    auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SftpAssertionMessages.SFTP_EXCEPTION_ERROR + ",server:" +getHostName(context, assertion)+ ",error:" +e.getMessage()}, e);
                }
                return AssertionStatus.FAILED;
            } catch (IOException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SftpAssertionMessages.SFTP_IO_EXCEPTION + ",server:"+getHostName(context, assertion)+ ",error:"+e.getMessage()}, e);
                logger.log(Level.WARNING, "SFTP Route Assertion IO error: " + e, ExceptionUtils.getDebugException(e));
                return AssertionStatus.FAILED;
            } finally {
                if (sftpClient != null){
                    sftpClient.disconnect();
                }
            }

            return AssertionStatus.NONE;
        } catch(IOException ioe) {
            if (ioe.getMessage().toString().startsWith("Malformed SSH")){
                auditor.logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {SftpAssertionMessages.SFTP_CERT_ISSUE_EXCEPTION, ioe.getMessage()}, ioe);
                logger.log(Level.WARNING, SftpAssertionMessages.SFTP_CERT_ISSUE_EXCEPTION);
            } else if ( ioe instanceof SocketException ){
                auditor.logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Socket Exception for SFTP connection. Ensure the timeout entered is valid" + ioe.getMessage()}, ioe);
                logger.log(Level.WARNING, SftpAssertionMessages.SFTP_SOCKET_EXCEPTION +ioe,new String[] {host, String.valueOf(port), username});
            } else {
                auditor.logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"IO Exception... SFTP connection establishment failed. Ensure the server trusted cert is valid" + ioe.getMessage()}, ioe);
                logger.log(Level.WARNING, SftpAssertionMessages.SFTP_CONNECTION_EXCEPTION +ioe,new String[] {host, String.valueOf(port), username});
            }
            return AssertionStatus.FAILED;
        } catch(Exception e) {
            auditor.logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {e.getMessage()}, e);
            logger.log(Level.WARNING, SftpAssertionMessages.SFTP_CONNECTION_EXCEPTION, new String[] {host, String.valueOf(port), username});
            logger.log(Level.WARNING, "SFTP Route Assertion error: " + e, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        } finally {
            if(sftpClient != null) {
                sftpClient.disconnect();
            }
        }
    }

    private String expandVariables(PolicyEnforcementContext context, String pattern) {
        final String[] variablesUsed = Syntax.getReferencedNames(pattern);
        final Map<String, Object> vars = context.getVariableMap(variablesUsed, auditor);
        return ExpandVariables.process(pattern, vars, auditor);
    }

    private String getHostName(PolicyEnforcementContext context, SftpRouteAssertion assertion) {
        return expandVariables(context, assertion.getHost());
    }

     public Pair<Boolean, String> validatePublicKeyData(String chardata) {
        boolean isValid = false;
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
                       logger.log(Level.WARNING, SftpAssertionMessages.SFTP_ALGO_NOT_SUPPORTED_EXCEPTION, keyType);
                }

            } else {
               isValid = false;
               logger.log(Level.WARNING, SftpAssertionMessages.SFTP_WRONG_FORMAT_SUPPORTED_EXCEPTION);
            }
        } catch (IOException e) {
            isValid = false;
            auditor.logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, e.toString(), "IO Exception... SFTP server trusted cert is INVALID");
            logger.log(Level.WARNING, SftpAssertionMessages.SFTP_INVALID_CERT_EXCEPTION);
        }

        return new Pair<Boolean, String>(isValid, keyString);
    }
}

