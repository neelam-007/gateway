package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.RequestSwAAssertion;

import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class ClientRequestSwAAssertion extends UnimplementedClientAssertion {
    private static final Logger log = Logger.getLogger(ClientRequestSwAAssertion.class.getName());
    private RequestSwAAssertion requestSwAAssertion;

    public ClientRequestSwAAssertion(RequestSwAAssertion requestSwAAssertion) {
        this.requestSwAAssertion = requestSwAAssertion;
    }
}
