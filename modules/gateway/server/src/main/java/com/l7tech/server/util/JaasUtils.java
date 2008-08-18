/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.util;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.server.admin.GroupPrincipal;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class JaasUtils {
    protected static final Logger logger = Logger.getLogger(JaasUtils.class.getName());

    private JaasUtils() { }

    public static User getCurrentUser() {
        Subject subject = getCurrentSubject();
        if (subject == null) return null;
        Set<User> users = subject.getPrincipals(User.class);
        if (users == null || users.isEmpty()) return null;
        return users.iterator().next();
    }

    /*
     * Get group membership for the current subject, if it exists.
     *
     * @return The set of group IdentityHeaders (may be empty but not null)
     */
    public static Set<IdentityHeader> getCurrentUserGroupInfo(){
        Subject subject = getCurrentSubject();
        if (subject == null) 
            return Collections.emptySet();

        Set<GroupPrincipal> cPs = subject.getPrincipals(GroupPrincipal.class);
        if (cPs == null || cPs.isEmpty())
            return Collections.emptySet();

        Set<IdentityHeader> returnSet = new HashSet<IdentityHeader>();
        for(GroupPrincipal gP: cPs){
            returnSet.add(gP.getGroupHeader());
        }
        return returnSet;
    }

    public static Subject getCurrentSubject() {
        return Subject.getSubject(AccessController.getContext());
    }
}
