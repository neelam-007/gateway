package com.l7tech.server.config.exceptions;

/**
 * User: megery
 * Date: Jul 30, 2007
 * Time: 3:04:24 PM
 */
public class WrongKeystorePasswordException extends KeystoreActionsException{

    public WrongKeystorePasswordException(String message) {
        super(message);
    }

    public WrongKeystorePasswordException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongKeystorePasswordException(Throwable cause) {
        super(cause);
    }
}
