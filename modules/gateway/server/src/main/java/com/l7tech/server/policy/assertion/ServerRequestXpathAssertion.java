/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.RequestXpathAssertion;
import org.springframework.context.ApplicationContext;

/**
 * Server-side processing for an assertion that verifies whether a request
 * matches a specified XPath pattern.
 *
 * @author alex
 * @version $Revision$
 * @see com.l7tech.policy.assertion.RequestXpathAssertion
 * @see com.l7tech.proxy.policy.assertion.ClientRequestXpathAssertion
 */
public class ServerRequestXpathAssertion extends ServerXpathAssertion<RequestXpathAssertion> {
    public ServerRequestXpathAssertion(RequestXpathAssertion data, ApplicationContext springContext) {
        super(data, springContext, true);
    }
}
