package com.l7tech.server.audit;

import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.gateway.common.cluster.ClusterProperty;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.*;
import java.util.Date;
import java.util.TimeZone;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.net.ssl.X509TrustManager;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.transport.ftp.FtpClientUtils;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.DefaultKey;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.common.io.DigestZipOutputStream;
import com.jscape.inet.ftp.FtpException;


/**
 * Archives the audit record range given to the archive() method to the configured FTP destination.
 *
 * @author jbufu
 */
public class FtpArchiveReceiver implements ArchiveReceiver, PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(FtpArchiveReceiver.class.getName());

    // terminates an active upload if no records are received in this interval
    private static final long TRANSFER_CLOSE_TIMEOUT = 600000; // milliseconds

    public static final String TIMESTAMP_PATTERN = "yyyyMMdd'T'HHmmss.S'Z'";

    private ServerConfig serverConfig;
    private ClusterPropertyManager clusterPropertyManager;
    private AuditExporter auditExporter;
    private X509TrustManager trustManager;      // may be needed for ftps verification
    private DefaultKey keyFinder;               // may be needed for ftp authentication
    private X509Certificate sslCert;            // needed for exported zip xml signature
    private PrivateKey sslPrivateKey;           // needed for exported zip xml signature


    private FtpClientConfig ftpConfig;
    private String ftpFilePrefix;
    private long maxUploadFileSize;

    private ReentrantLock lock = new ReentrantLock(); // guards access to zipOut and bytesRemaining
    private DigestZipOutputStream zipOut;
