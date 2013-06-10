package com.l7tech.server.search.objects;

/**
 * This was created: 6/10/13 as 11:29 AM
 *
 * @author Victor Kazakov
 */
public abstract class DependentObject {
    private final String name;

    public DependentObject(String name) {
        this.name = name;
    }

    /**
     * @return The name of the entity. Not all entities have names so this may be null.
     */
    public String getName() {
        return name;
    }
}
