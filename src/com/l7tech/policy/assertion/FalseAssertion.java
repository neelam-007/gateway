/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;



/**
 * An assertion that always returns a negative result.
 *
 * @author alex
 * @version $Revision$
 */
public class FalseAssertion extends Assertion {
    /**
     * Static FalseAssertion with no parent.  Useful as a "do-nothing" policy tree that needn't be
     * instantiated every time such a thing is needed.
     */
    private static final FalseAssertion INSTANCE = new FalseAssertion();

    public static FalseAssertion getInstance() {
        return INSTANCE;
    }
}
