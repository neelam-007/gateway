package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.*;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.*;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.event.admin.AuditPurgeInitiated;
import com.l7tech.server.event.system.AuditPurgeEvent;
import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Eithers.extract;
import com.l7tech.util.Functions.UnaryThrows;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;
import org.hibernate.*;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Full featured audit record manager, with database specific functionality.
 *
 * @author alex
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class AuditRecordManagerImpl
        extends SimpleAuditRecordManagerImpl
        implements AuditRecordManager, ApplicationContextAware
{
    //- PUBLIC

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        if(this.applicationContext != null) throw new IllegalStateException("applicationContext is already initialized.");
        this.applicationContext = applicationContext;
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

        final long systemMinAge =  TimeUnit.HOURS.toMillis( (long)minAgeHours );
        Runnable runnable = new DeletionTask( System.currentTimeMillis() -  Math.max(systemMinAge, minAge) );

        new Thread(runnable).start();
    }

    @Override
    public long getMinOid( final long lowerLimit ) throws SQLException {
        return doReadOnlyWork( new UnaryThrows<Long, Connection, SQLException>() {
            @Override
            public Long call( final Connection conn ) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
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
        }
                return -1L;
    }
        } );
    }

    @Override
    public int deleteRangeByOid(final long start, final long end) throws SQLException {
        return doReadOnlyWork( new UnaryThrows<Integer, Connection, SQLException>() {
            @Override
            public Integer call( final Connection conn ) throws SQLException {
        PreparedStatement deleteStmt = null;
        try {
            deleteStmt = conn.prepareStatement("DELETE FROM audit_main WHERE objectid >= ? AND objectid <= ? LIMIT 10000");
            deleteStmt.setLong(1, start);
            deleteStmt.setLong(2, end);
            return deleteStmt.executeUpdate();
        } finally {
            ResourceUtils.closeQuietly(deleteStmt);
        }
    }
        } );
    }

    /**
     * Gets the autoextend:max defined for the innodb table space, from the innodb_data_file_paths MySQL variable.
     *
     * @return  Max table space size in bytes, or -1 if not defined
     * @throws FindException if an error was encountered and the value could not be retrieved
     */
    @Override
    public long getMaxTableSpace() throws FindException {
        try {
            return extract( doReadOnlyWork( new UnaryThrows<Either<FindException, Long>, Connection, SQLException>() {
                @Override
                public Either<FindException, Long> call( final Connection conn ) throws SQLException {
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = conn.prepareStatement(SQL_INNODB_DATA);
            rs = statement.executeQuery();

            if (rs != null && rs.next()) {
                String innodbData = rs.getString("value");
                            return right( SqlUtils.getMaxTableSize( innodbData ) );
            }

                        return right( -1L ); // rs empty
        } catch (NumberFormatException ne) {
                        return left( new FindException( "Error retrieving max space allocated for the innodb tablespace.", ne ) );
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(statement);
        }
    }
            } ) );
        } catch (SQLException e) {
            throw new FindException("Error retrieving max space allocated for the innodb tablespace.", e );
        }
    }

    @Override
    public long getCurrentUsage() throws FindException {
        final Option<Long> usage;
        try {
            usage = doReadOnlyWork( new UnaryThrows<Option<Long>, Connection, SQLException>() {
                @Override
                public Option<Long> call( final Connection conn ) throws SQLException {
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
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
                            return some( usage );
            }
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(statement);
        }

                    return none();
    }
            } );
        } catch (SQLException e) {
            throw new FindException("Error retrieving max space allocated for the innodb tablespace.", e);
    }

        // shouldn't happen
        if ( !usage.isSome() )
            throw new FindException("Error retrieving max space allocated for the innodb tablespace; no data retrieved.");

        return usage.some();
    }

    //- PRIVATE

    private static final String SQL_GET_MIN_OID = "SELECT MIN(objectid) FROM audit_main WHERE objectid > ?";
    private static final String SQL_INNODB_DATA = "SHOW VARIABLES LIKE 'innodb_data_file_path'";
    private static final String SQL_CURRENT_USAGE = "SHOW TABLE STATUS";

    private static final String DELETE_MYSQL = "DELETE FROM audit_main WHERE audit_level <> ? AND time < ? LIMIT 10000";
    private static final String DELETE_DERBY = "DELETE FROM audit_main where objectid in (SELECT objectid FROM (SELECT ROW_NUMBER() OVER() as rownumber, objectid FROM audit_main WHERE audit_level <> ?  and time < ?) AS foo WHERE rownumber <= 10000)";
    private static final AtomicBoolean mySql = new AtomicBoolean(true);

    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private static final Logger logger = Logger.getLogger(AuditRecordManagerImpl.class.getName());
    private ApplicationContext applicationContext;

    private static class AuditRecordHolder {
        private SystemAuditRecord auditRecord = null;
    }

    private class DeletionTask implements Runnable {
        private final AuditRecordHolder auditPurgeRecordHolder = new AuditRecordHolder();
        private final long maxTime;

        private DeletionTask(long maxTime) {
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
                    numDeleted = new TransactionTemplate(transactionManager).execute(new TransactionCallback<Integer>() {
                        /**
                         * Commit the block delete and purge record creation/update in a transaction.
                         * Otherwise, without immediate commits, the total deletion time is exponential
                         * and audit events from other source will get "lock wait timeout".
                         */
                        @Override
                        public Integer doInTransaction(TransactionStatus status) {
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
                    logger.fine("Deletion progress: " + totalDeleted + " audit events in " + ((double) (System.currentTimeMillis() - startTime) / 1000.) + " sec");
                }
            } while (numDeleted != 0);

            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Deleted " + totalDeleted + " audit events in " + ((double) (System.currentTimeMillis() - startTime) / 1000.) + " sec.");
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
