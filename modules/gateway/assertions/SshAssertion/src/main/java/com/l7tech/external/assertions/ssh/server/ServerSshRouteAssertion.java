package com.l7tech.external.assertions.ssh.server;

import com.jscape.inet.scp.Scp;
import com.jscape.inet.scp.ScpException;
import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpConfiguration;
import com.jscape.inet.sftp.SftpException;
import com.jscape.inet.ssh.SshConfiguration;
import com.jscape.inet.ssh.transport.AlgorithmFactory;
import com.jscape.inet.ssh.types.SshNameList;
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
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.*;
import static com.l7tech.util.CollectionUtils.list;
import com.l7tech.util.Functions.Unary;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.map;
import com.l7tech.util.ThreadPool.ThreadPoolShutDownException;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.message.Message.getMaxBytes;

/**
 * Server side implementation of the SshRouteAssertion.
 *
 * @see com.l7tech.external.assertions.ssh.SshRouteAssertion
 */
public class ServerSshRouteAssertion extends ServerRoutingAssertion<SshRouteAssertion> {

    private static final boolean enableCipherNone = ConfigFactory.getBooleanProperty( "com.l7tech.external.assertions.ssh.server.enableCipherNone", false );
    private static final boolean enableMacNone = ConfigFactory.getBooleanProperty( "com.l7tech.external.assertions.ssh.server.enableMacNone", false );
    private static final boolean enableMacMd5 = ConfigFactory.getBooleanProperty( "com.l7tech.external.assertions.ssh.server.enableMacMd5", false );

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
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Request is not initialized; nothing to route");
            return AssertionStatus.BAD_REQUEST;
        }

        // if uploading from the Gateway, delete current security header if necessary
        if (!assertion.isDownloadCopyMethod()) {
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
            sshParams.setConnectionTimeout( TimeUnit.SECONDS.toMillis((long) assertion.getConnectTimeout()) );
            sshParams.setReadingTimeout( TimeUnit.SECONDS.toMillis((long) assertion.getReadTimeout()) );

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
                sshClient = new ScpClient(new Scp(sshParams,buildSshConfiguration()));
            } else {
                sshClient = new SftpClient(new Sftp(sshParams, new SftpConfiguration(buildSshConfiguration())));
            }
            sshClient.connect();

            if(!sshClient.isConnected()) {
                sshClient.disconnect();
                sshClient = null;
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

                    // response byte limit
                    long byteLimit = getMaxBytes();
                    if (assertion.getResponseByteLimit() != null) {
                        String byteLimitStr = ExpandVariables.process(assertion.getResponseByteLimit(), variables, getAudit());
                        try {
                            byteLimit = Long.parseLong(byteLimitStr);
                        } catch (NumberFormatException e) {
                            logger.log(Level.WARNING, "Used default response byte limit: " + byteLimit + ".  " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
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
                    sshClient.upload( mimeKnob.getEntireMessageBodyAsInputStream(), directory, filename );
                }
            } catch (NoSuchPartException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[] {SshAssertionMessages.SSH_NO_SUCH_PART_ERROR + ", server: " + host}, ExceptionUtils.getDebugException(e));
                return AssertionStatus.FAILED;
            } catch (ExecutionException e) {
                if (ExceptionUtils.getMessage(e).contains("jscape") || ExceptionUtils.getMessage(e).contains("No such file or directory")){
                    return handleJscapeException(e, username, host, directory, filename);
                }
                throw e;
            } catch (ScpException e) {
                return handleJscapeException(e, username, host, directory, filename);
            } catch (SftpException e) {
                return handleJscapeException(e, username, host, directory, filename);
            } catch (IOException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[]{SshAssertionMessages.SSH_IO_EXCEPTION + ", server: " + host}, ExceptionUtils.getDebugException(e));
                return AssertionStatus.FAILED;
            } catch (Throwable t) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[]{ExceptionUtils.getMessage(t)}, ExceptionUtils.getDebugException(t));
                return AssertionStatus.FAILED;
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

    private SshConfiguration buildSshConfiguration() {
        final AlgorithmFactory algorithmFactory = buildAlgorithmFactory();
        final SshConfiguration sshConfiguration = new SshConfiguration();
        sshConfiguration.getTransportConfiguration().setAlgorithmFactory( algorithmFactory );
        return sshConfiguration;
    }

    private AlgorithmFactory buildAlgorithmFactory() {
        final AlgorithmFactory algorithmFactory = new AlgorithmFactory(){
            @Override
            public SshNameList getAllCiphers() {
                // overridden to return list in priority order
                final List<String> cipherList = map( grep( list( SshCipher.values() ), SshCipher.available() ), SshCipher.sshName() );
                return new SshNameList( cipherList.toArray( new String[cipherList.size()] ) );
            }
        };

        // Remove "none" algorithms by default
        if (!enableMacNone) algorithmFactory.removeMac( "none" );
        if (!enableCipherNone) algorithmFactory.removeCipher( "none" );

        // Remove MD5 hash by default, always prefer SHA-1
        if ( !enableMacMd5 ) {
            algorithmFactory.removeMac( "hmac-md5" );
        }
        algorithmFactory.setPrefferedMac( "hmac-sha1" );

        // Register all available supported ciphers
        for ( final SshCipher cipher : SshCipher.values() ) {
            algorithmFactory.addCipher( cipher.getSshName(), cipher.getJavaCipherName(), cipher.getBlockSize() );
        }

        return algorithmFactory;
    }

    private enum SshCipher {
//        AES128CTR("aes128-ctr", "AES", "AES/CTR/NoPadding", 16),
//        AES128CBC("aes128-cbc", "AES", "AES/CBC/NoPadding", 16),
        TripleDESCBC("3des-cbc", "DESede", "DESede/CBC/NoPadding", 24),
        BlowfishCBC("blowfish-cbc", "Blowfish", "Blowfish/CBC/NoPadding", 16);//,
//        AES192CTR("aes192-ctr", "AES", "AES/CTR/NoPadding", 24),
//        AES192CBC("aes192-cbc", "AES", "AES/CBC/NoPadding", 24),
//        AES256CTR("aes256-ctr", "AES", "AES/CTR/NoPadding", 32),
//        AES256CBC("aes256-cbc", "AES", "AES/CBC/NoPadding", 32);

        public boolean isAvailable() {
            return available;
        }

        public int getBlockSize() {
            return blockSize;
        }

        public String getJavaCipherName() {
            return javaCipherName;
        }

        public String getSshName() {
            return sshName;
        }

        public static Unary<String,SshCipher> sshName() {
            return new Unary<String,SshCipher>() {
                @Override
                public String call( final SshCipher sshCipher ) {
                    return sshCipher.getSshName();
                }
            };
        }

        public static Unary<Boolean,SshCipher> available() {
            return new Unary<Boolean,SshCipher>() {
                @Override
                public Boolean call( final SshCipher sshCipher ) {
                    return sshCipher.isAvailable();
                }
            };
        }

        private final Logger logger = Logger.getLogger(SshCipher.class.getName()); // static logger not initialized early enough
        private final String sshName;
        private final String javaAlgorithmName;
        private final String javaCipherName;
        private final int blockSize;
        private final boolean available;

        private SshCipher( final String sshName,
                           final String javaAlgorithmName,
                           final String javaCipherName,
                           final int blockSize ) {
            this.sshName = sshName;
            this.javaAlgorithmName = javaAlgorithmName;
            this.javaCipherName = javaCipherName;
            this.blockSize = blockSize;
            this.available = checkCipherAvailable();
        }

        private boolean checkCipherAvailable() {
            boolean available = false;
            try {
                final Cipher cipher = Cipher.getInstance( javaCipherName );
                final byte[] key = new byte[blockSize];
                cipher.init( Cipher.ENCRYPT_MODE, new SecretKeySpec( key, javaAlgorithmName ));
                available = true;
            } catch (Exception e) {
                logger.log( Level.FINE, "SSH cipher not available: " + sshName, ExceptionUtils.getDebugException( e ) );
            }
            return available;
        }
    }

    AssertionStatus handleJscapeException(final Exception e, final String username, final String host, final String directory, String filename) {
        if (ExceptionUtils.getMessage(e).contains("No such file or directory") || ExceptionUtils.getMessage(e).contains("FileNotFoundException")){
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[] { SshAssertionMessages.SSH_DIR_FILE_DOESNT_EXIST_ERROR + ", directory: " + directory  + ", file: " + filename  +", username: " + username},
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

