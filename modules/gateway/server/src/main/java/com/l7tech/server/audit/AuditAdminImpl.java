/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.logging.SSGLogRecord;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.GatewayKeyAccessFilter;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.event.admin.AuditViewGatewayAuditsData;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.*;
import org.apache.commons.collections.map.LRUMap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of AuditAdmin in SSG.
 */
public class AuditAdminImpl implements AuditAdmin, InitializingBean, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(AuditAdminImpl.class.getName());
    private static final String CLUSTER_PROP_LAST_AUDITACK_TIME = "audit.acknowledge.highestTime";
    private static final int MAX_CRITERIA_HISTORY = SyspropUtil.getInteger( "com.l7tech.server.audit.maxAuditSearchCriteria", 20 );
    private static final String MAX_AUDIT_DATA_CACHE_SIZE = "com.l7tech.server.audit.maxAuditDataCacheSize";
    private static final int CLEANUP_DELAY = SyspropUtil.getInteger( "com.l7tech.server.audit.auditDataCacheDelay", 120000 );
    private static final long CLEANUP_PERIOD = SyspropUtil.getLong( "com.l7tech.server.audit.auditDataCachePeriod",  60000 );

    // map of IdentityHeader -> AuditViewData
    private final LRUMap auditedData = new LRUMap(SyspropUtil.getInteger(MAX_AUDIT_DATA_CACHE_SIZE, 100));

    private AuditDownloadManager auditDownloadManager;
    private AuditRecordManager auditRecordManager;
    private SecurityFilter filter;
    private LogRecordManager logRecordManager;
    private ServerConfig serverConfig;
    private ClusterPropertyManager clusterPropertyManager;
    private AuditArchiver auditArchiver;
    private ApplicationContext applicationContext;
    private AuditFilterPolicyManager auditFilterPolicyManager;
    private GatewayKeyAccessFilter keyAccessFilter;
    private Auditor auditor;

    public AuditAdminImpl() {
        Background.scheduleRepeated( new TimerTask(){
            @Override
            public void run() {
                cleanupAuditViewData();
            }
        }, CLEANUP_DELAY, CLEANUP_PERIOD );
    }

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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        auditor = new Auditor(this, applicationContext, logger);
    }

    @Override
    public AuditRecord findByPrimaryKey( final long oid ) throws FindException {
        return auditRecordManager.findByPrimaryKey(oid);
    }

    @Override
    public Collection<AuditRecord> find(final AuditSearchCriteria criteria) throws FindException {
        notifyAuditSearch(criteria);
        return auditRecordManager.find(criteria);
    }

    /**
     * Essentially adds to the queue to be audited because audit data is being viewed.
     * <br/>
     * This method should only be called within methods that retrieve audit data from the database.
     *
     * @param criteria  The search criteria for retrieving audits
     */
    private void notifyAuditSearch( final AuditSearchCriteria criteria ) {
        final AdminInfo adminInfo = AdminInfo.find();

        boolean audit;
        synchronized ( auditedData ) {
            AuditViewData data = (AuditViewData) auditedData.get( adminInfo.getIdentityHeader() );
            if ( data != null ) {
                audit = data.isNewCriteria( criteria );
            } else {
                auditedData.put( adminInfo.getIdentityHeader(), new AuditViewData(adminInfo, criteria) );
                audit = true;
            }
        }

        if ( audit ) {
            final Pair<String, String> details = criteria.getAuditQueryDetails(); // In a pair, Left = partial details; Right = full details
            auditor.logAndAudit(AdminMessages.AUDIT_SEARCH_CRITERIA_FULL_DETAILS, details.right);

            final AuditViewGatewayAuditsData auditEvent = new AuditViewGatewayAuditsData(this, details.left);
            applicationContext.publishEvent( auditEvent );
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void cleanupAuditViewData() {
        synchronized ( auditedData ) {
            //loop through the set and delete any data that has expired already
            for (Iterator i = auditedData.entrySet().iterator(); i.hasNext();) {
                Map.Entry<IdentityHeader, AuditViewData> entry = (Map.Entry<IdentityHeader, AuditViewData>) i.next();
                AuditViewData data = entry.getValue();
                if (data.isStale()) i.remove();
            }
        }
    }

    @Override
    public Collection<AuditRecordHeader> findHeaders(AuditSearchCriteria criteria) throws FindException{
        notifyAuditSearch(criteria);
        return auditRecordManager.findHeaders(criteria);
    }

    @Override
    public long hasNewAudits( final Date date, final Level level) {
        AuditSearchCriteria criteria = new AuditSearchCriteria.Builder().fromTime(date).fromLevel(level).maxRecords(1).build();
        long newAuditTime = 0;
        User user = JaasUtils.getCurrentUser();
        if ( user != null ) {
            try {
                Collection<AuditRecordHeader> newAudits = auditRecordManager.findHeaders(criteria);
                newAudits = filter.filter(newAudits, user, OperationType.READ, null );
                if ( newAudits.size() > 0 ) {
                    newAuditTime = newAudits.iterator().next().getTimestamp();
                }
            } catch (FindException fe) {
                logger.fine("Failed to find new audits for date " + date.toString() + " with level " + level.toString());
            }
        } else {
            logger.fine("User not found when checking for new audits.");
        }
        return newAuditTime;
    }

    @Override
    public void deleteOldAuditRecords() throws DeleteException {
        auditRecordManager.deleteOldAuditRecords(-1);
    }

    @Override
    public void doAuditArchive() {
        if (auditArchiver == null) {
            throw new NullPointerException("Null AuditArchiver! Cannot run requested archive command.");
        }
        auditArchiver.runNow();
    }

    @Override
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

    @Override
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

    public void setAuditFilterPolicyManager(AuditFilterPolicyManager auditFilterPolicyManager) {
        this.auditFilterPolicyManager = auditFilterPolicyManager;
    }

    public void setKeyAccessFilter(GatewayKeyAccessFilter keyAccessFilter) {
        this.keyAccessFilter = keyAccessFilter;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        checkAuditRecordManager();
    }

    private void checkAuditRecordManager() {
        if (auditRecordManager == null) {
            throw new IllegalArgumentException("audit record manager is required");
        }
    }

    @Override
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

    @Override
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
    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
    public Level serverMessageAuditThreshold() {
        return getAuditLevel(ServerConfig.PARAM_AUDIT_MESSAGE_THRESHOLD, "message", AuditContext.DEFAULT_MESSAGE_THRESHOLD);
    }

    @Override
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

    @Override
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

    @Override
    public boolean isAuditViewerPolicyAvailable() {
        return auditFilterPolicyManager.isAuditViewerPolicyAvailable();
    }

    @Override
    public String invokeAuditViewerPolicyForMessage(long auditRecordId, final boolean isRequest) throws FindException {

        final List<Pair<Level, String>> auditMessages = new ArrayList<Pair<Level, String>>();
        try {
            auditMessages.add(new Pair<Level, String>(
                    Level.INFO,
                    MessageFormat.format("Audit viewer policy invoked for AuditRecord ''{0}''. Invoked for {1}.",
                    String.valueOf(auditRecordId), ((isRequest) ? "request": "response") + " message")));
            
            final AuditRecord record = auditRecordManager.findByPrimaryKey(auditRecordId);
            if(record == null){
                final String params = "No audit record found for AuditRecord with id " + auditRecordId;
                addInvokeAuditViewerAuditMsg(auditMessages, params);
                return null;
            }

            if (!(record instanceof MessageSummaryAuditRecord)) {
                final String params = "Audit viewer policy is only applicable for message audits. Cannot process AuditRecord with id " + auditRecordId;
                addInvokeAuditViewerAuditMsg(auditMessages, params);
                return null;
            }

            final MessageSummaryAuditRecord messageAudit = (MessageSummaryAuditRecord) record;
            final String messageXml;
            if(isRequest){
                messageXml = messageAudit.getRequestXml();
            } else {
                messageXml = messageAudit.getResponseXml();
            }

            try {
                return keyAccessFilter.doWithRestrictedKeyAccess(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        return auditFilterPolicyManager.evaluateAuditViewerPolicy(messageXml, isRequest);
                    }
                });
            } catch (AuditFilterPolicyManager.AuditViewerPolicyException e) {
                final String params = "Exception processing audit viewer policy: " + ExceptionUtils.getMessage(e);
                //noinspection ThrowableResultOfMethodCallIgnored
                logger.log(Level.WARNING, params, ExceptionUtils.getDebugException(e));
                addInvokeAuditViewerAuditMsg(auditMessages, params);
            } catch (Exception e) {
                final String params = "Exception processing audit viewer policy: " + ExceptionUtils.getMessage(e);
                //noinspection ThrowableResultOfMethodCallIgnored
                logger.log(Level.WARNING, params, ExceptionUtils.getDebugException(e));
                addInvokeAuditViewerAuditMsg(auditMessages, params);
            }

            return null;
        } finally {
            publishInvokeAuditViewerAudits(auditMessages);
        }
    }

    @Override
    public String invokeAuditViewerPolicyForDetail(long auditRecordId, long ordinal) throws FindException {

        if(ordinal < 0) throw new IllegalArgumentException("ordinal must be >= 0");

        final List<Pair<Level, String>> auditMessages = new ArrayList<Pair<Level, String>>();

        try {
            auditMessages.add(new Pair<Level, String>(
                    Level.INFO,
                    MessageFormat.format("Audit viewer policy invoked for AuditRecord ''{0}''. Invoked for audit detail message in position {1}.",
                    String.valueOf(auditRecordId), ordinal + 1)));//users count from 1
            
            final AuditRecord record = auditRecordManager.findByPrimaryKey(auditRecordId);
            if(record == null){
                final String params = "No audit record found for AuditRecord with id " + auditRecordId;
                addInvokeAuditViewerAuditMsg(auditMessages, params);
                return null;
            }

            if (!(record instanceof MessageSummaryAuditRecord)) {
                final String params = "Audit viewer policy is only applicable for message audits. Cannot process AuditRecord with id " + auditRecordId;
                addInvokeAuditViewerAuditMsg(auditMessages, params, Level.FINE);
                return null;
            }

            final MessageSummaryAuditRecord messageAudit = (MessageSummaryAuditRecord) record;

            final Set<AuditDetail> details = messageAudit.getDetails();
            for (AuditDetail detail : details) {
                //ordinal is all that is actually needed to find the audit detail, so long as it has been created correctly
                if (USER_DETAIL_MESSAGES.contains(detail.getMessageId()) && detail.getOrdinal() == ordinal) {
                    final String[] detailParams = detail.getParams();
                    if (detailParams == null || detailParams[0] == null) {
                        final String auditMsg = "No parameter found for audit detail record with id " + detail.getMessageId() + " with ordinal " + ordinal;
                        addInvokeAuditViewerAuditMsg(auditMessages, auditMsg);
                        return null;
                    }
                    try {
                        return keyAccessFilter.doWithRestrictedKeyAccess(new Callable<String>() {
                            @Override
                            public String call() throws Exception {
                                return auditFilterPolicyManager.evaluateAuditViewerPolicy(detailParams[0], null);
                            }
                        });
                    } catch (Exception e) {
                        final String msg = "Exception processing audit viewer policy: " + ExceptionUtils.getMessage(e);
                        //noinspection ThrowableResultOfMethodCallIgnored
                        logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                        addInvokeAuditViewerAuditMsg(auditMessages, msg, Level.WARNING);
                    }
                }
            }

            return null;
        } finally {
            publishInvokeAuditViewerAudits(auditMessages);
        }
    }

    private void publishInvokeAuditViewerAudits(List<Pair<Level, String>> auditMessages) {
        if (!auditMessages.isEmpty()) {
            for (final Pair<Level, String> auditMessage : auditMessages) {
                applicationContext.publishEvent(new AdminEvent(this, auditMessage.right) {
                    @Override
                    public Level getMinimumLevel() {
                        return auditMessage.left;
                    }
                });
            }
        }
    }

    private void addInvokeAuditViewerAuditMsg(final List<Pair<Level, String>> auditMessages,
                                              final String params){
        addInvokeAuditViewerAuditMsg(auditMessages, params, Level.INFO);
    }

    private void addInvokeAuditViewerAuditMsg(final List<Pair<Level, String>> auditMessages,
                                              final String params,
                                              final Level level){
        final String avPolicyFailedAudit = "Audit viewer policy failed. {0}";
        auditMessages.add(new Pair<Level, String>(
                level,
                MessageFormat.format(avPolicyFailedAudit, params)));
    }
    
    /**
     * Temporary object which will hold the audit view event and administrative information which fired the audit
     * view event.
     */
    private class AuditViewData {
        private final AdminInfo adminInfo;
        private final List<AuditSearchCriteria> criteria = Collections.synchronizedList( new ArrayList<AuditSearchCriteria>() );
        private volatile long lastTimeChecked;

        private AuditViewData( final AdminInfo adminInfo,
                               final AuditSearchCriteria criteria ) {
            this.adminInfo = adminInfo;
            this.criteria.add( criteria );
            this.lastTimeChecked = System.currentTimeMillis();
        }

        public AdminInfo getAdminInfo() {
            return adminInfo;
        }

        public boolean isStale() {
            return (System.currentTimeMillis() - lastTimeChecked) > TimeUnit.HOURS.toMillis(1);
        }

        public boolean isNewCriteria( final AuditSearchCriteria criteria ) {
            lastTimeChecked = System.currentTimeMillis();

            boolean newCriteria = true;
            synchronized( this.criteria ) {
                for ( final AuditSearchCriteria previousCriteria : this.criteria ) {
                    if ( previousCriteria.containsSimilarCritiera(criteria) ) {
                        newCriteria = false;
                        break;
                    }
                }

                if ( newCriteria ) {
                    this.criteria.add( criteria );
                }

                // remove older criteria if maximum exceeded
                while ( this.criteria.size() > MAX_CRITERIA_HISTORY ) {
                    this.criteria.remove( 0 );
                }
            }

            return newCriteria;
        }
    }
}