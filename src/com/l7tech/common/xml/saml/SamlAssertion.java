/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml.saml;

import com.ibm.xml.dsig.IDResolver;
import com.ibm.xml.dsig.SignatureContext;
import com.ibm.xml.dsig.Validity;
import com.l7tech.common.security.saml.SamlException;
import com.l7tech.common.security.token.SamlSecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.xml.processor.MutableX509SigningSecurityToken;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TooManyChildElementsException;
import org.apache.xmlbeans.XmlException;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.X509DataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.*;

import java.io.IOException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates an abstract saml:Assertion SecurityToken.
 */
public class SamlAssertion extends MutableX509SigningSecurityToken implements SamlSecurityToken {
    protected static final Logger logger = Logger.getLogger(SamlAssertion.class.getName());
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    private final AssertionType assertion;

    private Element assertionElement = null;
    private boolean hasEmbeddedSignature = false;
    private ConfirmationMethod confirmationMethod = null;
    private X509Certificate subjectCertificate = null;
    private X509Certificate issuerCertificate = null;
    private String assertionId = null;
    private Calendar expires = null;
    private String nameIdentifierFormat;
    private String nameQualifier;
    private String nameIdentifierValue;
    private String authenticationMethod;

    /**
     * Currently only ever set by {@link com.l7tech.common.security.xml.processor.WssProcessorImpl#processSamlSecurityToken}
     */
    protected boolean possessionProved = false;

    public boolean isHolderOfKey() {
        return HOLDER_OF_KEY.equals(confirmationMethod);
    }

    public boolean isSenderVouches() {
        return SENDER_VOUCHES.equals(confirmationMethod);
    }

    public boolean isBearerToken() {
        return confirmationMethod == null || BEARER_TOKEN.equals(confirmationMethod);
    }

    public ConfirmationMethod getConfirmationMethod() {
        return confirmationMethod;
    }

