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

    @Override
    @Transactional(readOnly=true)
    public EntityHeader[] findHeaders(final long serviceOid, final String operationName) throws FindException {
        try {
            return (EntityHeader[]) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
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
                        out.add(new EntityHeader(sm.getOid(), EntityType.SAMPLE_MESSAGE, sm.getName(), null, sm.getVersion()));
                    }
                    return out.toArray(new EntityHeader[0]);
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Couldn't find SampleMessage headers", e);
        }
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    @Override
    public Class getImpClass() {
        return SampleMessage.class;
    }

    @Override
    public Class getInterfaceClass() {
        return SampleMessage.class;
    }

    @Override
    public String getTableName() {
        return "sample_messages";
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SAMPLE_MESSAGE;
    }
}
