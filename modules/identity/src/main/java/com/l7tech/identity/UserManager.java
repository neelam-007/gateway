package com.l7tech.identity;

import com.l7tech.objectmodel.*;

import java.util.Set;

/**
 * An interface for the user management component of {@link IdentityProvider}s.
 * @author alex
 */
public interface UserManager<UT extends User> {
    /**
     * Retrieves the {@link User} with the specified unique ID.
     */
    UT findByPrimaryKey( String identifier ) throws FindException;

    /**
     * Retrieves the {@link User} with the specified login.
     *
     * This method's return value presumes that there exists at most one user in each provider with a given login,
     * but this isn't always guaranteed.  This constraint is enforced in the {@link com.l7tech.server.identity.internal.InternalIdentityProvider}
     * but not in others.
     */
    UT findByLogin( String login ) throws FindException;

    /**
     * Deletes the specified user.
     */
    void delete(UT user) throws DeleteException;

    /**
     * Deletes the user with the specified primary key.
     */
    void delete( String identifier ) throws DeleteException, FindException;

    /**
     * Deletes all users.
     * @param ipoid is unnecessary, since the UserManager maintains a link to its provider.
     */
    void deleteAll(Goid ipoid) throws DeleteException, FindException;

    /**
     * Updates an existing user.
     */
    void update(UT user) throws UpdateException, FindException;

    /**
     * Saves a new user and replaces its group memberships based on a {@link Set} of {@link IdentityHeader}s pointing to {@link Group}s.
     */
    String save(UT user, Set<IdentityHeader> groupHeaders) throws SaveException;

    UT reify(UserBean bean);

    /**
     * Updates an existing user and replaces its group memberships based on a {@link Set} of {@link IdentityHeader}s pointing to {@link Group}s.
     * @param user
     * @param groupHeaders
     * @throws UpdateException
     * @throws FindException
     */
    void update(UT user, Set<IdentityHeader> groupHeaders ) throws UpdateException, FindException;

    /**
     * Finds users whose name or login matches the specified pattern.
     */
    EntityHeaderSet<IdentityHeader> search(String searchString) throws FindException;

    /**
     * Creates an {@link IdentityHeader} pointing to the specified {@link User}.
     */
    IdentityHeader userToHeader(UT user);

    /**
     * Creates a fake {@link User} containing any relevant fields from the provided {@link IdentityHeader}.
     */
    UT headerToUser(IdentityHeader header);

    /**
     * @return the {@link Class} that entities managed by this manager belong to.
     */
    Class<? extends User> getImpClass();

    EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException;
}
