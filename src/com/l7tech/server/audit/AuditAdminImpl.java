/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.admin.AccessManager;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.common.util.Background;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.OpaqueId;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ServerConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.rmi.RemoteException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class AuditAdminImpl extends HibernateDaoSupport implements AuditAdmin, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(AuditAdminImpl.class.getName());
    private static final long CONTEXT_TIMEOUT = 1000L * 60 * 5; // expire after 5 min of inactivity
    private static final int DEFAULT_DOWNLOAD_CHUNK_LENGTH = 8192;
    private static Map downloadContexts = Collections.synchronizedMap(new HashMap());

    private final AccessManager accessManager;
    private AuditRecordManager auditRecordManager;

    private static TimerTask downloadReaperTask = new TimerTask() {
        public void run() {
            Collection c = new ArrayList(downloadContexts.values());
            long now = System.currentTimeMillis();
            for (Iterator i = c.iterator(); i.hasNext();) {
                DownloadContext downloadContext = (DownloadContext)i.next();
                if (now - downloadContext.getLastUsed() > CONTEXT_TIMEOUT) {
                    logger.log(Level.WARNING, "Closing stale audit download context " + downloadContext);
                    downloadContext.close(); // will remove itself from the master set
                }
            }
        }
    };
    private ApplicationContext applicationContext;
    private KeystoreUtils keystore;
    private ServerConfig serverConfig;

    static {
        Background.schedule(downloadReaperTask, CONTEXT_TIMEOUT, CONTEXT_TIMEOUT);
    }

    public AuditAdminImpl(AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    public void setAuditRecordManager(AuditRecordManager auditRecordManager) {
        this.auditRecordManager = auditRecordManager;
    }

    public void setKeystore(KeystoreUtils keystore) {
        this.keystore = keystore;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public AuditRecord findByPrimaryKey( final long oid ) throws FindException, RemoteException {
        return auditRecordManager.findByPrimaryKey(oid);
    }

    public Collection find(final AuditSearchCriteria criteria) throws FindException, RemoteException {
        return auditRecordManager.find(criteria);
    }

    public void deleteOldAuditRecords() throws RemoteException, DeleteException {
        accessManager.enforceAdminRole();
        auditRecordManager.deleteOldAuditRecords();
    }

    public void initDao() throws Exception {
        checkAuditRecordManager();
    }

    private void checkAuditRecordManager() {
        if (auditRecordManager == null) {
            throw new IllegalArgumentException("audit record manager is required");
        }
    }

    /**
     * Set the ApplicationContext that this object runs in.
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private static class DownloadContext {
        private final OpaqueId opaqueId = new OpaqueId();
        private final X509Certificate sslCert;
        private final PrivateKey sslPrivateKey;
        private final PipedOutputStream pos = new PipedOutputStream();
        private final PipedInputStream pis = new PipedInputStream(pos);
        private final Thread producerThread = new Thread(new Runnable() {
            public void run() {
                try {
                    auditExporter.exportAuditsAsZipFile(pos,
                                                        sslCert,
                                                        sslPrivateKey);
                } catch (InterruptedException e) {
                    // avoid overwriting a real exception
                    if (getProducerException() == null)
                        setProducerException(e);
                } catch (Throwable t) {
                    setProducerException(t);
                }
            }
        });

        private final AuditExporter auditExporter;
        private final byte[] chunk;
        private volatile Throwable producerException = null;
        private long lastUsed = System.currentTimeMillis();

        private DownloadContext(int chunkLength, AuditExporter exporter, X509Certificate sslCert, PrivateKey sslPrivateKey) throws IOException {
            if (chunkLength < 1) chunkLength = DEFAULT_DOWNLOAD_CHUNK_LENGTH;
            chunk = new byte[chunkLength];
            this.sslCert = sslCert;
            this.sslPrivateKey = sslPrivateKey;
            this.auditExporter = exporter;
            producerThread.start();
            logger.info("Created audit download context " + this);
        }

        private synchronized OpaqueId getOpaqueId() {
            return opaqueId;
        }

        public synchronized long getLastUsed() {
            return lastUsed;
        }

        public synchronized DownloadChunk nextChunk() throws RemoteException {
            lastUsed = System.currentTimeMillis();
            checkForException();
            try {
                logger.log(Level.FINER, "Returning next audit download chunk for context " + this);
                int i = pis.read(chunk, 0, chunk.length);
                if (i < 1) {
                    // End of file
                    try {
                        producerThread.interrupt();
                        producerThread.join(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    checkForException();
                    return null;
                }
                byte[] got = new byte[i];
                System.arraycopy(chunk, 0, got, 0, i);
                long approxTotalAudits = 1;
                long auditsDownloaded = 0;
                if (auditExporter != null) {
                    approxTotalAudits = auditExporter.getApproxNumToExport();
                    auditsDownloaded = auditExporter.getNumExportedSoFar();
                }
                lastUsed = System.currentTimeMillis();
                checkForException();
                return new DownloadChunk(auditsDownloaded, approxTotalAudits, got);
            } catch (IOException e) {
                close();
                throw new RemoteException("Unable to read exported audit stream", e);
            }
        }

        public void checkForException() throws RemoteException {
            final Throwable producerException = getProducerException();
            if (producerException != null) {
                close();
                throw new RemoteException("Audit .zip producer thread exception: " + producerException.getMessage(), producerException);
            }
        }

        public synchronized void close() {
            logger.log(Level.INFO, "Closing audit download context " + this);
            downloadContexts.remove(this.getOpaqueId());
            try { producerThread.interrupt(); } catch (Throwable t) { /* ignored */ }
            try { pis.close(); } catch (Throwable t) { /* ignored */ }
            try { pos.close(); } catch (Throwable t) { /* ignored */ }
        }

        public synchronized boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DownloadContext)) return false;

            final DownloadContext downloadContext = (DownloadContext)o;

            if (!opaqueId.equals(downloadContext.opaqueId)) return false;

            return true;
        }

        public synchronized int hashCode() {
            return opaqueId.hashCode();
        }

        public synchronized String toString() {
            return opaqueId.toString();
        }

        public synchronized Throwable getProducerException() {
            return producerException;
        }

        public synchronized void setProducerException(Throwable producerException) {
            this.producerException = producerException;
        }
    }

    public OpaqueId downloadAllAudits(int chunkSizeInBytes) throws RemoteException {
        accessManager.enforceAdminRole();
        try {
            final DownloadContext downloadContext;
            downloadContext = new DownloadContext(0, (AuditExporter)applicationContext.getBean("auditExporter"), keystore.getSslCert(), keystore.getSSLPrivateKey());
            downloadContext.checkForException();
            downloadContexts.put(downloadContext.getOpaqueId(), downloadContext);
            return downloadContext.getOpaqueId();
        } catch (KeyStoreException e) {
            throw new RemoteException("Server configuration error: unable to prepare keys for signing exported audits", e);
        } catch (IOException e) {
            throw new RemoteException("IO error while preparing to export audits", e);
        } catch (CertificateException e) {
            throw new RemoteException("Server configuration error: unable to prepare certificate for signing exported audits", e);
        }
    }

    public DownloadChunk downloadNextChunk(OpaqueId context) throws RemoteException {
        DownloadContext downloadContext = (DownloadContext)downloadContexts.get(context);
        if (downloadContext == null)
            throw new RemoteException("No such download context is pending");
        downloadContext.checkForException();
        DownloadChunk chunk = downloadContext.nextChunk();
        if (chunk == null || chunk.chunk == null || chunk.chunk.length < 1)
            downloadContext.close();
        downloadContext.checkForException();
        return chunk;
    }

    public SSGLogRecord[] getSystemLog(final String nodeid, final long startMsgNumber, final long endMsgNumber, final int size)
      throws RemoteException, FindException {
        logger.finest("get audits interval ["+startMsgNumber+", "+endMsgNumber+"] for node '"+nodeid+"'");
        return (SSGLogRecord[])auditRecordManager.find(new AuditSearchCriteria(nodeid, startMsgNumber, endMsgNumber, size)).toArray(new SSGLogRecord[] {});
    }


    public Level serverMessageAuditThreshold() throws RemoteException {
        // todo: consider moving this and the same code from AuditContextImpl in ServerConfig
        String msgLevel = serverConfig.getProperty(ServerConfig.PARAM_AUDIT_MESSAGE_THRESHOLD);
        Level output = null;
        if (msgLevel != null) {
            try {
                output = Level.parse(msgLevel);
            } catch(IllegalArgumentException e) {
                logger.warning("Invalid message threshold value '" + msgLevel + "'. Will use default " +
                               AuditContext.DEFAULT_MESSAGE_THRESHOLD.getName() + " instead.");
            }
        }
        if (output == null) {
            output = AuditContext.DEFAULT_MESSAGE_THRESHOLD;
        }
        return output;
    }

    public int serverMinimumPurgeAge() throws RemoteException {
        String sAge = serverConfig.getProperty(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE);
        int age = 168;
        try {
            return Integer.valueOf(sAge).intValue();
        } catch (NumberFormatException nfe) {
            throw new RemoteException("Configured minimum age value '" + sAge +
                                      "' is not a valid number. Using " + age + " (one week) by default" );
        }
    }
}
