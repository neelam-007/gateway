/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

/**
 * @author alex
 */
public class ObjectModelException extends Exception {
    public ObjectModelException() {
        super();
    }

    public ObjectModelException( String message ) {
        super( message );
    }

    public ObjectModelException( String message, Throwable cause ) {
        super( message, cause );
    }

    public ObjectModelException(Throwable cause) {
        super(cause);
    }
}
