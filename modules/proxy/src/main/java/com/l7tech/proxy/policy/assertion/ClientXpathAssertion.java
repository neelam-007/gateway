/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.xml.xpath.DeferredFailureDomCompiledXpathHolder;
import com.l7tech.xml.xpath.DomCompiledXpath;
import com.l7tech.xml.xpath.XpathExpression;
import org.jaxen.JaxenException;

/**
 * Superclass for policy application xpath assertions (request and response xpath, with or without hardware).
 */
public abstract class ClientXpathAssertion extends ClientAssertion {
    protected final XpathBasedAssertion xpathBasedAssertion;
    protected final boolean isRequest;
    protected final DeferredFailureDomCompiledXpathHolder compiledXpath;

    public ClientXpathAssertion(XpathBasedAssertion xpathBasedAssertion, boolean isRequest) {
        this.xpathBasedAssertion = xpathBasedAssertion;
        this.isRequest = isRequest;
        this.compiledXpath = new DeferredFailureDomCompiledXpathHolder(getXpathExpression());
    }

    protected DomCompiledXpath getCompiledXpath() throws JaxenException {
        return compiledXpath.getCompiledXpath();
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
