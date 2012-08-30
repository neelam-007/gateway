/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.assertion.xmlsec.RequireSaml;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.util.ContextVariableUtils;
import com.l7tech.util.*;
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
    protected final RequireSaml requestSaml;

    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestSaml the saml assertion that specifies constraints
     */
    public SamlStatementValidate(RequireSaml requestSaml) {
        this.requestSaml = requestSaml;
    }


    /**
     * Validate the specific <code>SubjectStatementAbstractType</code> and collect eventual validation
     * errors in the validationResults collection.
     *
     * @param statementObject   the subject statement type, that may be authentication statement
     *                          authorization statement or attribute statement
     * @param validationResults where the valida
     * @param collectAttrValues
     * @param serverVariables variables used by assertion
     * @param auditor required to process server variables.
     */
    protected abstract void validate(XmlObject statementObject,
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
            final List<String> customAuthMethodsResolved = ContextVariableUtils.getAllResolvedStrings(customAuthMethods,
                    serverVariables,
                    auditor,
                    TextUtils.URI_STRING_SPLIT_PATTERN, new Functions.UnaryVoid<Object>() {
                @Override
                public void call(Object unexpectedNonString) {
                    //todo get an auditor and audit warning for this configuration error.
                    logger.log(Level.WARNING, "Found non string value for custom authentication method: " + unexpectedNonString);
                }
            });
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
                //todo [Donal] Audit as this is audited else where it occurs
                logger.warning("Ignored invalid custom URI found: " + customAuthMethod);
            }
        }
        return false;
    }
}
