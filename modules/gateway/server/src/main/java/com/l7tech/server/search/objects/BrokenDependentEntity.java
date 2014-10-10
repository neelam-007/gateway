package com.l7tech.server.search.objects;

import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;

/**
 * The DependentEntity is a reference to an entity that cannot be found.
 *
 */
public class BrokenDependentEntity extends DependentEntity {
    @NotNull
    private final EntityHeader entityHeader;

    /**
     * Creates a new DependentEntity.
     *
     * @param entityHeader The Dependent Entity Header.
     */
    public BrokenDependentEntity(@NotNull final EntityHeader entityHeader) {
        super(entityHeader.getStrId(), entityHeader);
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

        BrokenDependentEntity that = (BrokenDependentEntity) o;

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
