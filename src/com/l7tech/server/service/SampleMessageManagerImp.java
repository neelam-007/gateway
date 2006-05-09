package com.l7tech.server.service;

import com.l7tech.objectmodel.*;
import com.l7tech.service.SampleMessage;
import org.springframework.dao.DataAccessException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages persistent instances of {@link SampleMessage}.
 */
public class SampleMessageManagerImp extends HibernateEntityManager implements SampleMessageManager {
    private static final String PROP_SERVICE_OID = "serviceOid";
    private static final String PROP_OPERATION_NAME = "operationName";

    public SampleMessage findByPrimaryKey(long oid) throws FindException {
        return (SampleMessage)super.findByPrimaryKey(getImpClass(), oid);
    }

    public EntityHeader[] findHeaders(long serviceOid, String operationName) throws FindException {
        try {
            Criteria crit = getSession().createCriteria(SampleMessage.class);

            if (serviceOid == -1) {
                crit.add(Restrictions.isNull(PROP_SERVICE_OID));
            } else {
                crit.add(Restrictions.eq(PROP_SERVICE_OID, new Long(serviceOid)));
            }

            if (operationName == null) {
                crit.add(Restrictions.isNull(PROP_OPERATION_NAME));
            } else {
                crit.add(Restrictions.ilike(PROP_OPERATION_NAME, operationName));
            }

            List results = crit.list();
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

    public EntityType getEntityType() {
        return EntityType.SAMPLE_MESSAGE;
    }
}
