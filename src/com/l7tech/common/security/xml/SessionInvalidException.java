package com.l7tech.common.security.xml;

/**
 * User: flascell
 * Date: Aug 25, 2003
 * Time: 5:24:48 PM
 *
 * A session is invalid because it has expired.
 */
public class SessionInvalidException extends Exception {
    public SessionInvalidException() {
    }

    public SessionInvalidException(String message) {
        super(message);
    }

    public SessionInvalidException(String message, Throwable cause) {
        super(message, cause);
    }

    public SessionInvalidException(Throwable cause) {
        super(cause);
    }
}
