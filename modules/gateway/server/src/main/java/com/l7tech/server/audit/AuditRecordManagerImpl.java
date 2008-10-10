/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.admin.AuditPurgeInitiated;
import com.l7tech.server.event.system.AuditPurgeEvent;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Criterion;
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

    public void setApplicationContext(ApplicationContext applicationContext) {
        if(this.applicationContext != null) throw new IllegalStateException("applicationContext is already initialized.");
        this.applicationContext = applicationContext;
    }

    @Transactional(readOnly=true)
    public Collection<AuditRecord> find( final AuditSearchCriteria criteria ) throws FindException {
        if (criteria == null) throw new IllegalArgumentException("Criteria must not be null");
        Session session = null;
        try {
            Class findClass = criteria.recordClass;
            if (findClass == null) findClass = getInterfaceClass();

            if(criteria.requestId != null && findClass != MessageSummaryAuditRecord.class) findClass = MessageSummaryAuditRecord.class;

            session = getSession();

            Criteria query = session.createCriteria(findClass);
            int maxRecords = criteria.maxRecords;
            if (maxRecords <= 0) maxRecords = 4096;
            query.setMaxResults(maxRecords);

            for ( Criterion criterion : asCriterion( criteria ) ) {
                query.add( criterion );
            }

            query.addOrder(Order.desc(PROP_TIME));

            //noinspection unchecked
            return query.list();
        } catch ( HibernateException e ) {
            throw new FindException("Couldn't find Audit Records", e);
        } finally {
            releaseSession(session);
        }
    }

    @Transactional(readOnly = true)
    public Collection<AuditRecordHeader> findHeaders(final AuditSearchCriteria criteria) throws FindException {
        Collection<AuditRecord> auditRecords = find(criteria);
        List<AuditRecordHeader> auditRecordHeaders = new ArrayList<AuditRecordHeader>();
        for (AuditRecord auditRecord : auditRecords) {
            auditRecordHeaders.add(newHeader(auditRecord));
        }
        return Collections.unmodifiableList(auditRecordHeaders);
    }

    @Override
    protected AuditRecordHeader newHeader(final AuditRecord auditRecord) {
        AuditRecordHeader arh = new AuditRecordHeader(auditRecord);
        arh.setSignatureDigest(auditRecord.computeSignatureDigest());
        return arh;
    }

    public int findCount( final AuditSearchCriteria criteria ) throws FindException {
        return super.findCount( asCriterion(criteria) );
    }

    public Collection<AuditRecord> findPage( final SortProperty sortProperty,
                                             final boolean ascending,
                                             final int offset,
                                             final int count,
                                             final AuditSearchCriteria criteria) throws FindException {
        return super.findPage( sortProperty.getPropertyName(), ascending, offset, count, asCriterion(criteria) );
    }

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

    public long getMinOid(long lowerLimit) throws SQLException {
        final Session session = getSession();
        PreparedStatement stmt = null;
        try {
            final Connection conn = session.connection();
            stmt = conn.prepareStatement(SQL_GET_MIN_OID);
            stmt.setLong(1, lowerLimit);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String min = rs.getString(1);
                if (min == null || "null".equalsIgnoreCase(min)) {
                    logger.log(Level.FINE, "Min audit record object id not retrieved (table empty?).");
                } else {
                    return Long.parseLong(min);
                }
            }
        } finally {
            ResourceUtils.closeQuietly(stmt);
            releaseSession(session);
        }
        return -1;
    }

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
    public long getMaxTableSpace() throws FindException {
        final Session session = getSession();
        PreparedStatement statement = null;
        try {
            final Connection conn = session.connection();
            statement = conn.prepareStatement(SQL_INNODB_DATA);
            ResultSet rs = statement.executeQuery();

            if (rs != null && rs.next()) {
                String innodbData = rs.getString("value");
                int index = innodbData.lastIndexOf(":autoextend:max:");
                if (index > 0) {
                    String max = innodbData.substring(index + 16);
                    return getLongSize(max);
                } else if (innodbData.indexOf(":autoextend") > 0) {
                    return -1;
                } else {
                    // use fixed size(es)
                    long max = 0;
                    String[] datafiles = innodbData.split(";");
                    for (String datafile : datafiles) {
                        String[] tokens = datafile.split(":");
                        if (tokens.length > 1) {
                            max += getLongSize(tokens[1]);
                        }
                    }
                    return max;
                }
            }

            return -1; // rs empty

        } catch (SQLException e) {
            throw new FindException("Error retrieving max space allocated for the innodb tablespace.", e);
        } catch (NumberFormatException ne) {
            throw new FindException("Error retrieving max space allocated for the innodb tablespace.", ne);
        } finally {
            ResourceUtils.closeQuietly(statement);
            releaseSession(session);
        }
    }

    // gets a long out of a mysql / innodb size specification NNNN[M|G]
    private long getLongSize(String max) {
        long multiplier = 1L;
        if ("m".equalsIgnoreCase(max.substring(max.length()-1)))
            multiplier = 0x100000L;
        else if ("g".equalsIgnoreCase(max.substring(max.length()-1)))
            multiplier = 0x40000000L;
        return multiplier * Long.parseLong(multiplier > 1 ? max.substring(0, max.length() -1) : max);
    }

    public long getCurrentUsage() throws FindException {
        final Session session = getSession();
        PreparedStatement statement = null;
        try {
            final Connection conn = session.connection();
            statement = conn.prepareStatement(SQL_CURRENT_USAGE);
            ResultSet rs = statement.executeQuery();
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
            ResourceUtils.closeQuietly(statement);
            releaseSession(session);
        }

        // shouldn't happen
        throw new FindException("Error retrieving max space allocated for the innodb tablespace; no data retrieved.");
    }

    public Class<AuditRecord> getImpClass() {
        return AuditRecord.class;
    }

    public Class<AuditRecord> getInterfaceClass() {
        return AuditRecord.class;
    }

    public String getTableName() {
        return "audit";
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
    private static final String PROP_SERVICE_NAME = "name";
    private static final String PROP_REQUEST_ID = "strRequestId";
    public static final String PROP_NAME = "name";
    public static final String PROP_MSG = "message";

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

        return criterion.toArray( new Criterion[criterion.size()] );
    }

    private class DeletionTask implements Runnable {
        private final AuditRecordHolder auditPurgeRecordHolder = new AuditRecordHolder();
        private final long maxTime;

        public DeletionTask(long maxTime) {
            this.maxTime = maxTime;
        }

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
                    if ( ismysql & "42X01".equals(se.getSQLState()) ) {
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
