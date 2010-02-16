/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.logging.SSGLogRecord;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.admin.AuditViewGatewayAuditsData;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.OpaqueId;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.TimeUnit;
import org.apache.commons.collections.map.LRUMap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of AuditAdmin in SSG.
 */
public class AuditAdminImpl implements AuditAdmin, InitializingBean, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(AuditAdminImpl.class.getName());
    private static final String CLUSTER_PROP_LAST_AUDITACK_TIME = "audit.acknowledge.highestTime";

    private AuditDownloadManager auditDownloadManager;
    private AuditRecordManager auditRecordManager;
    private SecurityFilter filter;
    private LogRecordManager logRecordManager;
    private ServerConfig serverConfig;
    private ClusterPropertyManager clusterPropertyManager;
    private AuditArchiver auditArchiver;
    private PlatformTransactionManager transactionManager; // required for TransactionTemplate
    private ApplicationContext applicationContext;

    private final BlockingQueue<AuditViewData> queue = new ArrayBlockingQueue<AuditViewData>(10);

    public void setAuditDownloadManager(AuditDownloadManager auditDownloadManager) {
        this.auditDownloadManager = auditDownloadManager;
    }

    public void setAuditRecordManager(AuditRecordManager auditRecordManager) {
        this.auditRecordManager = auditRecordManager;
    }

    public void setSecurityFilter(SecurityFilter filter) {
        this.filter = filter;
    }

    public void setLogRecordManager(LogRecordManager logRecordManager) {
        this.logRecordManager = logRecordManager;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void setClusterPropertyManager(ClusterPropertyManager clusterPropertyManager) {
        this.clusterPropertyManager = clusterPropertyManager;
    }

    public void setAuditArchiver(AuditArchiver auditArchiver) {
        this.auditArchiver = auditArchiver;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public AuditRecord findByPrimaryKey( final long oid ) throws FindException {
        return auditRecordManager.findByPrimaryKey(oid);
    }

    public Collection<AuditRecord> find(final AuditSearchCriteria criteria) throws FindException {
        addToAudit(criteria);
        return auditRecordManager.find(criteria);
    }

    /**
     * Essentially adds to the queue to be audited because audit data is being viewed.
     * <br/>
     * This method should only be called within methods that retrieve audit data from the database.
     *
     * @param criteria  The search criteria for retrieving audits
     */
    private void addToAudit(final AuditSearchCriteria criteria) {
        final AdminInfo adminInfo = AdminInfo.find();
        AuditViewGatewayAuditsData audit = new AuditViewGatewayAuditsData(this, criteria.getAuditQueryDetails(false));
        try {
            queue.put(new AuditViewData(audit, adminInfo, criteria));
        } catch (InterruptedException ie) {
            logger.warning("Failed to add audit view event: " + ie.getMessage());
        }
    }

    public Collection<AuditRecordHeader> findHeaders(AuditSearchCriteria criteria) throws FindException{
        addToAudit(criteria);
        return auditRecordManager.findHeaders(criteria);
    }

    public boolean hasNewAudits( final Date date, final Level level) {
        AuditSearchCriteria criteria = new AuditSearchCriteria.Builder().fromTime(date).fromLevel(level).maxRecords(1).build();
        boolean hasNewAudits = false;
        User user = JaasUtils.getCurrentUser();
        if ( user != null ) {
            try {
                Collection<AuditRecordHeader> newAudits = auditRecordManager.findHeaders(criteria);
                newAudits = filter.filter(newAudits, user, OperationType.READ, null );
                if ( newAudits.size() > 0 ) {
                    hasNewAudits = true;
                }
            } catch (FindException fe) {
                logger.fine("Failed to find new audits for date " + date.toString() + " with level " + level.toString());
            }
        } else {
            logger.fine("User not found when checking for new audits.");
        }
        return hasNewAudits;
    }

    public void deleteOldAuditRecords() throws DeleteException {
        auditRecordManager.deleteOldAuditRecords(-1);
    }

    public void doAuditArchive() {
        if (auditArchiver == null) {
            throw new NullPointerException("Null AuditArchiver! Cannot run requested archive command.");
        }
        auditArchiver.runNow();
    }

    public ClusterProperty getFtpAuditArchiveConfig() {
        ClusterProperty result = null;
        try {
            result = clusterPropertyManager.findByUniqueName(ServerConfig.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION);
        }
        catch (FindException fe) {
            logger.warning("Error getting cluster property: " + fe.getMessage());
        }

        return result != null ? result : new ClusterProperty(ServerConfig.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION, null);
    }

    public void setFtpAuditArchiveConfig(ClusterProperty prop) throws UpdateException {

        if (prop == null || ! ServerConfig.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION.equals(prop.getName()))
            throw new UpdateException("Invalid cluster property provided for FTP archiver configuration: " + prop);

        // bug #6574 - error calling update() for the first time to set the cluster property
        //             for ftp archiver -- Call save to create the first time
        ClusterProperty cp;
        try {
            cp = clusterPropertyManager.findByUniqueName(ServerConfig.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION);
        } catch (FindException fe) {
            cp = null;
        }

        if (cp != null)
            clusterPropertyManager.update(prop);
        else {
            try {
                clusterPropertyManager.save(prop);
            } catch (SaveException se) {
                clusterPropertyManager.update(prop);
            }
        }
    }

    public void afterPropertiesSet() throws Exception {
        checkAuditRecordManager();
        AuditViewerTask task = new AuditViewerTask();
        task.start();   //start daemon thread
    }

    private void checkAuditRecordManager() {
        if (auditRecordManager == null) {
            throw new IllegalArgumentException("audit record manager is required");
        }
    }

    public OpaqueId downloadAllAudits(long fromTime,
                                      long toTime,
                                      long[] serviceOids,
                                      int chunkSizeInBytes) {
        try {
            return auditDownloadManager.createDownloadContext( fromTime, toTime, serviceOids );
        } catch (IOException e) {
            throw new RuntimeException("IO error while preparing to export audits", e);
        }
    }

    public DownloadChunk downloadNextChunk(OpaqueId context) {
        DownloadChunk chunk = null;

        byte[] data = auditDownloadManager.nextDownloadChunk( context );
        if ( data != null ) {
            long auditTotal = auditDownloadManager.getEstimatedTotalAudits( context );
            long auditCount = auditDownloadManager.getDownloadedAuditCount( context );
            chunk = new DownloadChunk( auditCount, auditTotal, data );
       }

        return chunk;
    }

    /**
     * Find audit records using the given parameters
     *
     * @param nodeid The node identifier
     * @param startMsgDate The starting date
     * @param endMsgDate The ending date 
     * @param size The maximum number of records to return
     * @throws FindException
     */
    public Collection<AuditRecord> findAuditRecords(final String nodeid,
                                                    final Date startMsgDate,
                                                    final Date endMsgDate,
                                                    final int size)
                                             throws FindException {
        logger.finest("Get audits interval ["+startMsgDate+", "+endMsgDate+"] for node '"+nodeid+"'");
        //return auditRecordManager.find(new AuditSearchCriteria(startMsgDate, endMsgDate, null, null, null, nodeid, -1, -1, size));
        return auditRecordManager.find(new AuditSearchCriteria.Builder().fromTime(startMsgDate).
                toTime(endMsgDate).nodeId(nodeid).startMessageNumber(-1).endMessageNumber(-1).maxRecords(size).build());
    }

    public Date getLastAcknowledgedAuditDate() {
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

    public Date markLastAcknowledgedAuditDate() {
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
                                throws FindException {
        logger.finest("Get logs interval ["+startMsgNumber+", "+endMsgNumber+"] for node '"+nodeid+"'");
        return logRecordManager.find(nodeid, startMsgNumber, size);
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

    public Level serverMessageAuditThreshold() {
        return getAuditLevel(ServerConfig.PARAM_AUDIT_MESSAGE_THRESHOLD, "message", AuditContext.DEFAULT_MESSAGE_THRESHOLD);
    }

    public Level serverDetailAuditThreshold() {
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

    public int serverMinimumPurgeAge() {
        String sAge = serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE);
        int age = 168;
        try {
            return Integer.valueOf(sAge);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException("Configured minimum age value '" + sAge +
                                      "' is not a valid number. Using " + age + " (one week) by default" );
        }
    }

    @Override
    public boolean isSigningEnabled() throws FindException {
        final String prop = clusterPropertyManager.getProperty("audit.signing");
        return Boolean.valueOf(prop);
    }

    /**
     * A daemon thread which will persist audit view events.
     */
    private class AuditViewerTask extends Thread {
        private final LRUMap auditedData;
        private static final int DEFAULT_MAX_AUDIT_DATA_SIZE = 100;
        private static final String MAX_AUDIT_DATA_CACHE_SIZE = "com.l7tech.server.audit.maxAuditDataCacheSize";

        public AuditViewerTask() {
            auditedData = new LRUMap(SyspropUtil.getInteger(MAX_AUDIT_DATA_CACHE_SIZE, DEFAULT_MAX_AUDIT_DATA_SIZE));
            setDaemon(true);
        }

        /**
         * Determines whether a particular audit event should be persisted.
         *
         * @param data  The audit view data containing all the necessary informaiton to processing
         * @return  TRUE if the audit should be persisted
         */
        private boolean shouldPersistAudit(final AuditViewData data) {
            AdminInfo admin = data.getAdminInfo();
            AuditSearchCriteria criteria = data.getCriteria();

            //determine if we really need to audit this.  It the user has requested to view actual new audit data we'll need
            //to audit it, if it's just the refresh update from the audit view data then we'll need to audit this
             if (!auditedData.containsKey(admin.getIdentityHeader())) {
                //brand new one, we need to audit this admin
                auditedData.put(admin.getIdentityHeader(), data);
                return true;
            } else {
                //find out when was the it's a new audit view query or just from refresh rate
                AuditViewData lastData = (AuditViewData) auditedData.get(admin.getIdentityHeader());
                AuditSearchCriteria lastCriteria = lastData.getCriteria();

                //if the criteria content are already different then we'll need to audit them
                 long now = System.currentTimeMillis();
                if (!lastCriteria.containsSimilarCritiera(criteria)) {
                    auditedData.put(admin.getIdentityHeader(), data);
                    return true;
                } else {
                    //update last checked time on this particular audit view data
                    AuditViewData temp = (AuditViewData) auditedData.get(admin.getIdentityHeader());
                    temp.setLastTimeChecked(now);
                    auditedData.put(admin.getIdentityHeader(), temp);
                }
            }
            return false;
        }

        /**
         * Clean up any expired audited data that has lived passed it's time to live time frame.  This is needed
         * so that the thread doesn't keep on accumulating when there are unnecessary data.
         */
        @SuppressWarnings({"unchecked"})
        private void cleanUp() {
            //loop through the set and delete any data that has expired already
            for (Iterator i = auditedData.entrySet().iterator(); i.hasNext();) {
                Map.Entry<IdentityHeader, AuditViewData> entry = (Map.Entry<IdentityHeader, AuditViewData>) i.next();
                AuditViewData data = entry.getValue();
                if (data.isStale()) i.remove();
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    final AuditViewData auditData = queue.take();   //block until more data
                    if (shouldPersistAudit(auditData)) {
                        auditData.getAdminInfo().invokeCallable(new Callable<Object>() {
                            public Object call() throws Exception {
                                new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
                                    protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                                        applicationContext.publishEvent(auditData.getAudit());
                                    }
                                });
                                return null;
                            }
                        });
                    }
                } catch (InterruptedException ie) {
                    if (queue.remainingCapacity() > 0) {
                        logger.warning("Some view audit data were not recorded.");
                    }
                    break;
                } catch (Exception e) {
                    //shouldn't happen
                    logger.warning("Failed to publish audit view event: " + e.getMessage());
                } finally {
                    //cleanup any expired ones
                    cleanUp();
                }
            }
        }
    }


    /**
     * Temporary object which will hold the audit view event and administrative information which fired the audit
     * view event.
     */
    private class AuditViewData {
        private final AuditViewGatewayAuditsData audit;
        private final AdminInfo adminInfo;
        private final AuditSearchCriteria criteria;
        private volatile long lastTimeChecked;

        public AuditViewData(AuditViewGatewayAuditsData audit, AdminInfo adminInfo, AuditSearchCriteria criteria) {
            this.audit = audit;
            this.adminInfo = adminInfo;
            this.criteria = criteria;
            this.lastTimeChecked = System.currentTimeMillis();
        }

        public AdminInfo getAdminInfo() {
            return adminInfo;
        }

        public AuditViewGatewayAuditsData getAudit() {
            return audit;
        }

        public AuditSearchCriteria getCriteria() {
            return criteria;
        }

        public boolean isStale() {
            return (System.currentTimeMillis() - lastTimeChecked) > TimeUnit.HOURS.toMillis(1);
        }

        public long getLastTimeChecked() {
            return lastTimeChecked;
        }

        public void setLastTimeChecked(long lastTimeChecked) {
            this.lastTimeChecked = lastTimeChecked;
        }
    }
}
