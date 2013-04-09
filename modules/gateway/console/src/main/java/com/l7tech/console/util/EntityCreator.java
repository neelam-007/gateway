package com.l7tech.console.util;

/**
 * Interface implemented by users of EntityCrudController in order to create new entity instances.
 */
public interface EntityCreator<ET> {
    /**
     * @return a new instance of ET, with no persistent identifier yet assigned.
     */
    ET createNewEntity();
}
