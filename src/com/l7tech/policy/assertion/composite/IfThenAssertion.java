/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionError;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.proxy.datamodel.PendingRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

/**
 * Evaluates child assertions in order until one fails or all children have been evaluated.
 * Returns the last return code encountered.
 * There is no WS-Policy analog of this construct..
 *
 * Semantically equivalent to a short-circuited AND.
 *
 * User: mike
 * Date: Jun 13, 2003
 * Time: 5:08:00 PM
 */
public class IfThenAssertion extends CompositeAssertion {
    public IfThenAssertion() {
    }

    /** Convenience constructor for the common case of "if precondition then action" */
    public IfThenAssertion(Assertion precondition, Assertion action) {
        super(Arrays.asList(new Assertion[] { precondition, action }));
    }

    public IfThenAssertion(List children) {
        super(children);
    }

    public IfThenAssertion(CompositeAssertion parent, List children) {
        super(parent, children);
    }

    public AssertionError checkRequest(Request request, Response response) throws PolicyAssertionException {
        AssertionError result = AssertionError.FALSIFIED;
        for (Iterator iterator = children(); iterator.hasNext();) {
            Assertion child = (Assertion)iterator.next();
            result = child.checkRequest(request, response);
            if (result != AssertionError.NONE)
                return result;
        }
        return result;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param requst    The request to decorate.
     * @return AssertionError.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionError decorateRequest(PendingRequest requst) throws PolicyAssertionException {
        return AssertionError.NOT_YET_IMPLEMENTED;
    }
}
