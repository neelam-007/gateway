package com.l7tech.security.wsfederation;

import com.l7tech.util.CausedIOException;

/**
 * Exception thrown when authorization fails.
 *
 * @author $Author$
 * @version $Revision$
 */
public class NotAuthorizedException extends CausedIOException {
    
    public NotAuthorizedException(Throwable cause) {
        super(cause);
    }

    public NotAuthorizedException() {
        super();
    }

    public NotAuthorizedException(String s) {
        super(s);
    }

    public NotAuthorizedException(String s, Throwable cause) {
        super(s, cause);
    }
}
