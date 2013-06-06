package com.l7tech.server.search;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * This is a dependency object representing a dependency of an entity.
 *
 * @author Victor Kazakov
 */
public class Dependency {

    private final DependencyEntity entity;
    private List<Dependency> dependencies;
    private boolean dependenciesSet = false;

    /**
     * Creates a new dependency object for the given entity. The entities dependencies will be marked as undiscovered
     * ({@link #areDependenciesSet()} will return false )
     *
     * @param entity The entity for this dependency object.
     */
    protected Dependency(@NotNull DependencyEntity entity) {
        this.entity = entity;
    }

    /**
     * Creates a new dependency object for the given entity. The entity will also have the given dependencies. The
     * dependencies for this dependency will be marked are being discovered. {@link #areDependenciesSet()} will return
     * true.
     *
     * @param entity       The entity for this dependency object.
     * @param dependencies The set of dependencies that this dependency has.
     */
    protected Dependency(@NotNull DependencyEntity entity, @NotNull List<Dependency> dependencies) {
        this.entity = entity;
        this.dependencies = Collections.unmodifiableList(dependencies);
        dependenciesSet = true;
    }

    /**
     * Returns the entity that this dependency represents.
     *
     * @return The entity for this dependency
     */
    public DependencyEntity getEntity() {
        return entity;
    }

    /**
     * Returns the set of dependencies that this dependency have. Note that if the dependencies have not yet been
     * discovered null is returned. If an empty set is returned that means that this entity does not have any
     * dependencies.
     *
     * @return The set of dependencies that this entity has or null if the dependencies have not been discovered.
     */
    @Nullable
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * Sets the dependencies that this dependency has.
     *
     * @param dependencies The dependencies that this dependency has.
     */
    protected void setDependencies(@NotNull List<Dependency> dependencies) {
        this.dependencies = dependencies;
        dependenciesSet = true;
    }

    /**
     * Checks if the dependencies for this dependency have been discovered.
     *
     * @return returns true if the dependencies have been discovered. false otherwise.
     */
    public boolean areDependenciesSet() {
        return dependenciesSet;
    }
}
