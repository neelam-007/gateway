/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionExcepion;
import com.l7tech.message.Request;
import com.l7tech.message.Response;

/**
 * Immutable.
 *
 * @author alex
 */
public abstract class Policy {
    public Policy( Assertion root ) {
        _rootAssertion = root;
    }

    public abstract int checkRequest( Request request, Response response ) throws PolicyAssertionExcepion;

    public abstract Assertion getRootAssertion();

    protected final Assertion _rootAssertion;
}
