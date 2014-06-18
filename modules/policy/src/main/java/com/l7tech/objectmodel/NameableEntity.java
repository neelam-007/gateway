package com.l7tech.objectmodel;

/**
 * Nameable entities are named entities that can have their names changed.
 */
public interface NameableEntity extends NamedEntity {
    /**
     * Set the name of this entity.
     *
     * @param name The name to set on the entity
     */
    void setName(String name);
}
