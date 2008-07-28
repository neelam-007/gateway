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
import java.util.logging.Logger;
import java.util.logging.Level;

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
    * From the current subject, if it exists, retrieve the current users group membership.
    * */
    public static Set<IdentityHeader> getCurrentUserGroupInfo(){
        Subject subject = getCurrentSubject();
        if (subject == null) return null;
        Set<GroupPrincipal> cPs = subject.getPrincipals(GroupPrincipal.class);
        if (cPs == null || cPs.isEmpty()) return null;
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
