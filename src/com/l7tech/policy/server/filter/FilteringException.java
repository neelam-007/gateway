package com.l7tech.policy.server.filter;

/**
 * User: flascell
 * Date: Aug 14, 2003
 * Time: 3:17:10 PM
 * $Id$
 *
 * An exception thrown while filtering a policy.
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
