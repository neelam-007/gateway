package com.l7tech.security.wsfederation;

import com.l7tech.util.CausedIOException;

/**
 * Exception thrown when the status code from the federation server indicates
 * failure.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ResponseStatusException extends CausedIOException {

    //- PUBLIC

    public ResponseStatusException(int status, Throwable cause) {
        super(cause);
        this.status = status;
    }

    public ResponseStatusException(int status) {
        super();
        this.status = status;
    }

    public ResponseStatusException(String s, int status) {
        super(s);
        this.status = status;
    }

    public ResponseStatusException(String s, int status, Throwable cause) {
        super(s, cause);
        this.status = status;
    }

    /**
     * Get the failure status code.
     *
     * @return the status code that triggered this exception
     */
    public int getStatus() {
        return status;
    }

    //- PRIVATE

    private final int status;
}
