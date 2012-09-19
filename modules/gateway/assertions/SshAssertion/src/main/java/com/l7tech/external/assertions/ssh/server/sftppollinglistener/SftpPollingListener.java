package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpException;
import com.jscape.inet.sftp.SftpFile;
import com.jscape.inet.ssh.util.HostKeyFingerprintVerifier;
import com.jscape.inet.ssh.util.SshHostKeys;
import com.jscape.inet.ssh.util.SshParameters;
import com.l7tech.external.assertions.ssh.keyprovider.SshKeyUtil;
import com.l7tech.external.assertions.ssh.server.sftppollinglistener.SftpClient.SftpConnectionListener;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.event.system.TransportEvent;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.*;
import com.l7tech.util.Functions.UnaryThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.ssh.server.SshAssertionMessages.SSH_INVALID_PUBLIC_KEY_FINGERPRINT_EXCEPTION;
import static com.l7tech.external.assertions.ssh.server.client.SshClientConfiguration.defaultCipherOrder;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.util.CollectionUtils.toSet;
import static com.l7tech.util.Functions.*;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.trim;
import static java.text.MessageFormat.format;
import static java.util.Collections.list;

/**
 * Abstract base class for SftpPollingListener implementations.  This class provides the common thread life-cycle operations,
 * plus code that listens to a SFTP directory.
 * <br/>
 * All request messages are delegated to the handleFile() method.
 */
abstract class SftpPollingListener {
    private static final Logger logger = Logger.getLogger(SftpPollingListener.class.getName());

    static final String RESPONSE_FILE_EXTENSION = ".response";
    static final String PROCESSED_FILE_EXTENSION = ".processed";
    static final String PROCESSING_FILE_EXTENSION = ".processing";

    static final int MAXIMUM_OOPSES = 5;
    static final long SHUTDOWN_TIMEOUT = 7L * 1000L;
    static final int OOPS_RETRY = 5000; // Five seconds
    static final int DEFAULT_OOPS_SLEEP = 60 * 1000; // One minute
    static final int MIN_OOPS_SLEEP = 10 * 1000; // 10 seconds
    static final int MAX_OOPS_SLEEP = TimeUnit.DAYS.getMultiplier(); // 24 hours
    static final int OOPS_AUDIT = 15 * 60 * 1000; // 15 mins;

    /** The properties for the SFTP resource that the listener is processing files on */
    protected final SsgActiveConnector ssgActiveConnector;
    protected final ApplicationEventPublisher eventPublisher;
    private final SecurePasswordManager securePasswordManager;
    private List<String> ignoredFileExtensionList = new ArrayList<String>(0);

    /** The listener thread that performs the polling loop, it's responsible for looking for messages on the SFTP server */
    protected final SftpPollingListenerPollThread listenerThread;
    protected final SftpClient sftpClient;

    // Runtime stuff
    private boolean threadStopped;
    private final Object sync = new Object();
    private long lastStopRequestedTime;
    private long lastAuditErrorTime;

    SftpPollingListener( @NotNull final SsgActiveConnector ssgActiveConnector,
                         @NotNull final ApplicationEventPublisher eventPublisher,
                         @NotNull final SecurePasswordManager securePasswordManager ) throws SftpPollingListenerConfigException {
        this.ssgActiveConnector = ssgActiveConnector;
        this.eventPublisher = eventPublisher;
        this.securePasswordManager = securePasswordManager;
        this.sftpClient = buildSftpClient();
        this.listenerThread = new SftpPollingListenerPollThread(this, toString());
    }

    public String getDisplayName() {
        final StringBuilder stringBuilder = new StringBuilder(128);
        stringBuilder.append( ssgActiveConnector.getName() );
        stringBuilder.append( " (#" );
        stringBuilder.append( ssgActiveConnector.getOid() );
        stringBuilder.append( ",v" );
        stringBuilder.append( ssgActiveConnector.getVersion() );
        stringBuilder.append( ")" );
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return "SftpPollingListener; " + getDisplayName();
    }

    /**
     * Perform the processing on a file.  This is the point where an implementation
     * of the SftpPollingListener would override.
     *
     * @param file the file to process
     * @throws SftpPollingListenerException error encountered while processing the file
     */
    abstract void handleFile(SftpFile file) throws SftpPollingListenerException;

