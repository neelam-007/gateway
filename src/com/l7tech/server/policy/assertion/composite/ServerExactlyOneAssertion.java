/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.composite;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerExactlyOneAssertion extends ServerCompositeAssertion implements ServerAssertion {
    public ServerExactlyOneAssertion( ExactlyOneAssertion data ) {
        super( data );
        this.data = data;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        data.mustHaveChildren();
        ServerAssertion[] kids = getChildren();
        ServerAssertion child;
        AssertionStatus result = null;
        int numSucceeded = 0;
        for (int i = 0; i < kids.length; i++) {
            child = kids[i];
            result = child.checkRequest( request, response );
            if ( result == AssertionStatus.NONE )
                ++numSucceeded;
        }

        result =  numSucceeded == 1 ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;
        if (result != AssertionStatus.NONE)
            rollbackDeferredAssertions(request, response);
        return result;
    }

    protected ExactlyOneAssertion data;
}
