/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server;

import com.l7tech.common.util.Locator;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.Set;

/**
 * The <code>AdminAuthorizer</code> abstract class provide authorization methods for
 * users. The concrete implementaiton implement the abstract method {@link Authorizer#getUserRoles(javax.security.auth.Subject)}
 *
 * @author emil
 * @version Sep 2, 2004
 */
public abstract class Authorizer {
    private static Authorizer defaultAuthorizer = newDefaultAdminAuthorizer();

    public static Authorizer getAuthorizer() {
        Authorizer aa = (Authorizer)Locator.getDefault().lookup(Authorizer.class);
        if (aa == null) {
            aa = defaultAuthorizer;
        }
        return aa;
    }

    /**
     * Determines whether an subject belongs to the specified Role (group).
     *
     * @param subject the subject to test
     * @param roles   the string array of role names
     * @return true if the subject, belongs to one of the the specified roles,
     *         false, otherwise.
     */
    public boolean isSubjectInRole(Subject subject, String[] roles) {
        Set principalGroups = getUserRoles(subject);
        for (int i = 0; i < roles.length; i++) {
            String role = roles[i];
            if (principalGroups.contains(role)) {
                return true;
            }
        }
        return false;

    }

    /**
     * Determine the roles (groups) for the given subject
     *
     * @param subject the subject
     * @return the set of user roles for the given subject
     * @throws RuntimeException o nerror retrieving user roles
     */
    public abstract Set getUserRoles(Subject subject) throws RuntimeException;


    /**
     * @return the default admin authorizer that is used if no authorizer is specified
     *         by the configuration. This authorizer always returns false.
     */
    private static Authorizer newDefaultAdminAuthorizer() {
        return new Authorizer() {

            public boolean isSubjectInRole(Subject subject, String[] roles) {
                return false;
            }

            public Set getUserRoles(Subject subject) throws RuntimeException {
                return Collections.EMPTY_SET;
            }
        };
    }
}