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
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerOneOrMoreAssertion extends ServerCompositeAssertion implements ServerAssertion {
    public ServerOneOrMoreAssertion( OneOrMoreAssertion data ) {
        super( data );
        this.data = data;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        data.mustHaveChildren();
        ServerAssertion[] kids = getChildren();
        ServerAssertion child;
        AssertionStatus result = AssertionStatus.FALSIFIED;
        for (int i = 0; i < kids.length; i++) {
            child = kids[i];
            result = child.checkRequest(request, response);
            if (result == AssertionStatus.NONE) return result;
        }
        if (result != AssertionStatus.NONE)
            rollbackDeferredAssertions(request, response);
        return result;
    }

    protected OneOrMoreAssertion data;
}
