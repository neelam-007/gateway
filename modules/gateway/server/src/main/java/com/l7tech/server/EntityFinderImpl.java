package com.l7tech.server;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.DesignTimeEntityProvider;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.PolicyUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.UsesEntitiesAtDesignTime;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.server.audit.AuditRecordManager;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.GoidUpgradeMapper;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class EntityFinderImpl extends HibernateDaoSupport implements EntityFinder, DesignTimeEntityProvider {
    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private static final Logger logger = Logger.getLogger(EntityFinderImpl.class.getName());
    private IdentityProviderFactory identityProviderFactory;
    private PolicyManager policyManager;
    private SsgKeyStoreManager keyStoreManager;
    private GenericEntityManager genericEntityManager;
    private EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;
    private AuditRecordManager auditRecordManager;

    private static final int MAX_RESULTS = 100;

    public void setEncapsulatedAssertionConfigManager(EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager) {
        this.encapsulatedAssertionConfigManager = encapsulatedAssertionConfigManager;
    }

    public void setIdentityProviderFactory(IdentityProviderFactory ipf) {
        identityProviderFactory = ipf;        
    }

    public void setPolicyManager(PolicyManager policyManager) {
        this.policyManager = policyManager;
    }

    public void setKeyStoreManager(SsgKeyStoreManager keyStoreManager) {
        this.keyStoreManager = keyStoreManager;
    }

    public void setGenericEntityManager(GenericEntityManager genericEntityManager) {
        this.genericEntityManager = genericEntityManager;
    }

    public void setAuditRecordManager(@NotNull final AuditRecordManager auditRecordManager) {
        this.auditRecordManager = auditRecordManager;
    }

    /**
     * In most cases this will return a set of generic EntityHeader.
     *
     * Do not use this if you need the type-specific EntityHeaders. (use EntityCrudImpl instead).
     */
    @Override
    @Transactional(readOnly=true)
    public EntityHeaderSet<EntityHeader> findAll(final Class<? extends Entity> entityClass) throws FindException {
        if(entityClass == null) throw new IllegalArgumentException("entity class can not be null");
        final EntityType type = EntityType.findTypeByEntity(entityClass);
        try {
            if (EntityType.SSG_KEY_ENTRY == type)
                return findAllKeyHeaders();
            else if (EntityType.SSG_KEYSTORE == type)
                return findAllKeyStoreHeaders();
            //noinspection unchecked
            else if(!(Entity.class.isAssignableFrom(entityClass) || EntityType.USER.equals(type) || EntityType.GROUP.equals(type)))
                throw new UnsupportedEntityTypeException("The entity type of '" + type.getName() + "' is not supported.");
            else return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<EntityHeaderSet<EntityHeader>>() {
                @Override
                public EntityHeaderSet<EntityHeader> doInHibernateReadOnly(Session session) throws HibernateException {
                    final ClassMetadata metadata = getSessionFactory().getClassMetadata(entityClass);
                    final Criteria criteria = session.createCriteria(entityClass);
                    final ProjectionList pl = Projections.projectionList();
                    pl.add(Projections.property("goid"));
                    final boolean names = hasName(entityClass, metadata);
                    if (names) pl.add(Projections.property("name"));
                    criteria.setProjection(pl);
                    criteria.setMaxResults(MAX_RESULTS);
                    List arrays = criteria.list();
                    EntityHeaderSet<EntityHeader> headers = new EntityHeaderSet<EntityHeader>();
                    for (Iterator i = arrays.iterator(); i.hasNext();) {
                        String id;
                        String name;
                        if (names) {
                            Object[] array = (Object[]) i.next();
                            id = array[0].toString();
                            name = (String) array[1];
                        } else {
                            id = i.next().toString();
                            name = null;
                        }
                        if(name == null || name.isEmpty()) name = id;
                        headers.add(new EntityHeader(id, type, name, null));
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
                    final SsgKeyEntry key = keyFinder.getCertificateChain(alias);
                    result.add(new SsgKeyHeader(key.getId(), key.getKeystoreId(), key.getAlias(), key.getName()));
                }
            }
            return result;
        } catch (KeyStoreException e) {
            throw new FindException("Error looking up private key headers: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private EntityHeaderSet<EntityHeader> findAllKeyStoreHeaders() throws FindException {
        try {
            EntityHeaderSet<EntityHeader> result = new EntityHeaderSet<EntityHeader>();
            for (SsgKeyFinder ssgKeyFinder : keyStoreManager.findAll()) {
                result.add(new KeystoreFileEntityHeader(ssgKeyFinder.getGoid(), ssgKeyFinder.getName(), ssgKeyFinder.getType().toString(), !ssgKeyFinder.isMutable()));
            }
            return result;
        } catch (KeyStoreException e) {
            throw new FindException("Error looking up key store headers: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    @Transactional(readOnly=true)
    public Entity find(@NotNull EntityHeader header) throws FindException {
        if (header instanceof IdentityHeader) {
            IdentityHeader identityHeader = (IdentityHeader)header;
            IdentityProvider provider = identityProviderFactory.getProvider(identityHeader.getProviderGoid());
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
            // some policies are identified by GOID, others by GUID
            // prefer the strID/GUID, but also handle the cases when the strId is an GOID
            String id = header.getStrId();
            try {
                return policyManager.findByPrimaryKey(Goid.parseGoid(id));
            } catch (IllegalArgumentException e) {
                return policyManager.findByGuid(id);
            }
        } else if (EntityType.ENCAPSULATED_ASSERTION == header.getType()) {
            if (header instanceof GuidEntityHeader && encapsulatedAssertionConfigManager != null) {
                GuidEntityHeader guidHeader = (GuidEntityHeader) header;
                final String guid = guidHeader.getGuid();
                if (guid != null)
                    return encapsulatedAssertionConfigManager.findByGuid(guid);
            }
            return encapsulatedAssertionConfigManager.findByPrimaryKey(header.getGoid());
        } else if (header instanceof ExternalAuditRecordHeader){
            // use mock entity objects for audit records from the external audits system
            ExternalAuditRecordHeader auditHeader = (ExternalAuditRecordHeader) header;
            if(auditHeader.getRecordType().equals(AuditRecordUtils.TYPE_ADMIN)){
                return new AdminAuditRecord(
                        auditHeader.getLevel(),auditHeader.getNodeId(),new Goid(987,123),null, auditHeader.getName(),AdminAuditRecord.ACTION_OTHER,null,new Goid(123,456),"fake",null,null);
            }else if(auditHeader.getRecordType().equals(AuditRecordUtils.TYPE_MESSAGE)){
                return new MessageSummaryAuditRecord(
                        auditHeader.getLevel(),auditHeader.getNodeId(),  null,AssertionStatus.NONE,null,null, 3,
                        null, 3, 4, 3,new Goid(0,45), null, null,false, SecurityTokenType.HTTP_BASIC,new Goid(123,456),null, null,null);
            }else if(auditHeader.getRecordType().equals(AuditRecordUtils.TYPE_SYSTEM)){
               return  new SystemAuditRecord(auditHeader.getLevel(), auditHeader.getNodeId(), Component.GW_AUDIT_SYSTEM,
                       "fake", false, new Goid(0,0), null, null, "fake", "0.0.0.0");
            }
            throw new FindException("Error looking audit record type: " + auditHeader.getRecordType());
        } else if(header instanceof AuditRecordHeader) {
            return auditRecordManager.findByHeader(header);
        } else {
            return find(EntityTypeRegistry.getEntityClass(header.getType()), header.getStrId());
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    @Transactional(readOnly=true)
    public <ET extends Entity> ET find(final Class<ET> clazz, Serializable pk) throws FindException {
        try {
            EntityType type = EntityTypeRegistry.getEntityType(clazz);
            Serializable tempPk;
            if (EntityType.SSG_KEY_ENTRY == type) {
                String id = (String) pk;
                int sepIndex = id.indexOf(":");
                return (ET) keyStoreManager.lookupKeyByKeyAlias(id.substring(sepIndex+1), GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, id.substring(0, sepIndex)));
            } else if (EntityType.SSG_KEYSTORE == type) {
                return (ET) keyStoreManager.findByPrimaryKey(GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, (String)pk));
            } else if (EntityType.GENERIC == type) {
                return (ET) genericEntityManager.findByGenericClassAndPrimaryKey((Class<GenericEntity>)clazz, (pk instanceof Goid) ? (Goid) pk : GoidUpgradeMapper.mapId(EntityType.GENERIC, pk.toString()));
            } else if (PersistentEntity.class.isAssignableFrom(clazz)) {
                try {
                    tempPk = (pk instanceof Goid)?(Goid)pk:Goid.parseGoid(pk.toString());
                } catch (IllegalArgumentException iae) {
                    logger.fine("Primary key "+pk+" is not a valid key; using String value instead");
                    tempPk = pk;
                }
            } else {
                tempPk = pk;
            }

            final Serializable finalPk = tempPk;
            //noinspection unchecked
            return (ET)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
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

    @Override
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

    @Override
    public Collection<EntityHeader> findByEntityTypeAndSecurityZoneGoid(@NotNull final EntityType type, final Goid securityZoneGoid) throws FindException {
        if (!type.isSecurityZoneable()) {
            throw new IllegalArgumentException("EntityType must be support security zones.");
        }
        final Collection<? extends Entity> found = findByClassAndSecurityZoneGoid(type.getEntityClass(), securityZoneGoid);
        final Collection<EntityHeader> headers = new ArrayList<>();
        for (final Entity entity : found) {
            headers.add(EntityHeaderUtils.fromEntity(entity));
        }
        return headers;
    }

    @SuppressWarnings({"unchecked"})
    private <ET extends Entity> Collection<ET> findByClassAndSecurityZoneGoid(@NotNull final Class<ET> clazz, final Goid securityZoneGoid) throws FindException {
        if (!ZoneableEntity.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class must be a ZoneableEntity.");
        }
        try {
            final List<ET> results = (List<ET>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Collection doInHibernateReadOnly(Session session) throws HibernateException {
                    final Criteria criteria = session.createCriteria(clazz);
                    criteria.add(Restrictions.eq("securityZone.goid", securityZoneGoid));
                    return criteria.list();
                }
            });
            return results;
        } catch (final HibernateException e) {
            // can happen if the class doesn't store its security zone in the database
            throw new FindException("Unable to retrieve entities with class " + clazz.getName() + " and security zone oid " + securityZoneGoid, e);
        }
    }

    private boolean hasName(final Class entityClass, final ClassMetadata metadata) {
        if(Group.class.isAssignableFrom(entityClass) || User.class.isAssignableFrom(entityClass)) return true;
        if(metadata != null){
            try {
                for(String prop : metadata.getPropertyNames()){
                    if("name".equals(prop)) return true;
                }
            } catch (MappingException e) {
                //name column/property does not exist
                logger.warning("property 'name' does not exist in " + entityClass.getClass().getName());
            }
        }
        return false;
    }

    @Override
    public void provideNeededEntities(@NotNull UsesEntitiesAtDesignTime entityUser, @Nullable Functions.BinaryVoid<EntityHeader, FindException> errorHandler) throws FindException {
        PolicyUtil.provideNeededEntities(entityUser, this, errorHandler);
    }
}
