/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.security.keystore.SsgKeyHeader;
import com.l7tech.identity.*;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.*;
import static com.l7tech.objectmodel.EntityType.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;

import java.util.Collection;
import java.util.HashSet;

public final class EntityHeaderUtils {
    /**
     * Creates and returns an {@link com.l7tech.objectmodel.AnonymousEntityReference} that's as close a reflection of the given
     * {@link com.l7tech.objectmodel.EntityHeader} as possible.  This method will create either an
     * {@link com.l7tech.objectmodel.AnonymousEntityReference}, an {@link com.l7tech.identity.AnonymousGroupReference}, or throw an exception.
     * @param header the EntityHeader to translate
     * @return the anonymous entity reference
     */
    public static AnonymousEntityReference fromHeader( EntityHeader header) {
        EntityType type = header.getType();
        if (type == ID_PROVIDER_CONFIG) {
            return new AnonymousEntityReference(IdentityProviderConfig.class, header.getOid(), header.getName());
        } else if (type == USER || type == GROUP) {
            long providerOid = IdentityProviderConfig.DEFAULT_OID;
            if (header instanceof IdentityHeader ) {
                IdentityHeader identityHeader = (IdentityHeader) header;
                providerOid = identityHeader.getProviderOid();
            }

            if (type == USER) {
                return new AnonymousUserReference(header.getStrId(), providerOid, header.getName());
            } else {
                return new AnonymousGroupReference(header.getStrId(), providerOid, header.getName());
            }
        } else {
            Class<? extends Entity> entityClass = EntityTypeRegistry.getEntityClass(type);
            if (ANY == type || entityClass == null)
                throw new IllegalArgumentException("Can't get reference to " + header.toString());
            else
                return new AnonymousEntityReference(entityClass, header.getOid(), header.getName());

        }
    }

    /**
     * Creates an EntityHeader from the provided Entity.  
     */
    @SuppressWarnings({"unchecked"})
    public static EntityHeader fromEntity(Entity e) {
        if (e instanceof Policy) {
            return new PolicyHeader((Policy)e);
        } else if (e instanceof PublishedService) {
            return new ServiceHeader((PublishedService)e);
        } else if ( e instanceof ServiceDocument) {
            return new EntityHeader(e.getId(), SERVICE_DOCUMENT, ((ServiceDocument)e).getUri(), null);
        } else if (e instanceof Folder) {
            return new FolderHeader((Folder)e);
        } else if (e instanceof FederatedUser) {
            FederatedUser user = (FederatedUser)e;
            return new IdentityHeader(user.getProviderId(), user.getOid(), USER, user.getName(), null, user.getName(), user.getVersion());
        } else if (e instanceof PersistentUser) {
            PersistentUser user = (PersistentUser)e;
            return new IdentityHeader(user.getProviderId(), user.getOid(), USER, user.getLogin(), null, user.getName(), user.getVersion());
        } else if (e instanceof User) {
            User user = (User)e;
            return new IdentityHeader(user.getProviderId(), user.getId(), USER, user.getLogin(), null, user.getName(), null);
        } else if (e instanceof PersistentGroup) {
            PersistentGroup group = (PersistentGroup)e;
            return new IdentityHeader(group.getProviderId(), group.getOid(), GROUP, group.getName(), null, group.getName(), group.getVersion());
        } else if (e instanceof Group) {
            Group group = (Group)e;
            return new IdentityHeader(group.getProviderId(), group.getId(), GROUP, group.getName(), null, group.getName(), null);
        } else if (e instanceof Alias) {
            return new AliasHeader( (Alias) e );
        } else if (e instanceof PersistentEntity) {
            PersistentEntity entity = (PersistentEntity) e;
            return new EntityHeader(entity.getOid(),
                                    EntityTypeRegistry.getEntityType(entity.getClass()),
                                    entity instanceof NamedEntity ? ((NamedEntity)entity).getName() : null,
                                    null,
                                    entity.getVersion());
        } else {
            return new EntityHeader(e.getId(),
                                    EntityTypeRegistry.getEntityType(e.getClass()),
                                    e instanceof NamedEntity ? ((NamedEntity)e).getName() : null,
                                    null);
        }
    }

    public static Collection<ExternalEntityHeader> toExternal(Collection<EntityHeader> headers) {
        Collection<ExternalEntityHeader> result = new HashSet<ExternalEntityHeader>();
        for (EntityHeader header : headers) {
            result.add(toExternal(header));
        }
        return result;
    }

