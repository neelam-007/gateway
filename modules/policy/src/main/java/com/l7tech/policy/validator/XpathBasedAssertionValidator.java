/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.validator;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathUtil;
import org.jaxen.NamespaceContext;
import org.jaxen.UnresolvableException;
import org.jaxen.VariableContext;
import org.jaxen.XPathFunctionContext;
import org.jaxen.dom.DOMXPath;

import java.text.ParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author mike
 * @version 1.0
 */
public class XpathBasedAssertionValidator implements AssertionValidator {
    private static final Logger logger = Logger.getLogger(XpathBasedAssertionValidator.class.getName());

    private static final String SOAP_NS_REMEDIAL_ACTION_CLASSNAME = "com.l7tech.console.action.XpathBasedAssertionSoapVersionMigrator";

    private final XpathBasedAssertion assertion;
    private final Set<String> namespacePrefixesUsedByExpression;
    private String errString;
    private Throwable errThrowable;

    public XpathBasedAssertionValidator( final XpathBasedAssertion xpathBasedAssertion ) {
        assertion = xpathBasedAssertion;
        String pattern = null;
        if (assertion.getXpathExpression() != null)
            pattern = assertion.getXpathExpression().getExpression();

        Set<String> usedPrefixes = null;

        if (pattern == null) {
            errString = "XPath pattern is missing";
            logger.info(errString);
        } else {
            try {
                final Map namespaces = xpathBasedAssertion.namespaceMap();
                DOMXPath xpath = new DOMXPath(pattern);

                usedPrefixes = XpathUtil.getNamespacePrefixesUsedByXpath(pattern, true);

                xpath.setFunctionContext(new XPathFunctionContext(false));
                xpath.setNamespaceContext(new NamespaceContext(){
                    @Override
                    public String translateNamespacePrefixToUri(String prefix) {
                        if (namespaces == null)
                            return null;
                        else
                            return (String) namespaces.get(prefix);
                    }
                });
                xpath.setVariableContext( new VariableContext(){
                    @Override
                    public Object getVariableValue( String ns, String prefix, String localName ) throws UnresolvableException {
                        return ""; // this will always succeed, variable usage already has a validator
                    }
                } );
                xpath.evaluate( XmlUtil.stringToDocument("<blah xmlns=\"http://bzzt.com\"/>"));
            } catch (Exception e) {
                errString = "XPath pattern is not valid";
                errThrowable = e;
            }
        }
        namespacePrefixesUsedByExpression = usedPrefixes;
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        if (errString != null)
            result.addError(new PolicyValidatorResult.Error(assertion, path, errString, errThrowable));

        SoapVersion soapVersion = pvc.getSoapVersion();
        if (soapVersion != null && soapVersion.getNamespaceUri() != null) {
            SoapVersion unwantedSoapVersion = checkForUnwantedSoapVersion(assertion, soapVersion, namespacePrefixesUsedByExpression);
            if (unwantedSoapVersion != null) {
                result.addWarning(new PolicyValidatorResult.Warning(assertion, path,
                        String.format("This assertion contains an XPath that uses the %s envelope namespace URI, but the service is configured as using only %s.  The XPath will always fail.",
                                unwantedSoapVersion.getLabel(), soapVersion.getLabel()),
                        null, SOAP_NS_REMEDIAL_ACTION_CLASSNAME));
            }
        }

    }

    /**
     * Check whether the specified XpathBasedAssertion appears to use the namespace URI for a SOAP version other
     * than the specified SOAP version.
     *
     * @param assertion the assertion to examine.  Required.
     * @param expectedSoapVersion the expected SOAP version.  Required.
     * @return null if this assertion does not appear to use any unexpected SOAP namespace URIs; otherwise, the first unexpected SOAP namespace URI used.
     */
    public static SoapVersion checkForUnwantedSoapVersion(XpathBasedAssertion assertion, SoapVersion expectedSoapVersion) {
        return checkForUnwantedSoapVersion(assertion, expectedSoapVersion, null);
    }

    static SoapVersion checkForUnwantedSoapVersion(XpathBasedAssertion assertion, SoapVersion expectedSoapVersion, Set<String> usedPrefixes) {
        SoapVersion unwantedSoapVersion = null;
        XpathExpression xpath = assertion.getXpathExpression();
        if (xpath != null && xpath.getExpression() != null) {
            if (usedPrefixes == null) {
                try {
                    usedPrefixes = XpathUtil.getNamespacePrefixesUsedByXpath(xpath.getExpression(), true);
                } catch (ParseException e) {
                    usedPrefixes = Collections.emptySet();
                }
            }

            Map<String, String> nsmap = xpath.getNamespaces();
            nsmap.keySet().retainAll(usedPrefixes);
            Set<String> valueSet = new HashSet<String>(nsmap.values());

            // Look for all SOAP namespace URIs other than the one the service is configured to use
            Set<String> unwantedNamespaces = expectedSoapVersion.getOtherNamespaceUris();
            valueSet.retainAll(unwantedNamespaces);
            if (!valueSet.isEmpty()) {
                unwantedSoapVersion = SoapVersion.namespaceToSoapVersion(valueSet.iterator().next());
            }
        }
        return unwantedSoapVersion;
    }
}
