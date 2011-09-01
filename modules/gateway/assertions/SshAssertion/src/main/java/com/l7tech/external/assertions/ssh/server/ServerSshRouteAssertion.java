package com.l7tech.external.assertions.ssh.server;

import com.jscape.inet.scp.Scp;
import com.jscape.inet.scp.ScpException;
import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpException;
import com.jscape.inet.ssh.util.HostKeyFingerprintVerifier;
import com.jscape.inet.ssh.util.SshHostKeys;
import com.jscape.inet.ssh.util.SshParameters;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.external.assertions.ssh.keyprovider.SshKeyUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the SshRouteAssertion.
 *
 * @see com.l7tech.external.assertions.ssh.SshRouteAssertion
 */
public class ServerSshRouteAssertion extends ServerRoutingAssertion<SshRouteAssertion> {

    private SecurePasswordManager securePasswordManager;
    private ClusterPropertyCache clusterPropertyCache;
    private final StashManagerFactory stashManagerFactory;

    public ServerSshRouteAssertion(SshRouteAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion, context);

        securePasswordManager = context.getBean("securePasswordManager", SecurePasswordManager.class);
        clusterPropertyCache = context.getBean("clusterPropertyCache", ClusterPropertyCache.class);
        stashManagerFactory = applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
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

        // determine username and password based or if they were pass-through or specified
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
                String publicKeyFingerprint = ExpandVariables.process(assertion.getSshPublicKey().trim(), context.getVariableMap(
                        Syntax.getReferencedNames(assertion.getSshPublicKey().trim()), getAudit()), getAudit());

