package com.l7tech.admin;

import java.io.Serializable;

/**
 * The return value from {@link AdminLogin#login(String, String)}.
 */
public final class AdminLoginResult implements Serializable {

    private static final long serialVersionUID = -3273786615759730505L;
    private final String sessionCookie;
    private final AdminContext adminContext;

    public AdminLoginResult(AdminContext context, String secret) {
        if (secret == null || secret.length() == 0 || context == null) throw new IllegalArgumentException();
        this.sessionCookie = secret;
        this.adminContext = context;
    }

    public String getSessionCookie() {
        return sessionCookie;
    }

    public AdminContext getAdminContext() {
        return adminContext;
    }
}
