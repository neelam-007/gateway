/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class AuthenticationException extends Exception {
    public AuthenticationException() {
        super();
    }

    public AuthenticationException( String message ) {
        super( message );
    }

    public AuthenticationException( String message, Throwable cause ) {
        super( message, cause );
    }
}
