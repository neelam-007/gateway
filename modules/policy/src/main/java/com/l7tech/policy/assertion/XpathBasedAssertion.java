/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.annotation.RequiresXML;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;

import javax.xml.soap.SOAPConstants;
import java.util.List;
import java.util.Map;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * Base class for XML security assertions whose primary configurable feature is an Xpath expression.
 */
@RequiresXML()
public abstract class XpathBasedAssertion extends Assertion implements UsesVariables {
    protected XpathExpression xpathExpression;

    protected XpathBasedAssertion() {
    }

    public XpathExpression getXpathExpression() {
        return xpathExpression;
    }

    public void setXpathExpression(XpathExpression xpathExpression) {
        this.xpathExpression = xpathExpression;
    }
    
    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        if (xpathExpression == null)
            return new String[0];
        String expr = xpathExpression.getExpression();
        if (expr == null)
            return new String[0];
        List<String> varlist = XpathUtil.getUnprefixedVariablesUsedInXpath(expr);
        return varlist.toArray(new String[varlist.size()]);
    }

    @Override
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
    public Map<String, String> namespaceMap() {
        if (getXpathExpression() != null)
            return getXpathExpression().getNamespaces();
        return null;
    }

    /**
     * Create a default XpathExpression value for this assertion for either a non-SOAP or SOAP policy,
     * using the specified SOAP version (if for a SOAP policy).
     * <p/>
     * This method just uses "/" for non-SOAP policies, or "/s:Envelope/s:Body" for SOAP policies (with the "s:"
     * prefix defined as the SOAP 1.2 namespace URI if the SoapVersion is 1.2, and the SOAP 1.1 namespace otherwise).
     *
     * @param soapPolicy true if it is known that the value will be used only by a SOAP policy; otherwise false.
     * @param soapVersion the SOAP version to use if a SOAP-based expression is created.  May be UNSPECIFIED or null.
     * @return a new XpathExpression instance suitable for the specified situation.  Never null.
     */
    public XpathExpression createDefaultXpathExpression(boolean soapPolicy, SoapVersion soapVersion) {
        String nsuri = SoapVersion.SOAP_1_2.equals(soapVersion) ? SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE : SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE;
        return new XpathExpression("/s:Envelope/s:Body", "s", nsuri);
    }

    /**
     * @return the previous default xpath value for many XpathBasedAssertions, for deserialization purposes.
     *         This value should not be used for any other purpose; users of xpath based assertions should
     *         override it with a contextually correct value instead (at the very least ensuring the correct SOAP version is used).
     */
    public static XpathExpression compatOrigDefaultXpathValue() {
        return new XpathExpression("/soapenv:Envelope/soapenv:Body", "soapenv", SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);
    }
}
