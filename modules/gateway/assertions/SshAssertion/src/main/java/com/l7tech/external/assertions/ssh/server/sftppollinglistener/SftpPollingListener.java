package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpException;
import com.jscape.inet.sftp.SftpFile;
import com.jscape.inet.ssh.util.HostKeyFingerprintVerifier;
import com.jscape.inet.ssh.util.SshHostKeys;
import com.jscape.inet.ssh.util.SshParameters;
import com.l7tech.external.assertions.ssh.keyprovider.SshKeyUtil;
import com.l7tech.external.assertions.ssh.server.SshAssertionMessages;
import com.l7tech.gateway.common.Component;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.event.system.TransportEvent;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for SftpPollingListener implementations.  This class provides the common thread life-cycle operations,
 * plus code that listens to a SFTP directory.
 * <br/>
 * All request messages are delegated to the handleFile() method.
 * Currently SftpPollingListenerThreadPoolFileHandler is the only file handler implementation.
 */
public abstract class SftpPollingListener implements PropertyChangeListener {
    public static final String RESPONSE_FILE_EXTENSION = ".response";
    public static final String PROCESSED_FILE_EXTENSION = ".processed";
    public static final String PROCESSING_FILE_EXTENSION = ".processing";

    private static final Logger _logger = Logger.getLogger(SftpPollingListener.class.getName());

    protected static final int MAXIMUM_OOPSES = 5;
    protected static final long SHUTDOWN_TIMEOUT = 7 * 1000;
    protected static final int OOPS_RETRY = 5000; // Five seconds
    protected static final int DEFAULT_OOPS_SLEEP = 60 * 1000; // One minute
    protected static final int OOPS_AUDIT = 15 * 60 * 1000; // 15 mins;

    /** The properties for the SFTP resource that the listener is processing files on */
    protected final SftpPollingListenerConfig _sftpPollingListenerCfg;

    /** The listener thread that performs the polling loop, it's responsible for looking for messages on the SFTP server */
    protected final SftpPollingListenerPollThread _listenerThread;

    /** Flag specifying whether the listener has started */
    private boolean _connected;

    private ThreadSafeSftpClient sftpClient;

    // Runtime stuff
    private boolean _threadStopped;
    private final Object sync = new Object();
    private long lastStopRequestedTime;
    private long lastAuditErrorTime;

    public SftpPollingListener(final SftpPollingListenerConfig sftpPollingListenerCfg) {
        this._sftpPollingListenerCfg = sftpPollingListenerCfg;
        _listenerThread = new SftpPollingListenerPollThread(this, toString());
        _listenerThread.setDaemon(true);
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer("SftpPollingListener-");
        s.append(_sftpPollingListenerCfg.getDisplayName());
        return s.toString();
    }

    /**
     * Perform the processing on a file.  This is the point where an implementation
     * of the SftpPollingListener would override.
     *
     * @param filename the file to process
     * @throws SftpPollingListenerRuntimeException error encountered while processing the file
     */
    protected abstract void handleFile(String filename) throws SftpPollingListenerRuntimeException;

    /**
     * Starts the listener thread.
     */
    public void start() throws LifecycleException {
        synchronized(sync) {
            log(Level.FINE, SftpPollingListenerMessages.INFO_LISTENER_START, toString());
            _listenerThread.start();
            log(Level.FINE, SftpPollingListenerMessages.INFO_LISTENER_STARTED, toString());
        }
    }

    /**
     * Tells the listener thread to stop.
     */
    public void stop() {
        synchronized(sync) {
            log(Level.FINE, SftpPollingListenerMessages.INFO_LISTENER_STOP, toString());
            _threadStopped = true;
            lastStopRequestedTime = System.currentTimeMillis();
        }
    }

    /**
     * Give the listener thread a set amount of time to shutdown, before it gets interrupted.
     */
    public void ensureStopped() {
        long stopRequestedTime;

        synchronized(sync) {
            stop();
            stopRequestedTime = lastStopRequestedTime;
        }

        try {
            long waitTime = SHUTDOWN_TIMEOUT - (System.currentTimeMillis() - stopRequestedTime);
            if ( waitTime > 10 ) {
                _listenerThread.join( SHUTDOWN_TIMEOUT );
            }
        } catch ( InterruptedException ie ) {
            Thread.currentThread().interrupt();
        }

        if ( _listenerThread.isAlive() ) {
            log(Level.WARNING, SftpPollingListenerMessages.WARN_LISTENER_THREAD_ALIVE, this);
        }
    }

