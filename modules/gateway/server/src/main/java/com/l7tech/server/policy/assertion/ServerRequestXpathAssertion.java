package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.RequestXpathAssertion;

/**
 * Server-side processing for an assertion that verifies whether a request
 * matches a specified XPath pattern.
 *
 * @author alex
 * @see com.l7tech.policy.assertion.RequestXpathAssertion
 * @see com.l7tech.proxy.policy.assertion.ClientRequestXpathAssertion
 */
public class ServerRequestXpathAssertion extends ServerXpathAssertion<RequestXpathAssertion> {
    public ServerRequestXpathAssertion(RequestXpathAssertion data) {
        super(data, true);
    }
}
