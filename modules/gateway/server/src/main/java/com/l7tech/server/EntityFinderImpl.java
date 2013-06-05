package com.l7tech.server;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyHeader;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.DesignTimeEntityProvider;
import com.l7tech.policy.PolicyUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.UsesEntitiesAtDesignTime;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.server.audit.AuditRecordManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
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
import java.sql.SQLException;
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

    public void setAuditRecordManager(@NotNull final AuditRecordManager auditRecordManager) {
        this.auditRecordManager = auditRecordManager;
    }

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
            else return getHibernateTemplate().execute(new ReadOnlyFindAll(entityClass, type));
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
            throw new FindException("Error looking up private key headers: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private EntityHeaderSet<EntityHeader> findAllKeyStoreHeaders() throws FindException {
        try {
            EntityHeaderSet<EntityHeader> result = new EntityHeaderSet<EntityHeader>();
            for (SsgKeyFinder ssgKeyFinder : keyStoreManager.findAll()) {
                result.add(new KeystoreFileEntityHeader(ssgKeyFinder.getOid(), ssgKeyFinder.getName(), ssgKeyFinder.getType().toString(), !ssgKeyFinder.isMutable()));
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
        } else if (EntityType.ENCAPSULATED_ASSERTION == header.getType()) {
            if (header instanceof GuidEntityHeader && encapsulatedAssertionConfigManager != null) {
                GuidEntityHeader guidHeader = (GuidEntityHeader) header;
                final String guid = guidHeader.getGuid();
                if (guid != null)
                    return encapsulatedAssertionConfigManager.findByGuid(guid);
            }
            return encapsulatedAssertionConfigManager.findByPrimaryKey(header.getOid());
        } else if (header instanceof ExternalAuditRecordHeader){
            // use mock entity objects for audit records from the external audits system
            ExternalAuditRecordHeader auditHeader = (ExternalAuditRecordHeader) header;
            if(auditHeader.getRecordType().equals(AuditRecordUtils.TYPE_ADMIN)){
                return new AdminAuditRecord(
                        auditHeader.getLevel(),auditHeader.getNodeId(),123,null, auditHeader.getName(),AdminAuditRecord.ACTION_OTHER,null,-2,"fake",null,null);
            }else if(auditHeader.getRecordType().equals(AuditRecordUtils.TYPE_MESSAGE)){
                return new MessageSummaryAuditRecord(
                        auditHeader.getLevel(),auditHeader.getNodeId(),  null,AssertionStatus.NONE,null,null, 3,
                        null, 3, 4, 3,45, null, null,false, SecurityTokenType.HTTP_BASIC,4,null, null,null);
            }else if(auditHeader.getRecordType().equals(AuditRecordUtils.TYPE_SYSTEM)){
               return  new SystemAuditRecord(auditHeader.getLevel(), auditHeader.getNodeId(), Component.GW_AUDIT_SYSTEM,
                       "fake", false, 0L, null, null, "fake", "0.0.0.0");
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
                return (ET) keyStoreManager.lookupKeyByKeyAlias(id.substring(sepIndex+1), Long.parseLong(id.substring(0,sepIndex)));
            } else if (EntityType.SSG_KEYSTORE == type) {
                return (ET) keyStoreManager.findByPrimaryKey(Long.valueOf((String)pk));
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
        EntityHeader header = new EntityHeader(e.getId(), etype, name, null);
        if (e instanceof ZoneableEntity) {
            final ZoneableEntity zoneableEntity = (ZoneableEntity) e;
            final SecurityZone zone = zoneableEntity.getSecurityZone();
            final ZoneableEntityHeader zoneableHeader = new ZoneableEntityHeader(header);
            zoneableHeader.setSecurityZoneOid(zone == null ? null : zone.getOid());
            header = zoneableHeader;
        }

        return header;
    }

    @Override
    public Collection<Entity> findByEntityTypeAndSecurityZoneOid(@NotNull final EntityType type, final long securityZoneOid) throws FindException {
        if (!type.isSecurityZoneable()) {
            throw new IllegalArgumentException("EntityType must be support security zones.");
        }
        final Collection<? extends Entity> found = findByClassAndSecurityZoneOid(type.getEntityClass(), securityZoneOid);
        return new ArrayList<>(found);
    }

    @SuppressWarnings({"unchecked"})
    private <ET extends Entity> Collection<ET> findByClassAndSecurityZoneOid(@NotNull final Class<ET> clazz, final long securityZoneOid) throws FindException {
        if (!ZoneableEntity.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("Class must be a ZoneableEntity.");
        }
        try {
            final List<ET> results = (List<ET>) getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Collection doInHibernateReadOnly(Session session) throws HibernateException {
                    final Criteria criteria = session.createCriteria(clazz);
                    criteria.add(Restrictions.eq("securityZone.oid", securityZoneOid));
                    return criteria.list();
                }
            });
            return results;
        } catch (final HibernateException e) {
            // can happen if the class doesn't store its security zone in the database
            throw new FindException("Unable to retrieve entities with class " + clazz.getName() + " and security zone oid " + securityZoneOid, e);
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

    class ReadOnlyFindAll extends ReadOnlyHibernateCallback<EntityHeaderSet<EntityHeader>> {
        private final Class<? extends Entity> entityClass;
        private final EntityType entityType;
        ReadOnlyFindAll(@NotNull final Class<? extends Entity> entityClass, @NotNull final EntityType entityType) {
            this.entityClass = entityClass;
            this.entityType = entityType;
        }
        @Override
        protected EntityHeaderSet<EntityHeader> doInHibernateReadOnly(final Session session) throws HibernateException, SQLException {
            final ClassMetadata metadata = getSessionFactory().getClassMetadata(entityClass);
            final Criteria criteria = session.createCriteria(entityClass);
            final ProjectionList pl = Projections.projectionList();

            // oid is required
            pl.add(Projections.property("oid"));

            // name is optional
            final boolean names = hasName(entityClass, metadata);
            if (names) pl.add(Projections.property("name"));

            // zone is optional
            boolean hasZone = false;
            if (ZoneableEntity.class.isAssignableFrom(entityClass)) {
                hasZone = true;
                pl.add(Projections.property("securityZone.oid"));
            }

            criteria.setProjection(pl);
            criteria.setMaxResults(MAX_RESULTS);
            List arrays = criteria.list();
            EntityHeaderSet<EntityHeader> headers = new EntityHeaderSet<EntityHeader>();
            for (Iterator i = arrays.iterator(); i.hasNext();) {
                final Long oid;
                String name = null;
                Long zoneOid = null;
                final Object next = i.next();
                if (next instanceof Object[]) {
                    final Object[] array = (Object[]) next;
                    oid = (Long) array[0];
                    if (names && hasZone) {
                        name = (String) array[1];
                        zoneOid = (Long) array[2];
                    } else if (names) {
                        name = (String) array[1];
                    } else if (hasZone) {
                        zoneOid = (Long) array[1];
                    }
                } else {
                    oid = (Long) next;
                }
                if(name == null || name.isEmpty()) name = oid.toString();
                EntityHeader header = new EntityHeader(oid.toString(), entityType, name, null);
                if (hasZone) {
                    final ZoneableEntityHeader zoneableHeader = new ZoneableEntityHeader(header);
                    zoneableHeader.setSecurityZoneOid(zoneOid);
                    header = zoneableHeader;
                }
                headers.add(header);
            }

            if (arrays.size() >= MAX_RESULTS) headers.setMaxExceeded(MAX_RESULTS);

            return headers;
        }
    }
}
