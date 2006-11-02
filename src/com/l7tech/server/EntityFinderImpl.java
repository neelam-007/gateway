/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.IdProvConfManagerServer;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.common.util.ExceptionUtils;
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
import java.util.logging.Logger;

@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class EntityFinderImpl extends HibernateDaoSupport implements EntityFinder {
    private static final Logger logger = Logger.getLogger(EntityFinderImpl.class.getName());
    private final IdentityProviderFactory identityProviderFactory;

    public EntityFinderImpl(IdentityProviderFactory ipf) {
        this.identityProviderFactory = ipf;
    }

    @Transactional(readOnly=true)
    public EntityHeader[] findAll(final Class<? extends Entity> entityClass) throws FindException {
        final boolean names = NamedEntity.class.isAssignableFrom(entityClass);
        final EntityType type = EntityHeaderUtils.getEntityHeaderType(entityClass);
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
                        headers.add(new EntityHeader(oid.toString(), type, name, null));
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
    public Entity find(EntityHeader header) throws FindException {
        if (header instanceof IdentityHeader) {
            IdentityHeader identityHeader = (IdentityHeader)header;
            IdentityProvider provider = identityProviderFactory.getProvider(identityHeader.getProviderOid());
            if (provider == null) return null;
            if (header.getType() == EntityType.USER) {
                return provider.getUserManager().findByPrimaryKey(header.getStrId());
            } else if (header.getType() == EntityType.GROUP) {
                return provider.getGroupManager().findByPrimaryKey(header.getStrId());
            } else {
                throw new IllegalArgumentException("EntityHeader is an IdentityHeader, but type is neither USER nor GROUP");
            }
        } else if (header.getType() == EntityType.ID_PROVIDER_CONFIG && header.getOid() == IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID) {
            return identityProviderFactory.getProvider(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID).getConfig();
        }
        return find(EntityHeaderUtils.getEntityClass(header), header.getStrId());
    }

    @Transactional(readOnly=true)
    public <ET extends Entity> ET find(final Class<ET> clazz, Serializable pk) throws FindException {
        try {
            Serializable tempPk;
            if (pk instanceof String) {
                try {
                    tempPk = Long.valueOf((String)pk);
                } catch (NumberFormatException nfe) {
                    // TODO make this metadata-based?
                    logger.fine("Primary key {0} is not a valid Long; using String value instead");
                    tempPk = pk;
                }
            } else {
                tempPk = pk;
            }

            final Serializable finalPk = tempPk;
            //noinspection unchecked
            return (ET)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException {
                    //noinspection unchecked
                    return session.load(clazz, finalPk);
                }
            });
        } catch (Exception e) {
            if (ExceptionUtils.causedBy(e, org.hibernate.ObjectNotFoundException.class)) return null; 
            throw new FindException("Couldn't find entity ", e);
        }
    }

    @Transactional(readOnly = true)
    public EntityHeader findHeader(com.l7tech.common.security.rbac.EntityType etype, Serializable pk) throws FindException {
        Entity e = find(etype.getEntityClass(), pk);
        if (e == null) return null;
        
        com.l7tech.objectmodel.EntityType oldType = etype.getOldEntityType();
        String name = null;
        if (e instanceof NamedEntity) {
            NamedEntity ne = (NamedEntity) e;
            name = ne.getName();
        }
        return new EntityHeader(e.getId(), oldType, name, null);
    }
}
