/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.AuditSearchCriteria;
import com.l7tech.objectmodel.*;
import com.l7tech.server.event.EventManager;
import com.l7tech.server.event.system.AuditPurgeEvent;
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
     * @param criteria the {@link AuditSearchCriteria}
     * @return a Collection of AuditRecords
     * @throws FindException
     */
    public Collection find(AuditSearchCriteria criteria) throws FindException
    {
        try {
            Class findClass = criteria.recordClass;
            if (findClass == null) findClass = getInterfaceClass();

            Criteria query = getContext().getAuditSession().createCriteria(findClass);
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

    public void deleteOldAuditRecords() throws DeleteException {
        EventManager.fire(new AuditPurgeEvent( this ));
        throw new IllegalStateException("Not yet implemented");
/*
        try {
            HibernatePersistenceContext context = (HibernatePersistenceContext)HibernatePersistenceContext.getCurrent();
            Session s = context.getSession();
            StringBuffer query = new StringBuffer("FROM ").append(getTableName()).append(" IN CLASS ").append(getInterfaceClass().getName());
            query.append("WHERE ").append(getTableName()).append(".").append(PROP_LEVEL).append("");
            s.delete()

        } catch ( SQLException e ) {
            throw new DeleteException("Couldn't purge audit events", e);
        } catch ( HibernateException e ) {
            throw new DeleteException("Couldn't purge audit events", e);
        }
*/
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
