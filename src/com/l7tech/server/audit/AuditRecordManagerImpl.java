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
import com.l7tech.server.event.EventManager;
import com.l7tech.server.event.admin.AuditPurgeInitiated;
import com.l7tech.server.event.system.AuditPurgeEvent;
import net.sf.hibernate.Criteria;
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;
import net.sf.hibernate.expression.Expression;
import net.sf.hibernate.type.Type;

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
    private EventManager eventManager;

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

            if (fromTime != null) query.add(Expression.ge(PROP_TIME, new Long(fromTime.getTime())));
            if (toTime != null) query.add(Expression.le(PROP_TIME, new Long(toTime.getTime())));

            Level fromLevel = criteria.fromLevel;
            if (fromLevel == null) fromLevel = Level.FINEST;
            Level toLevel = criteria.toLevel;
            if (toLevel == null) toLevel = Level.SEVERE;

            if (fromLevel.equals(toLevel)) {
                query.add(Expression.eq(PROP_LEVEL, fromLevel.getName()));
            } else {
                if (fromLevel.intValue() > toLevel.intValue()) throw new FindException("fromLevel " + fromLevel.getName() + " is not lower in value than toLevel " + toLevel.getName());
                Set levels = new HashSet();
                for ( int i = 0; i < LEVELS_IN_ORDER.length; i++ ) {
                    Level level = LEVELS_IN_ORDER[i];
                    if (level.intValue() >= fromLevel.intValue() && level.intValue() <= toLevel.intValue()) {
                        levels.add(level.getName());
                    }
                }
                query.add(Expression.in(PROP_LEVEL, levels));
            }

            // The semantics of these start & end parameters seem to be kinda backwards
            if (criteria.startMessageNumber > 0) query.add(Expression.le(PROP_OID, new Long(criteria.startMessageNumber)));
            if (criteria.endMessageNumber > 0) query.add(Expression.gt(PROP_OID, new Long(criteria.endMessageNumber)));

            if (criteria.nodeId != null) query.add(Expression.eq(PROP_NODEID, criteria.nodeId));

            List l = query.list();
            return l;
        } catch ( HibernateException e ) {
            throw new FindException("Couldn't find Audit Records", e);
        }
    }

    public long save(AuditRecord rec) throws SaveException {
        try {
            logger.fine("Saving AuditRecord " + rec);
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
        eventManager.fire(new AuditPurgeInitiated(this));
        try {
            String sMinAgeHours = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE);
            if (sMinAgeHours == null || sMinAgeHours.length() == 0) sMinAgeHours = "168";
            int minAgeHours = 168;
            try {
                minAgeHours = Integer.valueOf(sMinAgeHours).intValue();
            } catch (NumberFormatException e) {
                logger.info(ServerConfig.PARAM_AUDIT_PURGE_MINIMUM_AGE + " value '" + sMinAgeHours +
                            "' is not a valid number. Using " + minAgeHours + " instead." );
            }

            long maxTime = System.currentTimeMillis() - (minAgeHours * 60 * 60 * 1000);

            Session s = getSession();
            StringBuffer query = new StringBuffer("FROM audit IN CLASS ").append(getInterfaceClass().getName());
            query.append(" WHERE audit.").append(PROP_LEVEL).append(" <> ?");
            query.append(" AND audit.").append(PROP_TIME).append(" < ?");
            int numDeleted = s.delete( query.toString(),
                                       new Object[] { Level.SEVERE.getName(), new Long(maxTime) },
                                       new Type[] { Hibernate.STRING, Hibernate.LONG } );
            eventManager.fire(new AuditPurgeEvent( this, numDeleted ));
        } catch ( HibernateException e ) {
            throw new DeleteException("Couldn't purge audit events", e);
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

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }
}