    /**
     * Constructs a new SamlAssertion from an XML Element.
     * <p/>
     * The resulting object could be either sender-vouches or holder-of-key depending on the
     * SubjectConfirmation/ConfirmationMethod found in the XML; separate subclasses are no longer used.
     *
     * @param ass xmlassertion the xml element containing the assertion
     */
    public SamlAssertion(Element ass) throws SAXException, SamlException {
        super(ass);
        assertionElement = ass;
        assertionId = assertionElement.getAttribute("AssertionID");
        if (assertionId == null || assertionId.length() < 1)
            throw new SAXException("AssertionID missing or empty");
        try {
            assertion = AssertionDocument.Factory.parse(ass).getAssertion();
            Collection statementList = new ArrayList();
            statementList.addAll(Arrays.asList(assertion.getAuthenticationStatementArray()));
            statementList.addAll(Arrays.asList(assertion.getAuthorizationDecisionStatementArray()));
            statementList.addAll(Arrays.asList(assertion.getAttributeStatementArray()));
            SubjectStatementAbstractType[] subjectStatements =
              (SubjectStatementAbstractType[])statementList.toArray(new SubjectStatementAbstractType[] {});

            SubjectType subject = null;
            // all the statements must have the same subject (L7 requirement).
            for (int i = 0; i < subjectStatements.length; i++) {
                StatementAbstractType statementAbstractType = subjectStatements[i];
                if (statementAbstractType instanceof SubjectStatementAbstractType) {
                    // Extract subject certificate
                    SubjectStatementAbstractType subjectStatement = (SubjectStatementAbstractType)statementAbstractType;
                    subject = subjectStatement.getSubject();
                    NameIdentifierType nameIdentifier = subject.getNameIdentifier();
                    if (nameIdentifier != null) {
                        this.nameIdentifierFormat = nameIdentifier.getFormat();
                        this.nameQualifier = nameIdentifier.getNameQualifier();
                        this.nameIdentifierValue = nameIdentifier.getStringValue();
                    }
                    if (statementAbstractType instanceof AuthenticationStatementType) {
                        AuthenticationStatementType authenticationStatementType = (AuthenticationStatementType)statementAbstractType;
                        authenticationMethod = authenticationStatementType.getAuthenticationMethod();

                    }
                } else {
                    logger.warning("Unknown and skipped statement type " + statementAbstractType.getClass());
                }
            }
            if (subject == null) {
                String msg = "Could not find the subject in the assertion :\n" + XmlUtil.nodeToFormattedString(ass);
                logger.warning(msg);
                throw new SamlException(msg);
            }
            ConditionsType conditions = assertion.getConditions();
            if (conditions != null) {
                expires = conditions.getNotOnOrAfter();
            }
            SubjectConfirmationType subjectConfirmation = subject.getSubjectConfirmation();

            if (subjectConfirmation != null) {
                String[] confMethods = subjectConfirmation.getConfirmationMethodArray();
                if (confMethods == null || confMethods.length != 1) {
                    throw new IllegalArgumentException("One and only one SubjectConfirmation/ConfirmationMethod must be present");
                }
                String confMethod = confMethods[0];
                if (confMethod.indexOf("holder-of-key") >= 0) {
                    confirmationMethod = HOLDER_OF_KEY;
                } else if (confMethod.indexOf("sender-vouches") >= 0) {
                    confirmationMethod = SENDER_VOUCHES;
                } else {
                    String msg = "Could not determine the saml ConfirmationMethod (neither holder-of-key nor sender-vouches)";
                    logger.info(msg);
                    confirmationMethod = null;
                }

                KeyInfoType keyInfo = subjectConfirmation.getKeyInfo();
                if (keyInfo !=null) {
                    X509DataType[] x509datas = keyInfo.getX509DataArray();
                    if (x509datas !=null && x509datas.length > 0) {
                        X509DataType x509data = x509datas[0];
                        subjectCertificate = CertUtils.decodeCert(x509data.getX509CertificateArray(0));
                    }
                }
            }

            // Check if assertion is signed
            Element signature = XmlUtil.findOnlyOneChildElementByName(assertionElement, SoapUtil.DIGSIG_URI, "Signature");
            if (signature != null) {
                hasEmbeddedSignature = true;
                // Extract the issuer certificate
                Element keyinfo = XmlUtil.findOnlyOneChildElementByName(signature, SoapUtil.DIGSIG_URI, "KeyInfo");
                if (keyinfo == null) throw new SAXException("SAML issuer signature has no KeyInfo");
                Element x509Data = XmlUtil.findOnlyOneChildElementByName(keyinfo, SoapUtil.DIGSIG_URI, "X509Data");
                if (x509Data == null) throw new SAXException("SAML issuer signature has no KeyInfo/X509Data");
                Element x509CertEl = XmlUtil.findOnlyOneChildElementByName(x509Data, SoapUtil.DIGSIG_URI, "X509Certificate");
                if (x509CertEl == null) throw new SAXException("SAML issuer signature has no KeyInfo/X509Data/X509Certificate");
                String certBase64 = XmlUtil.getTextValue(x509CertEl);
                byte[] certBytes = HexUtils.decodeBase64(certBase64, true);
                issuerCertificate = CertUtils.decodeCert(certBytes);
                if (issuerCertificate == null) throw new SAXException("SAML assertion is signed but unable to recover issuer certificate"); // can't happen
            }

        } catch (XmlException e) {
            throw new SAXException(e);
        } catch ( CertificateException e ) {
            final String msg = "Certificate in SAML assertion could not be parsed";
            logger.log(Level.WARNING, msg, e );
            throw new SAXException(e);
        } catch (TooManyChildElementsException e) {
            throw new SAXException(e);
        } catch (IOException e) {
            throw new SAXException("Invalid Base64 in SAML token issuer certificate", e);
        }
    }

    public String getAssertionId() {
        return assertionId;
    }

    public boolean hasEmbeddedIssuerSignature() {
        return hasEmbeddedSignature;
    }

    public X509Certificate getSubjectCertificate() {
        return subjectCertificate;
    }

