/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.common.audit.SystemAuditRecord;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.admin.AuditPurgeInitiated;
import com.l7tech.server.event.system.AuditPurgeEvent;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
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
        extends HibernateEntityManager<AuditRecord, EntityHeader>
        implements AuditRecordManager, ApplicationContextAware
{
    private static final Logger logger = Logger.getLogger(AuditRecordManagerImpl.class.getName());
    //- PUBLIC

    public void setApplicationContext(ApplicationContext applicationContext) {
        if(this.applicationContext != null) throw new IllegalStateException("applicationContext is already initialized.");
        this.applicationContext = applicationContext;
    }

    @Transactional(readOnly=true)
    public Collection<AuditRecord> find(AuditSearchCriteria criteria) throws FindException {
        if (criteria == null) throw new IllegalArgumentException("Criteria must not be null");
        Session session = null;
        try {
            Class findClass = criteria.recordClass;
            if (findClass == null) findClass = getInterfaceClass();

            session = getSession();

            Criteria query = session.createCriteria(findClass);
            int maxRecords = criteria.maxRecords;
            if (maxRecords <= 0) maxRecords = 4096;
            query.setMaxResults(maxRecords);

            Date fromTime = criteria.fromTime;
            Date toTime = criteria.toTime;

            // TODO For some dumb reason this yellow becomes a red in IDEA if I autobox it...
            if (fromTime != null) query.add(Restrictions.ge(PROP_TIME, Long.valueOf(fromTime.getTime())));
            if (toTime != null) query.add(Restrictions.lt(PROP_TIME, Long.valueOf(toTime.getTime())));

            Level fromLevel = criteria.fromLevel;
            if (fromLevel == null) fromLevel = Level.FINEST;
            Level toLevel = criteria.toLevel;
            if (toLevel == null) toLevel = Level.SEVERE;

            if (fromLevel.equals(toLevel)) {
                query.add(Restrictions.eq(PROP_LEVEL, fromLevel.getName()));
            } else {
                if (fromLevel.intValue() > toLevel.intValue()) throw new FindException("fromLevel " + fromLevel.getName() + " is not lower in value than toLevel " + toLevel.getName());
                Set<String> levels = new HashSet<String>();
                for (Level level : LEVELS_IN_ORDER) {
                    if (level.intValue() >= fromLevel.intValue() && level.intValue() <= toLevel.intValue()) {
                        levels.add(level.getName());
                    }
                }
                query.add(Restrictions.in(PROP_LEVEL, levels));
            }

            // TODO For some dumb reason this yellow becomes a red in IDEA if I autobox it...
            if (criteria.startMessageNumber > 0) query.add(Restrictions.ge(PROP_OID, Long.valueOf(criteria.startMessageNumber)));
            if (criteria.endMessageNumber > 0) query.add(Restrictions.lt(PROP_OID, Long.valueOf(criteria.endMessageNumber)));

            if (criteria.nodeId != null) query.add(Restrictions.eq(PROP_NODEID, criteria.nodeId));

            query.addOrder(Order.desc(PROP_TIME));

            //noinspection unchecked
            return query.list();
        } catch ( HibernateException e ) {
            throw new FindException("Couldn't find Audit Records", e);
        } finally {
            releaseSession(session);
        }
    }

    public void deleteOldAuditRecords() throws DeleteException {
        applicationContext.publishEvent(new AuditPurgeInitiated(this));
        String sMinAgeHours = serverConfig.getPropertyCached(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE);
        if (sMinAgeHours == null || sMinAgeHours.length() == 0)
            sMinAgeHours = "168";
        int minAgeHours = 168;
        try {
            minAgeHours = Integer.valueOf(sMinAgeHours).intValue();
        } catch (NumberFormatException e) {
            logger.info(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE + " value '" + sMinAgeHours +
                    "' is not a valid number. Using " + minAgeHours + " instead.");
        }

        final long maxTime = System.currentTimeMillis() - (minAgeHours * 60 * 60 * 1000);

        Runnable runnable = new DeletionTask(maxTime);

        new Thread(runnable).start();
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    public Class getImpClass() {
        return AuditRecord.class;
    }

    public Class getInterfaceClass() {
        return AuditRecord.class;
    }

    public String getTableName() {
        return "audit";
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    //- PRIVATE

    private ServerConfig serverConfig;
    private ApplicationContext applicationContext;

    private static class AuditRecordHolder {
        private SystemAuditRecord auditRecord = null;
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
            int numDeleted = 0;
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

        private int deleteBatch(final AuditRecordHolder auditRecordHolder, final long maxTime, int totalDeleted) throws HibernateException, SQLException {
            final Session session = getSession();
            int numDeleted;
            PreparedStatement deleteStmt = null;
            try {
                final Connection conn = session.connection();
                deleteStmt = conn.prepareStatement("DELETE FROM audit_main WHERE audit_level <> ? AND time < ? LIMIT 10000");
                deleteStmt.setString(1, Level.SEVERE.getName());
                deleteStmt.setLong(2, maxTime);
                numDeleted = deleteStmt.executeUpdate();
            } finally {
                ResourceUtils.closeQuietly(deleteStmt);
                releaseSession(session);
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
