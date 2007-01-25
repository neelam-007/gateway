/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HtmlFormDataAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;

/**
 * @author rmak
 * @since SecureSpan 3.7
 */
public class ServerHtmlFormDataAssertion extends AbstractServerAssertion<HtmlFormDataAssertion> implements ServerAssertion {
    private final HtmlFormDataAssertion _assertion;

    public ServerHtmlFormDataAssertion(final HtmlFormDataAssertion assertion) {
        super(assertion);
        _assertion = assertion;

        // TODO To be implemented.








    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        // TODO To be implemented.

        //context.getRequest().getHttpRequestKnob().getParameterNames()






        return AssertionStatus.NONE;
    }
}
