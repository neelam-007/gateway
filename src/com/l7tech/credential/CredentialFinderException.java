/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential;

/**
 * @author alex
 */
public class CredentialFinderException extends Exception {
    public CredentialFinderException() {
        super();
    }

    public CredentialFinderException( String message ) {
        super( message );
    }

    public CredentialFinderException( String message, Throwable cause ) {
        super( message, cause );
    }
}
