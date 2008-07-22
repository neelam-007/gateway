/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.AssertionStatus;

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

    public CredentialFinderException( String message, AssertionStatus status ) {
        super( message );
        _status = status;
    }

    public CredentialFinderException( String message, Throwable cause ) {
        super( message, cause );
    }

    public CredentialFinderException( String message, Throwable cause, AssertionStatus status ) {
        super( message, cause );
        _status = status;
    }

    public AssertionStatus getStatus() {
        return _status;
    }

    public AssertionStatus _status;
}
