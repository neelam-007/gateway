/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.composite;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

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

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        mustHaveChildren();
        Iterator kids = children();
        Assertion child;
        AssertionStatus result = null;
        int numSucceeded = 0;
        while ( kids.hasNext() ) {
            child = (Assertion)kids.next();
            result = child.checkRequest( request, response );
            if ( result == AssertionStatus.NONE )
                ++numSucceeded;
        }
        return numSucceeded == 1 ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;
    }

    /**
     * Modify the provided PendingRequest to conform to this policy assertion.
     * For ExactlyOneAssertion, we'll run children until one succeeds or we run out.
     * @param req
     * @return AssertionStatus.NONE, or the rightmost-child's error if all children failed.
     * @throws PolicyAssertionException
     */
    public AssertionStatus decorateRequest(PendingRequest req) throws PolicyAssertionException {
        mustHaveChildren();
        AssertionStatus result = AssertionStatus.FALSIFIED;
        for (Iterator kids = children.iterator(); kids.hasNext();) {
            Assertion assertion = (Assertion) kids.next();
            AssertionStatus thisResult = assertion.decorateRequest(req);
            if (thisResult == AssertionStatus.NONE)
                return thisResult;
            result = thisResult;
        }
        return result;
    }
}
