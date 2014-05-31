package com.l7tech.server.search.objects;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.search.Dependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The DependentEntity is a reference to an entity. Contains the entity header.
 *
 * @author Victor Kazakov
 */
public class DependentEntity extends DependentObject {
    @NotNull
    private final EntityHeader entityHeader;

    /**
     * Creates a new DependentEntity.
     *
     * @param name         The name of the entity. This is meant to be a human readable name to help easily identify the
     *                     entity.
     * @param entityHeader The Dependent Entity Header.
     */
    public DependentEntity(@Nullable final String name, @NotNull final EntityHeader entityHeader) {
        super(name, Dependency.DependencyType.fromEntityType(entityHeader.getType()));
        this.entityHeader = entityHeader;
    }

    /**
     * @return The dependent entity Header.
     */
    @NotNull
    public EntityHeader getEntityHeader() {
        return entityHeader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DependentEntity that = (DependentEntity) o;

        if (!entityHeader.equals(that.entityHeader)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + entityHeader.hashCode();
        return result;
    }
}
