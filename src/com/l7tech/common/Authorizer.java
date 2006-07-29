/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common;

import com.l7tech.common.security.rbac.*;
import com.l7tech.objectmodel.Entity;

import javax.security.auth.Subject;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * The <code>Authorizer</code> abstract class provide authorization methods for
 * roles and access permissions. The concrete implementaiton implement the abstract
 * method {@link Authorizer#getUserRoles(javax.security.auth.Subject)}
 *
 * @author emil
 * @version Sep 2, 2004
 */
public abstract class Authorizer {
    private static final Logger logger = Logger.getLogger(Authorizer.class.getName());

    public boolean hasPermission(Subject subject, AttemptedOperation attempted) {
        Collection<Role> roles = getUserRoles(subject);
        if (roles == null || roles.isEmpty()) return false;

        for (Role role : roles) {
            perms: for (com.l7tech.common.security.rbac.Permission perm : role.getPermissions()) {
                if (perm.getEntityType() != EntityType.ANY && perm.getEntityType() != attempted.getType()) continue perms;
                if (perm.getOperation() != attempted.getOperation()) continue perms;

                if (attempted instanceof AttemptedEntityOperation) {
                    AttemptedEntityOperation aeo = (AttemptedEntityOperation) attempted;
                    Entity ent = aeo.getEntity();
                    if (perm.matches(ent)) return true;
                } else if (attempted instanceof AttemptedCreate) {
                    // CREATE doesn't support any scope yet, only a type
                    return true;
                } else if (attempted instanceof AttemptedRead) {
                    // Permission grants read access to anything with matching type
                    if (perm.getScope() == null || perm.getScope().size() == 0) return true;

                    AttemptedRead read = (AttemptedRead) attempted;
                    if (read.getOid() != Entity.DEFAULT_OID) {
                        if (perm.getScope().size() == 1) {
                            ScopePredicate pred = perm.getScope().iterator().next();
                            if (pred instanceof ObjectIdentityPredicate) {
                                ObjectIdentityPredicate oip = (ObjectIdentityPredicate) pred;
                                if (oip.getTargetEntityOid() == read.getOid()) {
                                    // Permission is granted to read this object
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Determine the roles (groups) for the given subject
     *
     * @param subject the subject
     * @return the set of user roles for the given subject
     * @throws RuntimeException on error retrieving user roles
     */
    public abstract Collection<Role> getUserRoles(Subject subject) throws RuntimeException;
}