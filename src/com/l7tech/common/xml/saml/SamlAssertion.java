/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml.saml;

import com.ibm.xml.dsig.IDResolver;
import com.ibm.xml.dsig.SignatureContext;
import com.ibm.xml.dsig.Validity;
import com.l7tech.common.security.token.SamlSecurityToken;
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
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates an abstract saml:Assertion SecurityToken.
 */
public class SamlAssertion implements SamlSecurityToken {
    Element assertionElement = null;
    AssertionType assertion = null;
    boolean isSigned = false;
    boolean holderOfKey = false;
    X509Certificate subjectCertificate = null;
    X509Certificate signingCertificate = null;
    X509Certificate issuerCertificate = null;
    private String assertionId = null;
    private Calendar expires = null;

    /**
     * Currently only ever set by {@link com.l7tech.common.security.xml.processor.WssProcessorImpl#processSamlSecurityToken}
     */
    protected boolean possessionProved = false;

    public SamlAssertion asSamlAssertion() {
        return this;
    }

    public boolean isHolderOfKey() {
        return holderOfKey;
    }

    /**
     * Constructs a new SamlAssertion from an XML Element.
     * <p>
     * The resulting object could be either sender-vouches or holder-of-key depending on the
     * SubjectConfirmation/ConfirmationMethod found in the XML; separate subclasses are no longer used.
     *
     * @param ass xmlassertion the xml element containing the assertion
     */
    public SamlAssertion(Element ass) throws SAXException {
        assertionElement = ass;
        assertionId = assertionElement.getAttribute("AssertionID");
        if (assertionId == null || assertionId.length() < 1)
            throw new SAXException("AssertionID missing or empty");
        try {
            assertion = AssertionDocument.Factory.parse(ass).getAssertion();
            AuthenticationStatementType[] authStatements = assertion.getAuthenticationStatementArray();

            // Extract subject certificate
            AuthenticationStatementType authStatement = authStatements[0];
            SubjectType subject = authStatement.getSubject();
            NameIdentifierType nameIdentifier = subject.getNameIdentifier();
            if (nameIdentifier != null) {
                this.nameIdentifierFormat = nameIdentifier.getFormat();
                this.nameQualifier = nameIdentifier.getNameQualifier();
                this.nameIdentifierValue = nameIdentifier.getStringValue();
            }

            ConditionsType conditions = assertion.getConditions();
            if (conditions == null)
                throw new SAXException("Assertion has no Conditions");
            expires = conditions.getNotOnOrAfter();
            if (expires == null)
                throw new SAXException("Assertion has no NotOnOrAfter (expiry date)");

            SubjectConfirmationType subjectConfirmation = subject.getSubjectConfirmation();

            KeyInfoType keyInfo = subjectConfirmation.getKeyInfo();
            X509DataType[] x509datas = keyInfo.getX509DataArray();
            X509DataType x509data = x509datas[0];
            subjectCertificate = CertUtils.decodeCert(x509data.getX509CertificateArray(0));

            // Check if assertion is signed
            Element signature = XmlUtil.findOnlyOneChildElementByName(assertionElement, SoapUtil.DIGSIG_URI, "Signature");
            if (signature != null) {
                isSigned = true;
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

            String[] confMethods = subjectConfirmation.getConfirmationMethodArray();
            if (confMethods == null || confMethods.length != 1) {
                throw new IllegalArgumentException("One and only one SubjectConfirmation/ConfirmationMethod must be present");
            }
            String confMethod = confMethods[0];
            if (confMethod.indexOf("holder-of-key") >= 0) {
                holderOfKey = true;
                signingCertificate = subjectCertificate;
            } else if (confMethod.indexOf("sender-vouches") >= 0) {
                holderOfKey = false;
                signingCertificate = issuerCertificate;
            } else {
                // todo, fallback on something?
                String msg = "Could not determine the saml ConfirmationMethod (neither holder-of-key nor sender-vouches)";
                logger.severe(msg);
                throw new SAXException(msg);
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

    public boolean isSigned() {
        return isSigned;
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
        if (holderOfKey) {
            return subjectCertificate;
        } else {
            return issuerCertificate;
        }
    }

    public X509Certificate getIssuerCertificate() {
        return issuerCertificate;
    }

    public Element asElement() {
        return assertionElement;
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

    /** Check signature of this saml assertion.  May only be called if isSigned() returns true. */
    public void verifyIssuerSignature() throws SignatureException {
        if (!isSigned) throw new IllegalStateException("May not verify signature; this assertion is not signed");

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

    public Calendar getExpires() {
        return expires;
    }

    public X509Certificate getSigningCertificate() {
        if (holderOfKey)
            return subjectCertificate;
        else
            return issuerCertificate;
    }

    private String nameIdentifierFormat;
    private String nameQualifier;
    private String nameIdentifierValue;
    protected static final Logger logger = Logger.getLogger(SamlAssertion.class.getName());
}
