/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.XpathBasedAssertion;

/**
 * Superclass for policy application xpath assertions (request and response xpath, with or without hardware).
 */
public abstract class ClientXpathAssertion extends ClientAssertion {
    protected final XpathBasedAssertion xpathBasedAssertion;
    protected final boolean isRequest;

    public ClientXpathAssertion(XpathBasedAssertion xpathBasedAssertion, boolean isRequest) {
        this.xpathBasedAssertion = xpathBasedAssertion;
        this.isRequest = isRequest;
    }

    protected XpathExpression getXpathExpression() {
        return xpathBasedAssertion.getXpathExpression();
    }

    public String getName() {
        String str = "";
        if (xpathBasedAssertion != null && xpathBasedAssertion.pattern() != null)
            str = " \"" + xpathBasedAssertion.pattern() + '"';
        final String mess = isRequest ? "Request" : "Response";
        return mess + " must match XPath expression" + str;
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }
}
