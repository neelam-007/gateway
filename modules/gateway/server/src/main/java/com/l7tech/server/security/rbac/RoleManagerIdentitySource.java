package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.identity.User;

import java.util.Set;

/**
 * Interface for provision of groups to the role manager.
 */
public interface RoleManagerIdentitySource {

    /**
     * Get the groups for the given user.
     *
     * <p>The groups will be used to determine security permissions for the
     * user, so should be validated.</p>
     *
     * @param user The user whose groups are required.
     * @return The set of group identity headers
     * @throws FindException if an error occurs finding the users groups
     */
    Set<IdentityHeader> getGroups( User user ) throws FindException;

    /**
     * Validate assignments of users to roles.
     *
     * @throws UpdateException if the role assignments are not acceptable
     */
    void validateRoleAssignments() throws UpdateException;
}
