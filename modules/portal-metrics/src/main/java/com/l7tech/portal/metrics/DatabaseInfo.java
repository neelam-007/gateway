package com.l7tech.portal.metrics;

import org.apache.commons.lang.Validate;

/**
 * Database info required for connecting to a database.
 */
public class DatabaseInfo {
    final String url;
    final String username;
    final String password;

    public DatabaseInfo(final String url, final String username, final String password) {
        Validate.notEmpty(url, "Source url cannot be null or empty.");
        Validate.notEmpty(username, "Source username cannot be null or empty.");
        this.url = url;
        this.username = username;
        this.password = password;

    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
