package com.l7tech.objectmodel;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Sep 2, 2003
 * Time: 1:57:10 PM
 * $Id$
 *
 *
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
