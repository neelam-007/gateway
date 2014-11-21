package com.l7tech.server.search.objects;

import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The dependency object contains the dependencies of a dependent object. Two dependency objects are considered equal if
 * their dependents are equal.
 *
 * @author Victor Kazakov
 */
public class Dependency {

    @NotNull
    protected DependentObject dependent;
    protected List<Dependency> dependencies;

    /**
     * Creates a new dependency object for the given dependent. The objects dependencies will be set to null.
     *
     * @param dependent The dependent object for this dependency.
     */
    public Dependency(@NotNull final DependentObject dependent) {
        this.dependent = dependent;
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
     * Sets the dependent on this dependency
     * @param dependent The dependent to set.
     */
    public void setDependent(@NotNull final DependentObject dependent) {
        this.dependent = dependent;
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
    public void setDependencies(final List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dependency that = (Dependency) o;

        if (!dependent.equals(that.dependent)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return dependent.hashCode();
    }

    @NotNull
    public static Dependency clone(@NotNull final Dependency dependency) {
        return clone(dependency, new HashMap<DependentObject, Dependency>());
    }

    @NotNull
    public static List<Dependency> clone(@NotNull final List<Dependency> dependencies) {
        return clone(dependencies, new HashMap<DependentObject, Dependency>());
    }

    @NotNull
    private static List<Dependency> clone(@NotNull final List<Dependency> dependencies, @NotNull final Map<DependentObject, Dependency> clonedDependenciesMap) {
        return Functions.map(dependencies, new Functions.Unary<Dependency, Dependency>() {
            @Override
            public Dependency call(Dependency dependency) {
                return Dependency.clone(dependency, clonedDependenciesMap);
            }
        });
    }

    @NotNull
    private static Dependency clone(@NotNull final Dependency dependency, @NotNull final Map<DependentObject, Dependency> clonedDependenciesMap) {
        Dependency clonedDependency = clonedDependenciesMap.get(dependency.getDependent());
        if (clonedDependency == null) {
            clonedDependency = new Dependency(dependency.getDependent());
            clonedDependenciesMap.put(dependency.getDependent(), clonedDependency);
            if (dependency.getDependencies() != null) {
                clonedDependency.setDependencies(clone(dependency.getDependencies(), clonedDependenciesMap));
            }
        }
        return clonedDependency;
    }
}
