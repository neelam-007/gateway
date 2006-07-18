/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdentityProviderFactory;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.dao.DataAccessResourceFailureException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EntityFinder extends HibernateDaoSupport {
    private IdentityProviderFactory identityProviderFactory;

    public EntityFinder(IdentityProviderFactory ipf) {
        this.identityProviderFactory = ipf;
    }

    protected EntityFinder() {
    }

    public EntityHeader[] findAll(Class<? extends Entity> entityClass) throws FindException {
        try {
            boolean names = NamedEntity.class.isAssignableFrom(entityClass);
            Criteria crit = getSession().createCriteria(entityClass);
            ProjectionList pl = Projections.projectionList();
            pl.add(Projections.property("oid"));
            if (names) pl.add(Projections.property("name"));
            crit.setProjection(pl);
            crit.setMaxResults(100);
            List arrays = crit.list();
            List<EntityHeader> headers = new ArrayList<EntityHeader>();
            for (Iterator i = arrays.iterator(); i.hasNext();) {
                Object[] array = (Object[])i.next();
                Long oid = (Long)array[0];
                String name = names ? (String)array[1] : null;
                headers.add(new EntityHeader(oid.toString(), EntityType.UNDEFINED, name, null));
            }
            if (arrays.size() >= 100) headers.add(new EntityHeader("-1", EntityType.MAXED_OUT_SEARCH_RESULT, "Too many results", null));
            return headers.toArray(new EntityHeader[0]);
        } catch (DataAccessResourceFailureException e) {
            throw new FindException("Couldn't find entities", e);
        } catch (IllegalStateException e) {
            throw new FindException("Couldn't find entities", e);
        } catch (HibernateException e) {
            throw new FindException("Couldn't find entities", e);
        }
    }

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

    public <ET> ET find(Class<ET> clazz, Serializable pk) throws FindException {
        Session s = null;
        FlushMode old = null;
        try {
            s = getSession();
            old = s.getFlushMode();
            s.setFlushMode(FlushMode.NEVER);
            return (ET)s.load(clazz, pk);
        } catch (org.hibernate.ObjectNotFoundException e) {
            return null;
        } catch (HibernateException e) {
            throw new FindException("Couldn't find entity ", e);
        } finally {
            if (s != null && old != null) s.setFlushMode(old);
        }
    }
}
