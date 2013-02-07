package com.l7tech.server.audit;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.jdbc.InvalidPropertyException;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.uddi.UDDIPublishStatus;
import com.l7tech.gateway.common.uddi.UDDIServiceControlRuntime;
import com.l7tech.identity.User;
import com.l7tech.identity.fed.FederatedGroupMembership;
import com.l7tech.identity.internal.InternalGroupMembership;
import com.l7tech.objectmodel.*;
import com.l7tech.server.*;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.admin.AdminEvent;
import com.l7tech.server.event.admin.AuditViewGatewayAuditsData;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.sharedkey.SharedKeyRecord;
import com.l7tech.server.sla.CounterRecord;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.*;
import org.apache.commons.collections.map.LRUMap;
import org.hibernate.EntityMode;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.event.AdminInfo.find;

/**
 * Implementation of AuditAdmin in SSG.
 */
public class AuditAdminImpl extends AsyncAdminMethodsImpl implements AuditAdmin, InitializingBean, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(AuditAdminImpl.class.getName());
    private static final String CLUSTER_PROP_LAST_AUDITACK_TIME = "audit.acknowledge.highestTime";
    private static final int MAX_CRITERIA_HISTORY = ConfigFactory.getIntProperty( "com.l7tech.server.audit.maxAuditSearchCriteria", 20 );
    private static final String MAX_AUDIT_DATA_CACHE_SIZE = "com.l7tech.server.audit.maxAuditDataCacheSize";
    private static final int CLEANUP_DELAY = ConfigFactory.getIntProperty( "com.l7tech.server.audit.auditDataCacheDelay", 120000 );
    private static final long CLEANUP_PERIOD = ConfigFactory.getLongProperty( "com.l7tech.server.audit.auditDataCachePeriod", 60000 );
    private static final Collection<String> ignoredEntityClassNames = Arrays.asList(new String[] {
        // If any entity is found as not relevant for auditing, add it into this list.
        AuditRecord.class.getName(),
        CounterRecord.class.getName(),
        SharedKeyRecord.class.getName(),
        UDDIPublishStatus.class.getName(),
        UDDIServiceControlRuntime.class.getName(),
        FederatedGroupMembership.class.getName(),
        InternalGroupMembership.class.getName(),
    });

    // map of IdentityHeader -> AuditViewData
    private final LRUMap auditedData = new LRUMap( ConfigFactory.getIntProperty( MAX_AUDIT_DATA_CACHE_SIZE, 100 ) );

    private AuditDownloadManager auditDownloadManager;
    private AuditRecordManager auditRecordManager;
    private AuditLookupPolicyEvaluator auditLookupPolicyEvaluator;
    private SecurityFilter filter;
    private Config config;
    private ClusterPropertyManager clusterPropertyManager;
    private AuditArchiver auditArchiver;
    private ApplicationContext applicationContext;
    private AuditFilterPolicyManager auditFilterPolicyManager;
    private GatewayKeyAccessFilter keyAccessFilter;
    private PersistenceEventInterceptor persistenceEventInterceptor;
    private SessionFactory sessionFactory;
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

    public void setAuditLookupPolicyEvaluator(AuditLookupPolicyEvaluator auditLookupPolicyEvaluator) {
        this.auditLookupPolicyEvaluator = auditLookupPolicyEvaluator;
    }

    public void setSecurityFilter(SecurityFilter filter) {
        this.filter = filter;
    }

    public void setServerConfig(Config config ) {
        this.config = config;
    }

    public void setClusterPropertyManager(ClusterPropertyManager clusterPropertyManager) {
        this.clusterPropertyManager = clusterPropertyManager;
    }

    public void setAuditArchiver(AuditArchiver auditArchiver) {
        this.auditArchiver = auditArchiver;
    }

    public void setPersistenceEventInterceptor(PersistenceEventInterceptor persistenceEventInterceptor) {
        this.persistenceEventInterceptor = persistenceEventInterceptor;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        auditor = new Auditor(this, applicationContext, logger);
    }

    @Override
    public AuditRecord findByPrimaryKey( String id, boolean fromInternal) throws FindException {
        if(fromInternal){
            try{
                long oid = Long.parseLong(id);
                return auditRecordManager.findByPrimaryKey(oid);
            }catch (NumberFormatException e){
                throw new FindException("Invalid oid:"+id, e);
            }
        }
        else
            return auditLookupPolicyEvaluator.findByGuid(id);
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
    public List<AuditRecordHeader> findHeaders(AuditSearchCriteria criteria) throws FindException{
        notifyAuditSearch(criteria);
        if(criteria.getFromPolicy)
            return auditLookupPolicyEvaluator.findHeaders(criteria);
        else
            return auditRecordManager.findHeaders(criteria);
    }

    @Override
    public Map<String, byte[]> getDigestsForAuditRecords(Collection<String> auditRecordIds, boolean fromPolicy) throws FindException {
        if(fromPolicy){
            return auditLookupPolicyEvaluator.getDigestForAuditRecords(auditRecordIds);
        }
        else{
            return auditRecordManager.getDigestForAuditRecords(auditRecordIds);
        }
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
                else {
                    // try external audits
                    newAudits = auditLookupPolicyEvaluator.findHeaders(criteria);
                    newAudits = filter.filter(newAudits, user, OperationType.READ, null );
                    if ( newAudits.size() > 0 ) {
                        newAuditTime = newAudits.iterator().next().getTimestamp();
                    }
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
    public boolean isAuditArchiveEnabled(){
        return auditArchiver.isEnabled();
    }

    @Override
    public ClusterProperty getFtpAuditArchiveConfig() {
        ClusterProperty result = null;
        try {
            result = clusterPropertyManager.findByUniqueName( ServerConfigParams.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION);
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error getting cluster property: " + fe.getMessage(), ExceptionUtils.getDebugException( fe ) );
        }

        return result != null ? result : new ClusterProperty( ServerConfigParams.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION, null);
    }

    @Override
    public void setFtpAuditArchiveConfig(ClusterProperty prop) throws UpdateException {

        if (prop == null || ! ServerConfigParams.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION.equals(prop.getName()))
            throw new UpdateException("Invalid cluster property provided for FTP archiver configuration: " + prop);

        // bug #6574 - error calling update() for the first time to set the cluster property
        //             for ftp archiver -- Call save to create the first time
        ClusterProperty cp;
        try {
            cp = clusterPropertyManager.findByUniqueName( ServerConfigParams.PARAM_AUDIT_ARCHIVER_FTP_DESTINATION);
        } catch (FindException fe) {
            cp = null;
        }

        if (cp != null) {
            prop.setOid(cp.getOid());
            clusterPropertyManager.update(prop);
        } else {
            try {
                clusterPropertyManager.save(prop);
            } catch (SaveException e) {
                throw new UpdateException(e);
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
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error getting cluster property '" + CLUSTER_PROP_LAST_AUDITACK_TIME + "' message is '" + fe.getMessage() + "'.", ExceptionUtils.getDebugException( fe ) );
        } catch (NumberFormatException nfe) {
            logger.log( Level.WARNING, "Error getting cluster property '" + CLUSTER_PROP_LAST_AUDITACK_TIME + "' invalid long value '"+value+"'.", ExceptionUtils.getDebugException( nfe ));
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
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error getting cluster property '" + CLUSTER_PROP_LAST_AUDITACK_TIME + "' message is '" + fe.getMessage() + "'.", ExceptionUtils.getDebugException( fe ) );
        } catch (SaveException se) {
            logger.log( Level.WARNING, "Error saving cluster property '" + CLUSTER_PROP_LAST_AUDITACK_TIME + "'.", se);
        } catch(UpdateException ue) {
            logger.log( Level.WARNING, "Error updating cluster property '" + CLUSTER_PROP_LAST_AUDITACK_TIME + "'.", ue);
        }

        return date;
    }


    @Override
    public int getSystemLogRefresh() {
        int refreshInterval = 0;
        int defaultRefreshInterval = 3;
        String valueInSecsStr = config.getProperty( ServerConfigParams.PARAM_AUDIT_REFRESH_PERIOD_SECS );
        if(valueInSecsStr!=null) {
            try {
                refreshInterval = Integer.parseInt(valueInSecsStr);
            }
            catch(NumberFormatException nfe) {
                refreshInterval = defaultRefreshInterval;
                logger.warning("Property '"+ ServerConfigParams.PARAM_AUDIT_REFRESH_PERIOD_SECS +"' has invalid value '"+valueInSecsStr
                        +"', using default '"+defaultRefreshInterval+"'.");
            }
        }
        else {
            refreshInterval = defaultRefreshInterval;
        }

        return refreshInterval;
    }

    @Override
    public Level serverMessageAuditThreshold() {
        return getAuditLevel( ServerConfigParams.PARAM_AUDIT_MESSAGE_THRESHOLD, "message", DefaultAuditThresholds.DEFAULT_MESSAGE_THRESHOLD);
    }

    @Override
    public Level serverDetailAuditThreshold() {
        return getAuditLevel( ServerConfigParams.PARAM_AUDIT_ASSOCIATED_LOGS_THRESHOLD, "detail", DefaultAuditThresholds.DEFAULT_ASSOCIATED_LOGS_THRESHOLD);
    }

    private Level getAuditLevel(String serverConfigParam, String which, Level defaultLevel) {
        // todo: consider moving this and the same code from AuditContextImpl in ServerConfig
        String msgLevel = config.getProperty( serverConfigParam );
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
        String sAge = config.getProperty( ServerConfigParams.PARAM_AUDIT_PURGE_MINIMUM_AGE );
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
    public int getMaxDigestRecords() {
        return auditRecordManager.getAuditValidatedConfig().getIntProperty( ServerConfigParams.PARAM_AUDIT_SIGN_MAX_VALIDATE, 100);
    }

    @Override
    public boolean isAuditViewerPolicyAvailable() {
        return auditFilterPolicyManager.isAuditViewerPolicyAvailable();
    }

    @Override
    public String invokeAuditViewerPolicyForMessage(long auditRecordId, final boolean isRequest)
            throws FindException, AuditViewerPolicyNotAvailableException {

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

            return evaluatePolicy(messageXml, isRequest, auditMessages);
        } finally {
            publishInvokeAuditViewerAudits(auditMessages);
        }
    }

    @Override
    public String invokeAuditViewerPolicyForDetail(long auditRecordId, long ordinal) throws FindException, AuditViewerPolicyNotAvailableException {

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
                    return evaluatePolicy(detailParams[0], null, auditMessages);
                }
            }

            return null;
        } finally {
            publishInvokeAuditViewerAudits(auditMessages);
        }
    }

    @Override
    public Collection<String> getAllEntityClassNames() {
        Collection<String> entityClassNamesList = new ArrayList<String>();
        Map<String, ClassMetadata> classMetaDataMap = sessionFactory.getAllClassMetadata();

        final Set<String> ignoredClassNames = persistenceEventInterceptor.getIgnoredClassNames();
        final Set<String> noAuditClassNames = persistenceEventInterceptor.getNoAuditClassNames();

        Class entityClass;
        for (String className: classMetaDataMap.keySet()) {
            entityClass = classMetaDataMap.get(className) == null?
                null : classMetaDataMap.get(className).getMappedClass(EntityMode.POJO);

            if (!ignoredEntityClassNames.contains(className) &&
                ignoredClassNames != null && !ignoredClassNames.contains(className) &&
                noAuditClassNames != null && !noAuditClassNames.contains(className) &&
                entityClass != null && EntityType.findTypeByEntity(entityClass).isDisplayedInGui()) {

                entityClassNamesList.add(className);
            }
        }

        return entityClassNamesList;
    }

    private String evaluatePolicy(final String messageXml,
                                  final Boolean isRequest,
                                  final List<Pair<Level, String>> auditMessages) throws AuditViewerPolicyNotAvailableException {
        try {
            return keyAccessFilter.doWithRestrictedKeyAccess(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return auditFilterPolicyManager.evaluateAuditViewerPolicy(messageXml, isRequest);
                }
            });
        } catch (AuditViewerPolicyNotAvailableException e) {
            final String params = ExceptionUtils.getMessage(e);
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.FINE, params, ExceptionUtils.getDebugException(e));
            addInvokeAuditViewerAuditMsg(auditMessages, params);
            throw e;//clients want to know about this issue explicitly
        } catch (AuditPolicyException e) {
            final String params = ExceptionUtils.getMessage(e);
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.FINE, params, ExceptionUtils.getDebugException(e));
            addInvokeAuditViewerAuditMsg(auditMessages, params);
        } catch (Exception e) {//only runtime, side effect of callable above.
            final String params = "Exception processing audit viewer policy: " + ExceptionUtils.getMessage(e);
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.FINE, params, ExceptionUtils.getDebugException(e));
            addInvokeAuditViewerAuditMsg(auditMessages, params);
        }

        return null;
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
        auditMessages.add(new Pair<Level, String>( level,params));
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

    private static final String sqlPath = "../config/etc/sql/externalAudits.sql";

    private ServerConfig serverConfig() {
        return ServerConfig.getInstance();
    }

    private String getJdbcDbType(String connectionDriverClass){
        String type = null;

        if(connectionDriverClass.contains("mysql"))
            type = "mysql";
        else if(connectionDriverClass.contains("sqlserver"))
            type = "sqlserver";
        else if(connectionDriverClass.contains("oracle"))
            type = "oracle";
        else if(connectionDriverClass.contains("db2"))
            type = "db2";
        return type;
    }

    @Override
    public String getExternalAuditsSchema(String connectionName, String auditRecordTableName, String auditDetailTableName){
        JdbcConnectionManager jdbcConnectionManager = (JdbcConnectionManager)applicationContext.getBean("jdbcConnectionManager");

        FileInputStream fin = null;
        ByteArrayOutputStream out = null;
        try {

            JdbcConnection connection = jdbcConnectionManager.getJdbcConnection(connectionName);
            String driverClass = connection.getDriverClass().toLowerCase();

            // get db type and translate schema
            String type =getJdbcDbType(driverClass);

            if(type == null){
                return "This database type is not supported.  Driver class:"+driverClass;
            }

            final File configDir = serverConfig().getLocalDirectoryProperty( ServerConfigParams.PARAM_SSG_HOME_DIRECTORY, false);
            File schemaFile = new File(configDir,"../../config/etc/sql/externalAudits_"+type+".sql");
            fin = new FileInputStream(schemaFile);
            out = new ByteArrayOutputStream(16384);

            IOUtils.copyStream(fin,out);
            String schemaString = out.toString(Charset.defaultCharset().name());
            schemaString = schemaString.replace("audit_main",auditRecordTableName);
            schemaString = schemaString.replace("audit_detail",auditDetailTableName);
            return schemaString;

        } catch (FileNotFoundException e) {
            logger.warning("Schema file not found: "+sqlPath);
        } catch (IOException e) {
            logger.warning("Error reading schema file: "+sqlPath);
        } catch (FindException e) {
            logger.warning("Jdbc connection not found:" +connectionName);
        }

        finally {
            ResourceUtils.closeQuietly(fin,out);
        }
        return null;
    }


    @Override
    public  AsyncAdminMethods.JobId<String> testAuditSinkSchema(final String connectionName, final String auditRecordTableName, final String auditDetailTableName){
        final FutureTask<String> queryTask = new FutureTask<String>( find( false ).wrapCallable( new Callable<String>(){
            @Override
            public String call() throws Exception {

                final JdbcQueryingManager jdbcQueryingManager = (JdbcQueryingManager)applicationContext.getBean("jdbcQueryingManager");
                final JdbcConnectionPoolManager jdbcConnectionPoolManager = (JdbcConnectionPoolManager)applicationContext.getBean("jdbcConnectionPoolManager");

                final DefaultKey defaultKey = (DefaultKey)applicationContext.getBean("defaultKey");

                DataSource ds ;
                try{
                    ds = jdbcConnectionPoolManager.getDataSource(connectionName);
                }catch (NamingException e){
                    return "Failed to retrieve connection data source.";
                }

                TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(ds));
                return transactionTemplate.execute(new TransactionCallback<String>() {
                    @Override
                    public String doInTransaction(TransactionStatus transactionStatus) {
                        try {
                            String result = ExternalAuditsUtils.testMessageSummaryRecord(connectionName, auditRecordTableName, auditDetailTableName, jdbcQueryingManager, defaultKey);
                            if(!result.isEmpty()){
                                transactionStatus.setRollbackOnly();
                                return result;
                            }
                            result =  ExternalAuditsUtils.testAdminAuditRecord(connectionName, auditRecordTableName, jdbcQueryingManager,defaultKey);
                            if(!result.isEmpty()){
                                transactionStatus.setRollbackOnly();
                                return result;
                            }
                            result  =  ExternalAuditsUtils.testSystemAuditRecord(connectionName, auditRecordTableName,jdbcQueryingManager,defaultKey);
                            if(!result.isEmpty())
                                transactionStatus.setRollbackOnly();
                            return result;
                        } catch (Exception e) {
                            transactionStatus.setRollbackOnly();
                            return e.getMessage();
                        }
                    }
                });
            }

        }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                queryTask.run();
            }
        }, 0L);

        return registerJob( queryTask, String.class);
    }

    @Override
    public AsyncAdminMethods.JobId<String> createExternalAuditDatabaseTables(
            final String connectionName,final  String auditRecordTableName, final String auditDetailTableName,final String username, final String  password) {
        final FutureTask<String> queryTask = new FutureTask<String>( find( false ).wrapCallable( new Callable<String>(){
            @Override
            public String call() throws Exception {

                final JdbcQueryingManager jdbcQueryingManager = (JdbcQueryingManager)applicationContext.getBean("jdbcQueryingManager");
                final JdbcConnectionPoolManager jdbcConnectionPoolManager = (JdbcConnectionPoolManager)applicationContext.getBean("jdbcConnectionPoolManager");
                final JdbcConnectionManager jdbcConnectionManager = (JdbcConnectionManager)applicationContext.getBean("jdbcConnectionManager");


                final String schema = getExternalAuditsSchema(connectionName,auditRecordTableName,auditDetailTableName);


                final DataSource ds;
                try{
                    // create temp datasource with new credentials
                    JdbcConnection connection = jdbcConnectionManager.getJdbcConnection(connectionName);
                    connection.setUserName(username);
                    connection.setPassword(password);

                    ds = jdbcConnectionPoolManager.getTestConnectionPool(connection);
                }catch (FindException e){
                    return "Failed to retrieve jdbc connection: "+e.getMessage();
                }catch (InvalidPropertyException e){
                    return "Invalid jdbc connection configuration: "+e.getMessage();
                }


                TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(ds));
                return transactionTemplate.execute(new TransactionCallback<String>() {
                    @Override
                    public String doInTransaction(TransactionStatus transactionStatus) {

                    int index = 0;
                    int oldIndex = 0;
                    while(schema.indexOf(";",index)>0){
                        oldIndex = index+1;
                        index = schema.indexOf(";",index);

                        String query = schema.substring(oldIndex-1,index);
                        index++;
                        Object result = jdbcQueryingManager.performJdbcQuery(ds,query,null,2,Collections.emptyList());
                        if(result instanceof String){
                            transactionStatus.setRollbackOnly();
                            return (String)result;

                        }
                    }
                    return "";
                    }
                });
            }
        }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                queryTask.run();
            }
        }, 0L);

        return registerJob(queryTask, String.class);
    }
}
