package com.l7tech.server.search.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * The dependency object contains the dependencies of a dependent object.
 *
 * @author Victor Kazakov
 */
public class Dependency {

    @NotNull
    private final DependentObject dependent;
    private List<Dependency> dependencies;

    /**
     * Creates a new dependency object for the given dependent. The objects dependencies will be set to null.
     *
     * @param dependent The dependent object for this dependency.
     */
    public Dependency(@NotNull final DependentObject dependent) {
        this.dependent = dependent;
    }

    /**
     * Creates a new dependency object for the given dependent. The dependent will also have the given dependencies.
     *
     * @param dependent    The dependent for this dependency object.
     * @param dependencies The set of dependencies that this dependency has.
     */
    protected Dependency(@NotNull final DependentEntity dependent, @NotNull final List<Dependency> dependencies) {
        this.dependent = dependent;
        this.dependencies = Collections.unmodifiableList(dependencies);
    }

    /**
     * Returns the dependent that this dependency represents.
     *
     * @return The dependent for this dependency
     */
    @NotNull
    public DependentObject getDependent() {
        return dependent;
    }

    /**
     * Returns the set of dependencies that this dependency has. Note that if the dependencies have not yet been
     * discovered null is returned. If an empty set is returned that means that this dependent does not have any
     * dependencies.
     *
     * @return The set of dependencies that this dependent has or null if the dependencies have not been discovered.
     */
    @Nullable
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * Sets the dependencies that this dependent has.
     *
     * @param dependencies The dependencies that this dependent has.
     */
    public void setDependencies(@NotNull final List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }
}
