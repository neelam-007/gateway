package com.l7tech.server.policy.filter;

/**
 * An exception thrown while filtering a policy.
 *
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 14, 2003<br/>
 * $Id$
 */
public class FilteringException extends Exception {

    public FilteringException() {
    }

    public FilteringException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    public FilteringException(String message) {
        super(message);
    }

    public FilteringException(String message, Throwable cause) {
        super(message, cause);
    }
}
