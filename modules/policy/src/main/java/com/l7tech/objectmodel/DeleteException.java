/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

/**
 * @author alex
 */
public class DeleteException extends ObjectModelException {
    public DeleteException() {
        super();
    }

    public DeleteException( String message ) {
        super( message );
    }

    public DeleteException( String message, Throwable cause ) {
        super( message, cause );
    }
}