    public static ExternalEntityHeader toExternal(final EntityHeader header) {
        ExternalEntityHeader externalEntityHeader;

        if (header instanceof ExternalEntityHeader) {
            externalEntityHeader = (ExternalEntityHeader) header;
        } else if (header instanceof OrganizationHeader && ((OrganizationHeader)header).isAlias()) {
            OrganizationHeader oh = ((OrganizationHeader)header);
            externalEntityHeader = new ExternalEntityHeader(oh.getAliasOid().toString(), // use the alias OID as the external ID
                EntityType.valueOf(header.getType().name() + "_ALIAS"),
                oh.getStrId(), oh.getName() + " Alias", oh.getDescription(), oh.getVersion());
        } else if (header instanceof PolicyHeader) {
            PolicyHeader policyHeader = (PolicyHeader) header;
            externalEntityHeader = new ExternalEntityHeader(policyHeader.getGuid(), header);
            externalEntityHeader.setProperty("SOAP", Boolean.toString(policyHeader.isSoap()));
        } else if (header instanceof ServiceHeader) {
            ServiceHeader serviceHeader = (ServiceHeader) header;
            externalEntityHeader = new ExternalEntityHeader(serviceHeader.getStrId(), header);
            externalEntityHeader.setProperty("Policy Version", Integer.toString(serviceHeader.getPolicyVersion()));
            externalEntityHeader.setProperty("Display Name", serviceHeader.getDisplayName());
            externalEntityHeader.setProperty("SOAP", Boolean.toString(serviceHeader.isSoap()));
            externalEntityHeader.setProperty("Enabled", Boolean.toString(!serviceHeader.isDisabled()));
        } else if (header instanceof IdentityHeader) {
            externalEntityHeader = new ExternalEntityHeader(((IdentityHeader)header).getProviderOid() + ":" + header.getStrId(), header);
            externalEntityHeader.setProperty("Scope Type", EntityType.ID_PROVIDER_CONFIG.toString());
        } else if (header instanceof SsgKeyHeader) {
            externalEntityHeader = new ExternalEntityHeader(((SsgKeyHeader)header).getKeystoreId() + ":" + ((SsgKeyHeader)header).getAlias(), header);
        } else if (header instanceof AliasHeader) {
            AliasHeader aliasHeader = (AliasHeader) header;
            externalEntityHeader = new ExternalEntityHeader( aliasHeader.getStrId(), aliasHeader );
            if ( aliasHeader.getAliasedEntityType() == EntityType.POLICY ) {
                externalEntityHeader.setProperty("Alias Of Internal", Long.toString(aliasHeader.getAliasedEntityId()));
            } else {
                externalEntityHeader.setProperty("Alias Of", Long.toString(aliasHeader.getAliasedEntityId()));
            }
            externalEntityHeader.setProperty("Alias Type", aliasHeader.getAliasedEntityType().toString());
        } else {
            externalEntityHeader = new ExternalEntityHeader(header.getStrId(), header);
        }

        return externalEntityHeader;
    }

    public static EntityHeader fromExternal(ExternalEntityHeader eh) {
        EntityHeader header;
        int sepIndex;
        switch (eh.getType()) {
            case POLICY:
                header = new GuidEntityHeader(eh.getStrId(), eh.getType(), eh.getName(), eh.getDescription(), eh.getVersion());
                ((GuidEntityHeader)header).setGuid(eh.getExternalId());
                break;

            case USER:
            case GROUP:
                if (!eh.getExternalId().contains(":"))
                    throw new IllegalArgumentException("Invalid ID found for external header of type " + eh.getType() + " : " + eh.getExternalId());
                sepIndex = eh.getExternalId().indexOf(":");
                header = new IdentityHeader(
                    Long.parseLong(eh.getExternalId().substring(0,sepIndex)),
                    eh.getExternalId().substring(sepIndex+1),
                    eh.getType(),
                    eh.getName(),
                    eh.getDescription(),
                    null,
                    eh.getVersion());
                break;

            case SSG_KEY_ENTRY:
                if (!eh.getExternalId().contains(":"))
                    throw new IllegalArgumentException("Invalid ID found for external header of type " + eh.getType() + " : " + eh.getExternalId());
                sepIndex = eh.getExternalId().indexOf(":");
                header = new SsgKeyHeader(
                    eh.getStrId(),
                    Long.parseLong(eh.getExternalId().substring(0,sepIndex)),
                    eh.getExternalId().substring(sepIndex+1),
                    eh.getName());
                break;

            case VALUE_REFERENCE:
                header = new ValueReferenceEntityHeader(eh);
                break;

            default:
                header = new EntityHeader(eh.getExternalId(), eh.getType(), eh.getName(), eh.getDescription(), eh.getVersion());
                break;
        }
        return header;
    }

    public static Class<? extends Entity> getEntityClass(EntityHeader header) {
        return EntityTypeRegistry.getEntityClass(header.getType());
    }

    private EntityHeaderUtils() { }
}