    /**
     * Starts the listener thread.
     * @throws com.l7tech.server.LifecycleException caused by any problems during start
     */
    void start() throws LifecycleException {
        synchronized(sync) {
            log(Level.FINE, SftpPollingListenerMessages.INFO_LISTENER_START, toString());
            listenerThread.start();
            log(Level.FINE, SftpPollingListenerMessages.INFO_LISTENER_STARTED, toString());
        }
    }

    /**
     * Tells the listener thread to stop.
     */
    void stop() {
        synchronized(sync) {
            log(Level.FINE, SftpPollingListenerMessages.INFO_LISTENER_STOP, toString());
            threadStopped = true;
            lastStopRequestedTime = System.currentTimeMillis();
        }
    }

    /**
     * Give the listener thread a set amount of time to shutdown, before it gets interrupted.
     */
    void ensureStopped() {
        long stopRequestedTime;
        synchronized(sync) {
            stop();
            stopRequestedTime = lastStopRequestedTime;
        }

        try {
            long waitTime = SHUTDOWN_TIMEOUT - (System.currentTimeMillis() - stopRequestedTime);
            if ( waitTime > 10L ) {
                listenerThread.join( SHUTDOWN_TIMEOUT );
            }
        } catch ( InterruptedException ie ) {
            Thread.currentThread().interrupt();
        }

        if ( listenerThread.isAlive() ) {
            log(Level.WARNING, SftpPollingListenerMessages.WARN_LISTENER_THREAD_ALIVE, this);
        }
    }

    /**
     * Returns flag specifying whether the listener is stopped.
     *
     * @return boolean flag
     */
    boolean isStop() {
        synchronized(sync) {
            return threadStopped;
        }
    }

    /**
     * Perform cleanup of resources and reset the listener status.
     */
    void cleanup() {
        ResourceUtils.closeQuietly( sftpClient );
    }

    SsgActiveConnector getSsgActiveConnector() {
        return this.ssgActiveConnector;
    }

    /**
     * Returns an SFTP client with connection to the destination directory for polling.
     *
     * @return SFTP client
     * @throws SftpPollingListenerConfigException if there's a misconfiguration causing problems connecting to the server
     */
    @NotNull
    private SftpClient buildSftpClient() throws SftpPollingListenerConfigException {
        final String host = getConnectorProperty( PROPERTIES_KEY_SFTP_HOST );
        final int port = getConnectorIntegerProperty( PROPERTIES_KEY_SFTP_PORT, 0 );
        final String username = getConnectorProperty( PROPERTIES_KEY_SFTP_USERNAME );
        final long pollingInterval = getConnectorLongProperty( PROPERTIES_KEY_POLLING_INTERVAL, 60L );
        final long passwordOid = getConnectorLongProperty( PROPERTIES_KEY_SFTP_SECURE_PASSWORD_OID, -1L );
        final String password = passwordOid == -1L ? null : getDecryptedPassword( passwordOid );
        final String directory = getConnectorProperty( PROPERTIES_KEY_SFTP_DIRECTORY );
        final long timeout = TimeUnit.SECONDS.toMillis( pollingInterval + 3L );

        if ( host == null ) throw new SftpPollingListenerConfigException( "Host name is not set" );
        if ( port == 0 ) throw new SftpPollingListenerConfigException( "Port is not set" );
        if ( directory == null ) throw new SftpPollingListenerConfigException( "Directory is not set" );

        final SshParameters sshParams = new SshParameters(host, port, username, password);
        sshParams.setConnectionTimeout( timeout );
        sshParams.setReadingTimeout( timeout );

        if ( getConnectorProperty( PROPERTIES_KEY_SFTP_SERVER_FINGER_PRINT ) != null){
            final String publicKeyFingerprint = getConnectorProperty( PROPERTIES_KEY_SFTP_SERVER_FINGER_PRINT ).trim();

            // validate public key fingerprint
            final Option<String> fingerprintValidationError = SshKeyUtil.validateSshPublicKeyFingerprint(publicKeyFingerprint);
            if( fingerprintValidationError.isSome() ){
                logger.log(Level.WARNING, SSH_INVALID_PUBLIC_KEY_FINGERPRINT_EXCEPTION);
                throw new SftpPollingListenerConfigException(SSH_INVALID_PUBLIC_KEY_FINGERPRINT_EXCEPTION);
            }
            SshHostKeys sshHostKeys = new SshHostKeys();
            try {
                sshHostKeys.addKey(InetAddress.getByName( host ), publicKeyFingerprint );
            } catch ( UnknownHostException e ) {
                // we're not passing an ip address so this should never occur
                throw new SftpPollingListenerConfigException("Host key error", e);
            }
            sshParams.setHostKeyVerifier(new HostKeyFingerprintVerifier(sshHostKeys));
        }

        final long privateKeyOid = getConnectorLongProperty( PROPERTIES_KEY_SFTP_SECURE_PASSWORD_KEY_OID, -1L );
        final String privateKeyText = privateKeyOid == -1L ? null : getDecryptedPassword( privateKeyOid );
        if( privateKeyText != null ) {
            sshParams.setSshPassword(null);
            if( password == null ) {
                sshParams.setPrivateKey(privateKeyText);
            }
        }

        final Set<String> ciphers = toSet( grep( map( CollectionUtils.list(ssgActiveConnector.getProperty(
                "l7.ssh.enabledCipherList", defaultCipherOrder).split("\\s*,\\s*")), trim() ), isNotEmpty() ) );
        return new SftpClient(sshParams, directory, ciphers, new SftpConnectionListener(){
            @Override
            public void notifyConnected() {
                fireConnected();
            }

            @Override
            public void notifyConnectionError( final String message ) {
                fireConnectError( message );
            }
        });
    }

