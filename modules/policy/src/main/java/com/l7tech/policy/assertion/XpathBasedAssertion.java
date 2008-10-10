/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.SoapConstants;
import com.l7tech.policy.assertion.annotation.RequiresXML;

import javax.xml.soap.SOAPConstants;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for XML security assertions whose primary configurable feature is an Xpath expression.
 */
@RequiresXML()
public abstract class XpathBasedAssertion extends Assertion implements AssertionServiceChangeListener {
    protected XpathExpression xpathExpression;
    protected SoapVersion soapVersion = SoapVersion.SOAP_1_1;

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
        nsmap.put( SoapConstants.SOAP_ENV_PREFIX, SOAPConstants.URI_NS_SOAP_ENVELOPE);
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

    public SoapVersion getSoapVersion() {
        return soapVersion;
    }

    public void updateSoapVersion(SoapVersion soapVersion) {
        this.soapVersion = soapVersion;
        
        if (getXpathExpression() == null) return;
        String expression = xpathExpression.getExpression();
        if(SoapUtil.SOAP_ENVELOPE_XPATH.equals(expression) && soapVersion == SoapVersion.SOAP_1_2) {
            xpathExpression.setExpression(expression.replaceAll("soapenv:", "s12:"));
            xpathExpression.getNamespaces().put("s12", SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE);
        } else if(SoapUtil.SOAP_1_2_ENVELOPE_XPATH.equals(expression) && soapVersion == SoapVersion.SOAP_1_1) {
            xpathExpression.setExpression(expression.replaceAll("s12:", "soapenv:"));
            xpathExpression.getNamespaces().remove("s12");
        }
    }
}
