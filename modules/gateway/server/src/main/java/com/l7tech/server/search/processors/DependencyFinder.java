package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.PropertiesUtil;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.*;
import com.l7tech.util.Either;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;
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
//TODO: this there some way to limit access to this class and its methods. Only dependency processors and the dependency analyzer should need to access it.
public class DependencyFinder {
    @NotNull
    private final HashSet<Dependency> dependenciesFound;
    @NotNull
    private final Map<String, Object> searchOptions;
    //This keeps track of the current search depth.
    private int searchDepth;
    @NotNull
    private final DependencyProcessorStore processorStore;

    /**
     * Creates a new Dependency finder with the given search options and dependency processor store.
     *
     * @param searchOptions  The search options to use during the dependency search.
     * @param processorStore The dependency processor store to retrieve the processors for different types of
     *                       dependencies.
     */
    public DependencyFinder(@NotNull final Map<String, Object> searchOptions, @NotNull final DependencyProcessorStore processorStore) {
        this.searchOptions = searchOptions;
        this.processorStore = processorStore;
        dependenciesFound = new HashSet<>();
    }

    /**
     * Finds the list of dependencies for the given entities.
     *
     * @param entities The entities to find the dependencies for.
     * @return The list of DependencySearchResults for the entities given. There will be one search result for every
     * entity. Note the contents of the list may be null if the entity list contains nulls or if any of the entities are
     * in the ignored list.
     * @throws FindException This is thrown if there was an error retrieving an entity.
     */
    @NotNull
    public synchronized List<DependencySearchResults> process(@NotNull final List<FindResults> entities) throws FindException, CannotRetrieveDependenciesException {
        //get the search depth from the options
        final int originalSearchDepth = getOption(DependencyAnalyzer.SearchDepthOptionKey, Integer.class, -1);
        final ArrayList<DependencySearchResults> results = new ArrayList<>(entities.size());
        for (@NotNull final FindResults entity : entities) {
            // only an entity can have a dependency
            if(entity.hasEntity()) {
                //reset the search depth
                searchDepth = originalSearchDepth;
                //retrieve the dependency object for the entity
                final Dependency dependency = getDependency(entity);
                if (dependency != null) {
                    //create the dependencySearchResults object
                    results.add(new DependencySearchResults(dependency.getDependent(), dependency.getDependencies(), searchOptions));
                } else {
                    //if the dependency is null add a null search result to the list to keep the lengths consistent.
                    // The dependency will be null if the entity is null, or if the entity is in the ignored list
                    results.add(null);
                }
            }
        }
        return results;
    }

