package com.l7tech.common.security.xml;

/**
 * User: flascell
 * Date: Aug 25, 2003
 * Time: 4:57:21 PM
 *
 * A session was not found
 */
public class SessionNotFoundException extends Exception {
    public SessionNotFoundException() {
    }

    public SessionNotFoundException(String message) {
        super(message);
    }

    public SessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SessionNotFoundException(Throwable cause) {
        super(cause);
    }
}
