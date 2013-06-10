package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GuidEntityHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.DependencyAnalyzer;
import com.l7tech.server.search.DependencyAnalyzerException;
import com.l7tech.server.search.DependencyProcessorStore;
import com.l7tech.server.search.objects.*;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

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
    private Map<String, String> searchOptions;
    private int searchDepth;
    private DependencyProcessorStore processorStore;

    /**
     * Creates a new Dependency finder with the given search options and dependency processor store.
     *
     * @param searchOptions  The search options to use during the dependency search.
     * @param processorStore The dependency processor store to retrieve the processors for different types of
     *                       dependencies.
     */
    public DependencyFinder(Map<String, String> searchOptions, DependencyProcessorStore processorStore) {
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
        int originalSearchDepth = getIntegerOption(DependencyAnalyzer.SearchDepthOptionKey);
        ArrayList<DependencySearchResults> results = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            //increment the search depth by 1 (this is done because the first thing the getDependenciesHelper does is decrement it by 1
            searchDepth = originalSearchDepth + 1;
            //retrieve the dependency object for the entity
            Dependency dependency = getDependencyHelper(entity);
            //create the dependencySearchResults object
            results.add(new DependencySearchResults(dependency.getDependent(), dependency.getDependencies(), searchOptions));
        }
        return results;
    }

    /**
     * Return the object as a dependency with its dependencies populated if the depth is not 0.
     *
     * @param dependent The object to search dependencies for.
     * @return The dependency object representing this object given.
     */
    protected Dependency getDependencyHelper(Object dependent) throws FindException {
        searchDepth--;
        //Checks if the dependencies for this dependent has already been found.
        Dependency dependencyFound = getFoundDependenciesForObject(dependent);
        //If it has already been found return it.
        if (dependencyFound != null)
            return dependencyFound;
        //Creates a dependency for this object.
        final Dependency dependency = new Dependency(createDependencyObject(dependent));

        // Adds the dependency to the dependencies found set. This needs to be done before calling the
        // getDependenciesHelper() method in order to handle the cyclical case
        dependenciesFound.add(dependency);
        if (searchDepth != 0)
            //If the depth is non 0 then find the dependencies for the given entity.
            dependency.setDependencies(getDependenciesHelper(dependent));
        return dependency;
    }


    /**
     * Returns the list of dependencies for the given object.
     *
     * @param dependent The entity to find the dependencies for
     * @return The set of dependencies that this entity has.
     */
    protected List<Dependency> getDependenciesHelper(final Object dependent) throws FindException {
        //if the depth is 0 return the empty set. Base case.
        if (searchDepth == 0) {
            return Collections.emptyList();
        }

        //find the dependency processor to use.
        DependencyProcessor processor = processorStore.getProcessor(getFromObject(dependent));
        //using the dependency processor find the dependencies and return the results.
        //noinspection unchecked
        return processor.findDependencies(dependent, this);
    }

    /**
     * Retrieves an entity given a search value and the Dependency annotation.
     */
    protected Entity retrieveEntity(Object searchValue, com.l7tech.search.Dependency dependency) throws DependencyAnalyzerException, FindException {
        //Finds the correct processor to use to retrieve the entity
        DependencyProcessor processor = processorStore.getProcessor(dependency.type());
        // use the processor to retrieve the entity using the search value.
        return processor.find(searchValue, dependency);
    }

    /**
     * Creates a DependentObject given an object.
     *
     * @param dependent The object to create a DependentObject for. This should either be an {@link Entity} or an {@link
     *                  Assertion}
     * @return The DependentObject for the given dependent
     */
    private DependentObject createDependencyObject(Object dependent) {
        if (dependent instanceof Entity) {
            return createDependencyEntity(EntityHeaderUtils.fromEntity((Entity) dependent));
        } else if (dependent instanceof Assertion) {
            return createDependencyAssertion((Assertion) dependent);
        } else {
            throw new IllegalStateException("Unknown entity type: " + dependent.getClass());
        }
    }

    private static DependentEntity createDependencyEntity(EntityHeader entity) {
        return new DependentEntity(entity.getName(), entity.getType(), entity instanceof GuidEntityHeader ? ((GuidEntityHeader) entity).getGuid() : null, entity.getStrId());
    }

    private static DependentAssertion createDependencyAssertion(Assertion assertion) {
        return new DependentAssertion((String) assertion.meta().get(AssertionMetadata.SHORT_NAME));
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
                return dependency.getDependent().equals(createDependencyObject(dependent));
            }
        });
    }

    /**
     * Returns an integer value from the search options for the given key. If the option is not set or the value cannot
     * be converted to an integer {@link IllegalArgumentException} is thrown
     *
     * @param optionKey The key to use to look up the value
     * @return The integer value
     */
    protected int getIntegerOption(String optionKey) {
        String optionValue = searchOptions.get(optionKey);
        if (optionValue == null)
            throw new IllegalArgumentException("Search option value for option '" + optionKey + "' was null.");
        final int value;
        try {
            value = Integer.parseInt(optionValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Search option value for option '" + optionKey + "' was not a valid integer. Value: " + optionValue, e);
        }
        return value;
    }

    /**
     * Returns a boolean value from the search options for the given key. If the option is not set {@link
     * IllegalArgumentException} is thrown
     *
     * @param optionKey The key to use to look up the value
     * @return The boolean value
     */
    protected boolean getBooleanOption(String optionKey) {
        String optionValue = searchOptions.get(optionKey);
        if (optionValue == null)
            throw new IllegalArgumentException("Search option value for option '" + optionKey + "' was null.");
        return Boolean.parseBoolean(optionValue);
    }

    /**
     * Returns a dependency type given an object. If a specific type could not be found DependencyType.GENERIC is
     * returned.
     *
     * @param obj The object to find the dependency type of.
     * @return The dependency type of the given object
     */
    @NotNull
    private static com.l7tech.search.Dependency.DependencyType getFromObject(Object obj) {
        if (obj instanceof Entity) {
            //if its an entity use the entity type to find the dependency type
            switch (EntityHeaderUtils.fromEntity((Entity) obj).getType()) {
                case POLICY:
                    return com.l7tech.search.Dependency.DependencyType.POLICY;
                case FOLDER:
                    return com.l7tech.search.Dependency.DependencyType.FOLDER;
                case JDBC_CONNECTION:
                    return com.l7tech.search.Dependency.DependencyType.JDBC_CONNECTION;
                case SECURE_PASSWORD:
                    return com.l7tech.search.Dependency.DependencyType.SECURE_PASSWORD;
                default:
                    return com.l7tech.search.Dependency.DependencyType.GENERIC;
            }
        } else if (obj instanceof Assertion) {
            return com.l7tech.search.Dependency.DependencyType.ASSERTION;
        } else {
            return com.l7tech.search.Dependency.DependencyType.GENERIC;
        }
    }
}
