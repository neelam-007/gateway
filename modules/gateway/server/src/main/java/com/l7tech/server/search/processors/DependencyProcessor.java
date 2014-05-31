package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependentObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

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
    public List<Dependency> findDependencies(@NotNull O object, @NotNull DependencyFinder finder) throws FindException;

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
    public List<DependentObject> createDependentObject(@NotNull Object searchValue, @NotNull com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull com.l7tech.search.Dependency.MethodReturnType searchValueType);


    /**
     * This will replace the dependencies referenced in the given object by the ones available in the replacement map.
     * If the object has a dependencies not in the replacement map then it will not be replaced.
     *
     * @param object         the object who's dependencies to replace.
     * @param replacementMap The replacement map is a map of dependentEntity objects to replace.
     * @param finder         The dependency finder replacing the dependencies.
     */
    public void replaceDependencies(@NotNull O object, @NotNull Map<EntityHeader, EntityHeader> replacementMap, @NotNull DependencyFinder finder) throws CannotRetrieveDependenciesException, CannotReplaceDependenciesException;
}
