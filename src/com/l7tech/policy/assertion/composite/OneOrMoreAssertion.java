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
import java.util.Iterator;
import java.util.List;

/**
 * Evaluate children until none left or one succeeds; returns last result evaluated.
 *
 * Semantically equivalent to a short-circuited OR.
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

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        mustHaveChildren();
        Iterator kids = children();
        Assertion child;
        AssertionStatus result = AssertionStatus.FALSIFIED;
        while (kids.hasNext()) {
            child = (Assertion)kids.next();
            result = child.checkRequest(request, response);
            if (result == AssertionStatus.NONE) return result;
        }
        return result;
    }

    /**
     * Modify the provided PendingRequest to conform to this policy assertion.
     * For OneOrMoreAssertion, we'll run children only until one succeeds (or we run out of children).
     * @param req
     * @return the AssertionStatus.NONE if at least one child succeeded; the rightmost-child error otherwise.
     * @throws PolicyAssertionException
     */
    public AssertionStatus decorateRequest(PendingRequest req) throws PolicyAssertionException {
        mustHaveChildren();
        AssertionStatus result = AssertionStatus.FAILED;
        for (Iterator kids = children.iterator(); kids.hasNext();) {
            Assertion assertion = (Assertion)kids.next();
            result = assertion.decorateRequest(req);
            if (result == AssertionStatus.NONE)
                return result;
        }
        return result;
    }
}
