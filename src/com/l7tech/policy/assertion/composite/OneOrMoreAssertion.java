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
import com.l7tech.proxy.datamodel.PendingRequest;

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
    public OneOrMoreAssertion() {
    }

    public OneOrMoreAssertion(List children) {
        super(children);
    }

    public OneOrMoreAssertion(CompositeAssertion parent, List children) {
        super(parent, children);
    }

    public AssertionError checkRequest(Request request, Response response) throws PolicyAssertionException {
        mustHaveChildren();
        Iterator kids = children();
        Assertion child;
        AssertionError result = null;
        AssertionError lastResult = AssertionError.FALSIFIED;
        while (kids.hasNext()) {
            child = (Assertion)kids.next();
            result = child.checkRequest(request, response);
            if (result == AssertionError.NONE) lastResult = result;
        }
        return lastResult;
    }

    /**
     * Modify the provided PendingRequest to conform to this policy assertion.
     * For a OneOrMoreAssertion, we'll have all our children attempt decorate the request, and return success
     * if even one of them succeeded.
     * @param req
     * @return the AssertionError.NONE if at least one child succeeded; the rightmost-child error otherwise.
     * @throws PolicyAssertionException
     */
    public AssertionError decorateRequest(PendingRequest req) throws PolicyAssertionException {
        mustHaveChildren();
        boolean oneWorked = false;
        AssertionError result = AssertionError.FAILED;
        for (Iterator kids = children.iterator(); kids.hasNext();) {
            Assertion assertion = (Assertion)kids.next();
            result = assertion.decorateRequest(req);
            if (result == AssertionError.NONE)
                oneWorked = true;
        }
        return oneWorked ? AssertionError.NONE : result;
    }
}
