package com.l7tech.server.search.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This is used to hold dependency search results. It contains search metadata used to perform the search, a reference
 * to the Object that had its dependencies searched, and a list of the dependencies that the searched for entity has.
 * Each dependency in the list of dependencies can have dependencies itself making this a dependency tree. Performing a
 * depth first traversal of the dependency tree will allow for the dependencies to be view/recreated in the correct
 * order. Note that it is possible for this tree to have cycles.
 *
 * @author Victor Kazakov
 */
public class DependencySearchResults {
    @NotNull
    private final Map<String, Object> searchOptions;
    @NotNull
    private final DependentObject dependentObject;
    @Nullable
    private final List<Dependency> dependencies;

    /**
     * Creates new DependencySearchResults.
     *
     * @param dependentObject The entity that these search results are for.
     * @param dependencies    The dependencies that were found
     * @param searchOptions   The search options used in the search.
     */
    public DependencySearchResults(@NotNull final DependentObject dependentObject, @Nullable final List<Dependency> dependencies, @NotNull final Map<String, Object> searchOptions) {
        this.searchOptions = Collections.unmodifiableMap(searchOptions);
        this.dependentObject = dependentObject;
        this.dependencies = dependencies == null ? null : Collections.unmodifiableList(dependencies);
    }

    /**
     * Returns the search options used for this search.
     *
     * @return The search options used for this search.
     */
    @NotNull
    public Map<String, Object> getSearchOptions() {
        return searchOptions;
    }

    /**
     * The dependent that these search results are for.
     *
     * @return The dependent that these search results are for.
     */
    @NotNull
    public DependentObject getDependent() {
        return dependentObject;
    }

    /**
     * @return The list of dependencies that this dependent has. This will be null if the search depth is 0.
     */
    @Nullable
    public List<Dependency> getDependencies() {
        return dependencies;
    }
}
