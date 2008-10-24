/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.AnonymousEntityReference;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.IdentityHeader;

/**
 * @author alex
 */
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
            } else if (type == EntityType.GROUP) {
                return new AnonymousGroupReference(header.getStrId(), providerOid, header.getName());
            } else {
                throw new IllegalStateException(); // Covered by outer if
            }
        } else {
            Class<? extends Entity> entityClass = EntityTypeRegistry.getEntityClass(type);
            if (EntityType.ANY == type || entityClass == null)
                throw new IllegalArgumentException("Can't get reference to " + header.toString());
            else
                return new AnonymousEntityReference(entityClass, header.getOid(), header.getName());

        }
    }

    public static Class<? extends Entity> getEntityClass(EntityHeader header) {
        return fromHeader(header).getEntityClass();
    }

    private EntityHeaderUtils() { }
}
