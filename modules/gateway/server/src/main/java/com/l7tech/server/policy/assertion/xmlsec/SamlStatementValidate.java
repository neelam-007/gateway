/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Pair;
import com.l7tech.util.ValidationUtils;
import org.w3c.dom.Document;
import org.apache.xmlbeans.XmlObject;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validates the SAML Assertion within the Document. The Document must represent a well formed
 * SOAP message.
 *
 * @author emil
 * @version Jan 25, 2005
 */
public abstract class SamlStatementValidate {
    protected static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected Collection errorCollector = new ArrayList();
    protected final RequireWssSaml requestWssSaml;

    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestWssSaml the saml assertion that specifies constraints
     */
    public SamlStatementValidate(RequireWssSaml requestWssSaml) {
        this.requestWssSaml = requestWssSaml;
    }


    /**
     * Validate the specific <code>SubjectStatementAbstractType</code> and collect eventual validation
     * errors in the validationResults collection.
     *
     * @param document          the message document
     * @param statementObject   the subject statement type, that may be authentication statement
     *                          authorization statement or attribute statement
     * @param wssResults        the wssresults collection
     * @param validationResults where the valida
     * @param collectAttrValues
     * @param serverVariables variables used by assertion
     * @param auditor required to process server variables.
     */
    protected abstract void validate(Document document,
                                     XmlObject statementObject,
                                     ProcessorResult wssResults,
                                     Collection<SamlAssertionValidate.Error> validationResults,
                                     Collection<Pair<String, String[]>> collectAttrValues,
                                     Map<String, Object> serverVariables,
                                     Audit auditor);

    /**
     * Validate the runtime authentication method from a SAML assertion's authentication statement.
     *
     * @param authenticationMethod The authentication method to validate is allowed based on configuration.
     * @param staticMethods List of available methods from our internally supported set.
     * @param customAuthMethods The list of custom authentication methods which are policy defined. Supports multiple
     * values (separated by space or comma) including multiple values in single and multi valued context variables.
     * @param validationResults Collection of validation results to add to when authentication method does not match configuration.
     * @param serverVariables Map of available variables.
     * @param auditor Auditor to audit to.
     */
    protected void validateAuthenticationMethods(final String authenticationMethod,
                                                 final List<String> staticMethods,
                                                 final String customAuthMethods,
                                                 final Collection<SamlAssertionValidate.Error> validationResults,
                                                 final Map<String, Object> serverVariables,
                                                 final Audit auditor) {
        boolean methodMatches = false;
        for (String method : staticMethods) {
            if (authenticationMethod.equals(method)) {
                methodMatches = true;
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Matched authentication method " + method);
                }
                break;
            }
        }

        List<String> allCustomMethods = new ArrayList<String>();
        final boolean hasCustom = customAuthMethods != null && !customAuthMethods.trim().isEmpty();
        if (!methodMatches && hasCustom) {
            final List<String> customAuthMethodsResolved = getAllCustomMethods(customAuthMethods, serverVariables, auditor);
            allCustomMethods.addAll(customAuthMethodsResolved);
            methodMatches = matchesCustomValue(authenticationMethod, customAuthMethodsResolved);
        }

        if (!methodMatches) {
            final String msg = "Authentication method not matched. Received value: {0} Expected one of: {1}"+
                    (hasCustom ? " or Custom: {2}": "");
            validationResults.add(
                    new SamlAssertionValidate.Error(msg, null, authenticationMethod,
                            staticMethods.size() == 1 ? staticMethods.get(0) : staticMethods.toString(),
                            hasCustom ? allCustomMethods.toString() : ""));
        }

    }

    /**
     * Get all String values referenced from a custom authentication method value.
     * @param customMethodExpression The expression to extract strings from. This may contain space or comma seperated
     * strings, context variables, which themselves may be multi valued or contain spare / comma separated values to split.
     * @param serverVariables Map of available variables.
     * @param auditor The auditor to audit to.
     * @return The list of Strings extracted from the customMethodExpression.
     */
    protected List<String> getAllCustomMethods(String customMethodExpression, Map<String, Object> serverVariables, Audit auditor) {
        final List<String> returnList = new ArrayList<String>();

        if (customMethodExpression != null && !customMethodExpression.trim().isEmpty()) {
            final String[] tokens = RequireWssSaml.CUSTOM_AUTH_SPLITTER.split(customMethodExpression);

            for (String token : tokens) {
                if (token.trim().isEmpty()) {
                    continue;
                }
                final List<Object> varValueList = ExpandVariables.processNoFormat(token, serverVariables, auditor, false);
                final List<String> customMethods = getStringsFromList(varValueList);
                returnList.addAll(customMethods);
            }
        }

        return returnList;
    }

    protected boolean matchesCustomValue(String valueToCheck, List<String> customAuthMethods) {
        if (valueToCheck == null || valueToCheck.trim().isEmpty()) {
            return false;
        }

        for (String customAuthMethod : customAuthMethods) {
            if (ValidationUtils.isValidUri(customAuthMethod)) {
                if (valueToCheck.equals(customAuthMethod)) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer("Matched custom authentication method " + customAuthMethod);
                    }

                    return true;
                }
            } else {
                logger.warning("Ignored invalid custom URI found: " + customAuthMethod);
            }
        }
        return false;
    }


    private static List<String> getStringsFromList(List<Object> objectList) {
        final List<String> returnList = new ArrayList<String>();
        for (Object val : objectList) {
            if (val instanceof String) {
                String customVal = (String) val;
                if (!customVal.trim().isEmpty()) {
                    final String[] authMethods = RequireWssSaml.CUSTOM_AUTH_SPLITTER.split(customVal);
                    for (String authMethod : authMethods) {
                        if (!authMethod.trim().isEmpty()) {
                            returnList.add(authMethod);
                        }
                    }
                }
            }
        }
        return returnList;
    }

}
