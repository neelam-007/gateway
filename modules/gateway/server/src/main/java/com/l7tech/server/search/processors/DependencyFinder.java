package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.DependencyProcessorStore;
import com.l7tech.server.search.objects.Dependency;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The dependency finder will find all dependencies that an Entity has. Do not use this object directly use the
 * dependency analyzer service instead. Note, this class is not thread safe. Any instance of this class should only be
 * used to process one search at a time.
 *
 * @author Victor Kazakov
 */
public class DependencyFinder {
    private HashSet<Dependency> dependenciesFound;
    private Map<String, Object> searchOptions;
    private int searchDepth;
    private DependencyProcessorStore processorStore;

    /**
     * Creates a new Dependency finder with the given search options and dependency processor store.
     *
     * @param searchOptions  The search options to use during the dependency search.
     * @param processorStore The dependency processor store to retrieve the processors for different types of
     *                       dependencies.
     */
    public DependencyFinder(Map<String, Object> searchOptions, DependencyProcessorStore processorStore) {
        this.searchOptions = searchOptions;
        this.processorStore = processorStore;
        dependenciesFound = new HashSet<>();
    }

    /**
     * Finds the list of dependencies for the given entities.
     *
     * @param entities The entities to find the dependencies for.
     * @return The list of DependencySearchResults for the entities given. There will be one search result for every
     *         entity.
     * @throws FindException This is thrown if there was an error retrieving an entity.
     */
    public synchronized List<DependencySearchResults> process(List<Entity> entities) throws FindException {
        //get the search depth from the options
        int originalSearchDepth = getOption(DependencyAnalyzer.SearchDepthOptionKey, Integer.class, -1);
        ArrayList<DependencySearchResults> results = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            //increment the search depth by 1 (this is done because the first thing the getDependencies does is decrement it by 1
            searchDepth = originalSearchDepth + 1;
            //retrieve the dependency object for the entity
            Dependency dependency = getDependency(entity);
            if(dependency != null) {
                //create the dependencySearchResults object
                results.add(new DependencySearchResults(dependency.getDependent(), dependency.getDependencies(), searchOptions));
            }
        }
        return results;
    }

    /**
     * Return the object as a dependency with its dependencies populated if the depth is not 0.
     *
     * @param dependent The object to search dependencies for.
     * @return The dependency object representing this object given.
     */
    @Nullable
    protected Dependency getDependency(Object dependent) throws FindException {
        searchDepth--;
        //Checks if the dependencies for this dependent has already been found.
        Dependency dependencyFound = getFoundDependenciesForObject(dependent);
        //If it has already been found return it.
        if (dependencyFound != null)
            return dependencyFound;
        //Creates a dependency for this object.
        final Dependency dependency = new Dependency(createDependentObject(dependent));

        List ignoreIds = getOption(DependencyAnalyzer.IgnoreSearchOptionKey, List.class, (List)Collections.emptyList());
        if(dependency.getDependent() instanceof DependentEntity && ignoreIds.contains(((DependentEntity) dependency.getDependent()).getEntityHeader().getStrId())){
            return null;
        }

        // Adds the dependency to the dependencies found set. This needs to be done before calling the
        // getDependencies() method in order to handle the cyclical case
        dependenciesFound.add(dependency);
        if (searchDepth != 0)
            //If the depth is non 0 then find the dependencies for the given entity.
            dependency.setDependencies(getDependencies(dependent));
        return dependency;
    }

    /**
     * Retrieve an option from the search options, verifying it is the correct type and casting to it.
     *
     * @param optionKey    The option to retrieve
     * @param type         The type of the option
     * @param <C>          This is the Type of the value that will be returned
     * @param <T>          This is the class type of the vlaue
     * @return The option value cast to the correct type. This will be the default value if no such option is set.
     * @throws IllegalArgumentException This is thrown if the option value is the wrong type.
     */
    @NotNull
    protected <C, T extends Class<C>> C getOption(final String optionKey, @NotNull final T type, @NotNull C defaultValue) {
        final Object value = searchOptions.get(optionKey);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        } else if (value == null) {
            return defaultValue;
        }
        throw new IllegalArgumentException("Search option value for option '" + optionKey + "' was not a valid type. Expected: " + type + " Given: " + value.getClass());
    }

    /**
     * Returns the list of dependencies for the given object.
     *
     * @param dependent The entity to find the dependencies for
     * @return The set of dependencies that this entity has.
     */
    protected List<Dependency> getDependencies(final Object dependent) throws FindException {
        //if the depth is 0 return the empty set. Base case.
        if (searchDepth == 0) {
            return Collections.emptyList();
        }

        //find the dependency processor to use.
        DependencyProcessor processor = processorStore.getProcessor(getTypeFromObject(dependent));
        //using the dependency processor find the dependencies and return the results.
        //noinspection unchecked
        return processor.findDependencies(dependent, this);
    }

    /**
     * Retrieves an entity given a search value and information about the search value.
     *
     * @param searchValue     The search value to search for the dependency by
     * @param dependencyType  The type of dependency that is to be found
     * @param searchValueType The search value type.
     * @return This is the list of entity found using the search value.
     */
    public List<Entity> retrieveEntities(Object searchValue, com.l7tech.search.Dependency.DependencyType dependencyType, com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException {
        //Finds the correct processor to use to retrieve the entity
        DependencyProcessor processor = processorStore.getProcessor(
                // If the search value type is an entity the get the processor based on the type of entity.
                com.l7tech.search.Dependency.MethodReturnType.ENTITY.equals(searchValueType) ? getTypeFromObject(searchValue) : dependencyType);
        // use the processor to retrieve the entity using the search value.
        //noinspection unchecked
        return processor.find(searchValue, dependencyType, searchValueType);
    }

    /**
     * Creates a DependentObject given an object.
     *
     * @param dependent The object to create a DependentObject for. This should either be an {@link Entity} or an {@link
     *                  Assertion}
     * @return The DependentObject for the given dependent
     */
    private DependentObject createDependentObject(Object dependent) {
        //Finds the correct processor to use
        DependencyProcessor processor = processorStore.getProcessor(getTypeFromObject(dependent));
        // use the processor to create the DependentObject from the dependent
        //noinspection unchecked
        return processor.createDependentObject(dependent);
    }

    /**
     * This will search through the given set of dependencies to see if the entity given has already been found as a
     * dependency. If it has that dependency is returned. Otherwise null is returned.
     *
     * @param dependent The entity to search for
     * @return A dependency for this entity if one has already been found, Null otherwise.
     */
    private Dependency getFoundDependenciesForObject(final Object dependent) {
        return Functions.grepFirst(dependenciesFound, new Functions.Unary<Boolean, Dependency>() {
            @Override
            public Boolean call(Dependency dependency) {
                //return true if the dependency is for the same entity as the one we are searching for.
                return dependency.getDependent().equals(createDependentObject(dependent));
            }
        });
    }

    /**
     * Returns a dependency type given an object. If a specific type could not be found DependencyType.GENERIC is
     * returned.
     *
     * @param obj The object to find the dependency type of.
     * @return The dependency type of the given object
     */
    @NotNull
    private static com.l7tech.search.Dependency.DependencyType getTypeFromObject(Object obj) {
        if (obj instanceof Entity) {
            //if its an entity use the entity type to find the dependency type
            try {
                //noinspection unchecked
                return com.l7tech.search.Dependency.DependencyType.valueOf(EntityType.findTypeByEntity((Class<? extends Entity>) obj.getClass()).toString());
            } catch (IllegalArgumentException e) {
                //Use the Generic dependency type for other entity types
                return com.l7tech.search.Dependency.DependencyType.GENERIC;
            }
        } else if (obj instanceof Assertion) {
            return com.l7tech.search.Dependency.DependencyType.ASSERTION;
        } else {
            return com.l7tech.search.Dependency.DependencyType.GENERIC;
        }
    }

    /**
     * This will return a list of dependencies given a list of entities. The dependencies will have their dependencies
     * discovered using the DependencyFinder.
     *
     * @param object            The object that the given entities belong to. This is used to make sure that the object
     *                          does not depend on itself.
     * @param finder            The finder used to get the Dependencies from the entities.
     * @param dependentEntities The Entities to return as Dependencies.
     * @return The list of dependencies representing the given list of entities.
     * @throws FindException This is thrown if there was an error finding an entity.
     */
    public List<Dependency> getDependenciesFromEntities(Object object, DependencyFinder finder, List<Entity> dependentEntities) throws FindException {
        ArrayList<Dependency> dependencies = new ArrayList<>();
        if (dependentEntities != null) {
            //if a dependency if found then search for its dependencies and add it to the set of dependencies found
            for (Entity entity : dependentEntities) {
                if (entity != null) {
                    //Making sure an entity does not depend on itself
                    if (!object.equals(entity)) {
                        final Dependency dependency = finder.getDependency(entity);
                        if(dependency != null)
                            dependencies.add(dependency);
                    }
                }
            }
        }
        return dependencies;
    }
}
