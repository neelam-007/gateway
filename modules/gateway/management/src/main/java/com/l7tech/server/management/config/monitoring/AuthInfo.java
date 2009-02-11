/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import java.io.Serializable;

/**
 * Holds a username and password for notification rules that can support authentication.  Exists because none of the
 * usual suspects is {@link Serializable} and we may want to customize properties for JAXB, JPA etc.
 */
public class AuthInfo implements Serializable {
    private String username;
    private char[] password;

    public AuthInfo(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }
}