    /**
     * @return true if the subject described by this assertion has proven possession of the private key
     *              corresponding to the subject certificate here. Only meaningful if {@link #isHolderOfKey()}.
     */
    public boolean isPossessionProved() {
        return possessionProved;
    }

    public void onPossessionProved() {
        possessionProved = true;
    }

    public X509Certificate getMessageSigningCertificate() {
        // TODO is this strictly true?
        if (isHolderOfKey()) {
            return subjectCertificate;
        } else if (isSenderVouches()) {
            return issuerCertificate;
        }
        return null;
    }

    public X509Certificate getIssuerCertificate() {
        return issuerCertificate;
    }

    public void setIssuerCertificate(X509Certificate issuerCertificate) {
        if (!isSenderVouches()) {
            throw new IllegalStateException("Can't set the issuer certificate for non Sender-Vouches assertion");
        }
        this.issuerCertificate = issuerCertificate;
    }

    public Element asElement() {
        return assertionElement;
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.SAML_ASSERTION;
    }

    public String getElementId() {
        return assertionId;
    }

    static class CausedSignatureException extends SignatureException {
        public CausedSignatureException() {}
        public CausedSignatureException(String msg) { super(msg); }
        public CausedSignatureException(String msg, Throwable t) { super(msg); initCause(t); }
        public CausedSignatureException(Throwable t) { super(); initCause(t); }
    }

    static class ResolveIdException extends RuntimeException {
        public ResolveIdException(String s) { super(s); }
    }

    public void verifyEmbeddedIssuerSignature() throws SignatureException {
        if (!hasEmbeddedSignature) throw new IllegalStateException("May not verify signature; this assertion is not signed");

        try {
            Element signature = XmlUtil.findOnlyOneChildElementByName(assertionElement, SoapUtil.DIGSIG_URI, "Signature");
            if (signature == null) throw new CausedSignatureException("missing Signature element"); // can't happen

            X509Certificate signingCert = getIssuerCertificate();
            if (signingCert == null) throw new CausedSignatureException("missing issuer certificate"); // can't happen
            PublicKey signingKey = signingCert.getPublicKey();

            // Validate signature
            SignatureContext sigContext = new SignatureContext();
            sigContext.setIDResolver(new IDResolver() {
                public Element resolveID(Document doc, String s) {
                    if (!s.equals(getAssertionId()))
                        throw new ResolveIdException("SAML signature contains signedinfo reference to unexpected element ID \"" + s + "\"");
                    return assertionElement;
                }
            });
            Validity validity = sigContext.verify(signature, signingKey);

            if (!validity.getCoreValidity()) {
                StringBuffer msg = new StringBuffer("Unable to verify signature of SAML assertion: Validity not achieved. " + validity.getSignedInfoMessage());
                for (int i = 0; i < validity.getNumberOfReferences(); i++) {
                    msg.append("\n\tElement " + validity.getReferenceURI(i) + ": " + validity.getReferenceMessage(i));
                }
                logger.warning(msg.toString());
                throw new CausedSignatureException(msg.toString());
            }


        } catch (TooManyChildElementsException e) {
            throw new CausedSignatureException(e);
        } catch (ResolveIdException e) {
            throw new CausedSignatureException(e);
        }
    }

    public String getNameIdentifierFormat() {
        return nameIdentifierFormat;
    }

    public String getNameQualifier() {
        return nameQualifier;
    }

    public String getNameIdentifierValue() {
        return nameIdentifierValue;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public Calendar getExpires() {
        return expires;
    }

    public boolean isExpiringSoon(int preexpireSec) {
        Calendar expires = getExpires();
        if (expires == null) {
            return false;
        }
        Calendar nowUtc = Calendar.getInstance(UTC_TIME_ZONE);
        nowUtc.add(Calendar.SECOND, preexpireSec);
        return !expires.after(nowUtc);
    }

    /** @return the Xml Beans assertion type.  Never null. */
    public AssertionType getXmlBeansAssertionType() {
        return assertion;
    }
}
