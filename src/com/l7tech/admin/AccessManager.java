/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.admin;

import com.l7tech.common.Authorizer;
import com.l7tech.identity.Group;

import javax.security.auth.Subject;
import java.security.AccessControlException;
import java.security.AccessController;

/**
 * A bag of utility method that deal with admin roles
 * @author emil
 * @version Dec 10, 2004
 */
public class AccessManager {
    private final Authorizer authorizer;

    public AccessManager(Authorizer authorizer) {
        this.authorizer = authorizer;
    }

    /**
     * Returns a boolean indicating whether the authenticated user is included in the specified
     * logical "roles".
     *
     * @param roles role - a String array specifying the role names
     * @return a boolean indicating whether the user making this request belongs to one or more given
     *         roles; false if not or the user has not been authenticated
     */
    public boolean isUserInRole(String[] roles) {
        Subject subject = Subject.getSubject(AccessController.getContext());
        if (subject == null) {
            return false;
        }
        return authorizer.isSubjectInRole(subject, roles);
    }

    /**
     * Makes sure that current subject has full write admin role.
     *
     * @throws java.security.AccessControlException if not the case
     */
    public void enforceAdminRole()
      throws AccessControlException {
        if (!isUserInRole(new String[]{Group.ADMIN_GROUP_NAME})) {
            throw new AccessControlException("Must be member of " + Group.ADMIN_GROUP_NAME +
              " to perform this operation.");
        }
    }
}