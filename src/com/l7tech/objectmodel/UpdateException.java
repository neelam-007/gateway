/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

/**
 * @author alex
 */
public class UpdateException extends ObjectModelException {
    public UpdateException() {
        super();
    }

    public UpdateException( String message ) {
        super( message );
    }

    public UpdateException( String message, Throwable cause ) {
        super( message, cause );
    }
}
