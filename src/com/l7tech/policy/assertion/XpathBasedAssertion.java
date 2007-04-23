/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.common.xml.xpath.XpathExpression;
import com.l7tech.policy.assertion.annotation.RequiresXML;

import javax.xml.rpc.NamespaceConstants;
import javax.xml.soap.SOAPConstants;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for XML security assertions whose primary configurable feature is an Xpath expression.
 */
@RequiresXML()
public abstract class XpathBasedAssertion extends Assertion {
    protected XpathExpression xpathExpression;

    protected XpathBasedAssertion() {
    }

    public XpathExpression getXpathExpression() {
        return xpathExpression;
    }

    public void setXpathExpression(XpathExpression xpathExpression) {
        this.xpathExpression = xpathExpression;
    }

    /** @return a new basic namespace map containing only xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/". */
    public static Map createDefaultNamespaceMap() {
        Map nsmap = new HashMap();
        nsmap.put(NamespaceConstants.NSPREFIX_SOAP_ENVELOPE, SOAPConstants.URI_NS_SOAP_ENVELOPE);
        return nsmap;
    }

    public String toString() {
        XpathExpression x = getXpathExpression();
        StringBuffer sb = new StringBuffer(super.toString());
        if (x != null && x.getExpression() != null)
            sb.append(" pattern=" + x.getExpression());
        if (x != null && x.getNamespaces() != null)
            sb.append(" namespacesmap=" + x.getNamespaces());
        return sb.toString();
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
}
