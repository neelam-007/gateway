/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.composite;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionError;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Assertion;

import java.util.Iterator;
import java.util.List;

/**
 * Asserts that every one of the child Assertions returned a positive result, and returns the last result.
 *
 * Semantically equivalent to a non-short-circuited AND.
 *
 * @author alex
 * @version $Revision$
 */
public class AllAssertion extends CompositeAssertion {
    public AllAssertion() {
    }

    public AllAssertion( List children ) {
        super( children );
    }

    public AllAssertion( CompositeAssertion parent, List children ) {
        super( parent, children );
    }

    public AssertionError checkRequest(Request request, Response response) throws PolicyAssertionException {
        Iterator kids = children();
        Assertion child;
        AssertionError result = null;
        while ( kids.hasNext() ) {
            child = (Assertion)kids.next();
            result = child.checkRequest( request, response );
            if ( result != AssertionError.NONE ) return result;
        }
        return result;
    }
}
