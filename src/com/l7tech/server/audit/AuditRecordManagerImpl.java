/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditRecord;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.SaveException;
import net.sf.hibernate.Criteria;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.expression.Expression;

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
public class AuditRecordManagerImpl extends HibernateEntityManager implements AuditRecordManager {
    public AuditRecordManagerImpl() {
    }

    public AuditRecord findByPrimaryKey(long oid) throws FindException {
        Entity obj = findEntity(oid);
        if (obj instanceof AuditRecord) return (AuditRecord)obj;
        throw new FindException("Expected to find '" + obj.getClass().getName() + "' but found '" + obj.getClass().getName() + "'");
    }

    /**
     * Finds {@link AuditRecord}s based on the specified criteria.
     * @param fromTime the time of the earliest record to retrieve (null = one day ago)
     * @param toTime the time of the latest record to retrieve (null = now)
     * @param fromLevel the level of the least severe record to retrieve (null = {@link Level#INFO})
     * @param toLevel the level of the most severe record to retrieve (null = {@link Level#INFO})
     * @param maxRecords the maximum number of records to retrieve (0 = 4096);
     * @return
     * @throws FindException
     */
    public Collection find(Date fromTime, Date toTime, Level fromLevel, Level toLevel, Class[] recordClasses, int maxRecords) throws FindException {
        try {
            Criteria crit = getContext().getAuditSession().createCriteria(getInterfaceClass());
            if (maxRecords <= 0) maxRecords = 4096;
            crit.setMaxResults(maxRecords);
            if (fromTime == null) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.roll(Calendar.DAY_OF_YEAR, -1);
                fromTime = cal.getTime();
            }

            if (toTime == null) toTime = new Date(System.currentTimeMillis());

            crit.add(Expression.ge(PROP_TIME, new Long(fromTime.getTime())));
            crit.add(Expression.le(PROP_TIME, new Long(toTime.getTime())));

            if (fromLevel == null) fromLevel = Level.INFO;
            if (toLevel == null) toLevel = Level.INFO;

            if (fromLevel.equals(toLevel)) {
                crit.add(Expression.eq(PROP_LEVEL, fromLevel.getName()));
            } else {
                if (fromLevel.intValue() > toLevel.intValue()) throw new FindException("fromLevel " + fromLevel.getName() + " is not lower in value than toLevel " + toLevel.getName());
                Set levels = new HashSet();
                for ( int i = 0; i < LEVELS_IN_ORDER.length; i++ ) {
                    Level level = LEVELS_IN_ORDER[i];
                    if (level.intValue() >= fromLevel.intValue() && level.intValue() <= toLevel.intValue()) {
                        levels.add(level);
                    }
                }
                crit.add(Expression.in(PROP_LEVEL, levels));
            }
            return crit.list();
        } catch ( HibernateException e ) {
            throw new FindException("Couldn't find Audit Records", e);
        } catch ( SQLException e ) {
            throw new FindException("Couldn't find Audit Records", e);
        }
    }

    public long save(AuditRecord rec) throws SaveException {
        try {
            logger.fine("Saving AuditRecord " + rec);
            final Session auditSession = getContext().getAuditSession();
            Object id = auditSession.save(rec);
            if (id instanceof Long)
                return ((Long)id).longValue();
            else
                throw new SaveException("Primary key was " + id.getClass().getName() + ", expected Long");
        } catch ( HibernateException e ) {
            throw new SaveException("Couldn't save AuditRecord", e);
        } catch ( SQLException e ) {
            throw new SaveException("Couldn't save AuditRecord", e);
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
}
