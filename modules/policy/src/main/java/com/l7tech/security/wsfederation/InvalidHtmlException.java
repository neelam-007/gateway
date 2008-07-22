package com.l7tech.security.wsfederation;

import com.l7tech.util.CausedIOException;

/**
 * Exception thrown when the HTML obtained from the server is invalid.
 *
 * @author $Author$
 * @version $Revision$
 */
public class InvalidHtmlException extends CausedIOException {
    public InvalidHtmlException(Throwable cause) {
        super(cause);
    }

    public InvalidHtmlException() {
        super();
    }

    public InvalidHtmlException(String s) {
        super(s);
    }

    public InvalidHtmlException(String s, Throwable cause) {
        super(s, cause);
    }
}