    /**
     * Returns flag specifying whether the listener is stopped.
     *
     * @return boolean flag
     */
    protected boolean isStop() {
        synchronized(sync) {
            return _threadStopped;
        }
    }

    /**
     * Perform cleanup of resources and reset the listener status.
     */
    protected void cleanup() {
        if (sftpClient != null) {
            sftpClient.close();
            sftpClient = null;
        }
        _connected = false;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // currently not implemented
    }

    public SftpPollingListenerConfig getSftpPollingListenerConfig() {
        return this._sftpPollingListenerCfg;
    }

    public SftpPollingListenerResource getResourceConfig() {
        return this._sftpPollingListenerCfg.getSftpPollingListenerResource();
    }

    public long getSftpPollingListenerResourceId() {
        return getResourceConfig().getResId();
    }

    /**
     * Returns an SFTP client with connection to the destination directory for polling.
     *
     * @return SFTP client
     * @throws java.io.IOException
     * @throws SftpPollingListenerConfigException
     */
    protected ThreadSafeSftpClient getSftpClient() throws SftpPollingListenerConfigException, IOException {
        if (this.sftpClient == null) {
            synchronized (sync) {
                if (this.sftpClient == null) {
                    try {
                        final SftpPollingListenerResource settings = getResourceConfig();

                        String host = settings.getHostname();
                        String password = settings.getPassword();
                        SshParameters sshParams = new SshParameters(host, settings.getPort(), settings.getUsername(), password);

                        if (settings.getHostKey() != null){
                            String publicKeyFingerprint = settings.getHostKey().trim();

                            // validate public key fingerprint
                            Pair<Boolean, String> fingerprintIsValid = SshKeyUtil.validateSshPublicKeyFingerprint(publicKeyFingerprint);
                            if(!fingerprintIsValid.left){
                                _logger.log(Level.WARNING, SshAssertionMessages.SSH_INVALID_PUBLIC_KEY_FINGERPRINT_EXCEPTION);
                                throw new SftpPollingListenerConfigException(SshAssertionMessages.SSH_INVALID_PUBLIC_KEY_FINGERPRINT_EXCEPTION);
                            }
                            String hostPublicKey = publicKeyFingerprint;
                            SshHostKeys sshHostKeys = new SshHostKeys();
                            sshHostKeys.addKey(InetAddress.getByName(host), hostPublicKey);
                            sshParams.setHostKeyVerifier(new HostKeyFingerprintVerifier(sshHostKeys));
                        }

                        if(settings.getPrivateKey() != null) {
                            String encryptedPrivateKeyText = settings.getPrivateKey();
                            SecurePasswordManager securePasswordManager = _sftpPollingListenerCfg.getApplicationContext().getBean("securePasswordManager", SecurePasswordManager.class);
                            try {
                                String privateKeyText = String.valueOf(securePasswordManager.decryptPassword(encryptedPrivateKeyText));
                                sshParams.setSshPassword(null);
                                if(password == null) {
                                    sshParams.setPrivateKey(privateKeyText);
                                } else {
                                    sshParams.setPrivateKey(privateKeyText, password);
                                }
                            } catch (FindException e) {
                                throw new SftpPollingListenerConfigException("Unable to decrypt private key.", e);
                            } catch (ParseException e) {
                                throw new SftpPollingListenerConfigException("Unable to decrypt private key.", e);
                            }
                        }

                        Sftp sftpClient = new Sftp(sshParams);
                        sftpClient.setTimeout(settings.getPollingInterval() * 1000L + 3000);   // use polling interval with a 3 second buffer
                        sftpClient.connect();
                        sftpClient.setDir(settings.getDirectory());

                        ThreadSafeSftpClient threadSafeSftpClient = new ThreadSafeSftpClient(sftpClient);
                        this.sftpClient = threadSafeSftpClient;
                        if (this.sftpClient == null) {
                            throw new SftpPollingListenerConfigException("Failed to instantiate SFTP polling listener: SFTP client is null");
                        }
                    } catch (IOException ioe) {
                        _logger.log(Level.WARNING, "Error while attempting to access SFTP destination.", ExceptionUtils.getDebugException(ioe));
                        throw ioe;
                    }
                }
            }
        } else if (!this.sftpClient.isConnected()) {
            synchronized (sync) {
                if (!this.sftpClient.isConnected()) {
                    this.sftpClient.connect();
                    this.sftpClient.setDir(getResourceConfig().getDirectory());
                }
            }
        }
        return sftpClient;
    }

