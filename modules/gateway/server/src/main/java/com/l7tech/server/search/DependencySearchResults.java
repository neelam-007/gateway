package com.l7tech.server.search;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This is used to hold dependency search results. It contains search metadata used to perform the search, a reference
 * to the Entity that had its dependencies searched, and a list of the dependencies that the searched for entity has.
 * Each dependency in the list of dependencies can have dependencies itself making this a dependency tree. Performing a
 * depth first traversal of the dependency tree will allow for the dependencies to be view/recreated in the correct
 * order. Note that it is possible for this tree to have identical subtrees and cycles. (Although dependencies should
 * not contain any cycles)
 *
 * @author Victor Kazakov
 */
public class DependencySearchResults {
    private final Map<String, String> searchOptions;
    private final DependencyEntity entity;
    private final List<Dependency> dependencies;

    /**
     * Creates new DependencySearchResults.
     *
     * @param entity        The entity that these search results are for.
     * @param dependencies  The dependencies that were found
     * @param searchOptions The search options used in the search.
     */
    protected DependencySearchResults(DependencyEntity entity, List<Dependency> dependencies, Map<String, String> searchOptions) {
        this.searchOptions = Collections.unmodifiableMap(searchOptions);
        this.entity = entity;
        this.dependencies = dependencies == null ? null : Collections.unmodifiableList(dependencies);
    }

    /**
     * Returns the search options used for this search.
     *
     * @return The search options used for this search.
     */
    public Map<String, String> getSearchOptions() {
        return searchOptions;
    }

    /**
     * The entity that these search results are for.
     *
     * @return The entity that these search results are for.
     */
    public DependencyEntity getEntity() {
        return entity;
    }

    /**
     * @return The list of dependencies that this dependency has. This will be null if the search depth is 0.
     */
    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
