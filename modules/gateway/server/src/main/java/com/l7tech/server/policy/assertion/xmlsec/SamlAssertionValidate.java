/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.security.token.*;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.w3c.dom.Document;
import org.apache.xmlbeans.XmlObject;
import x0Assertion.oasisNamesTcSAML1.*;

import java.util.*;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;
import java.io.StringWriter;
import java.io.IOException;
import java.text.MessageFormat;

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
    private Map validators = new HashMap();

    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestWssSaml     the saml assertion that specifies constraints
     */
    public SamlAssertionValidate(RequestWssSaml requestWssSaml) {
        this.requestWssSaml = requestWssSaml;
        if (requestWssSaml.getAuthenticationStatement() != null) {
            validators.put(AuthenticationStatementType.class, new SamlAuthenticationStatementValidate(requestWssSaml));
            validators.put(x0Assertion.oasisNamesTcSAML2.AuthnStatementType.class, new Saml2AuthenticationStatementValidate(requestWssSaml));
        }
        if (requestWssSaml.getAuthorizationStatement() != null) {
            validators.put(AuthorizationDecisionStatementType.class, new SamlAuthorizationDecisionStatementValidate(requestWssSaml));
            validators.put(x0Assertion.oasisNamesTcSAML2.AuthzDecisionStatementType.class, new Saml2AuthorizationDecisionStatementValidate(requestWssSaml));
        }
        if (requestWssSaml.getAttributeStatement() != null) {
            validators.put(AttributeStatementType.class, new SamlAttributeStatementValidate(requestWssSaml));
            validators.put(x0Assertion.oasisNamesTcSAML2.AttributeStatementType.class, new Saml2AttributeStatementValidate(requestWssSaml));
        }
    }

    /**
     * Validates the SAML statement.
     *
     * @param soapMessageDoc    the soapMessageDoc/message to validate
     * @param credentials       the  credenaitls that may have been collected, null otherwise
     * @param wssResults        the wssresults
     * @param validationResults
     */
    public void validate(Document soapMessageDoc, LoginCredentials credentials, ProcessorResult wssResults, Collection validationResults) {
        String securityNS = wssResults.getSecurityNS();
        if (null == securityNS) {  // assume no security header was found
            Error result = new Error("No Security Header found", soapMessageDoc, null, null);
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }

        boolean acceptV1 = requestWssSaml.getVersion()==null || requestWssSaml.getVersion().intValue()!=2;
        boolean acceptV2 = requestWssSaml.getVersion()!=null && requestWssSaml.getVersion().intValue()!=1;
        Calendar now = Calendar.getInstance(UTC_TIME_ZONE);
        now.clear(Calendar.MILLISECOND); //clear millis xsd:dateTime does not have it

        try {
            SamlAssertion assertion = null;
            XmlSecurityToken[] tokens = wssResults.getXmlSecurityTokens();
            for (int i = 0; i < tokens.length; i++) {
                SecurityToken token = tokens[i];
                if ((token.getType() == SecurityTokenType.SAML_ASSERTION && acceptV1)||
                    (token.getType() == SecurityTokenType.SAML2_ASSERTION && acceptV2)) {
                    assertion = (SamlAssertion)token;
                    XmlObject xmlObject = assertion.getXmlBeansAssertionType();
                    boolean assertionMatch = false;

                    if (xmlObject instanceof AssertionType) {
                        AssertionType assertionType = (AssertionType) xmlObject;
                        Collection statementList = new ArrayList();
                        statementList.addAll(Arrays.asList(assertionType.getAuthenticationStatementArray()));
                        statementList.addAll(Arrays.asList(assertionType.getAuthorizationDecisionStatementArray()));
                        statementList.addAll(Arrays.asList(assertionType.getAttributeStatementArray()));

                        StatementAbstractType[] statementArray = (StatementAbstractType[])statementList.toArray(new StatementAbstractType[]{});

                        for (int j = 0; j < statementArray.length; j++) {
                            StatementAbstractType statementAbstractType = statementArray[j];
                            Set keys = validators.keySet();
                            for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
                                Class clazz = (Class)iterator.next();
                                if (clazz.isAssignableFrom(statementAbstractType.getClass())) {
                                    assertionMatch = true;
                                    SamlStatementValidate statementValidate = (SamlStatementValidate)validators.get(clazz);
                                    validateSubjectConfirmation((SubjectStatementAbstractType)statementAbstractType, validationResults);
                                    validateConditions(assertionType, validationResults);
                                    statementValidate.validate(soapMessageDoc, statementAbstractType, wssResults, validationResults);
                                }
                            }
                        }
                    }
                    else if (xmlObject instanceof x0Assertion.oasisNamesTcSAML2.AssertionType) {
                        x0Assertion.oasisNamesTcSAML2.AssertionType assertionType =
                                (x0Assertion.oasisNamesTcSAML2.AssertionType) xmlObject;
                        validateConditions(assertionType.getConditions(), now, validationResults);
                        validateSubjectConfirmation(assertionType.getSubject(), now, validationResults);

                        Collection statementList = new ArrayList();
                        statementList.addAll(Arrays.asList(assertionType.getAuthnStatementArray()));
                        statementList.addAll(Arrays.asList(assertionType.getAuthzDecisionStatementArray()));
                        statementList.addAll(Arrays.asList(assertionType.getAttributeStatementArray()));

                        XmlObject[] statementArray = (XmlObject[]) statementList.toArray(new XmlObject[]{});

                        for (int j = 0; j < statementArray.length; j++) {
                            XmlObject statementAbstractType = statementArray[j];
                            Set keys = validators.keySet();
                            for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
                                Class clazz = (Class)iterator.next();
                                if (clazz.isAssignableFrom(statementAbstractType.getClass())) {
                                    assertionMatch = true;
                                    SamlStatementValidate statementValidate = (SamlStatementValidate)validators.get(clazz);
                                    statementValidate.validate(soapMessageDoc, statementAbstractType, wssResults, validationResults);
                                }
                            }
                        }

                        // allow no statements if none are required
                        if (!assertionMatch && validators.isEmpty())
                            assertionMatch = true;
                    }

                    if (!assertionMatch) {
                        Error result = new Error("No SAML assertion has been presented that matches specified constraints", soapMessageDoc, null, null);
                        validationResults.add(result);
                        logger.finer(result.toString());
                        return;
                    }
                }
            }

            if (assertion == null) {
                Error result = new Error("No SAML assertion found in security Header", soapMessageDoc, null, null);
                validationResults.add(result);
                logger.finer(result.toString());
                return;
            }
            final SigningSecurityToken[] signingTokens = wssResults.getSigningTokens(assertion.asElement());
            if (signingTokens.length == 0) {
                Error result = new Error("Unsigned SAML assertion found in security Header", soapMessageDoc, null, null);
                validationResults.add(result);
                logger.finer(result.toString());
                return;
            } else if (signingTokens.length > 1) {
                Error result = new Error("SAML assertion was signed by more than one security token", soapMessageDoc, null, null);
                validationResults.add(result);
                logger.finer(result.toString());
                return;
            }

            if (assertion.isSenderVouches()) {
                if (assertion.getIssuerCertificate() == null) {
                    if (signingTokens[0] instanceof X509SigningSecurityToken) {
                        X509SigningSecurityToken signingSecurityToken = (X509SigningSecurityToken)signingTokens[0];
                        assertion.setIssuerCertificate(signingSecurityToken.getMessageSigningCertificate());
                    } else {
                        Error result = new Error("Assertion was signed by non-X509 SecurityToken " + signingTokens[0].getClass(), soapMessageDoc, null, null);
                        validationResults.add(result);
                        logger.finer(result.toString());
                        return;
                    }
                }
            }

            if (assertion.isHolderOfKey() && requestWssSaml.isRequireHolderOfKeyWithMessageSignature()) {
                X509Certificate subjectCertificate = assertion.getSubjectCertificate();
                if (subjectCertificate == null) {
                    Error result = new Error("Subject Certificate is required for Holder-Of-Key Assertion", soapMessageDoc, null, null);
                    validationResults.add(result);
                    logger.finer(result.toString());
                    return;
                }
                // validate that the assertion is the soap:Body signer
                if (!isBodySigned(assertion.getSignedElements())) {
                    Error result = new Error("Can't validate proof of posession; the SOAP Body has not been signed with the Subject Confirmation Certificate", soapMessageDoc, null, null);
                    validationResults.add(result);
                    logger.finer(result.toString());
                    return;
                }

            } else if (assertion.isSenderVouches() && requestWssSaml.isRequireSenderVouchesWithMessageSignature()) {
                // make sure ther soap:Body is signed. The actual trust verification is in FIP SamlAuthorizationHandler
                X509Certificate bodySigner = getBodySigner(wssResults);

                if (bodySigner == null) {
                    Error result = new Error("Can't validate proof of posession; the SOAP Body has not been signed", soapMessageDoc, null, null);
                    validationResults.add(result);
                    logger.finer(result.toString());
                    return;
                } else {
                    assertion.setAttestingEntity(bodySigner);
                }
            } else {
                X509Certificate sslCert = credentials == null ? null : credentials.getClientCert();
                if (assertion.isHolderOfKey()) {
                    X509Certificate subjectCertificate = assertion.getSubjectCertificate();
                    if (subjectCertificate == null) {
                        Error result = new Error("Subject Certificate is required for Holder-Of-Key Assertion", soapMessageDoc, null, null);
                        validationResults.add(result);
                        logger.finer(result.toString());
                        return;
                    }
                    if (!subjectCertificate.equals(sslCert)) {
                        Error result = new Error("SSL Certificate and Holder-Of-Key Subject Certificate mismatch", soapMessageDoc, null, null);
                        validationResults.add(result);
                        logger.finer(result.toString());
                        return;
                    }
                    assertion.setAttestingEntity(subjectCertificate); // for HOK the attesting entity is subject cert
                } else if (assertion.isSenderVouches()) {
                    if (sslCert == null) {
                         Error result = new Error("SSL Client Certificate is required for Sender-Vouches Assertion and unsigned message", soapMessageDoc, null, null);
                         validationResults.add(result);
                         logger.finer(result.toString());
                         return;
                     }
                     assertion.setAttestingEntity(sslCert); // for SV the attesting entity is SSL cert and the trust is verified in FIP
                }
            }
        } catch (InvalidDocumentFormatException e) {
            validationResults.add(new Error("Can't process non SOAP messages", soapMessageDoc, null, e));
        } catch (IOException e) {
            validationResults.add(new Error("Can't process non SOAP messages", soapMessageDoc, null, e));
        }
    }

    private boolean isBodySigned(SignedElement[] signedElements)
      throws InvalidDocumentFormatException {
        for (int i = 0; i < signedElements.length; i++) {
            SignedElement signedElement = signedElements[i];
            if (SoapUtil.isBody(signedElement.asElement())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the X509Certificate that signed the soap:Body or <code>null</code> if Body
     * has not been signed.
     *
     * @param wssResult
     * @throws InvalidDocumentFormatException
     */
    private X509Certificate getBodySigner(ProcessorResult wssResult)
      throws InvalidDocumentFormatException {
        SignedElement[] signedElements = wssResult.getElementsThatWereSigned();
        for (int i = 0; i < signedElements.length; i++) {
            SignedElement signedElement = signedElements[i];
            if ( SoapUtil.isBody(signedElement.asElement())) {
                SigningSecurityToken signingSecurityToken = signedElement.getSigningSecurityToken();
                if (!(signingSecurityToken instanceof X509SecurityToken)) {
                    throw new InvalidDocumentFormatException("Response body was signed, but not with an X509 Security Token");
                }
                return ((X509SecurityToken)signingSecurityToken).getCertificate();
            }
        }
        return null;
    }


    /**
     * Validate the SAML assertion conditions for v1.x
     *
     * @param assertionType
     * @param validationResults
     */
    private void validateConditions(AssertionType assertionType, Collection validationResults) throws IOException {
        ConditionsType conditionsType = assertionType.getConditions();
        if (!requestWssSaml.isCheckAssertionValidity()) {
            logger.finer("No Assertion Validity requested");
        } else {
            if (conditionsType == null) {
                logger.finer("Can't validate conditions, no Conditions have been presented");
                StringWriter sw = new StringWriter();
                assertionType.save(sw);
                validationResults.add(new Error("Can't validate conditions, no Conditions have been presented", sw.toString(), null, null));
                return;
            }
            Calendar notBefore = conditionsType.getNotBefore();
            Calendar notOnOrAfter = conditionsType.getNotOnOrAfter();
            if (notBefore == null || notOnOrAfter == null) {
                logger.finer("No Validity Period conditions have been presented, cannot validate Conditions");
                StringWriter sw = new StringWriter();
                assertionType.save(sw);
                validationResults.add(new Error("No Validity Period conditions have been presented, cannot validate Conditions", sw.toString(), null, null));
                return;
            }

            Calendar now = Calendar.getInstance(UTC_TIME_ZONE);
            now.clear(Calendar.MILLISECOND); //clear millis xsd:dateTime does not have it
            if (now.before(notBefore)) {
                logger.finer("Condition 'Not Before' check failed, now :" + now.toString() + " Not Before:" + notBefore.toString());
                validationResults.add(new Error("SAML ticket does not become valid until: {0}",
                                                conditionsType.toString(), new Object[]{notBefore.getTime().toString()}, null));
            }

            if (now.equals(notOnOrAfter) || now.after(notOnOrAfter)) {
                logger.finer("Condition 'Not On Or After' check failed, now :" + now.toString() + " Not Before:" + notOnOrAfter.toString());
                validationResults.add(new Error("SAML ticket has expired as of: {0}",
                                                conditionsType.toString(), new Object[]{notOnOrAfter.getTime().toString()}, null));
            }
        }

        final String audienceRestriction = requestWssSaml.getAudienceRestriction();
        if (audienceRestriction == null || "".equals(audienceRestriction)) {
            logger.finer("No audience restriction requested");
            return;
        }

        if (conditionsType == null) {
            StringWriter sw = new StringWriter();
            assertionType.save(sw);
            Error result = new Error("Can't validate conditions, no Conditions have been found", sw.toString(), null, null);
            validationResults.add(result);
            logger.finer(result.toString());
            return;
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
            Error result = new Error("Audience Restriction Check Failed",
                                     conditionsType.toString(), new Object[]{audienceRestriction}, null);
            logger.finer(result.toString());
            validationResults.add(result);
        }
    }

    /**
     * Subject validation for 1.x
     */
    private void validateSubjectConfirmation(SubjectStatementAbstractType subjectStatementAbstractType, Collection validationResults) {
        SubjectType subject = subjectStatementAbstractType.getSubject();
        if (subject == null) {
            validationResults.add(new Error("Subject Statement Required", subjectStatementAbstractType.toString(), null, null));
        }
        final String nameQualifier = requestWssSaml.getNameQualifier();
        NameIdentifierType nameIdentifierType = subject.getNameIdentifier();
        if (nameQualifier != null && !"".equals(nameQualifier)) {
            if (nameIdentifierType != null) {
                String presentedNameQualifier = nameIdentifierType.getNameQualifier();
                if (!nameQualifier.equals(presentedNameQualifier)) {
                    Error result = new Error("Name Qualifiers does not match presented/required {0}/{1}",
                                             subjectStatementAbstractType.toString(),
                                             new Object[]{presentedNameQualifier, nameQualifier}, null);
                    validationResults.add(result);
                    logger.finer(result.toString());
                    return;
                } else {
                    logger.fine("Matched Name Qualifier " + nameQualifier);
                }
            }
        }
        String[] nameFormats = filterNameFormats(requestWssSaml.getNameFormats());
        boolean nameFormatMatch = false;
        String presentedNameFormat = null;
        if (nameIdentifierType != null) {
            presentedNameFormat = nameIdentifierType.getFormat();
            if (presentedNameFormat != null) {
                for (int i = 0; i < nameFormats.length; i++) {
                    String nameFormat = nameFormats[i];
                    if (nameFormat.equals(presentedNameFormat)) {
                        nameFormatMatch = true;
                        logger.fine("Matched Name Format " + nameFormat);
                        break;
            } else if (nameFormat.equals(SamlConstants.NAMEIDENTIFIER_UNSPECIFIED)) {
                        nameFormatMatch = true;
                        logger.fine("Matched Name Format " + nameFormat);
                        break;
                    }
                }
            }
        }
        if (presentedNameFormat == null) {
            presentedNameFormat = "";
        }
        if (!nameFormatMatch) {
            Error result = new Error("Name Format does not match presented/required {0}/{1}",
                                     subjectStatementAbstractType.toString(),
                                     new Object[]{presentedNameFormat, Arrays.asList(nameFormats)}, null);
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }
        String[] confirmations = requestWssSaml.getSubjectConfirmations();
        final SubjectConfirmationType subjectConfirmation = subject.getSubjectConfirmation();
        List presentedConfirmations = null;
        if (subjectConfirmation != null) {
            final String[] confirmationMethodArray = subjectConfirmation.getConfirmationMethodArray();
            if (confirmationMethodArray != null)
                presentedConfirmations = Arrays.asList(confirmationMethodArray);
        }
        if (presentedConfirmations == null)
            presentedConfirmations = Collections.EMPTY_LIST;

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
                logger.fine("Matched Subject Confirmation " + confirmation);
                break;
            }
        }

        if (!confirmationMatch) {
            List acceptedConfirmations = new ArrayList(Arrays.asList(confirmations));
            if (requestWssSaml.isNoSubjectConfirmation()) {
                acceptedConfirmations.add("None");
            }
            Error result = new Error("Subject Confirmations mismatch presented/accepted {0}/{1}",
                                     subjectStatementAbstractType.toString(),
                                     new Object[]{presentedConfirmations, acceptedConfirmations}, null);
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }
    }

    /**
     * Validate the SAML assertion conditions for v2.x
     */
    private void validateConditions(x0Assertion.oasisNamesTcSAML2.ConditionsType conditionsType, Calendar timeNow, Collection validationResults) {
        Saml2SubjectAndConditionValidate.validateConditions(requestWssSaml, conditionsType, timeNow, validationResults);
    }

    /**
     * Subject validation for 2.x
     */
    private void validateSubjectConfirmation(x0Assertion.oasisNamesTcSAML2.SubjectType subjectType, Calendar timeNow, Collection validationResults) {
        Saml2SubjectAndConditionValidate.validateSubject(requestWssSaml, subjectType, timeNow, validationResults);
    }

    /**
     * Filter out any name formats that are not allowed in v1
     */
    private String[] filterNameFormats(String[] formats) {
        return filter(formats, SamlConstants.ALL_NAMEIDENTIFIERS);
    }

    /**
     * Remove any items from values that are not in the allowedValues
     *
     * @param values        The items to be filtered
     * @param allowedValues The permitted items
     * @return              The filtered values
     */
    static String[] filter(String[] values, String[] allowedValues) {
        Set valueSet = new LinkedHashSet(Arrays.asList(values));
        valueSet.retainAll(Arrays.asList(allowedValues));
        return (String[]) valueSet.toArray(new String[valueSet.size()]);
    }

    static class Error {
        private final Object[] arguments;
        private final Exception exception;
        private final String formattedReason;

        protected Error(String reason, Object context, Object args, Exception exception) {
            if (reason == null) {
                throw new IllegalArgumentException("Reason is required");
            }
            this.arguments = getArguments(args);
            this.formattedReason = MessageFormat.format(reason, arguments);
            this.exception = exception;
        }

        private Object[] getArguments(Object args) {
            if (args == null) {
                return new Object[]{};
            }
            if (args instanceof Object[]) {
                return (Object[])args;
            }
            return new Object[]{args};
        }

        public String toString() {
            final String exceptionMessage = exception == null ? "" : "Exception :" + exception.getMessage();
            return "SAML Constraint Error: " + formattedReason + exceptionMessage;
        }
    }

}