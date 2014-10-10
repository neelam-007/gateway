package com.l7tech.server.search.objects;

import com.l7tech.objectmodel.EntityHeader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * The BrokenDependency contains a reference to an entity that can not be found. Contains the entity header.
 *
 */
public class BrokenDependency extends Dependency {

    /**
     * Creates a new dependency object for the given dependent.
     *
     * @param header The entity header broken dependency.
     */
    public BrokenDependency(@NotNull final EntityHeader header) {
        super(new BrokenDependentEntity(header));
    }

    /**
     * Creates a new dependency object for the given broken dependent.
     *
     * @param dependent The broken dependent object for this dependency.
     */
    public BrokenDependency(@NotNull final BrokenDependentEntity dependent) {
        super(dependent);
    }

    /**
     * A broken dependency will never have dependencies
     * @return an unmodifiable empty list
     */
    @Override
    @Nullable
    public List<Dependency> getDependencies() {
        return Collections.<Dependency>unmodifiableList(Collections.<Dependency>emptyList());
    }

    /**
     * A broken dependency should never have dependencies.  This does nothing.
     */
    @Override
    public void setDependencies(@NotNull List<Dependency> dependencies) {
        // do nothing
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
}
