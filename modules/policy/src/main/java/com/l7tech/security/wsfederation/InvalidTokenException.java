package com.l7tech.security.wsfederation;

import com.l7tech.util.CausedIOException;

/**
 * Exception thrown when an unknown or invalid token is obtained.
 *
 * @author $Author$
 * @version $Revision$
 */
public class InvalidTokenException extends CausedIOException {
    public InvalidTokenException(Throwable cause) {
        super(cause);
    }

    public InvalidTokenException() {
        super();
    }

    public InvalidTokenException(String s) {
        super(s);
    }

    public InvalidTokenException(String s, Throwable cause) {
        super(s, cause);
    }
}
