package com.l7tech.common.xml.saml;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.SignatureException;
import java.security.PublicKey;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.X509DataType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.TooManyChildElementsException;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.security.xml.KeyInfoElement;
import com.l7tech.common.security.token.SecurityTokenType;

import com.ibm.xml.dsig.SignatureContext;
import com.ibm.xml.dsig.IDResolver;
import com.ibm.xml.dsig.Validity;
import com.ibm.xml.dsig.Transform;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import x0Assertion.oasisNamesTcSAML1.AssertionType;
import x0Assertion.oasisNamesTcSAML1.AssertionDocument;
import x0Assertion.oasisNamesTcSAML1.SubjectStatementAbstractType;
import x0Assertion.oasisNamesTcSAML1.SubjectType;
import x0Assertion.oasisNamesTcSAML1.StatementAbstractType;
import x0Assertion.oasisNamesTcSAML1.NameIdentifierType;
import x0Assertion.oasisNamesTcSAML1.AuthenticationStatementType;
import x0Assertion.oasisNamesTcSAML1.ConditionsType;
import x0Assertion.oasisNamesTcSAML1.SubjectConfirmationType;

/**
 * Saml Assertion for version 1.x
 * 
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public final class SamlAssertionV1 extends SamlAssertion {
    private static final Logger logger = Logger.getLogger(SamlAssertionV1.class.getName());

    private final AssertionType assertion;
    private Element assertionElement = null;
    private boolean hasEmbeddedSignature = false;
    private ConfirmationMethod confirmationMethod = null;
    private X509Certificate subjectCertificate = null;
    private X509Certificate issuerCertificate = null;
    private X509Certificate attestingEntity = null;
    private String assertionId = null;
    private String uniqueId = null;
    private String subjectId = null;
    private Calendar expires = null;
    private String nameIdentifierFormat;
    private String nameQualifier;
    private String nameIdentifierValue;
    private String authenticationMethod;

   /**
     * Construct a new SamlAssertion from an XML element, using the specified thumbprint resolver to locate
     * certificates by thumbprint reference (as opposed to inline keyinfo).
     *
     * @param ass the XML element containing the assertion.  Must be a saml:Assertion element.
     * @param securityTokenResolver  the resolver for thumbprint KeyInfos, or null for no thumbprint support.
     * @throws org.xml.sax.SAXException    if the format of this assertion is invalid or not supported
     * @throws org.xml.sax.SAXException    if the KeyInfo used a thumbprint, but no thumbprint resolver was supplied.
     */
    public SamlAssertionV1(Element ass,
                         SecurityTokenResolver securityTokenResolver)
            throws SAXException
    {
        super(ass,1);
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
              (SubjectStatementAbstractType[])statementList.toArray(new SubjectStatementAbstractType[]{});

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
                throw new SAXException(msg);
            }
            subjectId = toString(subject);
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

                if (confirmationMethod == HOLDER_OF_KEY) {
                    KeyInfoType keyInfo = subjectConfirmation.getKeyInfo();
                    if (keyInfo != null) {
                        X509DataType[] x509datas = keyInfo.getX509DataArray();
                        if (x509datas != null && x509datas.length > 0) {
                            X509DataType x509data = x509datas[0];
                            subjectCertificate = CertUtils.decodeCert(x509data.getX509CertificateArray(0));
                        } else {
                            Element keyInfoEl = (Element)keyInfo.getDomNode();
                            List strs = XmlUtil.findChildElementsByName(keyInfoEl, SoapUtil.SECURITY_URIS_ARRAY, "SecurityTokenReference");
                            if (keyInfoEl != null && !strs.isEmpty()) {
                                try {
                                    KeyInfoElement kie = KeyInfoElement.parse((Element)keyInfo.getDomNode(), securityTokenResolver);
                                    subjectCertificate = kie.getCertificate();
                                } catch (Exception e) {
                                    logger.log(Level.INFO, "KeyInfo contained a SecurityTokenReference but it wasn't a thumbprint");
                                }
                            }
                        }
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
                KeyInfoElement keyInfo = KeyInfoElement.parse(keyinfo, securityTokenResolver);
                issuerCertificate = keyInfo.getCertificate();
            }

        } catch (XmlException e) {
            throw new SAXException(e);
        } catch (CertificateException e) {
            final String msg = "Certificate in SAML assertion could not be parsed";
            logger.log(Level.WARNING, msg, e);
            throw new SAXException(e);
        } catch (TooManyChildElementsException e) {
            throw new SAXException(e);
        } catch (IOException e) {
            throw new SAXException("Invalid Base64 in SAML token issuer certificate", e);
        } catch (KeyInfoElement.MissingResolverException e) {
            throw new SAXException(e);
        }
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.SAML_ASSERTION;
    }

    public int getVersionId() {
        return VERSION_1_1;
    }

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

    public String getAssertionId() {
        return assertionId;
    }

    public String getUniqueId() {
        if (uniqueId == null) {
            uniqueId = buildUniqueId();
        }

        return uniqueId;
    }

    public boolean hasEmbeddedIssuerSignature() {
        return hasEmbeddedSignature;
    }

    public X509Certificate getSubjectCertificate() {
        return subjectCertificate;
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

    public X509Certificate getAttestingEntity() {
        return attestingEntity;
    }

    public void setAttestingEntity(X509Certificate attestingEntity) {
        if (isHolderOfKey()) {
            if (!CertUtils.certsAreEqual(attestingEntity, subjectCertificate)) {
                throw new IllegalStateException("Can't set the attesting entity cert to different cert then subject cert for Holder-Of-Key assertion");
            }
        } else if (!isSenderVouches()) {
            throw new IllegalStateException("Can't set the attesting entity for non Sender-Vouches assertion");
        }
        this.attestingEntity = attestingEntity;
    }

    public Element asElement() {
        return assertionElement;
    }

    public String getElementId() {
        return assertionId;
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
            final boolean[] resolvedAssertionId = new boolean[1];
            final boolean[] accessedEnveloping = new boolean[1];
            SignatureContext sigContext = new SignatureContext();
            sigContext.setIDResolver(new IDResolver() {
                public Element resolveID(Document doc, String s) {
                    if (!s.equals(getAssertionId()))
                        throw new ResolveIdException("SAML signature contains signedinfo reference to unexpected element ID \"" + s + "\"");
                    resolvedAssertionId[0] = true;
                    return assertionElement;
                }
            });
            sigContext.setAlgorithmFactory(new AlgorithmFactoryExtn() {
                public Transform getTransform(String transform) throws NoSuchAlgorithmException {
                    if (Transform.ENVELOPED.equals(transform)) {
                        accessedEnveloping[0] = true;
                    }
                    return super.getTransform(transform);
                }
            });
            Validity validity = sigContext.verify(signature, signingKey);

            if (!validity.getCoreValidity()) {
                StringBuffer msg = new StringBuffer("Unable to verify signature of SAML assertion: Validity not achieved. " + validity.getSignedInfoMessage());
                for (int i = 0; i < validity.getNumberOfReferences(); i++) {
                    msg.append("\n\tElement ").append(validity.getReferenceURI(i)).append(": ").append(validity.getReferenceMessage(i));
                }
                throw new CausedSignatureException(msg.toString());
            }

            if (!resolvedAssertionId[0]) {
                throw new CausedSignatureException("SAML assertion signature does not reference assertion.");
            }

            if (!accessedEnveloping[0]) {
                throw new CausedSignatureException("SAML assertion signature has invalid transform (must be enveloped).");
            }

            if (validity.getNumberOfReferences() != 1) {
                throw new CausedSignatureException("SAML assertion signature has invalid number of references ("+validity.getNumberOfReferences()+").");
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

    public boolean isOneTimeUse() {
        return false;
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

    public XmlObject getXmlBeansAssertionType() {
        return assertion;
    }

    private String buildUniqueId() {
        String id;

        if (hasEmbeddedIssuerSignature()) {
            X509Certificate cert = getIssuerCertificate();
            String samlIssuerSubjectDn = cert.getSubjectDN().getName();
            try {
                id = HexUtils.encodeBase64(HexUtils.getMd5Digest(new byte[][]{
                        getAssertionId().getBytes("UTF-8"),
                        samlIssuerSubjectDn.getBytes("UTF-8"),
                        assertion.getSignature().getSignatureValue().getByteArrayValue()
                }));
            }
            catch(UnsupportedEncodingException uee) {
                throw new IllegalStateException("Support for UTF-8 is required.");
            }
        }
        else {
            try {
                id = HexUtils.encodeBase64(HexUtils.getMd5Digest(new byte[][]{
                        getAssertionId().getBytes("UTF-8"),
                        assertion.getIssuer().getBytes("UTF-8"),
                        BigInteger.valueOf(assertion.getIssueInstant().getTimeInMillis()).toByteArray(),
                        subjectId.getBytes("UTF-8")
                }));
            }
            catch(UnsupportedEncodingException uee) {
                throw new IllegalStateException("Support for UTF-8 is required.");
            }
        }

        return id;
    }

    private String toString(SubjectType subject) {
        StringBuffer subjectBuffer = new StringBuffer();

        if (subject != null) {
            NameIdentifierType nameIdType = subject.getNameIdentifier();
            if (nameIdType != null) {
                if(nameIdType.getFormat() != null) {
                    subjectBuffer.append(nameIdType.getFormat());
                    subjectBuffer.append(" ");
                }
                if(nameIdType.getNameQualifier() != null) {
                    subjectBuffer.append(nameIdType.getNameQualifier());
                    subjectBuffer.append(" ");
                }
                if(nameIdType.getStringValue() != null) {
                    subjectBuffer.append(nameIdType.getStringValue());
                }
            }
        }

        return subjectBuffer.toString().trim();
    }
}
