package com.l7tech.server.config;

import java.util.List;

/**
 * User: megery
 * Date: May 22, 2007
 * Time: 3:36:14 PM
 */
public interface KeystoreActionsListener {
    List<String> promptForKeystoreTypeAndPassword();

    void printKeystoreInfoMessage(String msg);
}
