/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.util.SoapUtil;

import javax.xml.soap.SOAPConstants;
import javax.xml.rpc.NamespaceConstants;
import java.util.Map;
import java.util.HashMap;

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
public class RequestXpathAssertion extends XpathBasedAssertion {
    public RequestXpathAssertion() {
        super();
        initDefaultXpath();
    }

    public RequestXpathAssertion( CompositeAssertion parent ) {
        super( parent );
        initDefaultXpath();
    }

    public RequestXpathAssertion( XpathExpression xpath ) {
        super();
        setXpathExpression( xpath );
    }

    public RequestXpathAssertion( CompositeAssertion parent, XpathExpression xpath ) {
        super( parent );
        setXpathExpression( xpath );
    }

    private void initDefaultXpath() {
        Map nsmap = new HashMap();
        nsmap.put(NamespaceConstants.NSPREFIX_SOAP_ENVELOPE, SOAPConstants.URI_NS_SOAP_ENVELOPE);
        setXpathExpression(new XpathExpression(SoapUtil.SOAP_ENVELOPE_XPATH, nsmap));
    }

    /** Shortcut to get xpath pattern.  Name doesn't use get, to hide it from policy serializer. */
    public String pattern() {
        if (getXpathExpression() != null)
            return getXpathExpression().getExpression();
        return null;
    }

    /** Shortcut to get namespace map.  Name doesn't use get, to hide it from policy serializer. */
    public Map namespaceMap() {
        if (getXpathExpression() != null)
            return getXpathExpression().getNamespaces();
        return null;
    }

    public String toString() {
        XpathExpression x = getXpathExpression();
        StringBuffer sb = new StringBuffer(super.toString());
        if (x != null && x.getNamespaces() != null)
            sb.append(" namespacesmap=" + x.getNamespaces());
        if (x != null && x.getExpression() != null)
            sb.append(" pattern=" + x.getExpression());
        return sb.toString();
    }
}
