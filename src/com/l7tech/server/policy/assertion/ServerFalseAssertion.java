/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.message.Request;
import com.l7tech.message.Response;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerFalseAssertion implements ServerAssertion {
    public ServerFalseAssertion( FalseAssertion ass ) {
        // meaningless
    }

    public ServerFalseAssertion() {
        // meaningless
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        return AssertionStatus.FALSIFIED;
    }
}
