/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class EntityFinderImpl extends HibernateDaoSupport implements EntityFinder {
    private final IdentityProviderFactory identityProviderFactory;

    public EntityFinderImpl(IdentityProviderFactory ipf) {
        this.identityProviderFactory = ipf;
    }

    @Transactional(readOnly=true)
    public EntityHeader[] findAll(final Class<? extends Entity> entityClass) throws FindException {
        final boolean names = NamedEntity.class.isAssignableFrom(entityClass);
        try {
            return (EntityHeader[])getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException {
                    Criteria crit = session.createCriteria(entityClass);
                    ProjectionList pl = Projections.projectionList();
                    pl.add(Projections.property("oid"));
                    if (names) pl.add(Projections.property("name"));
                    crit.setProjection(pl);
                    crit.setMaxResults(100);
                    List arrays = crit.list();
                    List<EntityHeader> headers = new ArrayList<EntityHeader>();
                    for (Iterator i = arrays.iterator(); i.hasNext();) {
                        Long oid;
                        String name;
                        if (names) {
                            Object[] array = (Object[]) i.next();
                            oid = (Long) array[0];
                            name = (String) array[1];
                        } else {
                            oid = (Long) i.next();
                            name = null;
                        }
                        headers.add(new EntityHeader(oid.toString(), EntityType.UNDEFINED, name, null));
                    }
                    if (arrays.size() >= 100)
                        headers.add(new EntityHeader("-1", EntityType.MAXED_OUT_SEARCH_RESULT, "Too many results", null));
                    return headers.toArray(new EntityHeader[0]);
                }
            });
        } catch (DataAccessResourceFailureException e) {
            throw new FindException("Couldn't find entities", e);
        } catch (IllegalStateException e) {
            throw new FindException("Couldn't find entities", e);
        } catch (HibernateException e) {
            throw new FindException("Couldn't find entities", e);
        }
    }

    @Transactional(readOnly=true)
    public Object find(EntityHeader header) throws FindException {
        if (header instanceof IdentityHeader) {
            IdentityHeader identityHeader = (IdentityHeader)header;
            IdentityProvider provider = identityProviderFactory.getProvider(identityHeader.getProviderOid());
            if (header.getType() == EntityType.USER) {
                return provider.getUserManager().findByPrimaryKey(header.getStrId());
            } else if (header.getType() == EntityType.GROUP) {
                return provider.getGroupManager().findByPrimaryKey(header.getStrId());
            } else {
                throw new IllegalArgumentException("EntityHeader is an IdentityHeader, but type is neither USER nor GROUP");
            }
        }
        return (Entity)find(header.getType().getClass(), header.getStrId());
    }

    @Transactional(readOnly=true)
    public <ET> ET find(final Class<ET> clazz, final Serializable pk) throws FindException {
        try {
            //noinspection unchecked
            return (ET)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException {
                    //noinspection unchecked
                    return (ET)session.load(clazz, pk);
                }
            });
        } catch (org.hibernate.ObjectNotFoundException e) {
            return null;
        } catch (HibernateException e) {
            throw new FindException("Couldn't find entity ", e);
        }
    }

    @Transactional(readOnly = true)
    public EntityHeader findHeader(com.l7tech.common.security.rbac.EntityType etype, long oid) throws FindException {
        Entity e = find(etype.getEntityClass(), oid);
        com.l7tech.objectmodel.EntityType oldType = etype.getOldEntityType();
        String name = null;
        if (e instanceof NamedEntity) {
            NamedEntity ne = (NamedEntity) e;
            name = ne.getName();
        }
        return new EntityHeader(Long.toString(e.getOid()), oldType, name, null);
    }
}
