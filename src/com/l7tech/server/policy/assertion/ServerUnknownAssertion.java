/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.UnknownAssertion;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * The <code>ServerUnknownAssertion</code> is an assertion that indicates
 * that there is an unknown assertion in the policy tree. This assertion
 * always return a negative result.
 * <p/>
 * One known scenario for unknown assertion is where, after the custom assertion
 * deinstall, there are remaining policies with custom assertions. We introduce
 * unknown assertion that always returns the {@link AssertionStatus#FALSIFIED}, and
 * logs the warning message.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ServerUnknownAssertion implements ServerAssertion {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private UnknownAssertion unknownAssertion;

    public ServerUnknownAssertion(UnknownAssertion a) {
        unknownAssertion = a;

    }

    public ServerUnknownAssertion() {}

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        final boolean hasDetailMessage = unknownAssertion != null && unknownAssertion.getDetailMessage() != null;
        String desc = hasDetailMessage ? unknownAssertion.getDetailMessage() : "No more description available";

        logger.warning("The unknown assertion invoked. Detail message is '" + desc + "'");
        return AssertionStatus.FALSIFIED;
    }
}
