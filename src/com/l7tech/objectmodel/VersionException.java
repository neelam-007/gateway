package com.l7tech.objectmodel;

/**
 * Object being updated is outdated.
 * Might mean that is was updated by another administrator.
 * 
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Sep 2, 2003<br/>
 * $Id$
 */
public class VersionException extends Exception{
    public VersionException() {
    }

    public VersionException(String message) {
        super(message);
    }

    public VersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public VersionException(Throwable cause) {
        super(cause);
    }
}
