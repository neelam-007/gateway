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
public class BadCredentialsException extends AuthenticationException {
    public BadCredentialsException() {
        super();
    }

    public BadCredentialsException( String message ) {
        super( message );
    }

    public BadCredentialsException( String message, Throwable cause ) {
        super( message, cause );
    }
}
