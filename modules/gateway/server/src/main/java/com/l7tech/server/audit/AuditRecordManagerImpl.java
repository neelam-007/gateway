package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.util.*;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.event.admin.AuditPurgeInitiated;
import com.l7tech.server.event.system.AuditPurgeEvent;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import javax.inject.Provider;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the finding and saving of {@link AuditRecord}s.
 *
 * Note that once an AuditRecord is saved, it must not be deleted or updated.
 *
 * @author alex
 * @version $Revision$
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class AuditRecordManagerImpl
        extends HibernateEntityManager<AuditRecord, AuditRecordHeader>
        implements AuditRecordManager, ApplicationContextAware, PropertyChangeListener
{
    private static final String SQL_GET_MIN_OID = "SELECT MIN(objectid) FROM audit_main WHERE objectid > ?";
    private static final String SQL_INNODB_DATA = "SHOW VARIABLES LIKE 'innodb_data_file_path'";
    private static final String SQL_CURRENT_USAGE = "SHOW TABLE STATUS";

    private static final String IDS_PARAMETER = "ids";
    private static final String MAX_SIZE_PARAMETER = "maxSize";
    /**
     * Concrete class tables are outer joined and hibernate generates correct sql to safely reference message_audit only
     * properties.
     */
    private static final String HQL_SELECT_AUDIT_RECORDS_SIZE_PROTECTED = "from AuditRecord where oid in (:"+IDS_PARAMETER+") and (requestXml is null or length(requestXml) < :"+MAX_SIZE_PARAMETER+") and (responseXml is null or length(responseXml) < :"+MAX_SIZE_PARAMETER+")";

    private ValidatedConfig validatedConfig;

    //- PUBLIC

    @Override
    public AuditRecord findByPrimaryKey(final long oid) throws FindException {
        final AuditRecord found = super.findByPrimaryKey(oid);
        if (found != null && found instanceof MessageSummaryAuditRecord) {
            final MessageSummaryAuditRecord messageSummary = (MessageSummaryAuditRecord) found;
            final PublishedService cachedService = serviceCache.get().getCachedService(messageSummary.getServiceOid());
            if (cachedService != null) {
                messageSummary.setSecurityZone(cachedService.getSecurityZone());
            }
        }
        return found;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        if(this.applicationContext != null) throw new IllegalStateException("applicationContext is already initialized.");
        this.applicationContext = applicationContext;
    }

    @Override
    public Map<String, byte[]> getDigestForAuditRecords(final Collection<String> auditRecordIds) throws FindException {
        if(auditRecordIds == null) throw new NullPointerException("auditRecordIds is null");

        final Map<String, byte[]> returnMap = new HashMap<String, byte[]>();

        if (auditRecordIds.isEmpty()) {
            return returnMap;
        }

        Session session = null;
        try {

            final int maxRecords = validatedConfig.getIntProperty( ServerConfigParams.PARAM_AUDIT_SIGN_MAX_VALIDATE, 100);

            if (auditRecordIds.size() > maxRecords) {
                int difference = auditRecordIds.size() - maxRecords;
                final Iterator<String> iterator = auditRecordIds.iterator();
                int index = 0;
                while (iterator.hasNext() && index < difference) {
                    iterator.remove();
                    index++;
                }
                logger.log(Level.INFO, "Number of audits to digest reduced to limit of " + maxRecords +" from " + auditRecordIds.size());
            }

            session = getSession();
            // Note: this produces exactly the same set of queries as session.createCriteria(interfaceClass).scroll()
            // This query must perform multiple queries to avoid cartesian product as there are several possible collections
            // to fill in the audit record hierarchy.
            final Query query = session.createQuery(HQL_SELECT_AUDIT_RECORDS_SIZE_PROTECTED);
            Collection<Long> auditRecordIdNumbers = new ArrayList<Long>();
            for(String idStr : auditRecordIds){
                try{ auditRecordIdNumbers.add(Long.parseLong(idStr));}
                catch(NumberFormatException e){}// ignore
            }
            query.setParameterList(IDS_PARAMETER, auditRecordIdNumbers);
            final int maxMsgSize = validatedConfig.getIntProperty( ServerConfigParams.PARAM_AUDIT_SEARCH_MAX_MESSAGE_SIZE, 2621440);
            query.setInteger(MAX_SIZE_PARAMETER, maxMsgSize);

            final ScrollableResults results = query.scroll();

            while (results.next()) {
                AuditRecord record = (AuditRecord) results.get(0);
                String sig = record.getSignature();
                if (sig != null && !sig.isEmpty()) {
                    final byte[] digest = record.computeSignatureDigest();
                    returnMap.put(Long.toString(record.getOid()), digest);
                }
                session.evict(record);
            }
        } catch ( HibernateException e ) {
            throw new FindException("Couldn't find Audit Records", e);
        } finally {
            releaseSession(session);
        }

        return returnMap;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditRecordHeader> findHeaders(final AuditSearchCriteria criteria) throws FindException {
        final Class findClass = getFindClass(criteria);

        // Enable discover of whether an audit is a message audit through the presence of not null request id property.
        // If this property is not available then the audit record is not a message audit.
        // This property can only be searched for when it may exist. If it will not exist e.g. were looking for admin
        // or system records, then it cannot be included in the projection or in the result processing.
        final boolean hasRequestIdProperty = findClass == getInterfaceClass() || findClass == MessageSummaryAuditRecord.class;

        final Functions.UnaryVoid<Criteria> criteriaConfigurator = new Functions.UnaryVoid<Criteria>(){
            @Override
            public void call(final Criteria hibernateCriteria) {

                final ProjectionList projectionList = Projections.projectionList()
                        .add(Property.forName(PROP_OID))
                        .add(Property.forName(PROP_NAME))
                        .add(Property.forName(PROP_MESSAGE))
                        .add(Property.forName(PROP_SIGNATURE))
                        .add(Property.forName(PROP_NODEID))
                        .add(Property.forName(PROP_TIME))
                        .add(Property.forName(PROP_LEVEL))
                        ;

                if (hasRequestIdProperty) {
                    projectionList.add(Property.forName(PROP_REQUEST_ID));
                }
                if (criteria.operation !=null ) {
                    projectionList.add(Property.forName(PROP_OPERATION));
                }

                if (criteria.messageId != null) {
                    //ensure distinct results only - only an issue when joining with audit_detail
                    hibernateCriteria.setProjection(Projections.distinct(projectionList));

                    hibernateCriteria.createAlias("details", "ad");
                    final SimpleExpression eq = Restrictions.eq("ad.messageId", criteria.messageId);
                    hibernateCriteria.add(eq);
                } else {
                    hibernateCriteria.setProjection(projectionList);
                }
            }
        };

        //todo: Could a ResultTransformer be useful here and simplify the mapping of results to AuditRecordHeader?
        List<AuditRecordHeader> auditRecordHeaders = find(criteria, criteriaConfigurator, new Functions.BinaryVoid<List<AuditRecordHeader>, Object>() {
            @Override
            public void call(List<AuditRecordHeader> auditRecordHeaders, Object o) {
                final Object [] values = (Object[]) o;

                boolean isMessageAudit = hasRequestIdProperty && values[7] != null;

                final Long id = Long.valueOf(values[0].toString());
                final String name = (values[1] == null || !isMessageAudit) ? null : values[1].toString();
                final String description = values[2].toString();
                final String signature = (values[3] == null) ? null : values[3].toString();
                final String nodeId = values[4].toString();
                final Long timestamp = Long.valueOf(values[5].toString());
                final Level level = Level.parse(values[6].toString());

                auditRecordHeaders.add(new AuditRecordHeader(
                        id,
                        name,
                        description,
                        null,
                        signature,
                        nodeId,
                        timestamp,
                        level,
                        0 //version property is not used for AuditRecord or sub classes!
                ));
            }
        });

        return Collections.unmodifiableList(auditRecordHeaders);
    }

    @Override
    public AuditRecord findByHeader(EntityHeader header) throws FindException {
        return super.findByHeader(header);
    }

    private <T> List<T> find( final AuditSearchCriteria criteria,
                              final Functions.UnaryVoid<Criteria> criteriaConfigurator,
                              final Functions.BinaryVoid<List<T>, Object> resultProcessor) throws FindException {
        if (criteria == null) throw new IllegalArgumentException("Criteria must not be null");

        int maxRecords = getMaxRecords(criteria);
        List<T> result = new ArrayList<T>(maxRecords);

        // If search criteria contain conflicts (for example, Request ID and Entity ID are specified in "criteria"),
        // then no search results will be returned.
        if (hasConflictSearchCriteria(criteria)) return result;

        Session session = null;
        try {
            session = getSession();

            final Criteria hibernateCriteria = getHibernateCriteriaFromAuditCriteria(criteria, session, maxRecords);
            if (criteriaConfigurator != null) {
                criteriaConfigurator.call(hibernateCriteria);
            }

            ScrollableResults results = hibernateCriteria.scroll();
            while( results.next() ) {
                //note if a project is used, this will be an array, otherwise it will be the entity type.
                //todo: refactor as only used for projections
                final Object object = results.get();
                resultProcessor.call(result, object);
                session.evict(object);
            }

            return result;
        } catch ( HibernateException e ) {
            throw new FindException("Couldn't find Audit Records", e);
        } finally {
            releaseSession(session);
        }
    }

    private int getMaxRecords(AuditSearchCriteria criteria) {
        int maxRecords = criteria.maxRecords;
        if (maxRecords <= 0) maxRecords = 4096; //todo: this default is different to UI default. Should be consistent.
        return maxRecords;
    }

    private Criteria getHibernateCriteriaFromAuditCriteria(final AuditSearchCriteria criteria,
                                                           final Session session,
                                                           final int maxRecords) throws FindException {
        Class findClass = getFindClass(criteria);

        Criteria hibernateCriteria = session.createCriteria(findClass);
        hibernateCriteria.setMaxResults(maxRecords);

        for ( Criterion criterion : asCriterion( criteria ) ) {
            hibernateCriteria.add( criterion );
        }

        hibernateCriteria.addOrder(Order.desc(PROP_TIME));

        return hibernateCriteria;
    }

    private Class getFindClass(AuditSearchCriteria criteria) {
        Class findClass = criteria.recordClass;
        if (findClass == null) findClass = getInterfaceClass();

        if(criteria.requestId != null && findClass != MessageSummaryAuditRecord.class) findClass = MessageSummaryAuditRecord.class;
        return findClass;
    }

    /**
     * Check if search criteria have conflict.  The "conflict" means that Request ID and Entity Search Parameters can exist at the same time in the search criteria.
     * @param criteria: the search criteria.
     * @return true if the search criteria include Request ID and Entity Search Parameters.
     */
    private boolean hasConflictSearchCriteria(final AuditSearchCriteria criteria) {
        final boolean requestIdEnabled = criteria.requestId != null && !criteria.requestId.trim().isEmpty();
        final boolean entityClassEnabled = criteria.entityClassName != null && !criteria.entityClassName.trim().isEmpty();
        final boolean entityIdEnabled = criteria.entityId != null;

        return requestIdEnabled && (entityClassEnabled || entityIdEnabled);
    }

    @Override
    protected AuditRecordHeader newHeader(final AuditRecord auditRecord) {
        AuditRecordHeader arh = new AuditRecordHeader(auditRecord);
        String sig = auditRecord.getSignature();
        arh.setSignatureDigest(sig != null && sig.length() > 0 ? auditRecord.computeSignatureDigest() : null);
        return arh;
    }

    @Override
    public int findCount( final AuditSearchCriteria criteria ) throws FindException {
        return super.findCount( criteria.recordClass, asCriterion(criteria) );
    }

    @Override
    public Collection<AuditRecord> findPage( final SortProperty sortProperty,
                                             final boolean ascending,
                                             final int offset,
                                             final int count,
                                             final AuditSearchCriteria criteria) throws FindException {
        return super.findPage( criteria.recordClass, sortProperty.getPropertyName(), ascending, offset, count, asCriterion(criteria) );
    }

    @Override
    public void deleteOldAuditRecords( final long minAge ) throws DeleteException {
        applicationContext.publishEvent(new AuditPurgeInitiated(this));
        String sMinAgeHours = config.getProperty( ServerConfigParams.PARAM_AUDIT_PURGE_MINIMUM_AGE );
        if (sMinAgeHours == null || sMinAgeHours.length() == 0)
            sMinAgeHours = "168";
        int minAgeHours = 168;
        try {
            minAgeHours = Integer.valueOf(sMinAgeHours);
        } catch (NumberFormatException e) {
            logger.info( ServerConfigParams.PARAM_AUDIT_PURGE_MINIMUM_AGE + " value '" + sMinAgeHours +
                    "' is not a valid number. Using " + minAgeHours + " instead.");
        }

        final long systemMinAge =  minAgeHours * 60 * 60 * 1000;
        Runnable runnable = new DeletionTask( System.currentTimeMillis() -  Math.max(systemMinAge, minAge) );

        new Thread(runnable).start();
    }

    @Override
    public long getMinOid(long lowerLimit) throws SQLException {
        final Session session = getSession();
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            final Connection conn = session.connection();
            stmt = conn.prepareStatement(SQL_GET_MIN_OID);
            stmt.setLong(1, lowerLimit);
            rs = stmt.executeQuery();
            if (rs.next()) {
                String min = rs.getString(1);
                if (min == null || "null".equalsIgnoreCase(min)) {
                    logger.log(Level.FINE, "Min audit record object id not retrieved (table empty?).");
                } else {
                    return Long.parseLong(min);
                }
            }
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(stmt);
            releaseSession(session);
        }
        return -1;
    }

    @Override
    public int deleteRangeByOid(final long start, final long end) throws SQLException {
        final Session session = getSession();
        PreparedStatement deleteStmt = null;
        try {
            final Connection conn = session.connection();
            deleteStmt = conn.prepareStatement("DELETE FROM audit_main WHERE objectid >= ? AND objectid <= ? LIMIT 10000");
            deleteStmt.setLong(1, start);
            deleteStmt.setLong(2, end);
            return deleteStmt.executeUpdate();
        } finally {
            ResourceUtils.closeQuietly(deleteStmt);
            releaseSession(session);
        }
    }

    /**
     * Gets the autoextend:max defined for the innodb table space, from the innodb_data_file_paths MySQL variable.
     *
     * @return  Max table space size in bytes, or -1 if not defined
     * @throws FindException if an error was encountered and the value could not be retrieved
     */
    @Override
    public long getMaxTableSpace() throws FindException {
        final Session session = getSession();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            final Connection conn = session.connection();
            statement = conn.prepareStatement(SQL_INNODB_DATA);
            rs = statement.executeQuery();

            if (rs != null && rs.next()) {
                String innodbData = rs.getString("value");
                return SqlUtils.getMaxTableSize(innodbData);
            }

            return -1; // rs empty

        } catch (SQLException e) {
            throw new FindException("Error retrieving max space allocated for the innodb tablespace.", e);
        } catch (NumberFormatException ne) {
            throw new FindException("Error retrieving max space allocated for the innodb tablespace.", ne);
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(statement);
            releaseSession(session);
        }
    }

    @Override
    public long getCurrentUsage() throws FindException {
        final Session session = getSession();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            final Connection conn = session.connection();
            statement = conn.prepareStatement(SQL_CURRENT_USAGE);
            rs = statement.executeQuery();
            if (rs != null) {
                long data_length = 0L;
                long index_length = 0L;
                while (rs.next()) {
                    data_length += rs.getLong("data_length");
                    index_length += rs.getLong("index_length");
                }
                long usage = data_length + index_length;
                logger.log(Level.FINE, "Current usage: ''{0}'' bytes", usage);
                return usage;
            }
        } catch (SQLException e) {
            throw new FindException("Error retrieving max space allocated for the innodb tablespace.", e);
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(statement);
            releaseSession(session);
        }

        // shouldn't happen
        throw new FindException("Error retrieving max space allocated for the innodb tablespace; no data retrieved.");
    }

    @Override
    public Class<AuditRecord> getImpClass() {
        return AuditRecord.class;
    }

    @Override
    public Class<AuditRecord> getInterfaceClass() {
        return AuditRecord.class;
    }

    @Override
    public String getTableName() {
        return "audit_main";
    }

    public void setServerConfig(Config config ) {
        this.config = config;
        validatedConfig = new ValidatedConfig( config, logger);
        validatedConfig.setMinimumValue( ServerConfigParams.PARAM_AUDIT_SIGN_MAX_VALIDATE, 100);
        validatedConfig.setMaximumValue( ServerConfigParams.PARAM_AUDIT_SIGN_MAX_VALIDATE, 1000);

        validatedConfig.setMinimumValue( ServerConfigParams.PARAM_AUDIT_SEARCH_MAX_MESSAGE_SIZE, 1024); // 1KB
        validatedConfig.setMaximumValue( ServerConfigParams.PARAM_AUDIT_SEARCH_MAX_MESSAGE_SIZE, 20971520); // 20MB

        validatedConfig.setMinimumValue( ServerConfigParams.PARAM_AUDIT_MESSAGE_LIMIT_SIZE, 0);
        validatedConfig.setMaximumValue( ServerConfigParams.PARAM_AUDIT_MESSAGE_LIMIT_SIZE, Long.MAX_VALUE);
        this.messageLimitSize = this.validatedConfig.getLongProperty( ServerConfigParams.PARAM_AUDIT_MESSAGE_LIMIT_SIZE, 10485760);  // 10MB
    }

    @Override
    public Config getAuditValidatedConfig() {
        return validatedConfig;
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    //- PRIVATE

    private static final Level[] LEVELS_IN_ORDER = { Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF };
    private static final String PROP_TIME = "millis";
    private static final String PROP_LEVEL = "strLvl";
    private static final String PROP_OID = "oid";
    private static final String PROP_NODEID = "nodeId";
    private static final String PROP_MESSAGE = "message";
    private static final String PROP_SIGNATURE = "signature";
    private static final String PROP_SERVICE_NAME = "name";
    private static final String PROP_REQUEST_ID = "strRequestId";
    private static final String PROP_OPERATION = "operationName";
    private static final String PROP_NAME = "name";
    private static final String PROP_PROV_ID = "identityProviderOid";
    private static final String PROP_USER_ID = "userId";
    private static final String PROP_USER_NAME = "userName";
    private static final String PROP_ENTITY_CLASS = "entityClassname";
    private static final String PROP_ENTITY_ID = "entityOid";

    private static final String DELETE_RANGE_START = "delete_range_start";
    private static final String DELETE_RANGE_END = "delete_range_end";

    private static final String DELETE_MYSQL = "DELETE FROM audit_main WHERE audit_level <> ? AND time < ? LIMIT 10000";
    private static final String DELETE_DERBY = "DELETE FROM audit_main where objectid in (SELECT objectid FROM (SELECT ROW_NUMBER() OVER() as rownumber, objectid FROM audit_main WHERE audit_level <> ?  and time < ?) AS foo WHERE rownumber <= 10000)";
    private static final AtomicBoolean mySql = new AtomicBoolean(true);

    private static final Logger logger = Logger.getLogger(AuditRecordManagerImpl.class.getName());
    private Config config;
    private ApplicationContext applicationContext;
    /**
     * Using Provider with Inject to work around circular dependency issues. ServiceCache will not be retrieved until it is needed.
     */
    @Inject
    private Provider<ServiceCache> serviceCache;
    private long messageLimitSize;

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (propertyName != null && propertyName.equals( ServerConfigParams.PARAM_AUDIT_MESSAGE_LIMIT_SIZE)) {
            this.messageLimitSize = this.validatedConfig.getLongProperty( ServerConfigParams.PARAM_AUDIT_MESSAGE_LIMIT_SIZE, 10485760);
        }
    }

    private static class AuditRecordHolder {
        private SystemAuditRecord auditRecord = null;
    }

    private Criterion[] asCriterion( final AuditSearchCriteria criteria ) throws FindException {
        List<Criterion> criterion = new ArrayList<Criterion>();

        Date fromTime = criteria.fromTime;
        Date toTime = criteria.toTime;

        if (fromTime != null) criterion.add(Restrictions.ge(PROP_TIME, fromTime.getTime()));
        if (toTime != null) criterion.add(Restrictions.lt(PROP_TIME, toTime.getTime()));

        Level fromLevel = criteria.fromLevel;
        if (fromLevel == null) fromLevel = Level.FINEST;
        Level toLevel = criteria.toLevel;
        if (toLevel == null) toLevel = Level.SEVERE;

        if (fromLevel.equals(toLevel)) {
            criterion.add(Restrictions.eq(PROP_LEVEL, fromLevel.getName()));
        } else if (fromLevel.equals( Level.ALL ) && toLevel.equals( Level.OFF )) {
            // no restriction
        } else {
            if (fromLevel.intValue() > toLevel.intValue())
                throw new FindException("fromLevel " + fromLevel.getName() + " is not lower in value than toLevel " + toLevel.getName());

            Set<String> levels = new HashSet<String>();
            for (Level level : LEVELS_IN_ORDER) {
                if (level.intValue() >= fromLevel.intValue() && level.intValue() <= toLevel.intValue()) {
                    levels.add(level.getName());
                }
            }
            criterion.add(Restrictions.in(PROP_LEVEL, levels));
        }

        // Note: a start or end constraint cannot be supplied with out an associated node id.
        // object ids do not increment across a cluster, they increment for a node.

        final Map<String, Long> nodeIdToStartMsg = criteria.nodeIdToStartMsg;
        if (!nodeIdToStartMsg.isEmpty()) {
            criterion.add(getObjectIdForNodeDisjunction(nodeIdToStartMsg, true));
        }

        final Map<String, Long> nodeIdToEndMsg = criteria.nodeIdToEndMsg;
        if (!nodeIdToEndMsg.isEmpty()) {
            criterion.add(getObjectIdForNodeDisjunction(nodeIdToEndMsg, false));
        }

        if (criteria.requestId != null) criterion.add(Restrictions.ilike(PROP_REQUEST_ID, criteria.requestId));
        if (criteria.operation != null) criterion.add(Restrictions.ilike(PROP_OPERATION, criteria.operation));
        if (criteria.serviceName != null) criterion.add(Restrictions.ilike(PROP_SERVICE_NAME, criteria.serviceName));

        if (criteria.message != null) criterion.add(Restrictions.ilike(PROP_MESSAGE, criteria.message));
        if (criteria.nodeId != null) criterion.add(Restrictions.eq(PROP_NODEID, criteria.nodeId));

        if (criteria.user != null) {
            criterion.add(Restrictions.eq(PROP_PROV_ID, criteria.user.getProviderId()));
            criterion.add(Restrictions.eq(PROP_USER_ID, criteria.user.getId()));
        } else {
            if (criteria.userName != null) {
                criterion.add(Restrictions.ilike(PROP_USER_NAME, criteria.userName));
            }
            if (criteria.userIdOrDn != null) {
                criterion.add(Restrictions.ilike(PROP_USER_ID, criteria.userIdOrDn));
            }
        }

        // Entity Type and Entity ID search criteria are only applied to Audit Type, ANY and Admin.
        final Class auditRecordType = criteria.recordClass;
        if (auditRecordType == null || auditRecordType.equals(AdminAuditRecord.class)) {
            if (criteria.entityClassName != null && !criteria.entityClassName.trim().isEmpty()) {
                criterion.add(Restrictions.eq(PROP_ENTITY_CLASS, criteria.entityClassName));
            }

            if (criteria.entityId != null) {
                criterion.add(Restrictions.eq(PROP_ENTITY_ID, criteria.entityId));
            }
        }

        return criterion.toArray( new Criterion[criterion.size()] );
    }

    /**
     * Get a disjunction which contains all nodes in the supplied map. If the returned Disjunction is added to a criteria
     * then only results for nodes contained within nodeIdToObjectIdValue will be returned.
     *
     * Returned disjunction will represent the following SQL:
     * ((nodeid=? and objectid <|> ?) or (nodeid=? and objectid <|> ?)) , one AND block for each node id in the supplied map.
     *
     * @param nodeIdToObjectIdValue map of node id to an object id, which will have a constraint applied to it.
     * Cannot be null or empty.
     * @param greaterThan if true, then the constraint added is Restrictions.gt, otherwise Restrictions.lt
     * @return Disjunction to add to a hibernate criteria object.
     */
    private Disjunction getObjectIdForNodeDisjunction(final Map<String, Long> nodeIdToObjectIdValue, boolean greaterThan) {
        final Disjunction disjunction = Restrictions.disjunction();
        for (Map.Entry<String, Long> nodeToObjectId : nodeIdToObjectIdValue.entrySet()) {
            final Conjunction conjunction = Restrictions.conjunction();
            conjunction.add(Restrictions.eq(PROP_NODEID, nodeToObjectId.getKey()));
            if (greaterThan) {
                conjunction.add(Restrictions.gt(PROP_OID, nodeToObjectId.getValue()));
            } else {
                conjunction.add(Restrictions.lt(PROP_OID, nodeToObjectId.getValue()));
            }

            disjunction.add(conjunction);
        }

        return disjunction;
    }

    private class DeletionTask implements Runnable {
        private final AuditRecordHolder auditPurgeRecordHolder = new AuditRecordHolder();
        private final long maxTime;

        public DeletionTask(long maxTime) {
            this.maxTime = maxTime;
        }

        @Override
        public void run() {
            // Delete in batches of 10000 audit events. Otherwise a single delete of millions
            // will fail with socket timeout. (Bugzilla # 3687)
            //
            // Note that these other solutions were tried but did not work:
            // 1. PreparedStatement.setQueryTimeout() requires MySQL 5.0.0 or newer.
            // 2. com.mysql.jdbc.Connection.setSocketTimeout() is not accessible through com.mchange.v2.c3p0.impl.NewProxyConnection.
            // 3. Setting MySQL session variables net_read_timeout and net_write_timeout has no effect.
            int totalDeleted = 0;
            int numDeleted;
            long startTime = System.currentTimeMillis();
            do {
                try {
                    final int tempTotal = totalDeleted;
                    numDeleted = (Integer) new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
                        /**
                         * Commit the block delete and purge record creation/update in a transaction.
                         * Otherwise, without immediate commits, the total deletion time is exponential
                         * and audit events from other source will get "lock wait timeout".
                         */
                        @Override
                        public Object doInTransaction(TransactionStatus status) {
                            try {
                                return deleteBatch(auditPurgeRecordHolder, maxTime, tempTotal);
                            } catch (Exception e) {
                                logger.log(Level.WARNING, "Unable to delete batch: " + ExceptionUtils.getMessage(e), e);
                                status.setRollbackOnly();
                                return 0;
                            }
                        }
                    });
                    totalDeleted += numDeleted;
                } catch (TransactionException e) {
                    logger.log(Level.WARNING, "Couldn't commit audit deletion batch: " + ExceptionUtils.getMessage(e), e);
                    break;
                }

                if (numDeleted > 0 && logger.isLoggable(Level.FINE)) {
                    logger.fine("Deletion progress: " + totalDeleted + " audit events in " + ((System.currentTimeMillis() - startTime) / 1000.) + " sec");
                }
            } while (numDeleted != 0);

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Deleted " + totalDeleted + " audit events in " + ((System.currentTimeMillis() - startTime) / 1000.) + " sec.");
            }

        }

        @SuppressWarnings({"deprecation"})
        private int deleteBatch(final AuditRecordHolder auditRecordHolder, final long maxTime, int totalDeleted) throws HibernateException, SQLException {
            Session session = null;
            boolean ismysql = mySql.get();
            int numDeleted = 0;
            PreparedStatement deleteStmt = null;
            boolean retry = true;
            while ( retry ) {
                retry = false;
                try {
                    session = getSession();
                    final Connection conn = session.connection();
                    deleteStmt = ismysql ?
                            conn.prepareStatement(DELETE_MYSQL) :
                            conn.prepareStatement(DELETE_DERBY);
                    deleteStmt.setString(1, Level.SEVERE.getName());
                    deleteStmt.setLong(2, maxTime);
                    numDeleted = deleteStmt.executeUpdate();
                } catch( SQLException se ) {
                    if ( ismysql && "42X01".equals(se.getSQLState()) ) {
                        mySql.set(ismysql = false);
                        retry = true;
                    } else {
                        throw se;
                    }
                } finally {
                    ResourceUtils.closeQuietly(deleteStmt);
                    releaseSession(session);
                }
            }

            final SystemAuditRecord rec = auditRecordHolder.auditRecord;
            if (rec == null) {
                // This is the first batch in this session, create a new audit record.
                final AuditPurgeEvent auditPurgeEvent = new AuditPurgeEvent(AuditRecordManagerImpl.this, numDeleted);
                applicationContext.publishEvent(auditPurgeEvent);
                auditRecordHolder.auditRecord = auditPurgeEvent.getSystemAuditRecord(); // Retrieves the newly created audit record for passing into the next call.
            } else {
                // Second or subsequent batch, we need to update the audit record.
                if (numDeleted == 0) {
                    // No increment. No need to update.
                } else {
                    totalDeleted += numDeleted;
                    final AuditPurgeEvent auditPurgeEvent = new AuditPurgeEvent(AuditRecordManagerImpl.this, rec, totalDeleted);    // creates an update event
                    applicationContext.publishEvent(auditPurgeEvent);
                }
            }

            return numDeleted;
        }
    }
}
