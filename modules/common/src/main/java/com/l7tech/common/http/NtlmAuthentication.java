/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.http;

/**
 * @author alex
 */
public class NtlmAuthentication {
    private final String username;
    private final char[] password;
    private final String domain;
    private final String host;

    /**
     * Creates a new <code>PasswordAuthentication</code> object from the given
     * user name and password.
     * <p/>
     * <p> Note that the given user password is cloned before it is stored in
     * the new <code>PasswordAuthentication</code> object.
     *
     * @param userName the user name
     * @param password the user's password
     */
    public NtlmAuthentication(String userName, char[] password, String domain, String host) {
        this.username = userName;
        this.password = password;
        this.domain = domain;
        this.host = host;
    }


    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return password;
    }

    public String getDomain() {
        return domain;
    }

    public String getHost() {
        return host;
    }
}