//    private long bytesRemaining;
    private AuditExporter.ExportedInfo exportedInfo;

    public FtpArchiveReceiver(ServerConfig serverConfig, ClusterPropertyManager cpm, AuditExporter ae,
                              X509TrustManager trustManager, DefaultKey defaultKey) {
        if (serverConfig == null)
            throw new NullPointerException("ServerConfig parameter must not be null.");
        if (cpm == null)
            throw new NullPointerException("ClusterPropertyManager parameter must not be null.");
        if (ae == null)
            throw new NullPointerException("AuditExporter parameter must not be null.");
        if (trustManager == null)
            throw new NullPointerException("TrustManager parameter must not be null.");
        if (defaultKey == null)
            throw new NullPointerException("SsgKeyStore parameter must not be null.");

        this.serverConfig = serverConfig;
        this.clusterPropertyManager = cpm;
        this.auditExporter = ae;
        this.trustManager = trustManager;
        this.keyFinder = defaultKey;

        try {
            this.sslCert = defaultKey.getSslInfo().getCertificate();
            this.sslPrivateKey = defaultKey.getSslInfo().getPrivateKey();
        } catch (Exception e) {
            throw new RuntimeException("Server configuration error: unable to obtain keys for signing exported audits.", e);
        }

        reloadConfig();
    }

    @Override
    public boolean isEnabled() {
        boolean enabled = false;

        FtpClientConfig config = ftpConfig;
        if ( config != null ) {
            enabled = config.isEnabled();            
        }

        return enabled;
    }

    @Override
    public AuditExporter.ExportedInfo archiveRecords(long startOid, long endOid) {
        if (ftpConfig == null) {
            logger.warning("FTP audit archive receiver not configured.");
            return null;
        }

        lock.lock();
        try {
            DigestZipOutputStream os = getOutputStream();

            if (os == null) {
                logger.warning("Couldn't get output stream for audit record exporter");
                return null;
            }

            if (logger.isLoggable(Level.FINE))
                logger.fine("Starting export for audit records with objectid in [" + startOid + " : " + endOid + "], " +
                            "destination configured to accept max " + maxUploadFileSize + " bytes." );

            AuditExporter.ExportedInfo result = auditExporter.exportAudits(startOid, endOid, os, maxUploadFileSize, exportedInfo);
            exportedInfo = result;

            if ( exportedInfo == null || ! exportedInfo.hasTransferredFullRange() ) {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Requested audit records range ([" + startOid + " : " + endOid + "]) not fully exported."); 
                endTransfer(); // resets exportedInfo
                
            } else {
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Exported audit records with objectid in " +
                                "[" + exportedInfo.getLowestId() + " : " + exportedInfo.getHighestId() + "], " +
                                exportedInfo.getTransferredBytes() + " bytes");
            }

            return result;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error exporting audit records.", e);
            endTransfer();
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Creates a DigestZipOutputStream connected to a FTP upload connection.
     */
    private DigestZipOutputStream getOutputStream() {
        if (! lock.isHeldByCurrentThread()) {
            logger.warning("Thread tried to get the output while not holding the lock; returning null." + Thread.currentThread().getName());
            return null;
        }

        if (zipOut == null) {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Creating new FTP audit archive zip upload stream...");
            try {
                zipOut = auditExporter.newAuditExportOutputStream(
                    FtpClientUtils.getUploadOutputStream(ftpConfig, newFileName(), keyFinder, trustManager));
                startWatcherThread();
            } catch (FtpException e) {
                logger.log(Level.WARNING, "Error creating output stream for audit export.", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error creating output stream for audit export.", e);
            }
        }
        return zipOut;
    }

    private String newFileName() {
        DateFormat df = new SimpleDateFormat(TIMESTAMP_PATTERN);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return ftpFilePrefix + df.format(new Date(System.currentTimeMillis())) + ".zip";
    }


    @Override
    public boolean flush() {
        if (lock.tryLock()) {
            try {
                return endTransfer();
            } finally {
                lock.unlock();
            }
        }

        return false;
    }


    /**
     * Terminates the current FTP upload transfer and re-initializes the state variables.
     *
     * @return true if the buffers have been flushed and ftp upload closed, or if there's not active transfer;
     *         false if there's an error flushing the active trasfer.
     */
    private boolean endTransfer() {
        if (! lock.isHeldByCurrentThread()) {
            logger.warning("Thread tried to close output stream while not holding the lock: " + Thread.currentThread().getName());
            return false;
        }

        if (zipOut == null) {
            if (logger.isLoggable(Level.FINE)) logger.fine("Zip output stream is null, nothing to close.");
            return true;
        }
        
        if (logger.isLoggable(Level.FINE))
            logger.fine("Ending FTPArchiveReceiver transfer...");

        try {
            auditExporter.addXmlSignature(zipOut, exportedInfo, sslCert, sslPrivateKey);
        } catch (SignatureException e) {
            logger.log(Level.WARNING, "Error adding signature to the exported audit archive.", e);
            return false;
        } finally {
            // reinit state
            exportedInfo = null;
            ResourceUtils.closeQuietly(zipOut);
            if (logger.isLoggable(Level.FINE))
                logger.fine("FTPArchiveReceiver transfer complete for " + zipOut);
            zipOut = null;
        }

        return true;
    }

    /**
     * Terminates the current transfer if there was no activity within the specified period.
     *
     * TODO: not needed anymore with the on-demand flush() mechanism
     */
    private void startWatcherThread() {
        if (! lock.isHeldByCurrentThread()) {
            logger.warning("Tried to start transfer watcher thread while not holding the lock." + Thread.currentThread().getName());
            return;
        }

        if (logger.isLoggable(Level.FINE))
            logger.fine("Starting FTPArchiveReceiver transfer watcher thread for " + zipOut);

        Thread t = new Thread(new Runnable() {
            // use initial value for reference and to track activity
            DigestZipOutputStream original = zipOut;
            long previouslyTransferred = original.getZippedByteCount();

            @Override
            public void run() {
                if (logger.isLoggable(Level.FINE)) logger.fine("FTPArchiveReceiver transfer watching " + original);
                while(true) {
                    try {
                        Thread.sleep(TRANSFER_CLOSE_TIMEOUT);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    boolean gotLock = lock.tryLock();
                    if (gotLock) {
                        try {
                            if (zipOut != original) { // somebody else must have closed it already
                                if (logger.isLoggable(Level.FINE))
                                    logger.fine("FTPArchiveReceiver zip stream (" + zipOut + ")different than the original watched (" + original + ").");
                                break;
                            }

                            if (previouslyTransferred == zipOut.getZippedByteCount()) {
                                if (logger.isLoggable(Level.FINE))
                                    logger.fine("FPTArchiveReceiver transfer timed out: no data received in " +
                                                TRANSFER_CLOSE_TIMEOUT / 1000 + " seconds.");
                                endTransfer();
                                break;
                            }
                            previouslyTransferred = zipOut.getZippedByteCount();

                        } finally {
                            lock.unlock();
                        }
                    }
                }
                if (logger.isLoggable(Level.FINE)) logger.fine("FTPArchiveReceiver transfer watcher thread done for " + original);
            }
        }, "FTPArchiveReceiver-UploadWatcher-" + System.currentTimeMillis());
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        reloadConfig();
    }

    private void reloadConfig() {
        if (logger.isLoggable(Level.FINE)) logger.fine("Reloading configuration.");
        try {
            ClusterProperty configProp = clusterPropertyManager.findByUniqueName(ServerConfig.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION);
            String serializedConfig = configProp == null ? null : configProp.getValue();
            if (serializedConfig == null || serializedConfig.length() == 0) {
                logger.warning("Invalid serialized configuration retrieved from the database: " + serializedConfig);
            } else {
                try {
                    // actual deserialization
                    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(HexUtils.decodeBase64(serializedConfig)));
                    ftpConfig = (FtpClientConfig) in.readObject();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Invalid serialized form retrieved from the database.", e);
                }
            }

            ftpFilePrefix = serverConfig.getProperty(ServerConfig.PARAM_AUDIT_ARCHIVER_FTP_FILEPREFIX);
            maxUploadFileSize = serverConfig.getLongProperty(ServerConfig.PARAM_AUDIT_ARCHIVER_FTP_MAX_UPLOAD_FILE_SIZE, 1000000000);


        } catch (FindException e) {
            logger.log(Level.WARNING, "Error reading FTP audit archiver receiver configuration.", e);
        }
    }
}
