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

    private final DependentObject dependent;
    private List<Dependency> dependencies;
    private boolean dependenciesSet = false;

    /**
     * Creates a new dependency object for the given dependent. The objects dependencies will be marked as undiscovered
     * ({@link #areDependenciesSet()} will return false )
     *
     * @param dependent The dependent object for this dependency.
     */
    public Dependency(@NotNull DependentObject dependent) {
        this.dependent = dependent;
    }

    /**
     * Creates a new dependency object for the given dependent. The dependent will also have the given dependencies. The
     * dependencies for this dependency will be marked are being discovered. {@link #areDependenciesSet()} will return
     * true.
     *
     * @param dependent    The dependent for this dependency object.
     * @param dependencies The set of dependencies that this dependency has.
     */
    protected Dependency(@NotNull DependentEntity dependent, @NotNull List<Dependency> dependencies) {
        this.dependent = dependent;
        this.dependencies = Collections.unmodifiableList(dependencies);
        dependenciesSet = true;
    }

    /**
     * Returns the dependent that this dependency represents.
     *
     * @return The dependent for this dependency
     */
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
    public void setDependencies(@NotNull List<Dependency> dependencies) {
        this.dependencies = dependencies;
        dependenciesSet = true;
    }

    /**
     * Checks if the dependencies for this dependent have been discovered.
     *
     * @return returns true if the dependencies have been discovered. false otherwise.
     */
    public boolean areDependenciesSet() {
        return dependenciesSet;
    }
}
