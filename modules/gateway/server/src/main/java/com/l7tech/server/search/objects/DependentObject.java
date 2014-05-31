package com.l7tech.server.search.objects;

import com.l7tech.search.Dependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is the dependent object base class
 *
 * @author Victor Kazakov
 */
public abstract class DependentObject {
    @Nullable
    private final String name;
    @NotNull
    private final Dependency.DependencyType dependencyType;

    /**
     * Creates a new Dependency Object with the given name and type
     *
     * @param name           The name of this dependency object. This is generally a nice human readable name.
     * @param dependencyType The dependency type
     */
    public DependentObject(@Nullable final String name, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType) {
        this.name = name;
        this.dependencyType = dependencyType;
    }

    /**
     * @return The name of the entity. Not all entities have names so this may be null.
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * @return The {@link com.l7tech.search.Dependency.DependencyType} of the entity
     */
    @NotNull
    public Dependency.DependencyType getDependencyType() {
        return dependencyType;
    }

    /*
    Don't check dependent object names when checking equality.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependentObject that = (DependentObject) o;

        if (dependencyType != that.dependencyType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return dependencyType.hashCode();
    }
}
