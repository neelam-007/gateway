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
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlStatementAssertion;
import org.apache.xmlbeans.XmlException;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import x0Assertion.oasisNamesTcSAML1.*;

import java.util.*;
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
    protected static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

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
     * @param applicationContext the application context to allow access to components and services
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
     * Validates the SAML statement.
     *
     * @param document the document/message to validate
     * @param wssResults  the wssresults
     * @param validationResults
     */
    public void validate(Document document, ProcessorResult wssResults, Collection validationResults) {
        String securityNS = wssResults.getSecurityNS();
        if (null == securityNS) {  // assume no security header was found
            validationResults.add(new Error("No Security Header found", document, null, null));
            return;
        }
        boolean assertionFound = false;
        boolean statementFound = false;
        SignedElement signedAssertionElement = null;
        SignedElement proofOfPossessionElement = null;

        SignedElement[] signedElements = wssResults.getElementsThatWereSigned();
        for (int i = 0; i < signedElements.length; i++) {
            SignedElement signedElement = signedElements[i];
            Element element = signedElement.asElement();

            if ("Assertion".equals(element.getNodeName()) && !statementFound) {
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
                            statementFound = true;
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
                        proofOfPossessionElement = signedElement;
                    }
                } catch (InvalidDocumentFormatException e) {
                    validationResults.add(new Error("Non SOAP document", document, null, null));
                }
            }
        }

        if (!assertionFound) {
            validationResults.add(new Error("No SAML assertion found in security Header", document, null, null));
            return;
        }

        if (signedAssertionElement == null) {
            validationResults.add(new Error("Unsigned SAML assertion found in security Header", document, null, null));
            return;
        }

        if (statementAssertionConstraints.isRequireProofOfPosession()) {
            if (proofOfPossessionElement == null) {
                validationResults.add(new Error("No Proof Of Possesion found", document, null, null));
                return;
            }
            if (proofOfPossessionElement.getSigningSecurityToken() != signedAssertionElement.getSigningSecurityToken()) {
                validationResults.add(new Error("Proof Of Possesion Security token and Assertion token do not match",
                                                document, new Object[] {proofOfPossessionElement.getSigningSecurityToken(),
                                                                        signedAssertionElement.getSigningSecurityToken()}, null));
            }
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
        if (!statementAssertionConstraints.isCheckAssertionValidity()) {
            logger.finer("No Assertion Validity requested");
        }

        Calendar notBefore = conditionsType.getNotBefore();
        Calendar notOnOrAfter = conditionsType.getNotOnOrAfter();
        Calendar now = Calendar.getInstance(UTC_TIME_ZONE);
        now.clear(Calendar.MILLISECOND); //clear millis xsd:dateTime does not have it
        if (!now.before(notBefore)) {
            logger.finer("Condition 'Not Before' check failed, now :"+now.toString()+" Not Before:"+notBefore.toString());
            validationResults.add(new Error("Condition 'Not Before' check failed",
                                            conditionsType.toString(), new Object[] {notBefore.toString(), now.toString()}, null));
        }

        if (now.equals(notOnOrAfter) || !now.after(notOnOrAfter)) {
            logger.finer("Condition 'Not On Or After' check failed, now :"+now.toString()+" Not Before:"+notOnOrAfter.toString());
            validationResults.add(new Error("Condition 'Not On Or After' check failed",
                                            conditionsType.toString(), new Object[] {notOnOrAfter.toString(), now.toString()}, null));
        }
        final String audienceRestriction = statementAssertionConstraints.getAudienceRestriction();
        if (audienceRestriction == null) {
            logger.finer("No audience restriction requested");
        }
        AudienceRestrictionConditionType[] audienceRestrictionArray = conditionsType.getAudienceRestrictionConditionArray();
        boolean audienceRestrictionMatch = false;
        for (int i = 0; i < audienceRestrictionArray.length; i++) {
            AudienceRestrictionConditionType audienceRestrictionConditionType = audienceRestrictionArray[i];
            String[] audienceArray = audienceRestrictionConditionType.getAudienceArray();
            for (int j = 0; j < audienceArray.length; j++) {
                String s = audienceArray[j];
                if (audienceRestriction.equals(s)) {
                    audienceRestrictionMatch = true;
                    break;
                }
            }
        }
        if (!audienceRestrictionMatch) {
            validationResults.add(new Error("Audience Restriction Check Failed",
                                               conditionsType.toString(), new Object[] {audienceRestriction}, null));
        }
    }

    private void validateSubjectConfirmation(SubjectStatementAbstractType subjectStatementAbstractType, Collection validationResults) {
        SubjectType subject = subjectStatementAbstractType.getSubject();
        if (subject == null) {
            validationResults.add(new Error("Subject Statement Required", subjectStatementAbstractType.toString(), null, null));
        }
        final String nameQualifier = statementAssertionConstraints.getNameQualifier();
        NameIdentifierType nameIdentifierType = subject.getNameIdentifier();
        if (nameQualifier != null) {
            if (nameIdentifierType !=null) {
                String presentedNameQualifier = nameIdentifierType.getNameQualifier();
                if (!nameQualifier.equals(presentedNameQualifier)) {
                    validationResults.add(new Error("Name Qualifiers does not match presented/required",
                                                     subjectStatementAbstractType.toString(),
                                                     new Object[] {presentedNameQualifier, nameQualifier}, null));
                    return;
                } else {
                    logger.fine("Matched Name Qualifier "+nameQualifier);
                }
            }
        }
        String[] nameFormats = statementAssertionConstraints.getNameFormats();
        String presentedNameFormat = nameIdentifierType.getFormat();
        boolean nameFormatMatch = false;
        for (int i = 0; i < nameFormats.length; i++) {
            String nameFormat = nameFormats[i];
            if (nameFormat.equals(presentedNameFormat)) {
                nameFormatMatch = true;
                logger.fine("Matched Name Format "+nameFormat);
                break;
            }
        }
        if (!nameFormatMatch) {
            validationResults.add(new Error("Name Format does not match presented/required",
                                              subjectStatementAbstractType.toString(),
                                              new Object[] {presentedNameFormat, Arrays.asList(nameFormats)}, null));
            return;
        }
        String[] confirmations = statementAssertionConstraints.getSubjectConfirmations();
        List presentedConfirmations = Arrays.asList(subject.getSubjectConfirmation().getConfirmationMethodArray());

        boolean confirmationMatch = false;
        for (int i = 0; i < confirmations.length; i++) {
            String confirmation = confirmations[i];
            if (presentedConfirmations.contains(confirmation)) {
                confirmationMatch = true;
                logger.fine("Matched Subject Confirmation "+confirmation);
                break;
            }
        }

        if (!confirmationMatch) {
             validationResults.add(new Error("Subject Confirmations mismatch presented/required",
                                               subjectStatementAbstractType.toString(),
                                               new Object[] {presentedConfirmations, Arrays.asList(confirmations)}, null));
            return;
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

        public String toString() {
            final String msg = exception == null ? "<null>" : exception.getMessage();
            return "SAML validation error: " + reason + ": " + msg;
        }
    }

}