/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.util;

import com.l7tech.identity.User;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.util.Set;

/**
 * @author alex
 */
public class JaasUtils {
    private JaasUtils() { }

    public static User getCurrentUser() {
        Subject subject = getCurrentSubject();
        if (subject == null) return null;
        Set<User> users = subject.getPrincipals(User.class);
        if (users == null || users.isEmpty()) return null;
        return users.iterator().next();
    }

    public static Subject getCurrentSubject() {
        return Subject.getSubject(AccessController.getContext());
    }
}
