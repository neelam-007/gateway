package com.l7tech.server.secureconversation;

/**
 * Represent a conflict between two sessions having the same identifier.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 3, 2004<br/>
 * $Id$<br/>
 */
public class DuplicateSessionException extends Exception {
    public DuplicateSessionException() {
	    super();
    }

    public DuplicateSessionException(String message) {
	    super(message);
    }

    public DuplicateSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
