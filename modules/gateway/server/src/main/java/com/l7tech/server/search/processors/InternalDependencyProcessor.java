package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependentObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;

interface InternalDependencyProcessor<O> extends DependencyProcessor<O> {
    /**
     * Returns an entity given a search value and the dependency info.
     *
     * @param searchValue     The search value that should uniquely identify the entity.
     * @param dependencyType  The type of dependency that is object is.
     * @param searchValueType The type of value the search value is.
     * @return The Entity specified by the given search value. This can return null if the search value references a
     * null entity
     * @throws FindException This is thrown if the entity cannot be found
     */
    @NotNull
    public List<O> find(@NotNull Object searchValue, @NotNull com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException;

    /**
     * Creates a DependentObject given an instance of the dependent.
     *
     * @param dependent The dependent to create the DependentObject from
     * @return The dependent object for the given dependent
     */
    @NotNull
    public DependentObject createDependentObject(@NotNull O dependent);

    /**
     * Creates a list of dependent objects with the given info
     *
     * @param searchValue     The search value
     * @param dependencyType  The dependency type
     * @param searchValueType The search value type
     * @return The list of dependent objects
     */
    @NotNull
    public List<DependentObject> createDependentObjects(@NotNull Object searchValue, @NotNull com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull com.l7tech.search.Dependency.MethodReturnType searchValueType) throws CannotRetrieveDependenciesException;
}
