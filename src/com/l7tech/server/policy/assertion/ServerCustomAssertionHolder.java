/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.io.IOException;

/**
 * The <code>ServerCustomAssertionHolder</code> class represents the server side of the
 * custom assertion. It implenets the <code>ServerAssertion</code> interface, and it
 * preapres the environment for executing a  custom assertion
 *
 * @author emil
 * @version 1.0
 */
public class ServerCustomAssertionHolder implements ServerAssertion {
    CustomAssertionHolder customAssertion;

    public ServerCustomAssertionHolder(CustomAssertionHolder ca) {
        customAssertion = ca;
    }

    public ServerCustomAssertionHolder() {
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        return AssertionStatus.NONE;
    }
}
