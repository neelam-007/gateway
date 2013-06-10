package com.l7tech.server.search;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.objects.DependencySearchResults;
import com.l7tech.util.CollectionUtils;

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

    /**
     * The default search options:
     * <pre><table>
     *     <tr><td><b>Option</b></td><td><b>Value</b></td></tr>
     *     <tr><td>searchDepth</td><td>-1</td></tr>
     *     <tr><td>returnAssertionsAsDependencies</td><td>true</td></tr>
     * </table></pre>
     */
    public static final Map<String, String> DefaultSearchOptions = CollectionUtils.MapBuilder.<String, String>builder().put(SearchDepthOptionKey, "-1").put(ReturnAssertionsAsDependenciesOptionKey, "true").unmodifiableMap();

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
    public DependencySearchResults getDependencies(EntityHeader entity, Map<String, String> searchOptions) throws FindException;

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
    public List<DependencySearchResults> getDependencies(List<EntityHeader> entityHeaders, Map<String, String> searchOptions) throws FindException;
}
