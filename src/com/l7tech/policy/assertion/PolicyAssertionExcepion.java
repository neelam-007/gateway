/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

/**
 * @author alex
 */
public class PolicyAssertionExcepion extends Exception {
    public PolicyAssertionExcepion() {
        super();
    }

    public PolicyAssertionExcepion( String message ) {
        super( message );
    }

    public PolicyAssertionExcepion( String message, Throwable cause ) {
        super( message, cause );
    }
}
