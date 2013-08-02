package com.l7tech.server.service;

import com.l7tech.gateway.common.service.SampleMessage;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateGoidEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
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
        extends HibernateGoidEntityManager<SampleMessage, EntityHeader>
        implements SampleMessageManager
{
    private static final String PROP_SERVICE_GOID = "serviceGoid";
    private static final String PROP_OPERATION_NAME = "operationName";

    @Override
    @Transactional(readOnly=true)
    public EntityHeader[] findHeaders(final Goid serviceGoid, final String operationName) throws FindException {
        try {
            return (EntityHeader[]) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(SampleMessage.class);

                    if (Goid.isDefault(serviceGoid)) {
                        crit.add(Restrictions.isNull(PROP_SERVICE_GOID));
                    } else {
                        crit.add(Restrictions.eq(PROP_SERVICE_GOID, serviceGoid));
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
                        out.add(new EntityHeader(sm.getGoid(), EntityType.SAMPLE_MESSAGE, sm.getName(), null, sm.getVersion()));
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
