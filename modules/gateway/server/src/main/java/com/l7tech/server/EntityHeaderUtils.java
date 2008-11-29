/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;

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
        if (type == EntityType.ID_PROVIDER_CONFIG) {
            return new AnonymousEntityReference(IdentityProviderConfig.class, header.getOid(), header.getName());
        } else if (type == EntityType.USER || type == EntityType.GROUP) {
            long providerOid = IdentityProviderConfig.DEFAULT_OID;
            if (header instanceof IdentityHeader ) {
                IdentityHeader identityHeader = (IdentityHeader) header;
                providerOid = identityHeader.getProviderOid();
            }

            if (type == EntityType.USER) {
                return new AnonymousUserReference(header.getStrId(), providerOid, header.getName());
            } else {
                return new AnonymousGroupReference(header.getStrId(), providerOid, header.getName());
            }
        } else {
            Class<? extends Entity> entityClass = EntityTypeRegistry.getEntityClass(type);
            if (EntityType.ANY == type || entityClass == null)
                throw new IllegalArgumentException("Can't get reference to " + header.toString());
            else
                return new AnonymousEntityReference(entityClass, header.getOid(), header.getName());

        }
    }

    /**
     * Creates an EntityHeader from the provided Entity.  
     */
    public static EntityHeader fromEntity(Entity e) {
        if (e instanceof Policy) {
            return new PolicyHeader((Policy)e);
        } else if (e instanceof PublishedService) {
            return new ServiceHeader((PublishedService)e);
        } else if (e instanceof Folder) {
            return new FolderHeader((Folder)e);
        } else if (e instanceof User) {
            User user = (User)e;
            return new IdentityHeader(user.getProviderId(), user.getId(), EntityType.USER, user.getLogin(), null, user.getName());
        } else if (e instanceof Group) {
            Group group = (Group)e;
            return new IdentityHeader(group.getProviderId(), group.getId(), EntityType.GROUP, group.getName(), null, group.getName());
        } else {
            return new EntityHeader(e.getId(),
                                    EntityTypeRegistry.getEntityType(e.getClass()),
                                    e instanceof NamedEntity ? ((NamedEntity)e).getName() : null,
                                    null);
        }
    }


    public static Class<? extends Entity> getEntityClass(EntityHeader header) {
        return EntityTypeRegistry.getEntityClass(header.getType());
    }

    private EntityHeaderUtils() { }
}