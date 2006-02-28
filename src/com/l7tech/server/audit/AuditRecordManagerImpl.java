/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.admin.AuditPurgeInitiated;
import com.l7tech.server.event.system.AuditPurgeEvent;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages the finding and saving of {@link AuditRecord}s.
 *
 * Note that once an AuditRecord is saved, it must not be deleted or updated.
 *
 * @author alex
 * @version $Revision$
 */
public class AuditRecordManagerImpl extends HibernateEntityManager implements AuditRecordManager, ApplicationContextAware {

    //- PUBLIC

    public void setApplicationContext(ApplicationContext applicationContext) {
        if(this.applicationContext != null) throw new IllegalStateException("applicationContext is already initialized.");
        this.applicationContext = applicationContext;
    }

    public AuditRecord findByPrimaryKey(long oid) throws FindException {
        Object obj = findByPrimaryKey(AuditRecord.class, oid);
        if (obj == null) return null;
        if (obj instanceof AuditRecord) return (AuditRecord)obj;
        throw new FindException("Expected to find '" + obj.getClass().getName() + "' but found '" + obj.getClass().getName() + "'");
    }

    public Collection find(AuditSearchCriteria criteria) throws FindException {
        if (criteria == null) throw new IllegalArgumentException("Criteria must not be null");
        try {
            Class findClass = criteria.recordClass;
            if (findClass == null) findClass = getInterfaceClass();

            Criteria query = getSession().createCriteria(findClass);
            int maxRecords = criteria.maxRecords;
            if (maxRecords <= 0) maxRecords = 4096;
            query.setMaxResults(maxRecords);

            Date fromTime = criteria.fromTime;
            Date toTime = criteria.toTime;

            if (fromTime != null) query.add(Restrictions.ge(PROP_TIME, new Long(fromTime.getTime())));
            if (toTime != null) query.add(Restrictions.le(PROP_TIME, new Long(toTime.getTime())));

            Level fromLevel = criteria.fromLevel;
            if (fromLevel == null) fromLevel = Level.FINEST;
            Level toLevel = criteria.toLevel;
            if (toLevel == null) toLevel = Level.SEVERE;

            if (fromLevel.equals(toLevel)) {
                query.add(Restrictions.eq(PROP_LEVEL, fromLevel.getName()));
            } else {
                if (fromLevel.intValue() > toLevel.intValue()) throw new FindException("fromLevel " + fromLevel.getName() + " is not lower in value than toLevel " + toLevel.getName());
                Set levels = new HashSet();
                for ( int i = 0; i < LEVELS_IN_ORDER.length; i++ ) {
                    Level level = LEVELS_IN_ORDER[i];
                    if (level.intValue() >= fromLevel.intValue() && level.intValue() <= toLevel.intValue()) {
                        levels.add(level.getName());
                    }
                }
                query.add(Restrictions.in(PROP_LEVEL, levels));
            }

            // The semantics of these start & end parameters seem to be kinda backwards
            if (criteria.startMessageNumber > 0) query.add(Restrictions.le(PROP_OID, new Long(criteria.startMessageNumber)));
            if (criteria.endMessageNumber > 0) query.add(Restrictions.gt(PROP_OID, new Long(criteria.endMessageNumber)));

            if (criteria.nodeId != null) query.add(Restrictions.eq(PROP_NODEID, criteria.nodeId));

            return query.list();
        } catch ( HibernateException e ) {
            throw new FindException("Couldn't find Audit Records", e);
        }
    }

    public long save(AuditRecord rec) throws SaveException {
        try {
            if(logger.isLoggable(Level.FINE)) logger.fine("Saving AuditRecord " + rec);
            Object id = getSession().save(rec);
            if (id instanceof Long)
                return ((Long)id).longValue();
            else
                throw new SaveException("Primary key was " + id.getClass().getName() + ", expected Long");
        } catch ( HibernateException e ) {
            throw new SaveException("Couldn't save AuditRecord", e);
        }
    }

    public void deleteOldAuditRecords() throws DeleteException {
        applicationContext.publishEvent(new AuditPurgeInitiated(this));
        String sMinAgeHours = serverConfig.getProperty(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE);
        if (sMinAgeHours == null || sMinAgeHours.length() == 0) sMinAgeHours = "168";
        int minAgeHours = 168;
        try {
            minAgeHours = Integer.valueOf(sMinAgeHours).intValue();
        } catch (NumberFormatException e) {
            logger.info(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE + " value '" + sMinAgeHours +
                    "' is not a valid number. Using " + minAgeHours + " instead.");
        }

        long maxTime = System.currentTimeMillis() - (minAgeHours * 60 * 60 * 1000);

        PreparedStatement deleteStmt = null;
        try {
            Session s = getSession();
            deleteStmt = s.connection().prepareStatement("DELETE FROM audit_main WHERE audit_level <> ? AND time < ?");
            deleteStmt.setString(1, Level.SEVERE.getName());
            deleteStmt.setLong(2, maxTime);
            int numDeleted = deleteStmt.executeUpdate();
            applicationContext.publishEvent(new AuditPurgeEvent(this, numDeleted));
        } catch (Exception e) {
            throw new DeleteException("Couldn't delete audit records", e);
        } finally {
            if (deleteStmt != null) try { deleteStmt.close(); } catch (SQLException e) { }
        }
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
}
