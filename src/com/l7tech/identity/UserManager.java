package com.l7tech.identity;

import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.Set;

/**
 * An interface for the user management component of {@link IdentityProvider}s.
 * @author alex
 */
public interface UserManager extends EntityManager {
    /**
     * Retrieves the {@link User} with the specified unique ID.
     */
    User findByPrimaryKey( String identifier ) throws FindException;

    /**
     * Retrieves the {@link User} with the specified login.
     *
     * This method's return value presumes that there exists at most one user in each provider with a given login,
     * but this isn't always guaranteed.  This constraint is enforced in the {@link com.l7tech.server.identity.internal.InternalIdentityProviderServer}
     * but not in others.
     */
    User findByLogin( String login ) throws FindException;

    /**
     * Deletes the specified user.
     */
    void delete( User user ) throws DeleteException, ObjectNotFoundException;

    /**
     * Deletes the user with the specified primary key.
     */
    void delete( String identifier ) throws DeleteException, ObjectNotFoundException;

    /**
     * Deletes all users.
     * @param ipoid is unnecessary, since the UserManager maintains a link to its provider.
     */
    void deleteAll(long ipoid) throws DeleteException, ObjectNotFoundException;

    /**
     * Saves a new user.
     * @return the unique identifier of the user
     */
    String save( User user ) throws SaveException;

    /**
     * Updates an existing user.
     */
    void update( User user ) throws UpdateException, ObjectNotFoundException;

    /**
     * Saves a new user and replaces its group memberships based on a {@link Set} of {@link EntityHeader}s pointing to {@link Group}s.
     */
    String save( User user, Set groupHeaders ) throws SaveException;

    /**
     * Updates an existing user and replaces its group memberships based on a {@link Set} of {@link EntityHeader}s pointing to {@link Group}s.
     * @param user
     * @param groupHeaders
     * @throws UpdateException
     * @throws ObjectNotFoundException
     */
    void update( User user, Set groupHeaders ) throws UpdateException, ObjectNotFoundException;

    /**
     * Finds users whose name or login matches the specified pattern.
     */
    Collection search(String searchString) throws FindException;

    /**
     * Creates an {@link EntityHeader} pointing to the specified {@link User}.
     */
    EntityHeader userToHeader(User user);

    /**
     * @return the {@link Class} that entities managed by this manager belong to.
     */
    Class getImpClass();
}
