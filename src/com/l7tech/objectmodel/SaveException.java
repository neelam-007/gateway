/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

/**
 * @author alex
 */
public class SaveException extends ObjectModelException {
    public SaveException() {
        super();
    }

    public SaveException( String message ) {
        super( message );
    }

    public SaveException( String message, Throwable cause ) {
        super( message, cause );
    }
}
