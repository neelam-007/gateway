package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.FindException;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 20, 2008
 * Time: 3:21:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceAliasManagerImpl
        extends HibernateEntityManager<PublishedServiceAlias, ServiceHeader>
        implements ServiceAliasManager {

    public Class getImpClass() {
        return PublishedServiceAlias.class;
    }

    public Class getInterfaceClass() {
        return PublishedServiceAlias.class;
    }

    public String getTableName() {
        return "published_service_alias";
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    public PublishedServiceAlias findAliasByServiceAndFolder(final Long serviceOid, final Long folderOid) throws FindException {
        if (serviceOid == null || folderOid == null) throw new NullPointerException();
        if (!(NamedEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not NamedEntities!");

        try {
            //noinspection unchecked
            return (PublishedServiceAlias)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("entityOid", serviceOid));
                    crit.add(Restrictions.eq("folderOid", folderOid));
                    return crit.uniqueResult();
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find unique entity", e);
        }
    }

    public Collection<PublishedServiceAlias> findAllAliasesForService(final Long serviceOid) throws FindException {
        if (serviceOid == null) throw new NullPointerException();
        if (!(NamedEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not NamedEntities!");

        try {
            //noinspection unchecked
            return (Collection<PublishedServiceAlias>)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("entityOid", serviceOid));
                    return crit.list();
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find unique entity", e);
        }
    }
}
