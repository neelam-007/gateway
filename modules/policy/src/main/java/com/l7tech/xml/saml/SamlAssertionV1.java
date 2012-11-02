package com.l7tech.xml.saml;

import com.ibm.xml.dsig.IDResolver;
import com.ibm.xml.dsig.SignatureContext;
import com.ibm.xml.dsig.Validity;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.*;
import com.l7tech.security.xml.processor.EncryptedKeyImpl;
import com.l7tech.security.xml.processor.WssProcessorAlgorithmFactory;
import com.l7tech.util.*;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.X509DataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.*;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saml Assertion for version 1.x
 * 
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class SamlAssertionV1 extends SamlAssertion {
    private static final Logger logger = Logger.getLogger(SamlAssertionV1.class.getName());

    private final AssertionType assertion;
    private Element assertionElement = null;
    private Element embeddedSignatureElement = null;
    private ConfirmationMethod confirmationMethod = null;
    private X509Certificate subjectCertificate = null;
    private X509Certificate issuerCertificate = null;
    private X509Certificate attestingEntity = null;
    private String assertionId = null;
    private String uniqueId = null;
    private String subjectId = null;
    private Calendar issueInstant = null;
    private Calendar starts = null;
    private Calendar expires = null;
    private String nameIdentifierFormat;
    private String nameQualifier;
    private String nameIdentifierValue;
    private String authenticationMethod;

    private EncryptedKey subjectConfirmationEncryptedKey = null;

   /**
     * Construct a new SamlAssertion from an XML element, using the specified thumbprint resolver to locate
     * certificates by thumbprint reference (as opposed to inline keyinfo).
     *
     * @param ass the XML element containing the assertion.  Must be a saml:Assertion element.
     * @param securityTokenResolver  the resolver for thumbprint KeyInfos, or null for no thumbprint support.
     * @throws org.xml.sax.SAXException    if the format of this assertion is invalid or not supported
     * @throws org.xml.sax.SAXException    if the KeyInfo used a thumbprint, but no thumbprint resolver was supplied.
     */
    public SamlAssertionV1(Element ass, SecurityTokenResolver securityTokenResolver) throws SAXException {
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
              (SubjectStatementAbstractType[])statementList.toArray(new SubjectStatementAbstractType[]{});

            SubjectType subject = null;

            // all the statements must have the same subject (L7 requirement).
            for (SubjectStatementAbstractType statement : subjectStatements) {
                // Extract subject certificate
                subject = statement.getSubject();
                if (subject != null) {
                    NameIdentifierType nameIdentifier = subject.getNameIdentifier();
                    if (nameIdentifier != null) {
                        this.nameIdentifierFormat = nameIdentifier.getFormat();
                        this.nameQualifier = nameIdentifier.getNameQualifier();
                        this.nameIdentifierValue = nameIdentifier.getStringValue();
                    }
                }

                if (statement instanceof AuthenticationStatementType) {
                    AuthenticationStatementType authenticationStatementType = (AuthenticationStatementType)statement;
                    authenticationMethod = authenticationStatementType.getAuthenticationMethod();
                }
            }
            if (subject == null) {
                String msg = "Could not find the subject in the assertion :\n" + XmlUtil.nodeToFormattedString(ass);
                logger.warning(msg);
                throw new SAXException(msg);
            }
            subjectId = toString(subject);
            issueInstant = assertion.getIssueInstant();
            ConditionsType conditions = assertion.getConditions();
            if (conditions != null) {
                starts = conditions.getNotBefore();
                expires = conditions.getNotOnOrAfter();
            }
            SubjectConfirmationType subjectConfirmation = subject.getSubjectConfirmation();

            if (subjectConfirmation != null) {
                String[] confMethods = subjectConfirmation.getConfirmationMethodArray();
                if (confMethods == null || confMethods.length != 1) {
                    throw new IllegalArgumentException("One and only one SubjectConfirmation/ConfirmationMethod must be present");
                }
                String confMethod = confMethods[0];
                if (confMethod != null) {
                    if (confMethod.indexOf("holder-of-key") >= 0) {
                        confirmationMethod = HOLDER_OF_KEY;
                    } else if (confMethod.indexOf("sender-vouches") >= 0) {
                        confirmationMethod = SENDER_VOUCHES;
                    } else {
                        String msg = "Could not determine the saml ConfirmationMethod (neither holder-of-key nor sender-vouches)";
                        logger.fine(msg);
                        confirmationMethod = null;
                    }
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
                            List strs = DomUtils.findChildElementsByName(keyInfoEl, SoapConstants.SECURITY_URIS_ARRAY, "SecurityTokenReference");
                            if (keyInfoEl != null && !strs.isEmpty()) {
                                try {
                                    KeyInfoElement kie = KeyInfoElement.parse((Element)keyInfo.getDomNode(), securityTokenResolver, KeyInfoInclusionType.ANY);
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
            Element signature = DomUtils.findOnlyOneChildElementByName(assertionElement, SoapConstants.DIGSIG_URI, "Signature");
            if (signature != null) {
                embeddedSignatureElement = signature;
                // Extract the issuer certificate
                Element keyinfo = DomUtils.findOnlyOneChildElementByName(signature, SoapConstants.DIGSIG_URI, "KeyInfo");
                if (keyinfo == null) throw new SAXException("SAML issuer signature has no KeyInfo");
                KeyInfoElement keyInfo = KeyInfoElement.parse(keyinfo, securityTokenResolver, KeyInfoInclusionType.ANY);
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
        return embeddedSignatureElement != null;
    }

    @Override
    public boolean hasSubjectConfirmationEncryptedKey() {
        try {
            return getSubjectConfirmationEncryptedKeyElement(false) != null;
        } catch (InvalidDocumentFormatException e) {
            // can't happen, passed false
            return false;
        }
    }

    public Element getEmbeddedIssuerSignature() {
        return embeddedSignatureElement;
    }

    @Override
    public EncryptedKey getSubjectConfirmationEncryptedKey(SecurityTokenResolver tokenResolver) throws InvalidDocumentFormatException, UnexpectedKeyInfoException, GeneralSecurityException {
        if (subjectConfirmationEncryptedKey != null) {
            return subjectConfirmationEncryptedKey;
        }

        Element encryptedKeyElement = getSubjectConfirmationEncryptedKeyElement(true);

        return subjectConfirmationEncryptedKey = new EncryptedKeyImpl(encryptedKeyElement, tokenResolver, SoapConstants.VALUETYPE_SAML_ASSERTIONID_SAML11, assertionId);
    }

    private Element getSubjectConfirmationEncryptedKeyElement(boolean failIfNotPresent) throws InvalidDocumentFormatException {
        Element samlElement = asElement();
        String samlNs = samlElement.getNamespaceURI();
        Element attrStatement = XmlUtil.findFirstChildElementByName(samlElement, samlNs, "AttributeStatement");
        Element authnStatement = XmlUtil.findFirstChildElementByName(samlElement, samlNs, "AuthenticationStatement");
        Element authzStatement = XmlUtil.findFirstChildElementByName(samlElement, samlNs, "AuthorizationDecisionStatement");

        if (attrStatement == null && authnStatement == null && authzStatement == null) {
            if (failIfNotPresent)
                throw new InvalidDocumentFormatException("DerivedKey KeyIdentifier refers to a SAML 1.1 assertion, but the assertion does not contain an AttributeStatement, AuthenticationStatement, or AuthorizationDecisionStatement");
            return null;
        }

        int numStatements = 0;
        Element statement = null;
        if (attrStatement != null) {
            statement = attrStatement;
            numStatements++;
        }
        if (authnStatement != null) {
            statement = authnStatement;
            numStatements++;
        }
        if (authzStatement != null) {
            statement = authzStatement;
            numStatements++;
        }
        assert numStatements > 0;
        //noinspection ConstantConditions
        assert statement != null;
        if (numStatements > 1) {
            if (failIfNotPresent)
                throw new InvalidDocumentFormatException("DerivedKey KeyIdentifier refers to a SAML 1.1 assertion, but the assertion contains more than one AttributeStatement, AuthenticationStatement, or AuthorizationDecisionStatement");
            return null;
        }

        Element subject = XmlUtil.findOnlyOneChildElementByName(statement, samlNs, "Subject");
        if(subject == null) {
            if (failIfNotPresent)
                throw new InvalidDocumentFormatException("DerivedKey KeyIdentifier refers to a SAML assertion, but the assertion does not contain a Subject");
            return null;
        }

        Element subjectConfirmation = XmlUtil.findOnlyOneChildElementByName(subject, samlNs, "SubjectConfirmation");
        if(subjectConfirmation == null) {
            if (failIfNotPresent)
                throw new InvalidDocumentFormatException("DerivedKey KeyIdentifier refers to a SAML assertion, but the assertion does not contain a SubjectConfirmation");
            return null;
        }

        Element keyInfo = XmlUtil.findOnlyOneChildElementByName(subjectConfirmation, SoapConstants.DIGSIG_URI, "KeyInfo");
        if(keyInfo == null) {
            if (failIfNotPresent)
                throw new InvalidDocumentFormatException("DerivedKey KeyIdentifier refers to a SAML assertion, but the assertion does not contain a KeyInfo");
            return null;
        }

        Element encryptedKeyElement = XmlUtil.findOnlyOneChildElementByName(keyInfo, SoapConstants.XMLENC_NS, "EncryptedKey");
        if(encryptedKeyElement == null) {
            if (failIfNotPresent)
                throw new InvalidDocumentFormatException("DerivedKey KeyIdentifier refers to a SAML assertion, but the assertion does not contain an EncryptedKey");
            return null;
        }

        return encryptedKeyElement;
    }

    @Override
    public boolean isSubjectConfirmationEncryptedKeyAvailable() {
        return subjectConfirmationEncryptedKey != null;
    }

    /**
     * Get the subject certificate.
     *
     * <p>This will only be available when using holder of key. A HOK SAML
     * assertion should only be considered valid once possession of the
     * subject key has been proven.</p>
     *
     * @return The certificate or null if not holder of key
     */
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
        if (!hasEmbeddedIssuerSignature()) throw new IllegalStateException("May not verify signature; this assertion is not signed");

        try {
            Element signature = DomUtils.findOnlyOneChildElementByName(assertionElement, SoapConstants.DIGSIG_URI, "Signature");
            if (signature == null) throw new CausedSignatureException("missing Signature element"); // can't happen

            X509Certificate signingCert = getIssuerCertificate();
            if (signingCert == null) throw new CausedSignatureException("missing issuer certificate"); // can't happen
            PublicKey signingKey = signingCert.getPublicKey();

            // Validate signature
            final boolean[] resolvedAssertionId = new boolean[1];
            final SignatureContext sigContext = DsigUtil.createSignatureContextForValidation();
            sigContext.setIDResolver(new IDResolver() {
                public Element resolveID(Document doc, String s) {
                    if (!s.equals(getAssertionId()))
                        throw new ResolveIdException("SAML signature contains signedinfo reference to unexpected element ID \"" + s + "\"");
                    resolvedAssertionId[0] = true;
                    return assertionElement;
                }
            });
            WssProcessorAlgorithmFactory algFactory = new WssProcessorAlgorithmFactory();
            sigContext.setAlgorithmFactory(algFactory);

            KeyUsageChecker.requireActivity(KeyUsageActivity.verifyXml, signingCert);
            Validity validity = DsigUtil.verify(sigContext, signature, signingKey);

            if (!validity.getCoreValidity()) {
                throw new CausedSignatureException("Unable to verify signature of SAML assertion: Validity not achieved. " + DsigUtil.getInvalidSignatureMessage(validity));
            }

            if (!resolvedAssertionId[0]) {
                throw new CausedSignatureException("SAML assertion signature does not reference assertion.");
            }

            if (!algFactory.isSawEnvelopedTransform()) {
                throw new CausedSignatureException("SAML assertion signature has invalid transform (must be enveloped).");
            }

            if (validity.getNumberOfReferences() != 1) {
                throw new CausedSignatureException("SAML assertion signature has invalid number of references ("+validity.getNumberOfReferences()+").");
            }

        } catch ( TooManyChildElementsException e) {
            throw new CausedSignatureException(e);
        } catch (ResolveIdException e) {
            throw new CausedSignatureException(e);
        } catch (KeyUsageException e) {
            throw new CausedSignatureException(e);
        } catch (CertificateParsingException e) {
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

    public Calendar getIssueInstant() {
        return issueInstant;
    }


    public Calendar getStarts() {
        return starts;
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
            id = HexUtils.encodeBase64(HexUtils.getMd5Digest(new byte[][]{
                    getAssertionId().getBytes(Charsets.UTF8),
                    samlIssuerSubjectDn.getBytes(Charsets.UTF8),
                    assertion.getSignature().getSignatureValue().getByteArrayValue()
            }));
        }
        else {
            id = HexUtils.encodeBase64(HexUtils.getMd5Digest(new byte[][]{
                    getAssertionId().getBytes(Charsets.UTF8),
                    assertion.getIssuer().getBytes(Charsets.UTF8),
                    BigInteger.valueOf(safeGetTimeInMillis(assertion.getIssueInstant())).toByteArray(),
                    subjectId.getBytes(Charsets.UTF8),
                    safeToString(authenticationMethod).getBytes(Charsets.UTF8),
                    safeToString(confirmationMethod).getBytes(Charsets.UTF8)
            }));
        }

        return id;
    }

    private long safeGetTimeInMillis(Calendar calendar) {
        long time = 0;

        if (calendar != null) {
            time = calendar.getTimeInMillis();
        }

        return time;
    }

    private String safeToString(Object object) {
        String string = "";

        if (object != null) {
            string = object.toString();
        }

        return string;
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
