/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.proxy.datamodel.PendingRequest;

/**
 * An assertion that always returns a positive result.
 *
 * @author alex
 * @version $Revision$
 */
public class TrueAssertion extends Assertion {
    /**
     * Static TrueAssertion with no parent.  Useful as a "do-nothing" policy tree that needn't be
     * instantiated every time such a thing is needed.
     */
    private static final TrueAssertion INSTANCE = new TrueAssertion();

    public AssertionError checkRequest(Request request, Response response) throws PolicyAssertionException {
        return AssertionError.NONE;
    }

    public AssertionError decorateRequest(PendingRequest requst) throws PolicyAssertionException {
        return AssertionError.NONE;
    }

    /**
     * Quickly get an existing TrueAssertion with no parent to use as a "do-nothing" policy tree.
     * @return
     */
    public static TrueAssertion getInstance() {
        return INSTANCE;
    }
}
