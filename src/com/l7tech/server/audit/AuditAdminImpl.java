/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.admin.RoleUtils;
import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.common.util.Background;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.OpaqueId;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import net.sf.hibernate.HibernateException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.rmi.RemoteException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class AuditAdminImpl extends ApplicationObjectSupport implements AuditAdmin, InitializingBean {
    private static final Logger logger = Logger.getLogger(AuditAdminImpl.class.getName());
    private static final long CONTEXT_TIMEOUT = 1000L * 60 * 5; // expire after 5 min of inactivity
    private static final int DEFAULT_DOWNLOAD_CHUNK_LENGTH = 8192;
    private static Map downloadContexts = Collections.synchronizedMap(new HashMap());
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

    static {
        Background.schedule(downloadReaperTask, CONTEXT_TIMEOUT, CONTEXT_TIMEOUT);
    }

    public void setAuditRecordManager(AuditRecordManager auditRecordManager) {
        this.auditRecordManager = auditRecordManager;
    }


    public AuditRecord findByPrimaryKey( final long oid ) throws FindException, RemoteException {
        try {
            return (AuditRecord) doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    return auditRecordManager.findByPrimaryKey(oid);
                }
            });
        } catch ( ObjectModelException e ) {
            throw new FindException("Couldn't find AuditRecord", e);
        }
    }

    public Collection find(final AuditSearchCriteria criteria) throws FindException, RemoteException {
        try {
            return (Collection)doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    return auditRecordManager.find(criteria);
                }
            });
        } catch ( ObjectModelException e ) {
            throw new FindException("Couldn't find AuditRecords", e);
        }
    }

    public void deleteOldAuditRecords() throws RemoteException {
        RoleUtils.enforceAdminRole(getApplicationContext());
        try {
            doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    auditRecordManager.deleteOldAuditRecords();
                    return null;
                }
            });
        } catch ( ObjectModelException e ) {
            throw new RemoteException("Couldn't find AuditRecords", e);
        }
    }

    public void afterPropertiesSet() throws Exception {
        checkAuditRecordManager();
    }

    private void checkAuditRecordManager() {
        if (auditRecordManager == null) {
            throw new IllegalArgumentException("audit record manager is required");
        }
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
                } catch (SQLException e) {
                    producerException = e;
                } catch (IOException e) {
                    producerException = e;
                } catch (SignatureException e) {
                    producerException = e;
                } catch (HibernateException e) {
                    producerException = e;
                } catch (InterruptedException e) {
                    // avoid overwriting a real exception
                    if (producerException == null)
                        producerException = e;
                }
            }
        });

        private final AuditExporter auditExporter = new AuditExporter();
        private final byte[] chunk;
        private Throwable producerException = null;
        private long lastUsed = System.currentTimeMillis();

        private DownloadContext(int chunkLength) throws KeyStoreException, IOException, CertificateException {
            if (chunkLength < 1) chunkLength = DEFAULT_DOWNLOAD_CHUNK_LENGTH;
            chunk = new byte[chunkLength];
            sslCert = KeystoreUtils.getInstance().getSslCert();
            sslPrivateKey = KeystoreUtils.getInstance().getSSLPrivateKey();
            producerThread.start();
            logger.info("Created audit download context " + this);
        }

        private OpaqueId getOpaqueId() {
            return opaqueId;
        }

        public long getLastUsed() {
            return lastUsed;
        }

        public synchronized DownloadChunk nextChunk() throws RemoteException {
            lastUsed = System.currentTimeMillis();
            if (producerException != null) {
                close();
                throw new RemoteException("Producer thread exception: " + producerException.getMessage(), producerException);
            }
            try {
                logger.log(Level.FINER, "Returning next audit download chunk for context " + this);
                int i = pis.read(chunk, 0, chunk.length);
                if (i < 1) return null;
                byte[] got = new byte[i];
                System.arraycopy(chunk, 0, got, 0, i);
                long approxTotalAudits = 1;
                long auditsDownloaded = 0;
                if (auditExporter != null) {
                    approxTotalAudits = auditExporter.getApproxNumToExport();
                    auditsDownloaded = auditExporter.getNumExportedSoFar();
                }
                lastUsed = System.currentTimeMillis();
                return new DownloadChunk(auditsDownloaded, approxTotalAudits, got);
            } catch (IOException e) {
                close();
                throw new RemoteException("Unable to read exported audit stream", e);
            }
        }

        public void close() {
            logger.log(Level.INFO, "Closing audit download context " + this);
            downloadContexts.remove(this.getOpaqueId());
            try { producerThread.interrupt(); } catch (Exception e) { /* ignored */ }
            try { pis.close(); } catch (IOException e) { /* ignored */ }
            try { pos.close(); } catch (IOException e) { /* ignored */ }
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DownloadContext)) return false;

            final DownloadContext downloadContext = (DownloadContext)o;

            if (!opaqueId.equals(downloadContext.opaqueId)) return false;

            return true;
        }

        public int hashCode() {
            return opaqueId.hashCode();
        }

        public String toString() {
            return opaqueId.toString();
        }
    }

    public OpaqueId downloadAllAudits(int chunkSizeInBytes) throws RemoteException {
        RoleUtils.enforceAdminRole(getApplicationContext());
        try {
            final DownloadContext downloadContext;
            downloadContext = new DownloadContext(0);
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
        DownloadChunk chunk = downloadContext.nextChunk();
        if (chunk == null || chunk.chunk == null || chunk.chunk.length < 1)
            downloadContext.close();
        return chunk;
    }

    public SSGLogRecord[] getSystemLog(final String nodeid, final long startMsgNumber, final long endMsgNumber, final int size) throws RemoteException {
        try {
            logger.finest("get audits interval ["+startMsgNumber+", "+endMsgNumber+"] for node '"+nodeid+"'");
            Collection c = (Collection)doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    return auditRecordManager.find(new AuditSearchCriteria(nodeid, startMsgNumber, endMsgNumber, size));
                }
            });
            return (SSGLogRecord[])c.toArray(new SSGLogRecord[0]);
        } catch ( ObjectModelException e ) {
            throw new RemoteException("Couldn't find AuditRecords", e);
        }
    }

    private Object doInTransactionAndClose(PersistenceAction r) throws ObjectModelException {
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            return context.doInTransaction(r);
        } catch (SQLException e) {
            throw new ObjectModelException(e);
        } finally {
            if (context != null) context.close();
        }
    }

    public Level serverMessageAuditThreshold() throws RemoteException {
        return AuditContext.getSystemMessageThreshold();
    }

    public int serverMinimumPurgeAge() throws RemoteException {
        String sAge = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE);
        int age = 168;
        try {
            return Integer.valueOf(sAge).intValue();
        } catch (NumberFormatException nfe) {
            throw new RemoteException("Configured minimum age value '" + sAge +
                                      "' is not a valid number. Using " + age + " (one week) by default" );
        }
    }

    private AuditRecordManager auditRecordManager;
}
