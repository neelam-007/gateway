/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.util.SoapConstants;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;

/**
 * Data for an assertion that verifies whether a response matches a specified
 * XPath pattern.
 *
 * <p>Related function specifications:
 * <ul>
 *  <li><a href="http://sarek.l7tech.com/mediawiki/index.php?title=XML_Variables">XML Variables</a> (4.3)
 * </ul>
 *
 * @see com.l7tech.server.policy.assertion.ServerResponseXpathAssertion
 * @see com.l7tech.proxy.policy.assertion.ClientResponseXpathAssertion
 * @author alex
 *
 * @version $Revision$
 */
@ProcessesResponse
public class ResponseXpathAssertion extends SimpleXpathAssertion implements UsesVariables {
    public static final String DEFAULT_VAR_PREFIX = "responseXpath";

    protected String xmlMsgSrc;

    public ResponseXpathAssertion() {
        super();
        initDefaultXpath();
    }

    public ResponseXpathAssertion( XpathExpression xpath ) {
        super();
        setXpathExpression( xpath );
    }

    private void initDefaultXpath() {
        setXpathExpression(new XpathExpression( SoapConstants.SOAP_ENVELOPE_XPATH, createDefaultNamespaceMap()));
    }

    protected String defaultVariablePrefix() {
        return DEFAULT_VAR_PREFIX;
    }

    /**
     * Returns the XML message source to operate on.
     *
     * @return <code>null</code> for default (i.e., message in policy enforcement context); otherwise name of a message type context variable
     */
    public String getXmlMsgSrc() {
        return xmlMsgSrc;
    }

    /**
     * Specifies the XML message source to operate on.
     *
     * @param src <code>null</code> for default (i.e., message in policy enforcement context); otherwise name of a message type context variable
     */
    public void setXmlMsgSrc(final String src) {
        xmlMsgSrc = src;
    }

    public String[] getVariablesUsed() {
        return xmlMsgSrc == null ? new String[0] : new String[]{xmlMsgSrc};
    }
}
