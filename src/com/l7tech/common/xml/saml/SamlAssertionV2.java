package com.l7tech.common.xml.saml;

import com.ibm.xml.dsig.IDResolver;
import com.ibm.xml.dsig.SignatureContext;
import com.ibm.xml.dsig.Validity;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.xml.KeyInfoElement;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.security.xml.processor.WssProcessorAlgorithmFactory;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TooManyChildElementsException;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.SignatureType;
import org.w3.x2000.x09.xmldsig.X509DataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML2.*;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saml Assertion for version 1.x
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public final class SamlAssertionV2 extends SamlAssertion {
    private static final Logger logger = Logger.getLogger(SamlAssertionV2.class.getName());

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
    private boolean oneTimeUse = false;
    private Calendar issueInstant = null;
    private Calendar starts = null;
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
    public SamlAssertionV2(Element ass,
                           SecurityTokenResolver securityTokenResolver) throws SAXException
    {
        super(ass);

        assertionElement = ass;
        assertionId = assertionElement.getAttribute("ID");
        if (assertionId == null || assertionId.length() < 1) {
            throw new SAXException("ID missing or empty");
        }

        try {
            assertion = AssertionDocument.Factory.parse(ass).getAssertion();

            SubjectType subject = assertion.getSubject();
            NameIDType nameIdentifier = subject.getNameID();
            if (nameIdentifier != null) {
                this.nameIdentifierFormat = nameIdentifier.getFormat();
                this.nameQualifier = nameIdentifier.getNameQualifier();
                this.nameIdentifierValue = nameIdentifier.getStringValue();
                this.subjectId = toString(nameIdentifier);
            }
            else {
                String msg = "Subject must have a NameID :\n" + XmlUtil.nodeToFormattedString(ass);
                logger.warning(msg);
                throw new SAXException(msg);
            }

            AuthnStatementType[] authenticationStatements = assertion.getAuthnStatementArray();
            if (authenticationStatements != null && authenticationStatements.length > 0) {
                int lastElementIndex = authenticationStatements.length - 1;
                AuthnStatementType authenticationStatement = authenticationStatements[lastElementIndex];
                AuthnContextType authnContext = authenticationStatement.getAuthnContext();
                authenticationMethod = authnContext.getAuthnContextClassRef();
            }

            if (subject == null) {
                String msg = "Could not find the subject in the assertion :\n" + XmlUtil.nodeToFormattedString(ass);
                logger.warning(msg);
                throw new SAXException(msg);
            }

            issueInstant = assertion.getIssueInstant();
            ConditionsType conditions = assertion.getConditions();
            if (conditions != null) {
                starts = conditions.getNotBefore();
                expires = conditions.getNotOnOrAfter();
                if (conditions.getOneTimeUseArray() != null &&
                    conditions.getOneTimeUseArray().length > 0) {
                    oneTimeUse = true;
                }
            }

            SubjectConfirmationType[] subjectConfirmations = subject.getSubjectConfirmationArray();

            if (subjectConfirmations != null && subjectConfirmations.length > 0) {
                if (subjectConfirmations.length > 1) {
                    throw new IllegalArgumentException("Only one SubjectConfirmation may be present");
                }

                String confMethod = subjectConfirmations[0].getMethod();
                if (SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY.equals(confMethod)) {
                    confirmationMethod = HOLDER_OF_KEY;
                } else if (SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES.equals(confMethod)) {
                    confirmationMethod = SENDER_VOUCHES;
                } else {
                    String msg = "Could not determine the saml ConfirmationMethod (neither holder-of-key nor sender-vouches)";
                    logger.info(msg);
                    confirmationMethod = null;
                }

                if(logger.isLoggable(Level.FINE))
                    logger.fine("Confirmation method is '"+confirmationMethod+"'.");

                SubjectConfirmationDataType subjectConfirmationData = subjectConfirmations[0].getSubjectConfirmationData();

                if (confirmationMethod == HOLDER_OF_KEY) {
                    if (subjectConfirmationData != null &&
                        ((Element)subjectConfirmationData.getDomNode()).getAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "type")
                                .equals("urn:KeyInfoConfirmationDataType")) {

                        if(logger.isLoggable(Level.FINE))
                            logger.fine("Processing KeyInfo confirmation.");

                        KeyInfoConfirmationDataType keyInfoConfirmationData = (KeyInfoConfirmationDataType)
                            subjectConfirmationData.changeType(KeyInfoConfirmationDataType.Factory.newInstance().schemaType());

                        KeyInfoType[] keyInfos = keyInfoConfirmationData.getKeyInfoArray();

                        if(logger.isLoggable(Level.FINE))
                            logger.fine("Got " + keyInfos.length + " KeyInfos.");

                        for (int i = 0; i < keyInfos.length; i++) {
                            KeyInfoType keyInfo = keyInfos[i];

                            if (subjectCertificate != null)
                                break; //TODO determine which cert to use?

                            X509DataType[] x509datas = keyInfo.getX509DataArray();
                            if (x509datas != null && x509datas.length > 0) {
                                X509DataType x509data = x509datas[0];
                                subjectCertificate = CertUtils.decodeCert(x509data.getX509CertificateArray(0));
                            } else {
                                if(logger.isLoggable(Level.FINE))
                                    logger.fine("Looking for STR.");

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
            }

            // Check if assertion is signed
            SignatureType signature = assertion.getSignature();
            if (signature != null) {
                hasEmbeddedSignature = true;
                // Extract the issuer certificate
                KeyInfoType keyInfo = signature.getKeyInfo();
                if (keyInfo == null) throw new SAXException("SAML issuer signature has no KeyInfo");
                KeyInfoElement keyInfoElement = KeyInfoElement.parse((Element)keyInfo.getDomNode(), securityTokenResolver, true);
                issuerCertificate = keyInfoElement.getCertificate();
            }

        } catch (XmlException e) {
            throw new SAXException(e);
        } catch (CertificateException e) {
            final String msg = "Certificate in SAML assertion could not be parsed";
            logger.log(Level.WARNING, msg, e);
            throw new SAXException(e);
        } catch (IOException e) {
            throw new SAXException("Invalid Base64 in SAML token issuer certificate", e);
        } catch (KeyInfoElement.MissingResolverException e) {
            throw new SAXException(e);
        }
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.SAML2_ASSERTION;
    }

    public int getVersionId() {
        return VERSION_2_0;
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
            SignatureContext sigContext = new SignatureContext();
            sigContext.setEntityResolver(XmlUtil.getXss4jEntityResolver());
            sigContext.setIDResolver(new IDResolver() {
                public Element resolveID(Document doc, String s) {
                    if (!s.equals(getAssertionId()))
                        throw new ResolveIdException("SAML signature contains signedinfo reference to unexpected element ID \"" + s + "\"");
                    resolvedAssertionId[0] = true;
                    return assertionElement;
                }
            });
            WssProcessorAlgorithmFactory algFactory = new WssProcessorAlgorithmFactory(null);
            sigContext.setAlgorithmFactory(algFactory);
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

            if (!algFactory.isSawEnvelopedTransform()) {
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
        return oneTimeUse;
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
            try {
                id = HexUtils.encodeBase64(HexUtils.getMd5Digest(new byte[][]{
                        getAssertionId().getBytes("UTF-8"),
                        samlIssuerSubjectDn.getBytes("UTF-8"),
                        BigInteger.valueOf(assertion.getIssueInstant().getTimeInMillis()).toByteArray(),
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
                        toString(assertion.getIssuer()).getBytes("UTF-8"),
                        BigInteger.valueOf(safeGetTimeInMillis(assertion.getIssueInstant())).toByteArray(),
                        subjectId.getBytes("UTF-8"),
                        safeToString(authenticationMethod).getBytes("UTF-8"),
                        safeToString(confirmationMethod).getBytes("UTF-8")
                }));
            }
            catch(UnsupportedEncodingException uee) {
                throw new IllegalStateException("Support for UTF-8 is required.");
            }
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

    private String toString(NameIDType nameIDType) {
        StringBuffer nameIdBuffer = new StringBuffer();

        if (nameIDType != null) {
            if(nameIDType.getFormat() != null) {
                nameIdBuffer.append(nameIDType.getFormat());
                nameIdBuffer.append(" ");
            }
            if(nameIDType.getNameQualifier() != null) {
                nameIdBuffer.append(nameIDType.getNameQualifier());
                nameIdBuffer.append(" ");
            }
            if(nameIDType.getSPNameQualifier() != null) {
                nameIdBuffer.append(nameIDType.getSPNameQualifier());
                nameIdBuffer.append(" ");
            }
            if(nameIDType.getSPProvidedID() != null) {
                nameIdBuffer.append(nameIDType.getSPProvidedID());
                nameIdBuffer.append(" ");
            }
            if(nameIDType.getStringValue() != null) {
                nameIdBuffer.append(nameIDType.getStringValue());
            }
        }

        return nameIdBuffer.toString().trim();
    }
}
