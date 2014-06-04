package com.l7tech.server.search;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.*;
import com.l7tech.server.search.processors.DependencyFinder;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This service is used to find dependencies of different entities.
 *
 * @author Victor Kazakov
 */
public class DependencyAnalyzerImpl implements DependencyAnalyzer {
    private static final Logger logger = Logger.getLogger(DependencyAnalyzerImpl.class.getName());

    @Inject
    private EntityCrud entityCrud;

    @Inject
    private DependencyProcessorStore processorStore;

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public DependencySearchResults getDependencies(@NotNull final EntityHeader entityHeader) throws FindException, CannotRetrieveDependenciesException {
        final DependencySearchResults dependencySearchResults = getDependencies(entityHeader, Collections.<String, Object>emptyMap());
        if (dependencySearchResults == null) {
            // This should never happen. The only time the dependencyFinder.process(entities) method can return null DependencySearchResults
            // is if the entity is null or if the entity is ignored in the search options. Neither of which happens here
            throw new IllegalStateException("Returned null dependency search results. This should not have happened.");
        }
        return dependencySearchResults;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public DependencySearchResults getDependencies(@NotNull final EntityHeader entityHeader, @NotNull final Map<String, Object> searchOptions) throws FindException, CannotRetrieveDependenciesException {
        return getDependencies(Arrays.asList(entityHeader), searchOptions).get(0);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public List<DependencySearchResults> getDependencies(@NotNull final List<EntityHeader> entityHeaders) throws FindException, CannotRetrieveDependenciesException {
        return getDependencies(entityHeaders, Collections.<String, Object>emptyMap());
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public List<DependencySearchResults> getDependencies(@NotNull final List<EntityHeader> entityHeaders, @NotNull final Map<String, Object> searchOptions) throws FindException, CannotRetrieveDependenciesException {
        logger.log(Level.FINE, "Finding dependencies for {0}", entityHeaders);

        //Load the entities from the given entity headers.
        final ArrayList<Entity> entities = new ArrayList<>(entityHeaders.size());
        for (final EntityHeader entityHeader : entityHeaders) {
            final Entity entity = entityCrud.find(entityHeader);
            if (entity == null) {
                throw new FindException("Could not find Entity with header: " + entityHeader.toStringVerbose());
            }
            entities.add(entity);
        }

        //create a new dependency finder to perform the search
        final DependencyFinder dependencyFinder = new DependencyFinder(searchOptions, processorStore);
        return dependencyFinder.process(entities);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Entity> void replaceDependencies(@NotNull final E entity, @NotNull final Map<EntityHeader, EntityHeader> replacementMap) throws CannotReplaceDependenciesException, CannotRetrieveDependenciesException {
        if (replacementMap.isEmpty()) {
            //nothing to replace, just shortcut to returning
            return;
        }

        //create a new dependency finder to perform the replacement
        final DependencyFinder dependencyFinder = new DependencyFinder(Collections.<String, Object>emptyMap(), processorStore);
        dependencyFinder.replaceDependencies(entity, replacementMap);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public List<Dependency> flattenDependencySearchResults(@NotNull final DependencySearchResults dependencySearchResult, final boolean includeRootNode) {
        return flattenDependencySearchResults(Arrays.asList(dependencySearchResult), includeRootNode);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public List<Dependency> flattenDependencySearchResults(@NotNull final List<DependencySearchResults> dependencySearchResults, final boolean includeRootNode) {
        //the flat list of dependency object
        final List<Dependency> dependencyObjects = new ArrayList<>();
        for (final DependencySearchResults dependencySearchResult : dependencySearchResults) {
            if (dependencySearchResult != null) {
                if (includeRootNode) {
                    //include the DependencySearchResults.dependentObject
                    buildDependentObjectsList(dependencyObjects, dependencySearchResult.getDependent(), dependencySearchResult.getDependencies(), new ArrayList<DependentObject>());
                } else if (dependencySearchResult.getDependencies() != null) {
                    //if we are not including the dependent object in the DependencySearchResults then process only the dependencies.
                    //keep track of processed object, this will improve processing time.
                    final ArrayList<DependentObject> processedObjects = new ArrayList<>();
                    //loop throw all the dependencies to build up the dependent objects list.
                    for (final Dependency dependency : dependencySearchResult.getDependencies()) {
                        buildDependentObjectsList(dependencyObjects, dependency.getDependent(), dependency.getDependencies(), processedObjects);
                    }
                }
            }
        }
        return dependencyObjects;
    }

    /**
     * Builds a list of dependency objects. This is called recursively to process the given dependent and list of
     * dependencies.
     *
     * @param dependencyObjects The list of dependency objects built so far.
     * @param dependent         The dependent currently being processed.
     * @param dependencies      current dependent's immediate dependencies.
     * @param processed         The List of dependent objects already processed.
     */
    private void buildDependentObjectsList(@NotNull final List<Dependency> dependencyObjects, @NotNull final DependentObject dependent, @Nullable final List<Dependency> dependencies, @NotNull final List<DependentObject> processed) {
        // check if dependency is already found.
        if (processed.contains(dependent)) {
            return;
        }
        //add to the processed list before processing to avoid circular dependency issues.
        processed.add(dependent);

        // Need to handle folder dependencies specially because they are reversed in the DependencySearchResults tree.
        // In the tree a folder dependencies are its children, however in the dependency list we want to list the parent
        // folder before its children in order to have it get created in the correct order
        if (com.l7tech.search.Dependency.DependencyType.FOLDER.equals(dependent.getDependencyType())) {
            //if it is a folder we still need to put security zone dependencies first.
            if (dependencies != null) {
                for (final Dependency dependency : dependencies) {
                    if (com.l7tech.search.Dependency.DependencyType.SECURITY_ZONE.equals(dependency.getDependent().getDependencyType())) {
                        buildDependentObjectsList(dependencyObjects, dependency.getDependent(), dependency.getDependencies(), processed);
                    }
                }
            }
            addDependentToDependencyList(dependencyObjects, dependent, dependencies);
        }
        //process the dependent objects dependencies.
        if (dependencies != null) {
            for (final Dependency dependency : dependencies) {
                buildDependentObjectsList(dependencyObjects, dependency.getDependent(), dependency.getDependencies(), processed);
            }
        }
        //A folder would already have been added to the dependency object list so only do this if it is not a folder.
        if (!com.l7tech.search.Dependency.DependencyType.FOLDER.equals(dependent.getDependencyType())) {
            addDependentToDependencyList(dependencyObjects, dependent, dependencies);
        }
    }

    /**
     * Adds the dependent object the the dependencyObjects list if it has not been added already. The added dependency
     * will have its immediate dependencies set as well
     *
     * @param dependencyObjects The list of dependency object built so far
     * @param dependent         The dependent object to add to the list
     * @param dependencies      The dependent objects dependencies
     */
    private void addDependentToDependencyList(@NotNull final List<Dependency> dependencyObjects, @NotNull final DependentObject dependent, @Nullable final List<Dependency> dependencies) {
        //Find if the dependent object has been added to the dependencyObjects list.
        Dependency dependency = Functions.grepFirst(dependencyObjects, new Functions.Unary<Boolean, Dependency>() {
            @Override
            public Boolean call(Dependency dependency) {
                return dependency.getDependent().equals(dependent);
            }
        });
        //if it has not been added then add it.
        if (dependency == null) {
            dependency = new Dependency(dependent);
            //add the immediate dependencies to the dependency
            final ArrayList<Dependency> immediateDependencies = new ArrayList<>();
            if (dependencies != null && !dependencies.isEmpty()) {
                for (final Dependency immediateDependency : dependencies) {
                    immediateDependencies.add(new Dependency(immediateDependency.getDependent()));
                }
            }
            //set the immediate dependencies
            dependency.setDependencies(immediateDependencies);
            //add the dependency to the dependencyObjects list
            dependencyObjects.add(dependency);
        }
    }
}
