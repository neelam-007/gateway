/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

/**
 * @author alex
 */
public class PolicyAssertionException extends Exception {
    public PolicyAssertionException() {
        super();
    }

    public PolicyAssertionException( String message ) {
        super( message );
    }

    public PolicyAssertionException( String message, Throwable cause ) {
        super( message, cause );
    }
}
