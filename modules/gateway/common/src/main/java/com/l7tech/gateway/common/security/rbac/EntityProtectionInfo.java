package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeaderRef;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * EntityProtection info.
 */
public final class EntityProtectionInfo implements Serializable {
    private static final long serialVersionUID = 2140250756247723622L;

    private final EntityType entityType;
    private final boolean readOnly;

    public EntityProtectionInfo(@NotNull final EntityType entityType, final boolean readOnly) {
        this.entityType = entityType;
        this.readOnly = readOnly;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean matchesTypeOf(@Nullable final Entity e) {
        return e != null && entityType.getEntityClass().isAssignableFrom(e.getClass());
    }

    public boolean matchesTypeOf(@Nullable final EntityHeaderRef eh) {
        return eh != null && entityType == eh.getType();
    }
}
