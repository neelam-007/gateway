/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditAdmin;
import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.common.util.Background;
import com.l7tech.common.util.OpaqueId;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.ServerConfig;
import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.cluster.ClusterProperty;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

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
public class AuditAdminImpl implements AuditAdmin, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(AuditAdminImpl.class.getName());
    private static final long CONTEXT_TIMEOUT = 1000L * 60 * 5; // expire after 5 min of inactivity
    private static final int DEFAULT_DOWNLOAD_CHUNK_LENGTH = 8192;
    private static Map<OpaqueId, DownloadContext> downloadContexts = Collections.synchronizedMap(new HashMap<OpaqueId, DownloadContext>());
    private static final String CLUSTER_PROP_LAST_AUDITACK_TIME = "audit.acknowledge.highestTime";


    private AuditRecordManager auditRecordManager;
    private LogRecordManager logRecordManager;
    private ApplicationContext applicationContext;
    private KeystoreUtils keystore;
    private ServerConfig serverConfig;
    private ClusterPropertyManager clusterPropertyManager;

    private static TimerTask downloadReaperTask = new TimerTask() {
        public void run() {
            Collection<DownloadContext> contexts = new ArrayList<DownloadContext>(downloadContexts.values());
            long now = System.currentTimeMillis();
            for (DownloadContext downloadContext : contexts) {
                if (now - downloadContext.getLastUsed() > CONTEXT_TIMEOUT) {
                    logger.log(Level.WARNING, "Closing stale audit download context " + downloadContext);
                    downloadContext.close(); // will remove itself from the master set
                }
            }
        }
    };

    static {
        Background.scheduleRepeated(downloadReaperTask, CONTEXT_TIMEOUT, CONTEXT_TIMEOUT);
    }

    public void setAuditRecordManager(AuditRecordManager auditRecordManager) {
        this.auditRecordManager = auditRecordManager;
    }

    public void setLogRecordManager(LogRecordManager logRecordManager) {
        this.logRecordManager = logRecordManager;
    }

    public void setKeystore(KeystoreUtils keystore) {
        this.keystore = keystore;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void setClusterPropertyManager(ClusterPropertyManager clusterPropertyManager) {
        this.clusterPropertyManager = clusterPropertyManager;
    }

    public AuditRecord findByPrimaryKey( final long oid ) throws FindException, RemoteException {
        return auditRecordManager.findByPrimaryKey(oid);
    }

    public Collection<AuditRecord> find(final AuditSearchCriteria criteria) throws FindException, RemoteException {
        return auditRecordManager.find(criteria);
    }

    public void deleteOldAuditRecords() throws RemoteException, DeleteException {
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

            //noinspection RedundantIfStatement
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
        DownloadContext downloadContext = downloadContexts.get(context);
        if (downloadContext == null)
            throw new RemoteException("No such download context is pending");
        downloadContext.checkForException();
        DownloadChunk chunk = downloadContext.nextChunk();
        if (chunk == null || chunk.chunk == null || chunk.chunk.length < 1)
            downloadContext.close();
        downloadContext.checkForException();
        return chunk;
    }

    public Collection<AuditRecord> findAuditRecords(final String nodeid,
                                                    final long startMsgNumber,
                                                    final long endMsgNumber,
                                                    final Date startMsgDate,
                                                    final Date endMsgDate,
                                                    final int size)
                                             throws RemoteException, FindException {
        logger.finest("Get audits interval ["+startMsgNumber+", "+endMsgNumber+"] for node '"+nodeid+"'");
        return auditRecordManager.find(new AuditSearchCriteria(startMsgDate, endMsgDate, null, null, null, nodeid, startMsgNumber, endMsgNumber, size));
    }

    public Date getLastAcknowledgedAuditDate() throws RemoteException {
        Date date = null;
        String value = null;

        try {
            ClusterProperty property = clusterPropertyManager.findByUniqueName(CLUSTER_PROP_LAST_AUDITACK_TIME);
            if (property != null) {
                value = property.getValue();
                date = new Date(Long.parseLong(value));
            }
        }
        catch (FindException fe) {
            logger.warning("Error getting cluster property '"+CLUSTER_PROP_LAST_AUDITACK_TIME+"' message is '"+fe.getMessage()+"'.");            
        }
        catch (NumberFormatException nfe) {
            logger.warning("Error getting cluster property '"+CLUSTER_PROP_LAST_AUDITACK_TIME+"' invalid long value '"+value+"'.");            
        }

        return date;
    }

    public Date markLastAcknowledgedAuditDate() throws RemoteException {
        Date date = new Date();
        String value = Long.toString(date.getTime());

        try {
            ClusterProperty property = clusterPropertyManager.findByUniqueName(CLUSTER_PROP_LAST_AUDITACK_TIME);
            if (property == null) {
                property = new ClusterProperty(CLUSTER_PROP_LAST_AUDITACK_TIME, value);
                clusterPropertyManager.save(property);
            } else {
                property.setValue(value);
                clusterPropertyManager.update(property);
            }
        }
        catch (FindException fe) {
            logger.warning("Error getting cluster property '"+CLUSTER_PROP_LAST_AUDITACK_TIME+"' message is '"+fe.getMessage()+"'.");
        }
        catch (SaveException se) {
            logger.log(Level.WARNING ,"Error saving cluster property '"+CLUSTER_PROP_LAST_AUDITACK_TIME+"'.", se);
        }
        catch(UpdateException ue) {
            logger.log(Level.WARNING ,"Error updating cluster property '"+CLUSTER_PROP_LAST_AUDITACK_TIME+"'.", ue);            
        }

        return date;
    }

    public SSGLogRecord[] getSystemLog(final String nodeid,
                                       final long startMsgNumber,
                                       final long endMsgNumber,
                                       final Date startMsgDate,
                                       final Date endMsgDate,
                                       final int size)
                                throws RemoteException, FindException {
        logger.finest("Get logs interval ["+startMsgNumber+", "+endMsgNumber+"] for node '"+nodeid+"'");
        return logRecordManager.find(nodeid, endMsgNumber, size);
    }

    public int getSystemLogRefresh(final int typeId) {
        int refreshInterval = 0;
        int defaultRefreshInterval = 3;
        String propertyName = null;

        switch(typeId) {
            case TYPE_AUDIT:
                propertyName = ServerConfig.PARAM_AUDIT_REFRESH_PERIOD_SECS;
                break;
            case TYPE_LOG:
                propertyName = ServerConfig.PARAM_AUDIT_LOG_REFRESH_PERIOD_SECS;
                break;
            default:
                logger.warning("System logs refresh period requested for an unknown type '"+typeId+"'.");
                break;
        }

        if(propertyName!=null) {
            String valueInSecsStr = serverConfig.getPropertyCached(propertyName);
            if(valueInSecsStr!=null) {
                try {
                    refreshInterval = Integer.parseInt(valueInSecsStr);
                }
                catch(NumberFormatException nfe) {
                    refreshInterval = defaultRefreshInterval;
                    logger.warning("Property '"+propertyName+"' has invalid value '"+valueInSecsStr
                            +"', using default '"+defaultRefreshInterval+"'.");
                }
            }
            else {
                refreshInterval = defaultRefreshInterval;
            }
        }

        return refreshInterval;
    }

    public Level serverMessageAuditThreshold() throws RemoteException {
        return getAuditLevel(ServerConfig.PARAM_AUDIT_MESSAGE_THRESHOLD, "message", AuditContext.DEFAULT_MESSAGE_THRESHOLD);
    }

    public Level serverDetailAuditThreshold() throws RemoteException {
        return getAuditLevel(ServerConfig.PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD, "detail", AuditContext.DEFAULT_ASSOCIATED_LOGS_THRESHOLD);
    }

    private Level getAuditLevel(String serverConfigParam, String which, Level defaultLevel) {
        // todo: consider moving this and the same code from AuditContextImpl in ServerConfig
        String msgLevel = serverConfig.getPropertyCached(serverConfigParam);
        Level output = null;
        if (msgLevel != null) {
            try {
                output = Level.parse(msgLevel);
            } catch(IllegalArgumentException e) {
                logger.warning("Invalid " + which + " threshold value '" + msgLevel + "'. Will use default " +
                               defaultLevel.getName() + " instead.");
            }
        }
        if (output == null) {
            output = defaultLevel;
        }
        return output;
    }

    public int serverMinimumPurgeAge() throws RemoteException {
        String sAge = serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE);
        int age = 168;
        try {
            return Integer.valueOf(sAge).intValue();
        } catch (NumberFormatException nfe) {
            throw new RemoteException("Configured minimum age value '" + sAge +
                                      "' is not a valid number. Using " + age + " (one week) by default" );
        }
    }
}
