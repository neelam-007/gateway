/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.XpathExpression;

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
public class ResponseXpathAssertion extends XpathBasedAssertion {
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
}
