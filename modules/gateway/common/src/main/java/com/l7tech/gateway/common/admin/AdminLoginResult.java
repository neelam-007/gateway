package com.l7tech.gateway.common.admin;

import com.l7tech.identity.User;

import java.io.Serializable;

/**
 * The return value from {@link AdminLogin#login(String, String)}.
 */
public final class AdminLoginResult implements Serializable {
    private static final long serialVersionUID = 13L;

    private final User user;
    private final String sessionCookie;
    private final AdminContext adminContext;

    public AdminLoginResult(User user, AdminContext context, String secret) {
        if (user == null || secret == null || secret.length() == 0 || context == null) throw new IllegalArgumentException();
        this.user = user;
        this.sessionCookie = secret;
        this.adminContext = context;
    }

    public User getUser() {
        return user;
    }

    public String getSessionCookie() {
        return sessionCookie;
    }

    public AdminContext getAdminContext() {
        return adminContext;
    }
}
