package com.l7tech.server.service;

import com.l7tech.objectmodel.*;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.gateway.common.service.SampleMessage;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages persistent instances of {@link SampleMessage}.
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SampleMessageManagerImp
        extends HibernateEntityManager<SampleMessage, EntityHeader>
        implements SampleMessageManager
{
    private static final String PROP_SERVICE_OID = "serviceOid";
    private static final String PROP_OPERATION_NAME = "operationName";

    @Transactional(readOnly=true)
    public EntityHeader[] findHeaders(final long serviceOid, final String operationName) throws FindException {
        try {
            return (EntityHeader[]) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(SampleMessage.class);

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
                    ArrayList<EntityHeader> out = new ArrayList<EntityHeader>();
                    for (Object result : results) {
                        SampleMessage sm = (SampleMessage) result;
                        out.add(new EntityHeader(Long.toString(sm.getOid()), EntityType.SAMPLE_MESSAGE, sm.getName(), null));
                    }
                    return out.toArray(new EntityHeader[0]);
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Couldn't find SampleMessage headers", e);
        }
    }

/*
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
*/

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
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
