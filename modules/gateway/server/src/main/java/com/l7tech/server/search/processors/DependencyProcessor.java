package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.Dependency;
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
    public List<Dependency> findDependencies(@NotNull O object, @NotNull DependencyFinder finder) throws FindException, CannotRetrieveDependenciesException;

    /**
     * This will replace the dependencies referenced in the given object by the ones available in the replacement map.
     * If the object has a dependencies not in the replacement map then it will not be replaced.
     *
     * @param object         the object who's dependencies to replace.
     * @param replacementMap The replacement map is a map of dependentEntity objects to replace.
     * @param finder         The dependency finder replacing the dependencies.
     * @param replaceAssertionsDependencies True to replace the assertion dependencies
     */
    public void replaceDependencies(@NotNull O object, @NotNull Map<EntityHeader, EntityHeader> replacementMap, @NotNull DependencyFinder finder, boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException;
}
