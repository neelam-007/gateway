package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.SimpleXpathAssertion;

/**
 * Server-side processing for an assertion that verifies whether a response
 * matches a specified XPath pattern.
 *
 * @see com.l7tech.policy.assertion.ResponseXpathAssertion
 * @see com.l7tech.proxy.policy.assertion.ClientResponseXpathAssertion
 * @author alex
 */
public class ServerResponseXpathAssertion extends ServerXpathAssertion<SimpleXpathAssertion> {
    public ServerResponseXpathAssertion(SimpleXpathAssertion data) {
        super(data, false);
    }
}
