package com.l7tech.gateway.common.security.rbac;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Role-specific EntityHeader.
 */
public class RoleEntityHeader extends EntityHeader {
    private boolean userCreated;
    private Goid entityGoid;
    private EntityType entityType;

    /**
     * @param goid        the role Goid.
     * @param name        the role name.
     * @param description the optional role description.
     * @param version     the optional role version.
     * @param userCreated true if the role is a custom, user-created role.
     * @param entityGoid  the optional oid of an entity that the role is scoped to.
     * @param entityType  the optional EntityType of the entity that the role is scoped to.
     */
    public RoleEntityHeader(final Goid goid, final @NotNull String name, @Nullable final String description,
                            @Nullable final Integer version, final boolean userCreated, @Nullable final Goid entityGoid,
                            @Nullable final EntityType entityType) {
        super(goid, EntityType.RBAC_ROLE, name, description, version);
        setUserCreated(userCreated);
        setEntityOid(entityGoid);
        setEntityType(entityType);
    }

    /**
     * @return true if the role is a custom, user-created role.
     */
    public boolean isUserCreated() {
        return userCreated;
    }

    /**
     * @param userCreated set to true if the role is a custom, user-created role.
     */
    public void setUserCreated(final boolean userCreated) {
        this.userCreated = userCreated;
    }

    /**
     * @return if the role is scoped to a particular entity, the goid of the entity.
     */
    @Nullable
    public Goid getEntityGoid() {
        return entityGoid;
    }

    /**
     * @param entityGoid the goid of an entity that the role is scoped to.
     */
    public void setEntityOid(@Nullable final Goid entityGoid) {
        this.entityGoid = entityGoid;
    }

    /**
     * @return if the role is scoped to a particular entity, the EntityType of the entity.
     */
    @Nullable
    public EntityType getEntityType() {
        return entityType;
    }

    /**
     * @param entityType the EntityType of an entity that the role is scoped to.
     */
    public void setEntityType(@Nullable final EntityType entityType) {
        this.entityType = entityType;
    }
}
