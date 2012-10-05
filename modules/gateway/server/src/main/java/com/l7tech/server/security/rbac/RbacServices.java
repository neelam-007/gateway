package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.server.EntityFinder;
import com.l7tech.util.Cacheable;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * Provides functionality formerly provided by {@link RoleManagerImpl} that isn't coupled to Hibernate and doesn't need
 * to be part of the same class.  This should make unit testing easier, and may eventually replace some direct uses of
 * RoleManager too.
 *  
 * @author alex
 */
public interface RbacServices {
    /**
     * Returns true if the specified operation is permitted for <em>all</em> of the specified types; false otherwise
     *
     * @param authenticatedUser the User who was authenticated; must not be null
     * @param requiredOperation the operation required; must be non-null and a member of {@link com.l7tech.gateway.common.security.rbac.OperationType#ALL_CRUD}
     * @param requiredTypes the Set of types against which the operation must be permitted; must not be null or empty
     * @return true if the specified operation is permitted for <em>all</em> of the specified types; false otherwise
     * @throws com.l7tech.objectmodel.FindException in the event of a database problem
     */
    boolean isPermittedForEntitiesOfTypes(User authenticatedUser,
                                                 OperationType requiredOperation,
                                                 Set<EntityType> requiredTypes)
            throws FindException;

    /**
     * Returns <em>true</em> if the specified operation is permitted against any entity of the specified type; false otherwise.
     *
     * <p>Another way to phrase this is that permission to perform the
     * specified operation against all entities of a type is required.</p>
     *
     * @param authenticatedUser the User who was authenticated; must not be null
     * @param requiredOperation the operation required; must be non-null and a member of {@link com.l7tech.gateway.common.security.rbac.OperationType#ALL_CRUD}
     * @param requiredType the type against which the operation must be permitted; must not be null or empty
     * @return true if the specified operation is permitted for any entity of the specified type; false otherwise
     * @throws FindException in the event of a database problem
     */
    boolean isPermittedForAnyEntityOfType(User authenticatedUser,
                                                 OperationType requiredOperation,
                                                 EntityType requiredType)
            throws FindException;


    /**
     * Returns <em>true</em> if the specified operation is permitted against at least one entity of the specified type; false otherwise.
     *
     * @param authenticatedUser the User who was authenticated; must not be null
     * @param requiredOperation the operation required; must be non-null and a member of {@link com.l7tech.gateway.common.security.rbac.OperationType#ALL_CRUD}
     * @param requiredType the type against which the operation must be permitted; must not be null or empty
     * @return true if the specified operation is permitted for some entity of the specified type; false otherwise
     * @throws FindException in the event of a database problem
     */
    boolean isPermittedForSomeEntityOfType(User authenticatedUser,
                                                 OperationType requiredOperation,
                                                 EntityType requiredType)
            throws FindException;

    boolean isPermittedForEntity(User user, Entity entity, OperationType operation, @Nullable String otherOperationName) throws FindException;

    /**
     * Filters a collection of {@link com.l7tech.objectmodel.EntityHeader}s, returning a new {@link Iterable} (<em>not necessarily of the same
     * type!</em>) containing only headers for entities on which the user has permission to invoke the specified
     * operation
     *
     * @param authenticatedUser the User who was authenticated; must not be null
     * @param requiredOperation the operation the user must be permitted to perform on all the entities
     * @param headers the headers of the entities the user is asking for
     * @param entityFinder the EntityFinder to use to look the real entities for the supplied headers
     * @return the headers for the entities that the user is permitted to invoke the operation against; will always be
     *         either the original iterable itself, or a List&lt;T&gt; derived from it.
     * @throws FindException if the user's roles cannot be retrieved, but <em>not</em> if the entity headers cannot be
     *                       resolved.
     */
    <T extends OrganizationHeader> Iterable<T> filterPermittedHeaders(User authenticatedUser,
                                                                       OperationType requiredOperation,
                                                                       Iterable<T> headers,
                                                                       EntityFinder entityFinder)
            throws FindException;

    /**
     * Check whether the specified user is in at least one role with at least one permission with an operation type other than NONE.
     * The result of this method call may be cached.
     *
     * @param providerAndUserId user ID and provider ID in a Pair, to serve as a cache key.  Required.
     * @param user user to examine.  Required.  User ID and provider ID must match that provided as cache key.
     * @return true if this user has at least one role with at least one permission with an operation type other than NONE.
     * @throws com.l7tech.objectmodel.FindException if there is a problem looking up the necessary information.
     */
    @Cacheable(relevantArg=0,maxAge=5000)
    boolean isAdministrativeUser(@NotNull Pair<Long, String> providerAndUserId, @NotNull User user) throws FindException;

    /**
     * Get the roles for the specified user, with default values for skipAccountValidation and disabledGroupIsError.
     * The result of this method may be cached.
     *
     * @param providerAndUserId user ID and provider ID in a Pair, to serve as a cache key.  Required.
     * @param user user to examine.  Required.  User ID and provider ID must match that provided as cache key.
     * @return the roles for this user.  May be empty but never null.
     * @throws com.l7tech.objectmodel.FindException if there is a problem looking up the necessary information.
     */
    @Cacheable(relevantArg=0,maxAge=1000)
    Collection<Role> getAssignedRoles(@NotNull Pair<Long, String> providerAndUserId, @NotNull User user) throws FindException;
}
