package com.l7tech.server.config;

/**
 * User: megery
 * Date: May 22, 2007
 * Time: 3:36:14 PM
 */
public interface KeystoreActionsListener {
    char[] promptForKeystorePassword(String message);
}
