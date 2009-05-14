/*
* Copyright (C) 2003 Layer 7 Technologies Inc.
*
* $Id$
*/

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.SimpleXpathAssertion;
import org.springframework.context.ApplicationContext;

/**
 * Server-side processing for an assertion that verifies whether a response
 * matches a specified XPath pattern.
 *
 * @see com.l7tech.policy.assertion.ResponseXpathAssertion
 * @see com.l7tech.proxy.policy.assertion.ClientResponseXpathAssertion
 * @author alex
 * @version $Revision$
 */
public class ServerResponseXpathAssertion extends ServerXpathAssertion<SimpleXpathAssertion> {
    public ServerResponseXpathAssertion(SimpleXpathAssertion data, ApplicationContext springContext) {
        super(data, springContext, false);
    }
}