    String getConnectorProperty( final String name ) {
        return ssgActiveConnector.getProperty( name );
    }

    long getConnectorLongProperty( final String name, final long defaultValue ) {
        return ssgActiveConnector.getLongProperty( name, defaultValue );
    }

    int getConnectorIntegerProperty( final String name, final int defaultValue ) {
        return ssgActiveConnector.getIntegerProperty( name, defaultValue );
    }

    /**
     * Looks in the given directory for files to process.
     *
     * @param sftp SFTP client with directory set for scanning
     * @return collection of files to process
     * @throws java.io.IOException caused by an error while reading the directory
     */
    @SuppressWarnings({ "unchecked" })
    Collection<SftpFile> scanDirectoryForFiles( final Sftp sftp ) throws IOException
    {
        final Collection<SftpFile> fileNames = new ArrayList<SftpFile>();

        // build a set of already processed files
        final Collection<String> processedFiles = new HashSet<String>();
        for( final SftpFile file : list((Enumeration<SftpFile>) sftp.getDirListing()) ) {
            if( !file.isDirectory() && file.exists() ) {
                final String fileName = file.getFilename();
                if(fileName.endsWith(PROCESSED_FILE_EXTENSION)) {
                    processedFiles.add(fileName.substring(0, fileName.length() - PROCESSED_FILE_EXTENSION.length()));
                }
            }
        }

        // look for any unprocessed files
        for ( final SftpFile file : list((Enumeration<SftpFile>) sftp.getDirListing()) ) {
            final String fileName = file.getFilename();
            if(isFileForProcessing(file, processedFiles)) {
                try {
                    sftp.renameFile(fileName, fileName + PROCESSING_FILE_EXTENSION);
                    fileNames.add(file);
                } catch(SftpException sftpe) {
                    // exception means that the file no longer exists
                }
            }
        }

        return fileNames;
    }

    private boolean isFileForProcessing(@NotNull SftpFile file, @NotNull final Collection<String> processedFiles) throws SftpException {
        final String fileName = file.getFilename();
        return !file.isDirectory() && file.exists()
                && !fileName.endsWith(PROCESSING_FILE_EXTENSION) && !fileName.endsWith(PROCESSED_FILE_EXTENSION) && !fileName.endsWith(RESPONSE_FILE_EXTENSION)
                && !processedFiles.contains(fileName)
                && !exists(ignoredFileExtensionList,
                new Unary<Boolean, String>() {
                    @Override
                    public Boolean call(String fileNameInList) {
                        return !"".equals(fileNameInList) && fileName.endsWith(fileNameInList);
                    }
                });
    }

    /**
     * Execute work in a callback function that requires a SFTP client.
     * @param callback the work logic requiring a SFTP client
     * @return the any result(s) from the work done
     * @throws java.io.IOException if there's an error
     */
    <R> R doWithSftpClient( final UnaryThrows<R,Sftp,IOException> callback ) throws IOException {
        return sftpClient.doWork( callback );
    }

    /**
     * Event class for use by SFTP Polling Listener.
     */
    public class SftpPollingEvent extends TransportEvent {
        private static final String NAME = "Connect";

