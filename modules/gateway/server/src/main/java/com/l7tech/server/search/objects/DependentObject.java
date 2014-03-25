package com.l7tech.server.search.objects;

import com.l7tech.search.Dependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * This was created: 6/10/13 as 11:29 AM
 *
 * @author Victor Kazakov
 */
public abstract class DependentObject {
    private final String name;
    private Dependency.DependencyType dependencyType;
    private List<DependentObject> dependencies = new ArrayList<>();

    public DependentObject(@Nullable String name, @NotNull com.l7tech.search.Dependency.DependencyType dependencyType) {
        this.name = name;
        this.dependencyType = dependencyType;
    }

    public DependentObject(final DependentObject object) {
        this.name = object.name;
        this.dependencyType = object.dependencyType;
        this.dependencies.addAll(object.dependencies);
    }

    /**
     * @return The name of the entity. Not all entities have names so this may be null.
     */
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

    @NotNull
    public List<DependentObject> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<DependentObject> dependencies) {
        this.dependencies = dependencies;
    }

    public void addDependency(DependentObject dependency){
        dependencies.add(dependency);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DependentObject)) return false;

        DependentObject that = (DependentObject) o;

        if (dependencyType != that.dependencyType) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (dependencyType != null ? dependencyType.hashCode() : 0);
        return result;
    }
}
