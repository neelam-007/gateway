package com.l7tech.admin;

import com.l7tech.identity.User;
import com.l7tech.common.security.rbac.Permission;

import java.io.Serializable;
import java.util.Set;
import java.util.Collections;

/**
 * The return value from {@link AdminLogin#login(String, String)}.
 */
public final class AdminLoginResult implements Serializable {
    private static final long serialVersionUID = -3273786615759730505L;

    private final User user;
    private final Set<Permission> permissions;
    private final String sessionCookie;
    private final AdminContext adminContext;

    public AdminLoginResult(User user, Set<Permission> permissions, AdminContext context, String secret) {
        if (user == null || secret == null || secret.length() == 0 || context == null) throw new IllegalArgumentException();
        this.user = user;
        this.permissions = Collections.unmodifiableSet(permissions);
        this.sessionCookie = secret;
        this.adminContext = context;
    }

    public User getUser() {
        return user;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public String getSessionCookie() {
        return sessionCookie;
    }

    public AdminContext getAdminContext() {
        return adminContext;
    }
}
