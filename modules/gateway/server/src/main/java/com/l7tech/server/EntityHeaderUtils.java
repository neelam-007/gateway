/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgActiveConnectorHeader;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.*;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

import static com.l7tech.objectmodel.EntityType.*;

public final class EntityHeaderUtils {
    /**
     * Creates and returns an {@link com.l7tech.objectmodel.AnonymousEntityReference} that's as close a reflection of the given
     * {@link com.l7tech.objectmodel.EntityHeader} as possible.  This method will create either an
     * {@link com.l7tech.objectmodel.AnonymousEntityReference}, an {@link com.l7tech.identity.AnonymousGroupReference}, or throw an exception.
     *
     * @param header the EntityHeader to translate
     * @return the anonymous entity reference
     */
    public static AnonymousEntityReference fromHeader(EntityHeader header) {
        EntityType type = header.getType();
        if (type == ID_PROVIDER_CONFIG) {
            return new AnonymousEntityReference(IdentityProviderConfig.class, header.getGoid(), header.getName());
        } else if (type == USER || type == GROUP) {
            Goid providerOid = IdentityProviderConfig.DEFAULT_GOID;
            if (header instanceof IdentityHeader) {
                IdentityHeader identityHeader = (IdentityHeader) header;
                providerOid = identityHeader.getProviderGoid();
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
            return new PolicyHeader((Policy) e);
        } else if (e instanceof PublishedService) {
            return new ServiceHeader((PublishedService) e);
        } else if (e instanceof ResourceEntry) {
            final ResourceEntry resourceEntry = (ResourceEntry) e;
            return new ResourceEntryHeader(resourceEntry);
        } else if (e instanceof ServiceDocument) {
            return new EntityHeader(e.getId(), SERVICE_DOCUMENT, ((ServiceDocument) e).getUri(), null);
        } else if (e instanceof Folder) {
            return new FolderHeader((Folder) e);
        } else if (e instanceof FederatedUser) {
            FederatedUser user = (FederatedUser) e;
            return new IdentityHeader(user.getProviderId(), user.getGoid(), USER, user.getLogin(), null, user.getName(), user.getVersion());
        } else if (e instanceof PersistentUser) {
            PersistentUser user = (PersistentUser) e;
            return new IdentityHeader(user.getProviderId(), user.getGoid(), USER, user.getLogin(), null, user.getName(), user.getVersion());
        } else if (e instanceof User) {
            User user = (User) e;
            return new IdentityHeader(user.getProviderId(), user.getId(), USER, user.getLogin(), null, user.getName(), null);
        } else if (e instanceof PersistentGroup) {
            PersistentGroup group = (PersistentGroup) e;
            return new IdentityHeader(group.getProviderId(), group.getGoid(), GROUP, group.getName(), null, group.getName(), group.getVersion());
        } else if (e instanceof Group) {
            Group group = (Group) e;
            return new IdentityHeader(group.getProviderId(), group.getId(), GROUP, group.getName(), null, group.getName(), null);
        } else if (e instanceof Alias) {
            return new AliasHeader((Alias) e);
        } else if (e instanceof JmsEndpoint) {
            JmsEndpoint endpoint = (JmsEndpoint) e;
            final JmsEndpointHeader header = new JmsEndpointHeader(endpoint.getId(), endpoint.getName(), endpoint.getDestinationName(), endpoint.getVersion(), endpoint.isMessageSource());
            header.setSecurityZoneGoid(endpoint.getSecurityZone() == null ? null : endpoint.getSecurityZone().getGoid());
            header.setConnectionGoid(endpoint.getConnectionGoid());
            return header;
        } else if (e instanceof SsgActiveConnector) {
            return new SsgActiveConnectorHeader((SsgActiveConnector) e);
        } else if (e instanceof EncapsulatedAssertionConfig) {
            final EncapsulatedAssertionConfig config = (EncapsulatedAssertionConfig) e;
            final ZoneableGuidEntityHeader guidEntityHeader = new ZoneableGuidEntityHeader(config.getGoid().toString(), ENCAPSULATED_ASSERTION, config.getName(), null, config.getVersion());
            guidEntityHeader.setGuid(config.getGuid());
            guidEntityHeader.setSecurityZoneGoid(config.getSecurityZone() == null ? null : config.getSecurityZone().getGoid());
            return guidEntityHeader;
        } else if (e instanceof PersistentEntity) {
            PersistentEntity entity = (PersistentEntity) e;
            EntityHeader entityHeader = new EntityHeader(entity.getOid(),
                    EntityTypeRegistry.getEntityType(entity.getClass()),
                    entity instanceof NamedEntity ? ((NamedEntity) entity).getName() : null,
                    null,
                    entity.getVersion());
            return createZoneableHeaderIfPossible(e, entityHeader);
        } else if (e instanceof GoidEntity) {
            GoidEntity entity = (GoidEntity) e;
            EntityHeader entityHeader = new EntityHeader(entity.getGoid(),
                    EntityTypeRegistry.getEntityType(entity.getClass()),
                    entity instanceof NamedEntity ? ((NamedEntity) entity).getName() : null,
                    null,
                    entity.getVersion());
            return createZoneableHeaderIfPossible(e, entityHeader);
        } else {
            final EntityHeader entityHeader = new EntityHeader(e.getId(),
                    EntityTypeRegistry.getEntityType(e.getClass()),
                    e instanceof NamedEntity ? ((NamedEntity) e).getName() : null,
                    null);
            return createZoneableHeaderIfPossible(e, entityHeader);
        }
    }

    /**
     * Creates a ZoneableEntityHeader by copying the existing EntityHeader if the Entity is a ZoneableEntity.
     *
     * @param entity the Entity which may or may not be a ZoneableEntity.
     * @param entityHeader the EntityHeader for the Entity.
     * @return a ZoneableEntityHeader if the entity is a ZoneableEntity otherwise just returns the given entityHeader.
     */
    private static EntityHeader createZoneableHeaderIfPossible(@NotNull final Entity entity, @NotNull final EntityHeader entityHeader) {
        if (entity instanceof ZoneableEntity) {
            final ZoneableEntity zoneable = (ZoneableEntity) entity;
            final ZoneableEntityHeader zoneableHeader = new ZoneableEntityHeader(entityHeader);
            zoneableHeader.setSecurityZoneGoid(zoneable.getSecurityZone() == null ? null : zoneable.getSecurityZone().getGoid());
            return zoneableHeader;
        }
        return entityHeader;
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
        } else if (header instanceof OrganizationHeader && ((OrganizationHeader) header).isAlias()) {
            OrganizationHeader oh = ((OrganizationHeader) header);
            externalEntityHeader = new ExternalEntityHeader(oh.getAliasGoid().toString(), // use the alias OID as the external ID
                    EntityType.valueOf(header.getType().name() + "_ALIAS"),
                    oh.getStrId(), oh.getName() + " Alias", oh.getDescription(), oh.getVersion());
        } else if (header instanceof PolicyHeader) {
            PolicyHeader policyHeader = (PolicyHeader) header;
            externalEntityHeader = new ExternalEntityHeader(policyHeader.getGuid(), header);
            externalEntityHeader.setProperty("Policy Revision", Long.toString(policyHeader.getPolicyRevision()));
            externalEntityHeader.setProperty("SOAP", Boolean.toString(policyHeader.isSoap()));
        } else if (header instanceof ServiceHeader) {
            ServiceHeader serviceHeader = (ServiceHeader) header;
            externalEntityHeader = new ExternalEntityHeader(serviceHeader.getStrId(), header);
            externalEntityHeader.setProperty("Policy Revision", Long.toString(serviceHeader.getPolicyRevision()));
            externalEntityHeader.setProperty("Display Name", serviceHeader.getDisplayName());
            externalEntityHeader.setProperty("SOAP", Boolean.toString(serviceHeader.isSoap()));
            externalEntityHeader.setProperty("Enabled", Boolean.toString(!serviceHeader.isDisabled()));
        } else if (header instanceof IdentityHeader) {
            IdentityHeader idHeader = (IdentityHeader) header;
            externalEntityHeader = new ExternalEntityHeader(idHeader.getProviderGoid() + ":" + header.getStrId(), header);
            externalEntityHeader.setProperty("Display Name", (idHeader.getName() != null ? idHeader.getName() : "") +
                    (idHeader.getCommonName() != null ? " (" + idHeader.getCommonName() + ")" : ""));
            externalEntityHeader.setProperty("Scope Type", EntityType.ID_PROVIDER_CONFIG.toString());
        } else if (header instanceof SsgKeyHeader) {
            externalEntityHeader = new ExternalEntityHeader(((SsgKeyHeader) header).getKeystoreId() + ":" + ((SsgKeyHeader) header).getAlias(), header);
        } else if (header instanceof AliasHeader) {
            AliasHeader aliasHeader = (AliasHeader) header;
            externalEntityHeader = new ExternalEntityHeader(aliasHeader.getStrId(), aliasHeader);
            if (aliasHeader.getAliasedEntityType() == EntityType.POLICY) {
                externalEntityHeader.setProperty("Alias Of Internal", Goid.toString(aliasHeader.getAliasedEntityId()));
            } else {
                externalEntityHeader.setProperty("Alias Of", Goid.toString(aliasHeader.getAliasedEntityId()));
            }
            externalEntityHeader.setProperty("Alias Type", aliasHeader.getAliasedEntityType().toString());
        } else if (header instanceof JmsEndpointHeader) {
            externalEntityHeader = new ExternalEntityHeader(header.getStrId(), header);
            externalEntityHeader.setProperty("messageSource", Boolean.toString(((JmsEndpointHeader) header).isIncoming()));
        } else if (header instanceof SsgActiveConnectorHeader) {
            final SsgActiveConnectorHeader ssgActiveConnectorHeader = (SsgActiveConnectorHeader) header;
            externalEntityHeader = new ExternalEntityHeader(header.getStrId(), header);
            externalEntityHeader.setProperty("inbound", Boolean.toString(ssgActiveConnectorHeader.isInbound()));
            externalEntityHeader.setProperty("connectorType", ssgActiveConnectorHeader.getConnectorType());
        } else if (header instanceof ResourceEntryHeader) {
            final ResourceEntryHeader resourceEntryHeader = (ResourceEntryHeader) header;
            externalEntityHeader = new ExternalEntityHeader(header.getStrId(), header.getType(), header.getStrId(), resourceEntryHeader.getUri(), header.getDescription(), header.getVersion());
            externalEntityHeader.setProperty("resourceType", resourceEntryHeader.getResourceType().name());
            if (resourceEntryHeader.getResourceKey1() != null)
                externalEntityHeader.setProperty("resourceKey1", resourceEntryHeader.getResourceKey1());
            if (resourceEntryHeader.getResourceKey2() != null)
                externalEntityHeader.setProperty("resourceKey2", resourceEntryHeader.getResourceKey2());
            if (resourceEntryHeader.getResourceKey3() != null)
                externalEntityHeader.setProperty("resourceKey3", resourceEntryHeader.getResourceKey3());
        } else if (header instanceof GuidEntityHeader) {
            final GuidEntityHeader guidEntityHeader = (GuidEntityHeader) header;
            externalEntityHeader = new ExternalEntityHeader(guidEntityHeader.getGuid(), header.getType(), guidEntityHeader.getGuid(), header.getName(), header.getDescription(), header.getVersion());
        } else {
            externalEntityHeader = new ExternalEntityHeader(header.getStrId(), header);
        }

        return externalEntityHeader;
    }

