/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;

import java.util.Iterator;
import java.util.List;

/**
 * Asserts that at least one of the child Assertions returned a positive result, and returns the last positive result.
 *
 * Semantically equivalent to a non-short-circuited OR.
 *

 * @author alex
 * @version $Revision$
 */
public class OneOrMoreAssertion extends CompositeAssertion {
    public OneOrMoreAssertion( List children ) {
        super( children );
    }

    public OneOrMoreAssertion( CompositeAssertion parent, List children ) {
        super( parent, children );
    }

    public AssertionError checkRequest( Request request, Response response ) throws PolicyAssertionException {
        Iterator kids = children();
        Assertion child;
        AssertionError result = null;
        AssertionError lastResult = AssertionError.FALSIFIED;
        while ( kids.hasNext() ) {
            child = (Assertion)kids.next();
            result = child.checkRequest( request, response );
            if ( result == AssertionError.NONE ) lastResult = result;
        }
        return lastResult;
    }
}
