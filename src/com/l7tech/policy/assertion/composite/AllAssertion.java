/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.composite;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.proxy.datamodel.PendingRequest;

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

    public AssertionStatus checkRequest(Request request, Response response) throws PolicyAssertionException {
        mustHaveChildren();
        Iterator kids = children();
        Assertion child;
        AssertionStatus result = null;
        while ( kids.hasNext() ) {
            child = (Assertion)kids.next();
            result = child.checkRequest( request, response );
            if ( result != AssertionStatus.NONE ) return result;
        }
        return result;
    }

    /**
     * Modify the provided PendingRequest to conform to this policy assertion.
     * For an AllAssertion, we'll have all our children decorate the request.
     * @param req
     * @return the AssertionStatus.NONE if no child returned an error; the rightmost-child error otherwise.
     * @throws PolicyAssertionException
     */
    public AssertionStatus decorateRequest(PendingRequest req) throws PolicyAssertionException {
        mustHaveChildren();
        AssertionStatus result = AssertionStatus.NONE;
        for (Iterator kids = children.iterator(); kids.hasNext();) {
            Assertion assertion = (Assertion)kids.next();
            AssertionStatus thisResult = assertion.decorateRequest(req);
            if (thisResult != AssertionStatus.NONE)
                result = thisResult;
        }
        return result;
    }
}
