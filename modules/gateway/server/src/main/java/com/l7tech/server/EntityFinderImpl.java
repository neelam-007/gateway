/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.security.keystore.SsgKeyHeader;
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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.security.KeyStoreException;

@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class EntityFinderImpl extends HibernateDaoSupport implements EntityFinder {
    private static final Logger logger = Logger.getLogger(EntityFinderImpl.class.getName());
    private IdentityProviderFactory identityProviderFactory;
    private PolicyManager policyManager;
    private SsgKeyStoreManager keyStoreManager;
    private static final int MAX_RESULTS = 100;

    public void setIdentityProviderFactory(IdentityProviderFactory ipf) {
        identityProviderFactory = ipf;        
    }

    public void setPolicyManager(PolicyManager policyManager) {
        this.policyManager = policyManager;
    }

    public void setKeyStoreManager(SsgKeyStoreManager keyStoreManager) {
        this.keyStoreManager = keyStoreManager;
    }

    @Transactional(readOnly=true)
    public EntityHeaderSet<EntityHeader> findAll(final Class<? extends Entity> entityClass) throws FindException {
        final boolean names = NamedEntity.class.isAssignableFrom(entityClass);
        final EntityType type = EntityType.findTypeByEntity(entityClass);
        try {
            if (EntityType.SSG_KEY_ENTRY == type)
                return findAllKeyHeaders();
            //noinspection unchecked
            else return (EntityHeaderSet<EntityHeader>)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException {
                    Criteria crit = session.createCriteria(entityClass);
                    ProjectionList pl = Projections.projectionList();
                    pl.add(Projections.property("oid"));
                    if (names) pl.add(Projections.property("name"));
                    crit.setProjection(pl);
                    crit.setMaxResults(MAX_RESULTS);
                    List arrays = crit.list();
                    EntityHeaderSet<EntityHeader> headers = new EntityHeaderSet<EntityHeader>();
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

                    if (arrays.size() >= MAX_RESULTS) headers.setMaxExceeded(MAX_RESULTS);

                    return headers;
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

    private EntityHeaderSet<EntityHeader> findAllKeyHeaders() throws FindException {
        try {
            EntityHeaderSet<EntityHeader> result = new EntityHeaderSet<EntityHeader>();
            for (SsgKeyFinder keyFinder : keyStoreManager.findAll()) {
                for (String alias : keyFinder.getAliases()) {
                    result.add(new SsgKeyHeader(keyFinder.getCertificateChain(alias)));
                }
            }
            return result;
        } catch (KeyStoreException e) {
            throw new FindException("Error looking up private key headers.", e);
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
        } else if (header instanceof SsgKeyHeader) {
            SsgKeyHeader keyHeader = (SsgKeyHeader) header;
            try {
                return keyStoreManager.lookupKeyByKeyAlias(keyHeader.getAlias(), keyHeader.getKeystoreId());
            } catch (Exception e) {
                throw new FindException("Error looking up key for: " + keyHeader, e);
            }
        } else if (EntityType.POLICY == header.getType()) {
            // some policies are identified by OID, others by GUID
            // prefer the strID/GUID, but also handle the cases when the strId is an OID
            String id = header.getStrId();
            try {
                return policyManager.findByPrimaryKey(Long.parseLong(id));
            } catch (NumberFormatException e) {
                return policyManager.findByGuid(id);
            }
        } else {
            return find(EntityTypeRegistry.getEntityClass(header.getType()), header.getStrId());
        }
    }

    @Transactional(readOnly=true)
    public <ET extends Entity> ET find(final Class<ET> clazz, Serializable pk) throws FindException {
        try {
            EntityType type = EntityTypeRegistry.getEntityType(clazz);
            Serializable tempPk;
            if (EntityType.SSG_KEY_ENTRY == type) {
                String id = (String) pk;
                int sepIndex = id.indexOf(":");
                return (ET) keyStoreManager.lookupKeyByKeyAlias(id.substring(sepIndex+1), Long.parseLong(id.substring(0,sepIndex)));
            } else if (pk instanceof String) {
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
    public EntityHeader findHeader( EntityType etype, Serializable pk) throws FindException {
        Entity e = find(etype.getEntityClass(), pk);
        if (e == null) return null;
        
        String name = null;
        if (e instanceof NamedEntity) {
            NamedEntity ne = (NamedEntity) e;
            name = ne.getName();
        }
        return new EntityHeader(e.getId(), etype, name, null);
    }
}
