package com.l7tech.server.search;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.search.exceptions.CannotReplaceDependenciesException;
import com.l7tech.server.search.exceptions.CannotRetrieveDependenciesException;
import com.l7tech.server.search.objects.DependencySearchResults;
import org.jetbrains.annotations.NotNull;

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
     * return the entire dependency tree. The default is -1
     */
    public static final String SearchDepthOptionKey = "searchDepth";

    /**
     * Specifies the entity types to search dependency for. Searching for FOLDER dependencies will return the dependencies of
     * FOLDER entities. This should be a List of EntityType.
     */
    public static final String SearchEntityTypeOptionKey = "searchEntityType";


    /**
     * By Default assertions are not returned as dependencies but this can be enabled by setting this property to true.
     * The Default is false
     */
    public static final String ReturnAssertionsAsDependenciesOptionKey = "returnAssertionsAsDependencies";

    /**
     * This will return a services policy as a dependency if it is set to true. This property will likely never be used.
     * The default is false
     */
    public static final String ReturnServicePoliciesAsDependencies = "returnServicePoliciesAsDependencies";

    /**
     * You can specify a list of entities id's to ignore and not return and not search for dependencies. The Default is
     * an empty list
     */
    public static final String IgnoreSearchOptionKey = "Ignore";

    /**
     * You can specify a list of headers to have their dependencies ignored and not return and not search for dependencies. The Default is
     * an empty list
     */
    public static final String IgnoreDependenciesSearchOptionKey = "IgnoreDependencies";

    /**
     * This enables or disables finding secure password dependencies from the ${secpass.name.plaintext} context
     * variable. The default is true
     */
    public static final String FindSecurePasswordDependencyFromContextVariablePlaintextOptionKey = "findSecurePasswordDependencyFromContextVariablePlaintext";

    /**
     * This includes or excludes Solution Kits from full gateway bundle. The default is false
     */
    public static final String IncludeSolutionKitsOptionKey = "includeSolutionKits";

    /**
     * By default encapsulated assertions are not returned as dependencies of policies this can be enabled by setting this property to true.
     * The Default is false
     */
    public static final String EncassAsPolicyDependencyOptionKey = "encassAsPolicyDependency";

    /**
     * Returns the DependencySearchResults for the given entity. This is the same as calling {@link
     * #getDependencies(com.l7tech.objectmodel.EntityHeader, java.util.Map)} with an empty map for search options,
     * implying default options
     *
     * @param entity The entity who's dependencies to find.
     * @return The DependencySearchResults of finding the dependencies for the given entity.
     * @throws FindException This is thrown if an entity cannot be found by the entity managers.
     */
    @NotNull
    public DependencySearchResults getDependencies(@NotNull EntityHeader entity) throws FindException, CannotRetrieveDependenciesException;

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
    @NotNull
    public DependencySearchResults getDependencies(@NotNull EntityHeader entity, @NotNull Map<String, Object> searchOptions) throws FindException, CannotRetrieveDependenciesException;

    /**
     * Returns the list of dependencies for the given entities. This is the same as calling {@link
     * #getDependencies(java.util.List, java.util.Map)} with an empty map for search options, implying default options
     *
     * @param entityHeaders The list of entity headers to get dependencies for. If the headers list is empty the full gateway dependencies will be returned.
     * @return The list of dependency search results. This list will be the same size as the entity headers list.
     * @throws FindException This is thrown if an entity cannot be found by the entity managers.
     */
    @NotNull
    public List<DependencySearchResults> getDependencies(@NotNull List<EntityHeader> entityHeaders) throws FindException, CannotRetrieveDependenciesException;

    /**
     * Returns the list of dependencies for the given entities.
     *
     * @param entityHeaders The list of entity headers to get dependencies for. If the headers list is empty the full gateway dependencies will be returned.
     * @param searchOptions The search options. These can be used to customize the search. It can be used to specify
     *                      that some entities can be ignored, or to make it so that individual assertions are returned
     *                      as dependencies.
     * @return The list of dependency search results. This list will be the same size as the entity headers list.
     * @throws FindException This is thrown if an entity cannot be found by the entity managers.
     */
    @NotNull
    public List<DependencySearchResults> getDependencies(@NotNull List<EntityHeader> entityHeaders, @NotNull Map<String, Object> searchOptions) throws FindException, CannotRetrieveDependenciesException;

    /**
     * This will replace the dependencies referenced in the given entity by the ones available in the replacement map.
     * If the entity has other dependencies not in the replacement map then it will not be replaced (it will be
     * ignored).
     *
     * @param entity         The entity who's dependencies to replace.
     * @param replacementMap The replacement map is a map of EntityHeaders to replace.
     * @param replaceAssertionsDependencies True to replace the assertion dependencies
     */
    //TODO: This can be generalized to take in a generic object instead of an entity. Is that something we want to do?
    public <E extends Entity> void replaceDependencies(@NotNull E entity, @NotNull Map<EntityHeader, EntityHeader> replacementMap, boolean replaceAssertionsDependencies) throws CannotReplaceDependenciesException;
}
