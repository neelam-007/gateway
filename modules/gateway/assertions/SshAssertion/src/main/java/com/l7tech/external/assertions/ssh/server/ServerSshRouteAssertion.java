package com.l7tech.external.assertions.ssh.server;

import com.jscape.inet.scp.Scp;
import com.jscape.inet.scp.ScpException;
import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpConfiguration;
import com.jscape.inet.sftp.SftpException;
import com.jscape.inet.ssh.transport.TransportException;
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
import com.l7tech.external.assertions.ssh.server.client.SshClientConfiguration;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.SshKnob;
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
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.*;
import com.l7tech.util.ThreadPool.ThreadPoolShutDownException;
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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

import static com.l7tech.external.assertions.ssh.server.SshAssertionMessages.*;
import static com.l7tech.external.assertions.ssh.server.client.SshClientConfiguration.defaultCipherOrder;
import static com.l7tech.gateway.common.audit.AssertionMessages.SSH_ROUTING_ERROR;
import static com.l7tech.gateway.common.audit.AssertionMessages.SSH_ROUTING_PASSTHRU_NO_USERNAME;
import static com.l7tech.message.Message.getMaxBytes;
import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.CollectionUtils.toSet;
import static com.l7tech.util.ExceptionUtils.*;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.trim;

/**
 * Server side implementation of the SshRouteAssertion.
 *
 * @see com.l7tech.external.assertions.ssh.SshRouteAssertion
 */
public class ServerSshRouteAssertion extends ServerRoutingAssertion<SshRouteAssertion> {

