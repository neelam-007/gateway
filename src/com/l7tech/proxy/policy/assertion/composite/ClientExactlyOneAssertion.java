/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientExactlyOneAssertion extends ClientCompositeAssertion implements ClientAssertion {
    private static final Category log = Category.getInstance(ClientExactlyOneAssertion.class);

    public ClientExactlyOneAssertion( ExactlyOneAssertion data ) {
        super( data );
        this.data = data;
    }

    /**
     * Modify the provided PendingRequest to conform to this policy assertion.
     * For ExactlyOneAssertion, we'll run children until one succeeds or we run out.
     * @param req
     * @return AssertionStatus.NONE, or the rightmost-child's error if all children failed.
     * @throws PolicyAssertionException
     */
    public AssertionStatus decorateRequest(PendingRequest req) throws PolicyAssertionException {
        data.mustHaveChildren();
        AssertionStatus result = AssertionStatus.FALSIFIED;
        for ( int i = 0; i < children.length; i++ ) {
            ClientAssertion assertion = children[i];
            AssertionStatus thisResult = assertion.decorateRequest(req);
            if (thisResult == AssertionStatus.NONE)
                return thisResult;
            result = thisResult;
        }
        return result;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected ExactlyOneAssertion data;
}