                // validate public key fingerprint
                Pair<Boolean, String> fingerprintIsValid = SshKeyUtil.validateSshPublicKeyFingerprint(publicKeyFingerprint);
                if(!fingerprintIsValid.left){
                    logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, SshAssertionMessages.SSH_INVALID_PUBLIC_KEY_FINGERPRINT_EXCEPTION);
                    return AssertionStatus.FAILED;
                }
                String hostPublicKey = publicKeyFingerprint;
                SshHostKeys sshHostKeys = new SshHostKeys();
                sshHostKeys.addKey(InetAddress.getByName(host), hostPublicKey);
                sshParams.setHostKeyVerifier(new HostKeyFingerprintVerifier(sshHostKeys));
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

            // download / upload the file
            try {
                if (assertion.isDownloadCopyMethod()) {
                    PipedInputStream pis = new PipedInputStream();
                    PipedOutputStream pos = new PipedOutputStream(pis);

                    // download file on a new thread
                    Thread thread = sshDownloadOnNewThread(sshClient, expandVariables(context, assertion.getDirectory()),
                            expandVariables(context, assertion.getFileName()), pos, logger);

                    Message response = context.getResponse();
                    response.initialize(stashManagerFactory.createStashManager(), ContentTypeHeader.create(assertion.getDownloadContentType()), pis);
                    // TODO impose configurable max size limit on response

                    // force all message parts to be initialized, it is by default lazy
                    response.getMimeKnob().getContentLength();

                    logger.log(Level.INFO, "Waiting for read thread join().");
                    thread.join(assertion.getConnectTimeout() * 1000L);
                    logger.log(Level.INFO, "Done read thread join().");
                } else {
                    sshClient.upload(mimeKnob.getEntireMessageBodyAsInputStream(), expandVariables(context, assertion.getDirectory()),  expandVariables(context, assertion.getFileName()));
                }
            } catch (NoSuchPartException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {SshAssertionMessages.SSH_NO_SUCH_PART_ERROR + ",server:" + getHostName(context, assertion)+ ",error:" + e.getMessage()}, e);
                return AssertionStatus.FAILED;
            } catch (ScpException e) {
                if (ExceptionUtils.getMessage(e).contains("SSH_FX_NO_SUCH_FILE")){
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SshAssertionMessages.SSH_DIR_DOESNT_EXIST_ERROR + ",server:" + getHostName(context, assertion)+ ",error:" + e.getMessage()}, e);
                } else{
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SshAssertionMessages.SSH_EXCEPTION_ERROR + ",server:" + getHostName(context, assertion)+ ",error:"  + e.getMessage()}, e);
                }
                return AssertionStatus.FAILED;
            } catch (SftpException e) {
                if (ExceptionUtils.getMessage(e).contains("SSH_FX_NO_SUCH_FILE")){
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SshAssertionMessages.SSH_DIR_DOESNT_EXIST_ERROR + ",server:" + getHostName(context, assertion)+ ",error:" + e.getMessage()}, e);
                } else{
                    logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SshAssertionMessages.SSH_EXCEPTION_ERROR + ",server:" + getHostName(context, assertion)+ ",error:"  + e.getMessage()}, e);
                }
                return AssertionStatus.FAILED;
            } catch (IOException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] { SshAssertionMessages.SSH_IO_EXCEPTION + ",server:" + getHostName(context, assertion) + ",error:" + e.getMessage()}, e);
                logger.log(Level.WARNING, "SFTP Route Assertion IO error: " + e, ExceptionUtils.getDebugException(e));
                return AssertionStatus.FAILED;
            } finally {
                if (sshClient != null){
                    sshClient.disconnect();
                }
            }

            context.setRoutingStatus(RoutingStatus.ROUTED);
            return AssertionStatus.NONE;
        } catch(IOException ioe) {
            if (ExceptionUtils.getMessage(ioe).startsWith("Malformed SSH")){
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {SshAssertionMessages.SSH_CERT_ISSUE_EXCEPTION, ioe.getMessage()}, ioe);
                logger.log(Level.WARNING, SshAssertionMessages.SSH_CERT_ISSUE_EXCEPTION);
            } else if ( ioe instanceof SocketException ){
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Socket Exception for SFTP connection. Ensure the timeout entered is valid" + ioe.getMessage()}, ioe);
                logger.log(Level.WARNING, SshAssertionMessages.SSH_SOCKET_EXCEPTION +ioe,new String[] {host, String.valueOf(port), username});
            } else {
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"IO Exception... SFTP connection establishment failed. Ensure the server trusted cert is valid" + ioe.getMessage()}, ioe);
                logger.log(Level.WARNING, SshAssertionMessages.SSH_CONNECTION_EXCEPTION +ioe,new String[] {host, String.valueOf(port), username});
            }
            return AssertionStatus.FAILED;
        } catch(Exception e) {
            logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {e.getMessage()}, e);
            logger.log(Level.WARNING, SshAssertionMessages.SSH_CONNECTION_EXCEPTION, new String[] {host, String.valueOf(port), username});
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

    /*
     * Download the given file on a new thread.
     */
    private static Thread sshDownloadOnNewThread(final ServerSshRouteClient sshClient, final String directory,
                                                 final String fileName, final PipedOutputStream pos, final Logger logger) throws IOException {
        final CountDownLatch startedSignal = new CountDownLatch(1);
        logger.log(Level.INFO, "Start new thread for downloading");
        Thread thread = new Thread(new Runnable(){
            public void run() {
                try {
                    startedSignal.countDown();
                    sshClient.download(pos, directory, fileName);
                }
                catch (Exception e) {
                    logger.log(Level.SEVERE, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
                finally {
                    logger.log(Level.INFO, "... downloading thread stopped.");
                    try {
                        pos.flush();
                        pos.close();
                    } catch(IOException ioe) {
                        logger.log(Level.SEVERE, ExceptionUtils.getMessage(ioe), ExceptionUtils.getDebugException(ioe));
                    }
                    startedSignal.countDown();
                }
            }
        }, "SshDownloadThread-" + System.currentTimeMillis());

        thread.setDaemon(true);
        thread.start();

        try {
            startedSignal.await();
        }
        catch(InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, ExceptionUtils.getMessage(ie), ExceptionUtils.getDebugException(ie));
        }

        return thread;
    }
}

