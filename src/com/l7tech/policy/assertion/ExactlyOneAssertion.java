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
import java.io.Serializable;

/**
 * Asserts that at least one of the child Assertions returned a positive result, and immediately returns the first positive result.
 *
 * Semantically equivalent to a short-circuited AND.
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
        while ( kids.hasNext() ) {
            child = (Assertion)kids.next();
            result = child.checkRequest( request, response );
            if ( result == AssertionError.NONE ) return result;
        }
        return AssertionError.FALSIFIED;
    }
}