    public static EntityHeader fromExternal(ExternalEntityHeader eh) {
        return fromExternal(eh, true);
    }

    public static EntityHeader fromExternal(ExternalEntityHeader eh, boolean strict) {
        EntityHeader header;
        int sepIndex;
        switch (eh.getType()) {
            case POLICY:
                header = new GuidEntityHeader(eh.getStrId(), eh.getType(), eh.getName(), eh.getDescription(), eh.getVersion());
                ((GuidEntityHeader) header).setGuid(eh.getExternalId());
                break;

            case USER:
            case GROUP:
                if (!eh.getExternalId().contains(":"))
                    throw new IllegalArgumentException("Invalid ID found for external header of type " + eh.getType() + " : " + eh.getExternalId());
                sepIndex = eh.getExternalId().indexOf(":");
                header = new IdentityHeader(
                        Goid.parseGoid(eh.getExternalId().substring(0, sepIndex)),
                        eh.getExternalId().substring(sepIndex + 1),
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
                        GoidUpgradeMapper.mapId(SSG_KEYSTORE, eh.getExternalId().substring(0, sepIndex)),
                        eh.getExternalId().substring(sepIndex + 1),
                        eh.getName());
                break;

            case JMS_ENDPOINT:
                header = new JmsEndpointHeader(eh.getStrId(), eh.getName(), eh.getDescription(), eh.getVersion(), Boolean.parseBoolean(eh.getProperty("messageSource")));
                break;

            case RESOURCE_ENTRY:
                final ResourceType resourceType;
                final String resourceTypeStr = eh.getProperty("resourceType");
                if (resourceTypeStr == null) {
                    if (strict) {
                        throw new IllegalArgumentException("Missing resource type");
                    } else {
                        resourceType = null;
                    }
                } else {
                    resourceType = ResourceType.valueOf(resourceTypeStr);
                }
                header = new ResourceEntryHeader(eh.getStrId(), eh.getName(), eh.getDescription(), resourceType, eh.getProperty("resourceKey1"), eh.getProperty("resourceKey2"), eh.getProperty("resourceKey3"), eh.getVersion(), null);
                break;

            case VALUE_REFERENCE:
                header = new ValueReferenceEntityHeader(eh);
                break;

            case ENCAPSULATED_ASSERTION:
                header = new GuidEntityHeader(eh.getStrId(), eh.getType(), eh.getName(), eh.getDescription(), eh.getVersion());
                ((GuidEntityHeader) header).setGuid(eh.getStrId());
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

    private EntityHeaderUtils() {
    }
}
