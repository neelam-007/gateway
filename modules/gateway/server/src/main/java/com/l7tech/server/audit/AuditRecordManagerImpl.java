/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.*;
import com.l7tech.util.*;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.admin.AuditPurgeInitiated;
import com.l7tech.server.event.system.AuditPurgeEvent;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

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
        implements AuditRecordManager, ApplicationContextAware
{
    private static final String SQL_GET_MIN_OID = "SELECT MIN(objectid) FROM audit_main WHERE objectid > ?";
    private static final String SQL_INNODB_DATA = "SHOW VARIABLES LIKE 'innodb_data_file_path'";
    private static final String SQL_CURRENT_USAGE = "SHOW TABLE STATUS";

    //- PUBLIC

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        if(this.applicationContext != null) throw new IllegalStateException("applicationContext is already initialized.");
        this.applicationContext = applicationContext;
    }

    @Override
    @Transactional(readOnly=true)
    public Collection<AuditRecord> find( final AuditSearchCriteria criteria ) throws FindException {
        //todo: test before using, no current usages.
        return find(criteria, null, new Functions.BinaryVoid<List<AuditRecord>, Object>() {
            @Override
            public void call(List<AuditRecord> auditRecords, Object o) {
                AuditRecord record = (AuditRecord) o;
                if (verifyRecordByAuditDetailsSearchCriteria(criteria, record)) {
                    auditRecords.add(record);
                }
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<AuditRecordHeader> findHeaders(final AuditSearchCriteria criteria) throws FindException {
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

                hibernateCriteria.setProjection(projectionList);

                if (criteria.messageId != null) {
                    hibernateCriteria.createAlias("details", "ad");
                    final SimpleExpression eq = Restrictions.eq("ad.messageId", criteria.messageId);
                    hibernateCriteria.add(eq);
                }
            }
        };

        final Class findClass = getFindClass(criteria);
        final boolean isMessageAudit = findClass == MessageSummaryAuditRecord.class;

        //todo: Could a ResultTransformer be useful here and simplify the mapping of results to AuditRecordHeader?
        List<AuditRecordHeader> auditRecordHeaders = find(criteria, criteriaConfigurator, new Functions.BinaryVoid<List<AuditRecordHeader>, Object>() {
            @Override
            public void call(List<AuditRecordHeader> auditRecordHeaders, Object o) {
                final Object [] values = (Object[]) o;

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
                //todo: test if used for anything other than header data - pre Chinook this was never used for any data other than headers, and is still not
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

    /**
     * //todo: This should not be used. Post Chinook update so this is removed. Filtering post search means we cannot guarantee that all applicable results are found. (due to existing audit viewer problem as paging is not supported).
     * Verify if the audit record matches the given audit details search criteria.
     * @param criteria: the whole search criteria
     * @param record: the audit record to be verified
     * @return true if the audit record matches the audit details search criteria.
     */
    private boolean verifyRecordByAuditDetailsSearchCriteria(final AuditSearchCriteria criteria, final AuditRecord record) {
        final boolean searchingMsgIdEnabled = criteria.messageId != null;
        final boolean searchingParamValueEnabled = criteria.paramValue != null && !criteria.paramValue.trim().isEmpty();

        // If message id is equal to Integer.MIN_VALUE, then this means message id is invalid, then return false.
        if (searchingMsgIdEnabled && criteria.messageId.equals(Integer.MIN_VALUE)) return false;

        // If both searching criteria are not enabled, then ignore the checking procedure and return true.
        if (!searchingMsgIdEnabled && !searchingParamValueEnabled) return true;

        int msgId;
        String[] params;
        boolean msgIdMatched;
        boolean paramValueMatched;

        for (AuditDetail auditDetail: record.getDetails()) {
            msgId = auditDetail.getMessageId();
            params = auditDetail.getParams();

            // If the "message id" searching criterion is not enabled, then ignore the message id checking and set msgIdMatched to true.
            msgIdMatched = (! searchingMsgIdEnabled) || (msgId == criteria.messageId);
            // If the "param value" searching criterion is not enabled, then ignore the param value checking  and set paramValueMatched to true.
            paramValueMatched = (! searchingParamValueEnabled) || ((params != null) && (params.length > 0) && matchFound(params, criteria.paramValue));

            if (msgIdMatched && paramValueMatched)
                return true;
        }

        return false;
    }

    private boolean matchFound(String[] values, String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) throw new IllegalArgumentException("Match pattern must be specified.");
        if (values == null || values.length == 0) return false;

        for (String value: values) {
            if (TextUtils.matches(pattern, value, true, true)) {
                return true;
            }
        }
        return false;
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
        String sMinAgeHours = serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE);
        if (sMinAgeHours == null || sMinAgeHours.length() == 0)
            sMinAgeHours = "168";
        int minAgeHours = 168;
        try {
            minAgeHours = Integer.valueOf(sMinAgeHours);
        } catch (NumberFormatException e) {
            logger.info(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE + " value '" + sMinAgeHours +
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

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
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
    private ServerConfig serverConfig;
    private ApplicationContext applicationContext;

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

        if (criteria.startMessageNumber > 0) criterion.add(Restrictions.ge(PROP_OID, criteria.startMessageNumber));
        if (criteria.endMessageNumber > 0) criterion.add(Restrictions.lt(PROP_OID, criteria.endMessageNumber));

        if (criteria.requestId != null) criterion.add(Restrictions.ilike(PROP_REQUEST_ID, criteria.requestId));
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
