package com.l7tech.server.service;

import com.l7tech.objectmodel.*;
import com.l7tech.service.SampleMessage;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages persistent instances of {@link SampleMessage}.
 */
public class SampleMessageManagerImp extends HibernateEntityManager implements SampleMessageManager {
    public SampleMessage findByPrimaryKey(long oid) throws FindException {
        return (SampleMessage)super.findByPrimaryKey(getImpClass(), oid);
    }

    public EntityHeader[] findHeaders(long serviceOid, String operationName) throws FindException {
        StringBuffer hql = new StringBuffer(getAllQuery());
        hql.append(" WHERE ").append(getTableName()).append(".serviceOid ");
        try {
            List results;
            Object[] params;
            if (serviceOid == -1) {
                hql.append("IS NULL");
                params = new Object[] { operationName };
            } else {
                hql.append("= ?");
                params = new Object[] { Long.toString(serviceOid), operationName };
            }
            hql.append(" AND ").append(getTableName()).append(".operationName = ?");
            results = getHibernateTemplate().find(hql.toString(), params);

            ArrayList out = new ArrayList();
            for (Iterator i = results.iterator(); i.hasNext();) {
                SampleMessage sm = (SampleMessage)i.next();
                out.add(new EntityHeader(Long.toString(sm.getOid()), EntityType.SAMPLE_MESSAGE, sm.getName(), null));
            }
            return (EntityHeader[])out.toArray(new EntityHeader[0]);
        } catch (DataAccessException e) {
            throw new FindException("Couldn't find SampleMessage headers", e);
        }

    }

    public long save(SampleMessage sm) throws SaveException {
        try {
            Long o = (Long)getHibernateTemplate().save(sm);
            return o.longValue();
        } catch (DataAccessException e) {
            throw new SaveException("Couldn't save SampleMessage", e);
        }
    }

    public void update(SampleMessage sm) throws UpdateException {
        try {
            SampleMessage original = findByPrimaryKey(sm.getOid());
            if (original == null) throw new UpdateException("Can't find SampleMessage #" + sm.getOid() + ": it was probably deleted by another transaction");

            if (original.getVersion() != sm.getVersion())
                throw new StaleUpdateException("SampleMessage #" + sm.getOid() + " was modified by another transaction");

            original.copyFrom(sm);
            getHibernateTemplate().update(original);
        } catch (Exception e) {
            throw new UpdateException("Couldn't update SampleMessage", e);
        }
    }

    public void delete(SampleMessage sm) throws DeleteException {
        try {
            getHibernateTemplate().delete(sm);
        } catch (DataAccessException e) {
            throw new DeleteException("Couldn't delete SampleMessage", e);
        }
    }

    public Class getImpClass() {
        return SampleMessage.class;
    }

    public Class getInterfaceClass() {
        return SampleMessage.class;
    }

    public String getTableName() {
        return "sample_messages";
    }
}