    /**
     * Looks in the given directory for files to process.
     *
     * @param fileNames list of files to process
     * @throws SftpPollingListenerConfigException if there's misconfiguration
     * @throws java.io.IOException caused by an error while reading the directory
     */
    protected void scanDirectoryForFiles(List<String> fileNames) throws SftpPollingListenerConfigException, IOException
    {
        ensureSftpClientConnected();

        // get directory listing
        ThreadSafeSftpClient sftpClient = getSftpClient();
        Enumeration dirListing = sftpClient.getDirListing();

        // build a set of already processed files
        HashSet<String> processedFiles = new HashSet<String>();
        while(dirListing.hasMoreElements()) {
            SftpFile file = (SftpFile) dirListing.nextElement();
            if(!file.isDirectory() && file.exists()) {
                String fileName = file.getFilename();
                if(fileName.endsWith(PROCESSED_FILE_EXTENSION)) {
                    processedFiles.add(fileName.substring(0, fileName.length() - 10));
                }
            }
        }

        // look for any unprocessed files
        dirListing = sftpClient.getDirListing();
        while(dirListing.hasMoreElements()) {
            SftpFile file = (SftpFile) dirListing.nextElement();
            String fileName = file.getFilename();
            if(!file.isDirectory() && file.exists() && !fileName.endsWith(PROCESSING_FILE_EXTENSION) && !fileName.endsWith(PROCESSED_FILE_EXTENSION)
                    && !fileName.endsWith(RESPONSE_FILE_EXTENSION) && !processedFiles.contains(fileName)) {
                try {
                    sftpClient.renameFile(fileName, fileName + PROCESSING_FILE_EXTENSION);
                    fileNames.add(fileName);
                } catch(SftpException sftpe) {
                    // exception means that the file no longer exists
                    continue;
                }
            }
        }
    }

    /**
     * Method used to ensure that the SFTP client used has been connected.
     *
     * @throws SftpPollingListenerConfigException when a ThreadSafeSftpClient could not be properly obtained
     * @throws java.io.IOException when a ThreadSafeSftpClient could not be properly obtained
     */
    private void ensureSftpClientConnected() throws SftpPollingListenerConfigException, IOException {
        synchronized(sync) {
            boolean ok = false;
            String message = null;
            try {
                if (getSftpClient() != null) {
                    ok = true;
                }
            } catch (SftpPollingListenerConfigException cex) {
                message = ExceptionUtils.getMessage(cex);
                throw cex;
            } catch (IOException ioe) {
                message = ExceptionUtils.getMessage(ioe);
                throw ioe;
            } catch (RuntimeException e) {
                message = ExceptionUtils.getMessage(e);
                throw e;
            } finally {
                if (ok) {
                    if (!_connected) {
                        _connected = true;
                        fireConnected();
                    }
                } else {
                    fireConnectError(message);
                }
            }
        }
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
                formatMessage(SftpPollingListenerMessages.INFO_EVENT_CONNECT_SUCCESS, _sftpPollingListenerCfg.getDisplayName())));
    }

    private void fireConnectError(String message) {
        fireEvent(new SftpPollingEvent(this, Level.WARNING,  null, formatMessage(
                        SftpPollingListenerMessages.INFO_EVENT_CONNECT_FAIL,
                        new Object[] {_sftpPollingListenerCfg.getDisplayName(), message})));
    }

    private void fireEvent(TransportEvent event) {
        if (_sftpPollingListenerCfg.getApplicationContext() != null) {
            long timeNow = System.currentTimeMillis();
            if ((lastAuditErrorTime+OOPS_AUDIT) < timeNow) {
                lastAuditErrorTime = timeNow;
                _sftpPollingListenerCfg.getApplicationContext().publishEvent(event);
            } else {
                log(Level.INFO, SftpPollingListenerMessages.INFO_EVENT_NOT_PUBLISHED, new Object[0]);
            }
        } else {
            log(Level.WARNING, SftpPollingListenerMessages.INFO_EVENT_NOT_PUBLISHABLE, event.getMessage());
        }
    }

    protected void log(Level level, String messageKey, Object parm) {
        if (parm == null)
            log(level, messageKey, new Object[0]);
        else
            log(level, messageKey, new Object[] {parm});
    }

    protected void log(Level level, String messageKey, Object[] parm) {
        _logger.log(level, messageKey, parm);
    }

    protected void log(Level level, String messageKey, Throwable ex) {
        _logger.log(level, messageKey, ex);
    }

    protected String formatMessage(String messageKey, Object[] parm) {
        return java.text.MessageFormat.format(messageKey, parm);
    }

    protected String formatMessage(String messageKey, Object parm) {
        return formatMessage(messageKey, new Object[] {parm});
    }
}