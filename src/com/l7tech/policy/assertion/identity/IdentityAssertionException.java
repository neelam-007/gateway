/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.policy.assertion.PolicyAssertionException;

/**
 * @author alex
 * @version $Revision$
 */
public class IdentityAssertionException extends PolicyAssertionException {
    public IdentityAssertionException() {
        super();
    }

    public IdentityAssertionException( String message ) {
        super( message );
    }

    public IdentityAssertionException( String message, Throwable cause ) {
        super( message, cause );
    }
}
