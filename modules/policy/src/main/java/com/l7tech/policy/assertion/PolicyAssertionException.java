/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

/**
 * Something is wrong in the policy.
 * @author alex
 */
public class PolicyAssertionException extends Exception {
    private final Assertion assertion;

    public PolicyAssertionException(Assertion ass) {
        super();
        this.assertion = ass;
    }

    public PolicyAssertionException(Assertion ass, String message) {
        super( message );
        this.assertion = ass;
    }

    public PolicyAssertionException(Assertion ass, Throwable cause) {
        super( cause );
        this.assertion = ass;
    }

    public PolicyAssertionException(Assertion ass, String message, Throwable cause) {
        super( message, cause );
        this.assertion = ass;
    }

    /** @return the assertion bean that threw, or null if not known. */
    public Assertion getAssertion() {
        return assertion;
    }
}