        public SftpPollingEvent(
                Object source,
                Level level,
                String ip,
                String message) {
            super(source, Component.GW_SFTP_POLL_RECV, ip, level, NAME, message);
        }
    }

    private void fireConnected() {
        lastAuditErrorTime = 0L;
        fireEvent(new SftpPollingEvent(this, Level.INFO, null,
                format(SftpPollingListenerMessages.INFO_EVENT_CONNECT_SUCCESS, getDisplayName())));
    }

    private void fireConnectError(String message) {
        fireEvent(new SftpPollingEvent(this, Level.WARNING,  null, format(
                SftpPollingListenerMessages.INFO_EVENT_CONNECT_FAIL,
                getDisplayName(), message )));
    }

    private void fireEvent(TransportEvent event) {
        if ( eventPublisher != null) {
            long timeNow = System.currentTimeMillis();
            if ((lastAuditErrorTime+ (long) OOPS_AUDIT) < timeNow) {
                lastAuditErrorTime = timeNow;
                eventPublisher.publishEvent( event );
            } else {
                log(Level.INFO, SftpPollingListenerMessages.INFO_EVENT_NOT_PUBLISHED);
            }
        } else {
            log(Level.WARNING, SftpPollingListenerMessages.INFO_EVENT_NOT_PUBLISHABLE, event.getMessage());
        }
    }

    void log(Level level, String messageKey, Object... parm) {
        logger.log(level, messageKey, parm);
    }

    void log(Level level, String messageKey, Throwable ex) {
        logger.log(level, messageKey, ex);
    }

    private SecurePassword getSecurePassword( final long passwordOid ) {
        SecurePassword securePassword = null;
        try {
            securePassword = securePasswordManager.findByPrimaryKey(passwordOid);
        } catch (FindException fe) {
            logger.log( Level.WARNING, "The password could not be found in the password manager storage.  The password should be fixed or set in the password manager."
                    + ExceptionUtils.getMessage( fe ), ExceptionUtils.getDebugException( fe ) );
        }
        return securePassword;
    }

    private String getDecryptedPassword( final long passwordOid ) {
        String decrypted = null;
        try {
            final SecurePassword securePassword = getSecurePassword( passwordOid );
            if ( securePassword != null ) {
                final String encrypted = securePassword.getEncodedPassword();
                final char[] pwd = securePasswordManager.decryptPassword(encrypted);
                decrypted = new String(pwd);
            }
        } catch (ParseException pe) {
            logger.log( Level.WARNING, "The password could not be parsed, the stored password is corrupted. "
                    + ExceptionUtils.getMessage( pe ), ExceptionUtils.getDebugException( pe ) );
        } catch (FindException fe) {
            logger.log( Level.WARNING, "The password could not be found in the password manager storage.  The password should be fixed or set in the password manager."
                    + ExceptionUtils.getMessage( fe ), ExceptionUtils.getDebugException( fe ) );
        } catch (NullPointerException npe) {
            logger.log( Level.WARNING, "The password could not be found in the password manager storage.  The password should be fixed or set in the password manager."
                    + ExceptionUtils.getMessage( npe ), ExceptionUtils.getDebugException( npe ) );
        }
        return decrypted;
    }

    protected void setErrorSleepTime(String stringValue) {
        long newErrorSleepTime = DEFAULT_OOPS_SLEEP;

        try {
            newErrorSleepTime = TimeUnit.parse(stringValue, TimeUnit.SECONDS);
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, "Ignoring invalid SFTP Polling error sleep time ''{0}'' (using default).", stringValue);
        }

        if ( newErrorSleepTime < MIN_OOPS_SLEEP ) {
            logger.log(Level.WARNING, "Ignoring invalid SFTP Polling error sleep time ''{0}'' (using minimum).", stringValue);
            newErrorSleepTime = MIN_OOPS_SLEEP;
        } else if ( newErrorSleepTime > MAX_OOPS_SLEEP ) {
            logger.log(Level.WARNING, "Ignoring invalid SFTP Polling error sleep time ''{0}'' (using maximum).", stringValue);
            newErrorSleepTime = MAX_OOPS_SLEEP;
        }

        logger.log(Level.CONFIG, "Updated SFTP Polling error sleep time to {0}ms.", newErrorSleepTime);
        listenerThread.setOopsSleep((int)newErrorSleepTime);
    }

    public void setIgnoredFileExtensionList(List<String> ignoredFileExtensionList) {
        this.ignoredFileExtensionList = ignoredFileExtensionList;
    }
}