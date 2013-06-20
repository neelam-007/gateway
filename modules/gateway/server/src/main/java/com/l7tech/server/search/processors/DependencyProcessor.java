package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * This is the dependency processor interface. A dependency processor is used to find the dependencies of an object. It
 * is also used to find the entity for a dependency type given a search value
 *
 * @author Victor Kazakov
 */
public interface DependencyProcessor<O> {

    /**
     * Returns the list of dependencies for the given object.
     *
     * @param object The object to find dependencies for.
     * @param finder The finder that is performing the current dependency search
     * @return The List of dependencies that have been found.
     * @throws FindException This is thrown if an entity cannot be found.
     */
    @NotNull
    public List<Dependency> findDependencies(O object, DependencyFinder finder) throws FindException;

    /**
     * Returns an entity given a search value and the dependency info.
     *
     * @param searchValue The search value that should uniquely identify the entity.
     * @param dependencyType The type of dependency that is object is.
     * @param searchValueType The type of value the search value is.
     * @param <E>         The Entity
     * @return The Entity specified by the given search value
     * @throws FindException This is thrown if the entity cannot be found
     */
    @Nullable
    public <E extends Entity> List<E> find(@NotNull Object searchValue, com.l7tech.search.Dependency.DependencyType dependencyType, com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException;

    /**
     * Creates a DependentObject given an instance of the dependent.
     *
     * @param dependent The dependent to create the DependentObject from
     * @return The dependent object for the given dependent
     */
    public DependentObject createDependentObject(O dependent);
}
