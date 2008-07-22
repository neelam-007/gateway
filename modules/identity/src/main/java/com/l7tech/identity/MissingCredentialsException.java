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
public class MissingCredentialsException extends AuthenticationException {
    public MissingCredentialsException() {
        super();
    }

    public MissingCredentialsException( String message ) {
        super( message );
    }

    public MissingCredentialsException( String message, Throwable cause ) {
        super( message, cause );
    }
}
