package com.l7tech.server.search.objects;

import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;

/**
 * The DependentEntity is a reference to an entity that cannot be found.
 *
 */
public class BrokenDependentEntity extends DependentEntity {

    /**
     * Creates a new DependentEntity.
     *
     * @param entityHeader The Dependent Entity Header.
     */
    public BrokenDependentEntity(@NotNull final EntityHeader entityHeader) {
        super(entityHeader.getStrId(), entityHeader);
    }
}