    /**
     * This will replace the dependencies referenced in the given object by the ones available in the replacement map.
     * If the object has a dependencies not in the replacement map then they will not be replaced.
     *
     * @param object         the object who's dependencies to replace.
     * @param replacementMap The replacement map is a map of entity headers to replace.
     * @param replaceAssertionsDependencies True to replace the assertion dependencies
     */
    public synchronized <O> void replaceDependencies(@NotNull final O object, @NotNull final Map<EntityHeader, EntityHeader> replacementMap, final boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException {
        //find the dependency processor to use.
        final DependencyProcessor processor = processorStore.getProcessor(object);
        //noinspection unchecked
        processor.replaceDependencies(object, replacementMap, this, replaceAssertionsDependencies);
    }

    /**
     * Return the object as a dependency with its dependencies populated if the depth is not 0.
     *
     * @param dependent The object to search dependencies for.
     * @return The dependency object representing this object given. This can return null if the dependent is supposed
     * to be ignored, or if the given dependent is null.
     */
    @Nullable
    Dependency getDependency(@NotNull final FindResults dependent) throws FindException, CannotRetrieveDependenciesException {
        //return null if the dependent is null.
        if (!dependent.isSome()) {
            return null;
        }

        // create broken dependency if entity is not found
        if (dependent.hasEntityHeader()) {
            return new BrokenDependency(dependent.getEntityHeader());
        }
        //Checks if the dependencies for this dependent has already been found.
        final Dependency dependencyFound = getFoundDependenciesForObject(dependent.getEntity());
        //If it has already been found return it.
        if (dependencyFound != null) {
            return dependencyFound;
        }
        //Creates a dependency for this object. This will create a new dependency object that does not have its dependencies set yet.
        final Dependency dependency = new Dependency(createDependentObject(dependent.getEntity()));

        //get the list of id's to ignore
        final List ignoreIds = getOption(DependencyAnalyzer.IgnoreSearchOptionKey, List.class, (List) Collections.emptyList());
        //check to see if the dependent is supposed to be ignored. if it is return null
        if (dependency.getDependent() instanceof DependentEntity && ignoreIds.contains(((DependentEntity) dependency.getDependent()).getEntityHeader().getStrId())) {
            return null;
        }

        // Adds the dependency to the dependencies found set. This needs to be done before calling the
        // getDependencies() method in order to handle the cyclical case
        dependenciesFound.add(dependency);
        //check to make sure the max search depth has not been reached.
        if (searchDepth != 0) {
            //decrement the search depth.
            searchDepth--;
            //If the depth is non 0 then find the dependencies for the given entity.
            dependency.setDependencies(getDependencies(dependent.getEntity()));
        }
        return dependency;
    }

    /**
     * Returns the list of dependencies for the given object. If the given object is null the empty list is returned.
     *
     * @param dependent The entity to find the dependencies for
     * @return The set of dependencies that this entity has.
     */
    @NotNull
    List<Dependency> getDependencies(@Nullable final Object dependent) throws FindException, CannotRetrieveDependenciesException {
        if (dependent == null) {
            return Collections.emptyList();
        }

        if(!isSearchType(dependent)){
            return Collections.emptyList();
        }

        //find the dependency processor to use.
        final DependencyProcessor processor = processorStore.getProcessor(dependent);
        //using the dependency processor find the dependencies and return the results.
        //noinspection unchecked
        return processor.findDependencies(dependent, this);
    }

    private boolean isSearchType(Object dependent) {
        final List searchTypes = getOption(DependencyAnalyzer.SearchEntityTypeOptionKey, List.class, (List) Collections.emptyList());
        if(searchTypes.isEmpty())
            return true;

        if(dependent instanceof Entity){
            return searchTypes.contains(EntityHeaderUtils.fromEntity((Entity)dependent).getType());
        }
        return false;
    }

    /**
     * Retrieve an option from the search options, verifying it is the correct type and casting to it.
     *
     * @param optionKey    The option to retrieve
     * @param type         The type of the option
     * @param defaultValue The default value to return if the option is not specified
     * @param <C>          This is the Type of the value that will be returned
     * @param <T>          This is the class type of the value
     * @return The option value cast to the correct type. This will be the default value if no such option is set.
     * @throws IllegalArgumentException This is thrown if the option value is the wrong type.
     */
    @NotNull
    <C, T extends Class<C>> C getOption(@NotNull final String optionKey, @NotNull final T type, @NotNull final C defaultValue) {
        return PropertiesUtil.getOption(optionKey, type, defaultValue, searchOptions);
    }

    /**
     * Retrieves either the objects or the headers describing the objects given a search value and information about the search value.
     *
     * @param searchValue     The search value to search for the dependency by
     * @param dependencyType  The type of dependency that is to be found
     * @param searchValueType The search value type.
     * @return This is the list of objects found using the search value.
     */
    @NotNull
    public List<FindResults> retrieveObjects(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) throws FindException {
        final InternalDependencyProcessor processor;
        //Finds the correct processor to use to retrieve the entity
        if (com.l7tech.search.Dependency.MethodReturnType.ENTITY.equals(searchValueType)) {
            // If the search value type is an entity the get the processor based on the type of entity.
            processor = processorStore.getProcessor(searchValue);
        } else {
            processor = processorStore.getProcessor(dependencyType);
        }
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
    @NotNull
    public DependentObject createDependentObject(@NotNull final Object dependent) {
        //Finds the correct processor to use
        InternalDependencyProcessor processor = processorStore.getProcessor(dependent);
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
    @Nullable
    private Dependency getFoundDependenciesForObject(@NotNull final Object dependent) {
        final DependentObject dependentObject = createDependentObject(dependent);
        return Functions.grepFirst(dependenciesFound, new Functions.Unary<Boolean, Dependency>() {
            @Override
            public Boolean call(Dependency dependency) {
                //return true if the dependency is for the same entity as the one we are searching for.
                return dependency.getDependent().equals(dependentObject);
            }
        });
    }

    /**
     * This will return a list of dependencies given a list of dependent objects. The dependencies will have their
     * dependencies discovered using the DependencyFinder.
     *
     * @param object           The object that the given dependentObjects belong to. This is used to make sure that the
     *                         object does not depend on itself.
     * @param finder           The finder used to get the Dependencies from the dependent objects.
     * @param dependentObjects The objects to return as Dependencies.
     * @return The list of dependencies representing the given list of dependent objects.
     * @throws FindException This is thrown if there was an error finding a dependent object.
     */
    @NotNull
    public List<Dependency> getDependenciesFromObjects(@NotNull final Object object, @NotNull final DependencyFinder finder, @NotNull final List<FindResults> dependentObjects) throws FindException, CannotRetrieveDependenciesException {
        final ArrayList<Dependency> dependencies = new ArrayList<>();
        //if a dependency if found then search for its dependencies and add it to the set of dependencies found
        for (final FindResults obj : dependentObjects) {
            //Making sure an entity does not depend on itself
            if (!object.equals(obj)) {
                final Dependency dependency = finder.getDependency(obj);
                if (dependency != null) {
                    dependencies.add(dependency);
                }
            }
        }
        return dependencies;
    }

    /**
     * This will create a dependent object from the given information
     *
     * @param searchValue     The value to use to create the dependent entity
     * @param dependencyType  The Type of the dependency
     * @param searchValueType The type of the search value
     * @return The list of created dependent objects
     */
    @NotNull
    List<DependentObject> createDependentObject(@NotNull final Object searchValue, @NotNull final com.l7tech.search.Dependency.DependencyType dependencyType, @NotNull final com.l7tech.search.Dependency.MethodReturnType searchValueType) throws CannotRetrieveDependenciesException {
        final InternalDependencyProcessor processor;
        //Finds the correct processor to use to retrieve the entity
        if (com.l7tech.search.Dependency.MethodReturnType.ENTITY.equals(searchValueType)) {
            // If the search value type is an entity the get the processor based on the type of entity.
            processor = processorStore.getProcessor(searchValue);
        } else {
            processor = processorStore.getProcessor(dependencyType);
        }
        // use the processor to retrieve the entity using the search value.
        return processor.createDependentObjects(searchValue, dependencyType, searchValueType);
    }

    static public class FindResults<O> extends Option<Either<O,EntityHeader>>
    {
        /**
         * Constructs the dependent search result object
         * @param entityHeader  header describing the dependent entity
         * @param entity    the dependent that is found
         * @return the dependent search result object
         */
        static public<O> FindResults create(O entity, EntityHeader entityHeader){
            if(entity != null){
                return new FindResults(Either.<O,EntityHeader>left(entity));
            }else if(entityHeader != null){
                return new FindResults(Either.<O,EntityHeader>right(entityHeader));
            }
            return new FindResults(null);
        }

        FindResults(Either<O, EntityHeader> value) {
            super(value);
        }

        /**
         * @return true if this has a value
         */
        @Override
        public boolean isSome() {
            return super.isSome();
        }

        /**
         * @return True if an entity is found
         */
        public boolean hasEntity(){
            return isSome() && some().isLeft();
        }

        /**
         * @return the entity found, null if not found
         */
        public O getEntity(){
            return isSome() ? some().left(): null;
        }

        /**
         * @return has header describing the dependent entity
         */
        public boolean hasEntityHeader(){
            return isSome() && some().isRight();
        }

        /**
         * @return the header describing the dependent entity
         */
        public EntityHeader getEntityHeader(){
            return isSome() ? some().right(): null;
        }
    }
}
