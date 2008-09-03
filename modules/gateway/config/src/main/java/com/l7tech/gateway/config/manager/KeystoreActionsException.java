package com.l7tech.gateway.config.manager;

/**
 * User: megery
 * Date: Jul 19, 2007
 * Time: 3:21:12 PM
 */
public class KeystoreActionsException extends Exception {
    public KeystoreActionsException(String message) {
        super(message);
    }

    public KeystoreActionsException(String message, Throwable cause) {
        super(message, cause);
    }

    public KeystoreActionsException(Throwable cause) {
        super(cause);
    }
}
