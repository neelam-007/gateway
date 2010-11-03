/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.validator;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import org.jaxen.NamespaceContext;
import org.jaxen.UnresolvableException;
import org.jaxen.VariableContext;
import org.jaxen.XPathFunctionContext;
import org.jaxen.dom.DOMXPath;

import java.util.Map;
import java.util.logging.Logger;

/**
 * @author mike
 * @version 1.0
 */
public class XpathBasedAssertionValidator extends NamespaceMigratableAssertionValidator {
    private static final Logger logger = Logger.getLogger(XpathBasedAssertionValidator.class.getName());

    private final XpathBasedAssertion assertion;
    private String errString;
    private Throwable errThrowable;

    public XpathBasedAssertionValidator( final XpathBasedAssertion xpathBasedAssertion ) {
        super(xpathBasedAssertion);
        assertion = xpathBasedAssertion;
        String pattern = null;
        if (assertion.getXpathExpression() != null)
            pattern = assertion.getXpathExpression().getExpression();

        if (pattern == null) {
            errString = "XPath pattern is missing";
            logger.info(errString);
        } else {
            try {
                final Map namespaces = xpathBasedAssertion.namespaceMap();
                DOMXPath xpath = new DOMXPath(pattern);

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
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        super.validate(path, pvc, result);
        if (errString != null)
            result.addError(new PolicyValidatorResult.Error(assertion, path, errString, errThrowable));
    }
}
