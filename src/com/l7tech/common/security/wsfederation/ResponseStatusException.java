package com.l7tech.common.security.wsfederation;

import com.l7tech.common.util.CausedIOException;

/**
 * Exception thrown when the status code from the federation server indicates
 * failure.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ResponseStatusException extends CausedIOException {
    public ResponseStatusException(Throwable cause) {
        super(cause);
    }

    public ResponseStatusException() {
        super();
    }

    public ResponseStatusException(String s) {
        super(s);
    }

    public ResponseStatusException(String s, Throwable cause) {
        super(s, cause);
    }
}
