/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.util.SoapConstants;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;

/**
 * Data for an assertion that verifies whether a request matches a specified
 * XPath pattern.
 *
 * @see com.l7tech.server.policy.assertion.ServerRequestXpathAssertion
 * @see com.l7tech.proxy.policy.assertion.ClientRequestXpathAssertion
 * @author alex
 *
 * @version $Revision$
 */
@ProcessesRequest
public class RequestXpathAssertion extends SimpleXpathAssertion {
    public static final String DEFAULT_VAR_PREFIX = "requestXpath";

    public RequestXpathAssertion() {
        super();
        initDefaultXpath();
    }

    public RequestXpathAssertion( XpathExpression xpath ) {
        super();
        setXpathExpression( xpath );
    }

    private void initDefaultXpath() {
        setXpathExpression(new XpathExpression( SoapConstants.SOAP_ENVELOPE_XPATH, createDefaultNamespaceMap()));
    }

    protected String defaultVariablePrefix() {
        return DEFAULT_VAR_PREFIX;
    }
}
