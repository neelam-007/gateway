/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

/**
 * @author alex
 */
public class FindException extends ObjectModelException {
    public FindException() {
        super();
    }

    public FindException( String message ) {
        super( message );
    }

    public FindException( String message, Throwable cause ) {
        super( message, cause );
    }
}