    @Inject
    private Config config;
    @Inject
    private SecurePasswordManager securePasswordManager;
    @Inject
    private StashManagerFactory stashManagerFactory;
    @Inject @Named("sshResponseDownloadThreadPool")
    private ThreadPoolBean threadPool;
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
            logAndAudit(SSH_ROUTING_ERROR, "Request is not initialized; nothing to route");
            return AssertionStatus.BAD_REQUEST;
        }

        // if uploading from the Gateway, delete current security header if necessary
        final boolean isDownloadCopyMethod = assertion.isDownloadCopyMethod();
        if (!isDownloadCopyMethod) {
            try {
                handleProcessedSecurityHeader(request);
            } catch(SAXException se) {
                logger.log(Level.INFO, "Error processing security header, request XML invalid ''{0}''", se.getMessage());
            }
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
                        logAndAudit(SSH_ROUTING_ERROR, "Unable to find stored password for OID: " + assertion.getPasswordOid());
                        return AssertionStatus.FAILED;
                    }
                    password = new String(securePasswordManager.decryptPassword(securePassword.getEncodedPassword()));
                } catch(FindException fe) {
                    logAndAudit(SSH_ROUTING_ERROR, new String[] { getMessage( fe )}, getDebugException( fe ));
                    return AssertionStatus.FAILED;
                } catch(ParseException pe) {
                    logAndAudit(SSH_ROUTING_ERROR, new String[] { getMessage( pe )}, getDebugException( pe ));
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
                logAndAudit(SSH_ROUTING_PASSTHRU_NO_USERNAME);
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
            logger.log(Level.INFO, "Unable to parse given port number, using default port 22.", getDebugException( e ));
        }

        SshClient sshClient = null;
        try {
            SshParameters sshParams = new SshParameters(host, port, username, password);
            sshParams.setConnectionTimeout( TimeUnit.SECONDS.toMillis((long) assertion.getConnectTimeout()) );
            sshParams.setReadingTimeout( TimeUnit.SECONDS.toMillis((long) assertion.getReadTimeout()) );

            if (assertion.isUsePublicKey() && assertion.getSshPublicKey() != null){
                final String publicKeyFingerprint = ExpandVariables.process(assertion.getSshPublicKey(), variables, getAudit());

                // validate public key fingerprint
                final Option<String> fingerprintIsValid = SshKeyUtil.validateSshPublicKeyFingerprint(publicKeyFingerprint);
                if( fingerprintIsValid.isSome() ){
                    logAndAudit(SSH_ROUTING_ERROR, SSH_INVALID_PUBLIC_KEY_FINGERPRINT_EXCEPTION);
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
                        logAndAudit(SSH_ROUTING_ERROR, "Unable to find stored PEM private key for OID: " + assertion.getPrivateKeyOid());
                        return AssertionStatus.FAILED;
                    }
                    privateKeyText = new String(securePasswordManager.decryptPassword(securePemPrivateKey.getEncodedPassword()));
                //TODO [jdk7] Multicatch
                } catch(FindException fe) {
                    logAndAudit(SSH_ROUTING_ERROR, new String[] { getMessage( fe )}, getDebugException( fe ));
                    return AssertionStatus.FAILED;
                } catch(ParseException pe) {
                    logAndAudit(SSH_ROUTING_ERROR, new String[] { getMessage( pe )}, getDebugException( pe ));
                    return AssertionStatus.FAILED;
                }

                sshParams.setSshPassword(null);
                sshParams.setPrivateKey(privateKeyText);
            }

            final Set<String> ciphers = toSet( grep( map( list( config.getProperty( "sshRoutingEnabledCiphers", defaultCipherOrder ).split( "\\s*,\\s*" ) ), trim() ), isNotEmpty() ) );
            if (assertion.isScpProtocol()) {
                sshClient = new ScpClient(new Scp(sshParams, new SshClientConfiguration(sshParams, ciphers)));
            } else {
                sshClient = new SftpClient(new Sftp(sshParams, new SftpConfiguration(new SshClientConfiguration(sshParams, ciphers))));
            }
            sshClient.connect();

            if(!sshClient.isConnected()) {
                sshClient.disconnect();
                sshClient = null;
                logAndAudit(SSH_ROUTING_ERROR, "Failed to authenticate with the remote server.");
                return AssertionStatus.FAILED;
            }

            // download / upload the file
            final String filename = ExpandVariables.process( assertion.getFileName(), variables, getAudit() );
            final String directory = ExpandVariables.process( assertion.getDirectory(), variables, getAudit() );
            try {
                if (isDownloadCopyMethod) {
                    final PipedInputStream pis = new PipedInputStream();
                    final PipedOutputStream pos = new PipedOutputStream(pis);

                    // response byte limit
                    long byteLimit = getMaxBytes();
                    if (assertion.getResponseByteLimit() != null) {
                        String byteLimitStr = ExpandVariables.process(assertion.getResponseByteLimit(), variables, getAudit());
                        try {
                            byteLimit = Long.parseLong(byteLimitStr);
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, "Used default response byte limit: " + byteLimit + ".  " + getMessage( e ), getDebugException( e ));
                        }
                    }
                    logger.log(Level.FINE, "Response byte limit: " + byteLimit + ".");

                    // start download task
                    final Future<Void> future = startSshDownloadTask( sshClient, directory, filename, pos );
                    final Message response = context.getOrCreateTargetMessage( assertion.getResponseTarget(), false );
                    response.initialize(stashManagerFactory.createStashManager(), ContentTypeHeader.create(assertion.getDownloadContentType()), pis, byteLimit);

                    // force all message parts to be initialized, it is by default lazy
                    logger.log(Level.FINER, "Reading SFTP/SCP response.");
                    response.getMimeKnob().getContentLength();
                    logger.log(Level.FINER, "Read SFTP/SCP response.");

                    future.get();
                } else {
                    final SshKnob sshKnob = request.getKnob(SshKnob.class);
                    SshKnob.FileMetadata fileMetadata = null;
                    if(assertion.isPreserveFileMetadata() && sshKnob != null){
                        fileMetadata = sshKnob.getFileMetadata();
                    }
                    sshClient.upload( mimeKnob.getEntireMessageBodyAsInputStream(), directory, filename, fileMetadata );
                }
            } catch (NoSuchPartException e) {
                logAndAudit(SSH_ROUTING_ERROR,
                        new String[] {SSH_NO_SUCH_PART_ERROR + ", server: " + host}, getDebugException( e ));
                return AssertionStatus.FAILED;
            } catch (ExecutionException e) {
                if ( getMessage( e ).contains("jscape") || getMessage( e ).contains("No such file or directory")){
                    return handleJscapeException(e, isDownloadCopyMethod, username, host, directory, filename);
                }
                throw e;
            } catch (ScpException e) {
                return handleJscapeException(e, isDownloadCopyMethod, username, host, directory, filename);
            } catch (SftpException e) {
                return handleJscapeException(e, isDownloadCopyMethod, username, host, directory, filename);
            } catch (IOException e) {
                if ( getMessage( e ).contains("No such file or directory")){
                    return handleJscapeException(e, isDownloadCopyMethod, username, host, directory, filename);
                } else {
                    logAndAudit(SSH_ROUTING_ERROR,
                            new String[]{SSH_IO_EXCEPTION + " '" + getMessage( e ) + "', server: " + host}, getDebugException( e ));
                }
                return AssertionStatus.FAILED;
            } catch (Throwable t) {
                logAndAudit(SSH_ROUTING_ERROR,
                        new String[]{ getMessage( t )}, getDebugException( t ));
                return AssertionStatus.FAILED;
            }

            context.setRoutingStatus(RoutingStatus.ROUTED);
            return AssertionStatus.NONE;
        } catch(IOException ioe) {
            if ( causedBy( ioe, TransportException.class ) && causedBy( ioe, NoSuchElementException.class ) && "no common elements found".equals(ExceptionUtils.unnestToRoot( ioe ).getMessage())){
                logAndAudit(SSH_ROUTING_ERROR, new String[] { SSH_ALGORITHM_EXCEPTION }, getDebugException( ioe ));
            } else if ( getMessage( ioe ).startsWith("Malformed SSH") ){
                logAndAudit(SSH_ROUTING_ERROR, new String[] { SSH_CERT_ISSUE_EXCEPTION + ": " + getMessage( ioe )}, getDebugException( ioe ));
            } else {
                final String detail = ioe instanceof SocketException ? " socket" : "";
                logAndAudit(SSH_ROUTING_ERROR,
                        new String[] { SSH_CONNECTION_EXCEPTION + " '" + getMessage( ioe ) + "', server: " + host + ", port:" + port + ", username: " + username + ". Failing Assertion with"+detail+" exception"},
                        getDebugException( ioe ));
            }
            return AssertionStatus.FAILED;
        } catch(Exception e) {
            if ( getMessage( e ).contains("com.jscape.inet.ssh.util.keyreader.FormatException")) {
                logAndAudit(SSH_ROUTING_ERROR, new String[] { SSH_CERT_ISSUE_EXCEPTION}, getDebugException( e ));
            } else {
                logAndAudit(SSH_ROUTING_ERROR, new String[] {"SSH2 Route Assertion error: " + getMessage( e )}, getDebugException( e ));
            }
            return AssertionStatus.FAILED;
        } finally {
            if(sshClient != null) {
                sshClient.disconnect();
            }
        }
    }

    AssertionStatus handleJscapeException(final Exception e, final boolean isDownloadCopyMethod, final String username,
                                          final String host, final String directory, String filename) {
        if ( getMessage( e ).contains("No such file or directory") || getMessage( e ).contains("File or directory No such file not found")){
            if (isDownloadCopyMethod) {
                logAndAudit(SSH_ROUTING_ERROR,
                        new String[] {SSH_DIR_FILE_DOESNT_EXIST_ERROR + " copy method: download, directory: " + directory  + ", file: " + filename  +", username: " + username},
                        getDebugException( e ));
            } else {
                // else upload copy method
                logAndAudit(SSH_ROUTING_ERROR,
                        new String[] {SSH_DIR_DOESNT_EXIST_ERROR + " copy method: upload, directory: " + directory + ", username: " + username},
                        getDebugException( e ));
            }
        } else{
            logAndAudit(SSH_ROUTING_ERROR,
                    new String[] {SSH_EXCEPTION_ERROR + " '" + getMessage( e ) + "', server: " + host}, getDebugException( e ));
        }
        return AssertionStatus.FAILED;
    }

    /*
     * Download the given file on a new thread.
     */
    private Future<Void> startSshDownloadTask( final SshClient sshClient,
                                               final String directory,
                                               final String fileName,
                                               final PipedOutputStream pos ) throws IOException, ThreadPoolShutDownException {
        final CountDownLatch startedSignal = new CountDownLatch(1);

        final Future<Void> future = threadPool.submitTask( new Callable<Void>() {
            @Override
            public Void call() throws IOException {
                try {
                    startedSignal.countDown();
                    sshClient.download( pos, directory, fileName );
                } finally {
                    ResourceUtils.closeQuietly( pos );
                    startedSignal.countDown();
                }
                return null;
            }
        } );

        try {
            startedSignal.await();
        } catch( InterruptedException ie ) {
            Thread.currentThread().interrupt();
            throw new CausedIOException("Interrupted waiting for download.", ie);
        }

        return future;
    }
}

