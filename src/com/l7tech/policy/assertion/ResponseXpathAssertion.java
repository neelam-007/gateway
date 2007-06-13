/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;

/**
 * Data for an assertion that verifies whether a response matches a specified
 * XPath pattern.
 *
 * @see com.l7tech.server.policy.assertion.ServerResponseXpathAssertion
 * @see com.l7tech.proxy.policy.assertion.ClientResponseXpathAssertion
 * @author alex
 *
 * @version $Revision$
 */
@ProcessesResponse
public class ResponseXpathAssertion extends SimpleXpathAssertion {
    public static final String DEFAULT_VAR_PREFIX = "responseXpath";

    public ResponseXpathAssertion() {
        super();
        initDefaultXpath();
    }

    public ResponseXpathAssertion( XpathExpression xpath ) {
        super();
        setXpathExpression( xpath );
    }

    private void initDefaultXpath() {
        setXpathExpression(new XpathExpression(SoapUtil.SOAP_ENVELOPE_XPATH, createDefaultNamespaceMap()));
    }

    protected String defaultVariablePrefix() {
        return DEFAULT_VAR_PREFIX;
    }
}
