/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.token.SignedElement;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.server.policy.assertion.xmlsec.SamlAuthenticationStatementValidate;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlStatementAssertion;
import org.apache.xmlbeans.XmlException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import x0Assertion.oasisNamesTcSAML1.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Validates the SAML Assertion within the Document. The Document must represent a well formed
 * SOAP message.
 *
 * @author emil
 * @version Jan 25, 2005
 */
public abstract class SamlStatementValidate {

    /**
     * Statement Assertion to xbean mapping
     */
    private static Map statementMapping = new HashMap();

    static {
        statementMapping.put(SamlAuthenticationStatement.class, AuthenticationStatementType.class);
        statementMapping.put(SamlAuthorizationStatement.class, AuthorizationDecisionStatementType.class);
        statementMapping.put(SamlAttributeStatement.class, AttributeStatementType.class);
    }

    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected Collection errorCollector = new ArrayList();
    protected final SamlStatementAssertion statementAssertionConstraints;
    protected final Class statementMapingType;

    private ApplicationContext applicationContext;

    /**
     * Create the <code>SamlStatementValidate</code> for the given statement assertion
     *
     * @param sa                 the saml statement assertion that
     * @param applicationContext the application context for accessing components
     * @return the specific <code>SamlStatementValidate</code> for the <code>SamlStatementAssertion</code>
     */
    public static SamlStatementValidate getValidate(SamlStatementAssertion sa, ApplicationContext applicationContext) {
        if (sa == null) {
            throw new IllegalArgumentException("Non Null Saml Statement Assertion is required");
        }
        if (sa instanceof SamlAuthenticationStatement) {
            return new SamlAuthenticationStatementValidate(sa, applicationContext);
        } else if (sa instanceof SamlAuthorizationStatement) {
            return new SamlAuthenticationStatementValidate(sa, applicationContext);

        } else if (sa instanceof SamlAttributeStatement) {

        }
        throw new IllegalArgumentException("Not supported statement thpe " + sa.getClass());
    }

    /**
     * Construct  the <code>SamlStatementValidate</code> for the statement assertion
     *
     * @param statementAssertion the saml statemenet assertion
     * @param applicationContext the applicaiton context to allo access to components and services
     */
    SamlStatementValidate(SamlStatementAssertion statementAssertion, ApplicationContext applicationContext) {
        this.statementAssertionConstraints = statementAssertion;
        statementMapingType = (Class)statementMapping.get(statementAssertion.getClass());
        if (statementMapingType == null) {
            throw new IllegalArgumentException("Could not determine mapping for " + statementAssertion.getClass());
        }
        this.applicationContext = applicationContext;
    }


    /**
     * Validate the Saml assertion document
     *
     * @param document
     */
    public void validate(Document document, ProcessorResult wssResults, Collection validationResults) {
        validateCommonAssertionProperties(document, wssResults, validationResults);


    }

    /**
     * Validates the Saml Assertion common properties
     */
    protected void validateCommonAssertionProperties(Document document, ProcessorResult wssResults, Collection validationResults) {
        String securityNS = wssResults.getSecurityNS();
        if (null == securityNS) {
            validationResults.add(new Error("No Security Header found", document, null, null));
        }
        boolean assertionFound = false;
        boolean proofOfPosession = false;
        SignedElement[] signedElements = wssResults.getElementsThatWereSigned();
        for (int i = 0; i < signedElements.length; i++) {
            SignedElement signedElement = signedElements[i];
            Element element = signedElement.asElement();

            if ("Assertion".equals(element.getNodeName())) {
                assertionFound = true;
                try {
                    AssertionType assertionType = AssertionType.Factory.parse(element);
                    SubjectStatementAbstractType[] statementArray = assertionType.getSubjectStatementArray();
                    for (int j = 0; j < statementArray.length; j++) {
                        SubjectStatementAbstractType subjectStatementAbstractType = statementArray[j];
                        if (subjectStatementAbstractType.getClass().equals(statementMapingType)) { // bingo
                            validateSubjectConfirmation(subjectStatementAbstractType, validationResults);
                            validateConditions(assertionType, validationResults);
                            validateStatement(document, subjectStatementAbstractType, wssResults, validationResults);
                        }
                    }
                } catch (XmlException e) {
                    if (!assertionFound) {
                        validationResults.add(new Error("Unable to parse SAML assertion", document, null, null));
                    }
                }
            } else { // there must be something signed in the message if the proof of posession is requied
                if (!statementAssertionConstraints.isRequireProofOfPosession()) {
                    continue;
                }
                try {
                    Element bodyElement = SoapUtil.getBodyElement(document);
                    if (XmlUtil.isElementAncestor(element, bodyElement)) {
                        proofOfPosession = true;
                    }
                } catch (InvalidDocumentFormatException e) {
                    validationResults.add(new Error("Non SOAP document", document, null, null));
                }
            }
        }
        if (!assertionFound) {
            validationResults.add(new Error("No SAML assertion found in security Header", document, null, null));
        }
    }

    /**
     * Validate the SAML assertion conditions
     *
     * @param assertionType
     * @param validationResults
     */
    private void validateConditions(AssertionType assertionType, Collection validationResults) {
        ConditionsType conditionsType = assertionType.getConditions();
        conditionsType.getNotBefore();

    }

    private void validateSubjectConfirmation(SubjectStatementAbstractType subjectStatementAbstractType, Collection validationResults) {
        SubjectType subject = subjectStatementAbstractType.getSubject();
        if (subject == null) {
            validationResults.add(new Error("Subject Statement Required", subjectStatementAbstractType.toString(), null, null));
        }
        final String nameQualifier = statementAssertionConstraints.getNameQualifier();
        if (nameQualifier != null) {
            NameIdentifierType nameIdentifierType = subject.getNameIdentifier();
            nameIdentifierType.getNameQualifier();
        }
    }

    /**
     * Validate the specific <code>SubjectStatementAbstractType</code> and collect eventual validation
     * errors in the validationResults collection.
     *
     * @param document              the message document
     * @param statementAbstractType the subject statement type, that may be authentication statement
     *                              authorization statement or attribute statement
     * @param wssResults            the wssresults collection
     * @param validationResults     where the valida
     */
    protected abstract void validateStatement(Document document,
                                              SubjectStatementAbstractType statementAbstractType,
                                              ProcessorResult wssResults, Collection validationResults);

    public static class Error {
        private final String reason;
        private final Object context;
        private final Object[] values;
        private final Exception exception;

        protected Error(String reason, Object context, Object[] values, Exception exception) {
            this.reason = reason;
            if (reason == null) {
                throw new IllegalArgumentException("Reason is required");
            }
            this.context = context;
            this.values = values;
            this.exception = exception;
        }

        public Object getContext() {
            return context;
        }

        public String getReason() {
            return reason;
        }

        public Object[] getValues() {
            return values;
        }

        public Exception getException() {
            return exception;
        }
    }

}