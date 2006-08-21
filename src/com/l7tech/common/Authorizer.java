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

/**
 * The <code>Authorizer</code> abstract class provide authorization methods for
 * roles and access permissions. The concrete implementaiton implement the abstract
 * method {@link Authorizer#getUserPermissions()}
 *
 * @author emil
 * @version Sep 2, 2004
 */
public abstract class Authorizer {
    public boolean hasPermission(Subject subject, AttemptedOperation attempted) {
        Collection<Permission> perms = getUserPermissions();
        if (perms == null || perms.isEmpty()) return false;

        for (com.l7tech.common.security.rbac.Permission perm : perms) {
            if (perm.getEntityType() != EntityType.ANY && perm.getEntityType() != attempted.getType()) continue;
            if (perm.getOperation() != attempted.getOperation()) continue;

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
                if (read.getId() != null) {
                    if (perm.getScope().size() == 1) {
                        ScopePredicate pred = perm.getScope().iterator().next();
                        if (pred instanceof ObjectIdentityPredicate) {
                            ObjectIdentityPredicate oip = (ObjectIdentityPredicate) pred;
                            if (read.getId().equals(oip.getTargetEntityId())) {
                                // Permission is granted to read this object
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public abstract Collection<Permission> getUserPermissions() throws RuntimeException;
}