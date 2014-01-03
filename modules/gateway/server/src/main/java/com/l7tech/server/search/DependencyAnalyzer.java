package com.l7tech.server.search;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.server.search.objects.DependentObject;

import java.util.List;
import java.util.Map;

/**
 * The Dependency Analyzer is used to find the dependencies of an Entity.
 *
 * @author Victor Kazakov
 */
public interface DependencyAnalyzer {

    /**
     * The search depth allows you to specify how deep to search for the dependencies. Setting this to 0 means that no
     * dependencies will be returned. 1 means that only immediate dependencies will be returned. A negative depth will
     * return the entire dependency tree.
     */
    public static final String SearchDepthOptionKey = "searchDepth";

    public static final String ReturnAssertionsAsDependenciesOptionKey = "returnAssertionsAsDependencies";

    public static final String ReturnServicePoliciesAsDependencies = "returnServicePoliciesAsDependencies";

    public static final String IgnoreSearchOptionKey = "Ignore";

    /**
     * Returns the DependencySearchResults for the given entity. This is the same as calling getDependencies(entity,
     * DefaultSearchOptions)
     *
     * @param entity The entity who's dependencies to find.
     * @return The DependencySearchResults of finding the dependencies for the given entity.
     * @throws FindException This is thrown if an entity cannot be found by the entity managers.
     */
    public DependencySearchResults getDependencies(EntityHeader entity) throws FindException;

    /**
     * Returns the DependencySearchResults for the given entity.
     *
     * @param entity        The entity who's dependencies to find.
     * @param searchOptions The search options. These can be used to customize the search. It can be used to specify
     *                      that some entities can be ignored, or to make it so that individual assertions are returned
     *                      as dependencies.
     * @return The dependency search results.
     * @throws FindException This is thrown if an entity cannot be found by the entity managers.
     */
    public DependencySearchResults getDependencies(EntityHeader entity, Map<String, Object> searchOptions) throws FindException;

    /**
     * Returns the list of dependencies for the given entities. This is the same as calling
     * getDependencies(entityHeaders, DefaultSearchOptions)
     *
     * @param entityHeaders The list of entity headers to get dependencies for.
     * @return The list of dependency search results.
     * @throws FindException This is thrown if an entity cannot be found by the entity managers.
     */
    public List<DependencySearchResults> getDependencies(List<EntityHeader> entityHeaders) throws FindException;

    /**
     * @param entityHeaders The list of entity headers to get dependencies for.
     * @param searchOptions The search options to use to perform the search.
     * @return The list of dependency search results.
     * @throws FindException This is thrown if an entity cannot be found by the entity managers.
     */
    public List<DependencySearchResults> getDependencies(List<EntityHeader> entityHeaders, Map<String, Object> searchOptions) throws FindException;

    public List<DependentObject> buildFlatDependencyList(DependencySearchResults dependencySearchResult);

    public List<DependentObject> buildFlatDependencyList(List<DependencySearchResults> dependencySearchResults);
}
