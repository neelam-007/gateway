/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.xml.processor.SecurityContext;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import x0Assertion.oasisNamesTcSAML1.*;

import java.util.*;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;

/**
 * Validates the SAML Assertion within the Document. The Document must represent a well formed
 * SOAP message.
 *
 * @author emil
 * @version Jan 25, 2005
 */
public class SamlAssertionValidate {
    protected static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected Collection errorCollector = new ArrayList();
    protected final RequestWssSaml requestWssSaml;
    private ApplicationContext applicationContext;
    private Map validators = new HashMap();

    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestWssSaml the saml assertion that specifies constraints
     * @param applicationContext the application context to allow access to components and services
     */
    public SamlAssertionValidate(RequestWssSaml requestWssSaml, ApplicationContext applicationContext) {
        this.requestWssSaml = requestWssSaml;
        this.applicationContext = applicationContext;
        if (requestWssSaml.getAuthenticationStatement() !=null) {
            validators.put(AuthenticationStatementType.class, new SamlAuthenticationStatementValidate(requestWssSaml, applicationContext));
        }
        if (requestWssSaml.getAuthorizationStatement() !=null) {
            validators.put(AuthorizationDecisionStatementType.class, new SamlAuthorizationDecisionStatementValidate(requestWssSaml, applicationContext));
        }
        if (requestWssSaml.getAttributeStatement() !=null) {
            validators.put(AttributeStatementType.class, new SamlAttributeStatementValidate(requestWssSaml, applicationContext));
        }
    }

    /**
     * Validates the SAML statement.
     *
     * @param soapMessageDoc the soapMessageDoc/message to validate
     * @param wssResults  the wssresults
     * @param validationResults
     */
    public void validate(Document soapMessageDoc, ProcessorResult wssResults, Collection validationResults) {
        String securityNS = wssResults.getSecurityNS();
        if (null == securityNS) {  // assume no security header was found
            validationResults.add(new Error("No Security Header found", soapMessageDoc, null, null));
            return;
        }

        SamlAssertion assertion = null;
        SecurityToken[] tokens = wssResults.getSecurityTokens();
        for (int i = 0; i < tokens.length; i++) {
            SecurityToken token = tokens[i];
            if (token.getType() == SecurityTokenType.SAML_ASSERTION) {
                assertion = (SamlAssertion)token;
                AssertionType assertionType = assertion.getXmlBeansAssertionType();
                StatementAbstractType[] statementArray = assertionType.getStatementArray();

                for (int j = 0; j < statementArray.length; j++) {
                    StatementAbstractType statementAbstractType = statementArray[j];
                    SamlStatementValidate statementValidate = (SamlStatementValidate)validators.get(statementAbstractType.getClass());

                    if (statementValidate !=null) { // bingo
                        validateSubjectConfirmation((SubjectStatementAbstractType)statementAbstractType, validationResults);
                        validateConditions(assertionType, validationResults);
                        statementValidate.validate(soapMessageDoc, (SubjectStatementAbstractType)statementAbstractType, wssResults, validationResults);
                    }
                }
            }
        }

        if (assertion == null) {
            validationResults.add(new Error("No SAML assertion found in security Header", soapMessageDoc, null, null));
            return;
        } else {

            SignedElement[] signedElements = wssResults.getElementsThatWereSigned();
            X509Certificate signingCert = null;
            for (int i = 0; i < signedElements.length; i++) {
                SignedElement signedElement = signedElements[i];
                if (signedElement == assertion.asElement()) {
                    if (signingCert != null) {
                        validationResults.add(new Error("SAML assertion was signed by more than one security token", soapMessageDoc, null, null));
                        return;
                    }

                    // This assertion is included in the message signature.
                    SigningSecurityToken signingToken = (SigningSecurityToken)signedElement.getSigningSecurityToken();
                    if (signingToken instanceof X509SigningSecurityToken) {
                        X509SigningSecurityToken x509SigningSecurityToken = (X509SigningSecurityToken)signingToken;
                        signingCert = x509SigningSecurityToken.getMessageSigningCertificate();
                        break;

                    } else {
                        validationResults.add(new Error("Unsupported signing security token type", soapMessageDoc, null, null));
                        return;
                    }
                }
            }

            if (signingCert == null && !assertion.hasEmbeddedIssuerSignature()) {
                validationResults.add(new Error("Unsigned SAML assertion found in security Header", soapMessageDoc, null, null));
                return;
            }

            // Use the signing cert somehow
            // TODO use it here

            if (requestWssSaml.isRequireProofOfPosession()) {
                if (!assertion.isPossessionProved()) {
                    validationResults.add(new Error("No Proof Of Possession found", soapMessageDoc, null, null));
                    return;
                }
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
        if (!requestWssSaml.isCheckAssertionValidity()) {
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
        final String audienceRestriction = requestWssSaml.getAudienceRestriction();
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
        final String nameQualifier = requestWssSaml.getNameQualifier();
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
        String[] nameFormats = requestWssSaml.getNameFormats();
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
        String[] confirmations = requestWssSaml.getSubjectConfirmations();
        List presentedConfirmations = Arrays.asList(subject.getSubjectConfirmation().getConfirmationMethodArray());

        // if no confirmations have been presented, and that is what corresponds to assertion requirements
        // no confirmation check is performed
        if (presentedConfirmations.isEmpty() && requestWssSaml.isNoSubjectConfirmation()) {
            logger.fine("Matched Subject Confirmation 'None'");
            return;
        }

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

    static class Error {
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