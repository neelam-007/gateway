/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssTimestamp;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import x0Assertion.oasisNamesTcSAML1.*;

import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

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
    protected final RequireWssSaml requestWssSaml;
    private Map<Class, SamlStatementValidate> validators = new HashMap<Class, SamlStatementValidate>();

    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestWssSaml     the saml assertion that specifies constraints
     */
    public SamlAssertionValidate(RequireWssSaml requestWssSaml) {
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
            Error result = new Error("No Security Header found", null);
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
            for (XmlSecurityToken token : tokens) {
                if ((token.getType() == SecurityTokenType.SAML_ASSERTION && acceptV1) ||
                    (token.getType() == SecurityTokenType.SAML2_ASSERTION && acceptV2)) {
                    assertion = (SamlAssertion)token;
                    XmlObject xmlObject = assertion.getXmlBeansAssertionType();
                    boolean assertionMatch = false;

                    if (xmlObject instanceof AssertionType) {
                        AssertionType assertionType = (AssertionType)xmlObject;
                        Collection statementList = new ArrayList();
                        statementList.addAll(Arrays.asList(assertionType.getAuthenticationStatementArray()));
                        statementList.addAll(Arrays.asList(assertionType.getAuthorizationDecisionStatementArray()));
                        statementList.addAll(Arrays.asList(assertionType.getAttributeStatementArray()));

                        StatementAbstractType[] statementArray = (StatementAbstractType[])statementList.toArray(new StatementAbstractType[]{ });

                        for (StatementAbstractType statementAbstractType : statementArray) {
                            Set keys = validators.keySet();
                            for (Object key : keys) {
                                Class clazz = (Class)key;
                                if (clazz.isAssignableFrom(statementAbstractType.getClass())) {
                                    assertionMatch = true;
                                    SamlStatementValidate statementValidate = validators.get(clazz);
                                    validateSubjectConfirmation((SubjectStatementAbstractType)statementAbstractType, validationResults);
                                    validateConditions(assertionType, validationResults);
                                    statementValidate.validate(soapMessageDoc, statementAbstractType, wssResults, validationResults);
                                }
                            }
                        }
                    } else if (xmlObject instanceof x0Assertion.oasisNamesTcSAML2.AssertionType) {
                        x0Assertion.oasisNamesTcSAML2.AssertionType assertionType =
                            (x0Assertion.oasisNamesTcSAML2.AssertionType)xmlObject;
                        validateConditions(assertionType.getConditions(), now, validationResults);
                        validateSubjectConfirmation(assertionType.getSubject(), now, validationResults);

                        Collection statementList = new ArrayList();
                        statementList.addAll(Arrays.asList(assertionType.getAuthnStatementArray()));
                        statementList.addAll(Arrays.asList(assertionType.getAuthzDecisionStatementArray()));
                        statementList.addAll(Arrays.asList(assertionType.getAttributeStatementArray()));

                        XmlObject[] statementArray = (XmlObject[])statementList.toArray(new XmlObject[]{ });

                        for (XmlObject statementAbstractType : statementArray) {
                            Set keys = validators.keySet();
                            for (Object key : keys) {
                                Class clazz = (Class)key;
                                if (clazz.isAssignableFrom(statementAbstractType.getClass())) {
                                    assertionMatch = true;
                                    SamlStatementValidate statementValidate = validators.get(clazz);
                                    statementValidate.validate(soapMessageDoc, statementAbstractType, wssResults, validationResults);
                                }
                            }
                        }

                        // allow no statements if none are required
                        if (!assertionMatch && validators.isEmpty())
                            assertionMatch = true;
                    }

                    if (!assertionMatch) {
                        Error result = new Error("No SAML assertion has been presented that matches specified constraints", null);
                        validationResults.add(result);
                        logger.finer(result.toString());
                        return;
                    }
                }
            }

            if (assertion == null) {
                Error result = new Error("No SAML assertion found in security Header", null);
                validationResults.add(result);
                logger.finer(result.toString());
                return;
            }
            final SigningSecurityToken[] signingTokens = wssResults.getSigningTokens(assertion.asElement());
            if (signingTokens.length == 0) {
                Error result = new Error("Unsigned SAML assertion found in security Header", null);
                validationResults.add(result);
                logger.finer(result.toString());
                return;
            } else if (signingTokens.length > 1) {
                Error result = new Error("SAML assertion was signed by more than one security token", null);
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
                        Error result = new Error("Assertion was signed by non-X509 SecurityToken " + signingTokens[0].getClass(), null);
                        validationResults.add(result);
                        logger.finer(result.toString());
                        return;
                    }
                }
            }

            if (assertion.isHolderOfKey() && requestWssSaml.isRequireHolderOfKeyWithMessageSignature()) {
                X509Certificate subjectCertificate = assertion.getSubjectCertificate();
                if (subjectCertificate == null) {
                    Error result = new Error("Subject Certificate is required for Holder-Of-Key Assertion", null);
                    validationResults.add(result);
                    logger.finer(result.toString());
                    return;
                }
                // validate that the assertion is the soap:Body signer
                if (!isBodyOrTimestampSigned(assertion.getSignedElements(), wssResults)) {
                    Error result = new Error("Can't validate proof of posession; the SOAP Body and Timestamp are not signed with the Subject Confirmation Certificate", null);
                    validationResults.add(result);
                    logger.finer(result.toString());
                }
            } else if (assertion.isSenderVouches() && requestWssSaml.isRequireSenderVouchesWithMessageSignature()) {
                // make sure ther soap:Body or timestamp is signed. The actual trust verification is in FIP SamlAuthorizationHandler
                X509Certificate messageSigner = getBodyOrTimestampSigner(wssResults);

                if ( messageSigner == null) {
                    Error result = new Error("Can't validate proof of posession; the SOAP Body and Timestamp are not signed", null);
                    validationResults.add(result);
                    logger.finer(result.toString());
                } else {
                    assertion.setAttestingEntity( messageSigner);
                }
            } else {
                X509Certificate sslCert = credentials == null ? null : credentials.getClientCert();
                if (assertion.isHolderOfKey()) {
                    X509Certificate subjectCertificate = assertion.getSubjectCertificate();
                    if (subjectCertificate == null) {
                        Error result = new Error("Subject Certificate is required for Holder-Of-Key Assertion", null);
                        validationResults.add(result);
                        logger.finer(result.toString());
                        return;
                    }
                    if (!subjectCertificate.equals(sslCert)) {
                        Error result = new Error("SSL Certificate and Holder-Of-Key Subject Certificate mismatch", null);
                        validationResults.add(result);
                        logger.finer(result.toString());
                        return;
                    }
                    assertion.setAttestingEntity(subjectCertificate); // for HOK the attesting entity is subject cert
                } else if (assertion.isSenderVouches()) {
                    if (sslCert == null) {
                         Error result = new Error("SSL Client Certificate is required for Sender-Vouches Assertion and unsigned message", null);
                         validationResults.add(result);
                         logger.finer(result.toString());
                         return;
                     }
                     assertion.setAttestingEntity(sslCert); // for SV the attesting entity is SSL cert and the trust is verified in FIP
                }
            }
        } catch (InvalidDocumentFormatException e) {
            validationResults.add(new Error("Can't process non SOAP messages", e));
        } catch (IOException e) {
            validationResults.add(new Error("Can't process non SOAP messages", e));
        }
    }

    private boolean isBodyOrTimestampSigned( final SignedElement[] signedElements,
                                             final ProcessorResult wssResult )
      throws InvalidDocumentFormatException {
        boolean signedBody = false;
        boolean signedTimestamp = false;
        boolean invalidSignedBody = false;
        final WssTimestamp timestamp = wssResult.getTimestamp();
        final Element timestampElement = timestamp!=null ? timestamp.asElement() : null;

        for (SignedElement signedElement : signedElements) {
            Element element = signedElement.asElement();
            if (SoapUtil.isBody(element)) {
                signedBody = true;
                break;
            } else if (timestampElement != null && timestampElement == element) {
                signedTimestamp = true;
            } else if ( SoapUtil.BODY_EL_NAME.equals( element.getLocalName() ) &&
                        SoapUtil.ENVELOPE_URIS.contains( element.getNamespaceURI() ) ) {
                invalidSignedBody = true;
            }
        }
        return signedBody || ( signedTimestamp && !invalidSignedBody );
    }

    /**
     * Get the X509Certificate that signed the soap:Body or the security header
     * timestamp or <code>null</code> if neither has been signed.
     */
    private X509Certificate getBodyOrTimestampSigner( final ProcessorResult wssResult )
      throws InvalidDocumentFormatException {
        final WssTimestamp timestamp = wssResult.getTimestamp();
        final SignedElement[] signedElements = WSSecurityProcessorUtils.filterSignedElementsByIdentity(null, wssResult, null, false);
        for (SignedElement signedElement : signedElements) {
            if (SoapUtil.isBody(signedElement.asElement())) {
                SigningSecurityToken signingSecurityToken = signedElement.getSigningSecurityToken();
                if (!(signingSecurityToken instanceof X509SigningSecurityToken)) {
                    throw new InvalidDocumentFormatException("Message body was signed, but not with an X.509 Security Token");
                }
                return ((X509SecurityToken)signingSecurityToken).getCertificate();
            }
        }

        if ( timestamp != null ) {
            X509SecurityToken token = null;
            final Element timestampElement = timestamp.asElement();
            for (SignedElement signedElement : signedElements) {
                Element element = signedElement.asElement();
                if (timestampElement == element) {
                    SigningSecurityToken signingSecurityToken = signedElement.getSigningSecurityToken();
                    if (!(signingSecurityToken instanceof X509SigningSecurityToken)) {
                        throw new InvalidDocumentFormatException("Message timestamp was signed, but not with an X.509 Security Token");
                    }
                    token = (X509SecurityToken)signingSecurityToken;
                } else if ( SoapUtil.BODY_EL_NAME.equals( element.getLocalName() ) &&
                            SoapUtil.ENVELOPE_URIS.contains( element.getNamespaceURI() ) ) {
                    token = null;
                    break;
                }
            }

            if (token != null) {
                return token.getCertificate();
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
                validationResults.add(new Error("Can't validate conditions, no Conditions have been presented", null));
                return;
            }
            Calendar notBefore = conditionsType.getNotBefore();
            Calendar notOnOrAfter = conditionsType.getNotOnOrAfter();
            if (notBefore == null || notOnOrAfter == null) {
                logger.finer("No Validity Period conditions have been presented, cannot validate Conditions");
                StringWriter sw = new StringWriter();
                assertionType.save(sw);
                validationResults.add(new Error("No Validity Period conditions have been presented, cannot validate Conditions", null));
                return;
            }

            Calendar now = Calendar.getInstance(UTC_TIME_ZONE);
            now.clear(Calendar.MILLISECOND); //clear millis xsd:dateTime does not have it
            if (now.before(notBefore)) {
                logger.finer("Condition 'Not Before' check failed, now :" + now.toString() + " Not Before:" + notBefore.toString());
                validationResults.add(new Error("SAML ticket does not become valid until: {0}",
                                                null,
                                                notBefore.getTime().toString()
                ));
            }

            if (now.equals(notOnOrAfter) || now.after(notOnOrAfter)) {
                logger.finer("Condition 'Not On Or After' check failed, now :" + now.toString() + " Not Before:" + notOnOrAfter.toString());
                validationResults.add(new Error("SAML ticket has expired as of: {0}",
                                                null,
                                                notOnOrAfter.getTime().toString()
                ));
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
            Error result = new Error("Can't validate conditions, no Conditions have been found", null);
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }

        AudienceRestrictionConditionType[] audienceRestrictionArray = conditionsType.getAudienceRestrictionConditionArray();
        boolean audienceRestrictionMatch = false;
        for (AudienceRestrictionConditionType audienceRestrictionConditionType : audienceRestrictionArray) {
            String[] audienceArray = audienceRestrictionConditionType.getAudienceArray();
            for (String s : audienceArray) {
                if (audienceRestriction.equals(s)) {
                    audienceRestrictionMatch = true;
                    break;
                }
            }
        }
        if (!audienceRestrictionMatch) {
            Error result = new Error("Audience Restriction Check Failed", null, audienceRestriction);
            logger.finer(result.toString());
            validationResults.add(result);
        }
    }

    /**
     * Subject validation for 1.x
     */
    private void validateSubjectConfirmation(SubjectStatementAbstractType subjectStatementAbstractType, Collection validationResults) {
        final SubjectType subject = subjectStatementAbstractType.getSubject();
        if (subject == null) {
            validationResults.add(new Error("Subject Statement Required", null));
            return;
        }

        final String nameQualifier = requestWssSaml.getNameQualifier();
        final NameIdentifierType nameIdentifierType = subject.getNameIdentifier();
        if (nameQualifier != null && !"".equals(nameQualifier)) {
            if (nameIdentifierType != null) {
                String presentedNameQualifier = nameIdentifierType.getNameQualifier();
                if (!nameQualifier.equals(presentedNameQualifier)) {
                    Error result = new Error("Name Qualifiers does not match presented/required {0}/{1}",
                                             null,
                                             presentedNameQualifier, nameQualifier
                    );
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
                for (String nameFormat : nameFormats) {
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
                                     null,
                                     presentedNameFormat, Arrays.asList(nameFormats)
            );
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
        for (String confirmation : confirmations) {
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
                                     null,
                                     presentedConfirmations, acceptedConfirmations
            );
            validationResults.add(result);
            logger.finer(result.toString());
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

        protected Error(String reason, Exception exception, Object... args) {
            if (reason == null) throw new IllegalArgumentException("Reason is required");

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