package com.l7tech.common.security.xml;

/**
 * A session is invalid because it has expired.
 *
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell<br/>
 * Date: Aug 25, 2003<br/>
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
