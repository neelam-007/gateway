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
import com.l7tech.external.assertions.ssh.server.client.ScpClient;
import com.l7tech.external.assertions.ssh.server.client.SftpClient;
import com.l7tech.external.assertions.ssh.server.client.SshClient;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.*;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Server side implementation of the SshRouteAssertion.
 *
 * @see com.l7tech.external.assertions.ssh.SshRouteAssertion
 */
public class ServerSshRouteAssertion extends ServerRoutingAssertion<SshRouteAssertion> {

    @Inject
    private SecurePasswordManager securePasswordManager;
    @Inject
    @Named("stashManagerFactory")
    private StashManagerFactory stashManagerFactory;
    private final String[] variablesUsed;

    public ServerSshRouteAssertion(SshRouteAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion, context);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {

        final Message request;
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

        final Map<String,?> variables = context.getVariableMap( variablesUsed, getAudit() );

        // determine username and password based or if they were pass-through or specified
        String username = null;
        String password = null;
        if (assertion.isCredentialsSourceSpecified()) {
            username = ExpandVariables.process(assertion.getUsername(), variables, getAudit());
            if(assertion.getPasswordOid() != null) {
                try {
                    SecurePassword securePassword = securePasswordManager.findByPrimaryKey(assertion.getPasswordOid());
                    if (securePassword == null) {
                        logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, "Unable to find stored password for OID: " + assertion.getPasswordOid());
                        return AssertionStatus.FAILED;
                    }
                    password = new String(securePasswordManager.decryptPassword(securePassword.getEncodedPassword()));
                } catch(FindException fe) {
                    logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {ExceptionUtils.getMessage(fe)}, ExceptionUtils.getDebugException(fe));
                    return AssertionStatus.FAILED;
                } catch(ParseException pe) {
                    logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {ExceptionUtils.getMessage(pe)}, ExceptionUtils.getDebugException(pe));
                    return AssertionStatus.FAILED;
                }
            }
        } else {
            final LoginCredentials credentials = context.getDefaultAuthenticationContext().getLastCredentials();
            if ( credentials != null && credentials.getFormat() == CredentialFormat.CLEARTEXT ) {
                username = credentials.getName();
                password = new String(credentials.getCredentials());
            }
            if (username == null) {
                logAndAudit(AssertionMessages.SSH_ROUTING_PASSTHRU_NO_USERNAME);
                return AssertionStatus.FAILED;
            }
        }

        final String host = ExpandVariables.process(assertion.getHost(), variables, getAudit());
        int port;
        try{
            port = Integer.parseInt(ExpandVariables.process(assertion.getPort(), variables, getAudit()));
        } catch (Exception e){
            //use default port
            port = 22;
            logger.log(Level.INFO, "Unable to parse given port number, using default port 22.", ExceptionUtils.getDebugException(e));
        }

        SshClient sshClient = null;
        try {
            SshParameters sshParams = new SshParameters(host, port, username, password);
            sshParams.setConnectionTimeout( (long) assertion.getConnectTimeout() );
            sshParams.setReadingTimeout( (long) assertion.getReadTimeout() );

            if (assertion.isUsePublicKey() && assertion.getSshPublicKey() != null){
                final String publicKeyFingerprint = ExpandVariables.process(assertion.getSshPublicKey(), variables, getAudit());

                // validate public key fingerprint
                final Option<String> fingerprintIsValid = SshKeyUtil.validateSshPublicKeyFingerprint(publicKeyFingerprint);
                if( fingerprintIsValid.isSome() ){
                    logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, SshAssertionMessages.SSH_INVALID_PUBLIC_KEY_FINGERPRINT_EXCEPTION);
                    return AssertionStatus.FAILED;
                }
                SshHostKeys sshHostKeys = new SshHostKeys();
                sshHostKeys.addKey(InetAddress.getByName(host), publicKeyFingerprint );
                sshParams.setHostKeyVerifier(new HostKeyFingerprintVerifier(sshHostKeys));
            }

            if(assertion.isUsePrivateKey() && assertion.getPrivateKeyOid() != null) {
                String privateKeyText;
                try {
                    SecurePassword securePemPrivateKey = securePasswordManager.findByPrimaryKey(assertion.getPrivateKeyOid());
                    if (securePemPrivateKey == null) {
                        logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, "Unable to find stored PEM private key for OID: " + assertion.getPrivateKeyOid());
                        return AssertionStatus.FAILED;
                    }
                    privateKeyText = new String(securePasswordManager.decryptPassword(securePemPrivateKey.getEncodedPassword()));
                } catch(FindException fe) {
                    logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {ExceptionUtils.getMessage(fe)}, ExceptionUtils.getDebugException(fe));
                    return AssertionStatus.FAILED;
                } catch(ParseException pe) {
                    logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {ExceptionUtils.getMessage(pe)}, ExceptionUtils.getDebugException(pe));
                    return AssertionStatus.FAILED;
                }

                sshParams.setSshPassword(null);
                sshParams.setPrivateKey(privateKeyText);
            }

            if (assertion.isScpProtocol()) {
                sshClient = new ScpClient(new Scp(sshParams));
            } else {
                sshClient = new SftpClient(new Sftp(sshParams));
            }

            sshClient.setTimeout( (long) assertion.getConnectTimeout() );   // connect timeout value from assertion UI
            sshClient.connect();

            if(!sshClient.isConnected()) {
                sshClient.disconnect();
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, "Failed to authenticate with the remote server.");
                return AssertionStatus.FAILED;
            }

            // download / upload the file
            final String filename = ExpandVariables.process( assertion.getFileName(), variables, getAudit() );
            final String directory = ExpandVariables.process( assertion.getDirectory(), variables, getAudit() );
            try {
                if (assertion.isDownloadCopyMethod()) {
                    final PipedInputStream pis = new PipedInputStream();
                    final PipedOutputStream pos = new PipedOutputStream(pis);

                    // download file on a new thread
                    final Future<Void> future = sshDownloadOnNewThread(sshClient, directory, filename, pos);

                    // TODO [steve] SSH routing should support download to a specified message
                    final Message response = context.getResponse();
                    // TODO [steve] SSH routing must enforce a response size limit
                    response.initialize(stashManagerFactory.createStashManager(), ContentTypeHeader.create(assertion.getDownloadContentType()), pis);

                    // force all message parts to be initialized, it is by default lazy
                    logger.log(Level.FINER, "Reading SFTP/SCP response.");
                    response.getMimeKnob().getContentLength();
                    logger.log(Level.FINER, "Read SFTP/SCP response.");

                    future.get();
                } else {
                    sshClient.upload( mimeKnob.getEntireMessageBodyAsInputStream(), directory, filename );
                }
            } catch (NoSuchPartException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[] {SshAssertionMessages.SSH_NO_SUCH_PART_ERROR + ", server: " + host}, ExceptionUtils.getDebugException(e));
                return AssertionStatus.FAILED;
            } catch (ExecutionException e) {
                if (ExceptionUtils.getMessage(e).contains("jscape")){
                    return handleJscapeException(e, username, host, directory);
                }
                throw e;
            } catch (ScpException e) {
                return handleJscapeException(e, username, host, directory);
            } catch (SftpException e) {
                return handleJscapeException(e, username, host, directory);
            } catch (IOException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[]{SshAssertionMessages.SSH_IO_EXCEPTION + ", server: " + host}, ExceptionUtils.getDebugException(e));
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
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {SshAssertionMessages.SSH_CERT_ISSUE_EXCEPTION + ": " + ExceptionUtils.getMessage(ioe)}, ExceptionUtils.getDebugException(ioe));
            } else if ( ioe instanceof SocketException ){
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[] {SshAssertionMessages.SSH_SOCKET_EXCEPTION + ", server: " + host + ", port:" + port + ", username: " + username + ". Failing Assertion with socket exception"},
                        ExceptionUtils.getDebugException(ioe));
            } else {
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[] {SshAssertionMessages.SSH_CONNECTION_EXCEPTION + ", server: " + host + ", port:" + port + ", username: " + username + ". Failing Assertion with exception"},
                        ExceptionUtils.getDebugException(ioe));
            }
            return AssertionStatus.FAILED;
        } catch(Exception e) {
            if (ExceptionUtils.getMessage(e).contains("com.jscape.inet.ssh.util.keyreader.FormatException")) {
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {SshAssertionMessages.SSH_CERT_ISSUE_EXCEPTION}, ExceptionUtils.getDebugException(e));
            } else {
                logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"SSH2 Route Assertion error: " + ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            }
            return AssertionStatus.FAILED;
        } finally {
            if(sshClient != null) {
                sshClient.disconnect();
            }
        }
    }

    AssertionStatus handleJscapeException(final Exception e, final String username, final String host, final String directory) {
        if (ExceptionUtils.getMessage(e).contains("No such file")){
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[] { SshAssertionMessages.SSH_DIR_DOESNT_EXIST_ERROR + ", directory: " + directory  + ", username: " + username},
                    ExceptionUtils.getDebugException(e));
        } else{
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[] { SshAssertionMessages.SSH_EXCEPTION_ERROR + ", server: " + host}, ExceptionUtils.getDebugException(e));
        }
        return AssertionStatus.FAILED;
    }

    /*
     * Download the given file on a new thread.
     */
    private Future<Void> sshDownloadOnNewThread( final SshClient sshClient,
                                                 final String directory,
                                                 final String fileName,
                                                 final PipedOutputStream pos ) throws IOException {
        final CountDownLatch startedSignal = new CountDownLatch(1);

        //TODO [steve] SSH routing file transfer should use a thread pool
        final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "SshDownloadThread-" + System.currentTimeMillis());
                thread.setDaemon(true);
                return thread;
            }
        });

        logger.log(Level.FINE, "Start new thread for downloading");

        final Future<Void> future = executorService.submit(new Callable<Void>(){
            @Override
            public Void call() throws IOException {
                try {
                    startedSignal.countDown();
                    sshClient.download(pos, directory, fileName);
                } finally {
                    logger.log(Level.FINE, "... downloading thread stopped.");
                    ResourceUtils.closeQuietly(pos);
                    ResourceUtils.closeQuietly(pos);
                    startedSignal.countDown();
                }
                return null;
            }
        });

        try {
            startedSignal.await();
        }
        catch(InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CausedIOException("Interrupted waiting for download.", ie);
        }

        return future;
    }
}

