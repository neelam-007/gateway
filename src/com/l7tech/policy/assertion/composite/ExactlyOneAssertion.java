/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.composite;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.AssertionError;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Assertion;

import java.util.Iterator;
import java.util.List;
import java.io.Serializable;

/**
 * Asserts that one and only one child assertion returns a true value.
 *
 * Semantically equivalent to a non-short-circuited exclusive-OR.
 *
 * @author alex
 * @version $Revision$
 */
public class ExactlyOneAssertion extends CompositeAssertion implements Serializable {
    public ExactlyOneAssertion() {
        super();
    }

    public ExactlyOneAssertion( List children ) {
        super( children );
    }

    public ExactlyOneAssertion( CompositeAssertion parent, List children ) {
        super( parent, children );
    }

    public AssertionError checkRequest(Request request, Response response) throws PolicyAssertionException {
        Iterator kids = children();
        Assertion child;
        AssertionError result = null;
        int numSucceeded = 0;
        while ( kids.hasNext() ) {
            child = (Assertion)kids.next();
            result = child.checkRequest( request, response );
            if ( result == AssertionError.NONE )
                ++numSucceeded;
        }
        return numSucceeded == 1 ? AssertionError.NONE : AssertionError.FALSIFIED;
    }
}
