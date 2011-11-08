package com.l7tech.security.saml;

import com.ibm.dom.util.IndentConfig;
import com.ibm.xml.dsig.*;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.security.xml.*;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.DecoratorException;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.security.xml.processor.WssProcessorAlgorithmFactory;
import com.l7tech.util.*;
import com.l7tech.xml.MessageNotSoapException;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.InetAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.TimeZone;

/**
 * Class <code>SamlAssertionGenerator</code> is a central entry point
 * for generating saml messages and attaching them to soap messages.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SamlAssertionGenerator {
    public static final String BEFORE_OFFSET_SYSTEM_PROPERTY = "saml.before.minute.offset.l7tech.com";
    public static final String AFTER_OFFSET_SYSTEM_PROPERTY = "saml.after.minute.offset.l7tech.com";
    public static final String SUBJECT_ENABLE_DNS_SYSTEM_PROPERTY = "com.l7tech.security.saml.enableDNS";

    static final TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
    private static final String DEFAULT_PREFIX = "SamlAssertion";
    private static final SecureRandom random = new SecureRandom();
    private final SignerInfo assertionSigner;
    private final SamlAssertionGeneratorSaml1 sag1;
    private final SamlAssertionGeneratorSaml2 sag2;
    private static final IndentConfig nullIndentConfig = new IndentConfig() {
        @Override
        public boolean doIndentation() {
            return false;
        }

        @Override
        public int getUnit() {
            return 0;
        }
    };

    /**
     * Instantiate the <code>SamlAssertionGenerator</code> with the assertion
     * signer (Issuing Authority). If assertion signer is null no assertion signing
     * will be performed
     *
     * @param assertionSigner the assertion signer signer
     */
    public SamlAssertionGenerator(SignerInfo assertionSigner) {
        this.assertionSigner = assertionSigner;
        this.sag1 = new SamlAssertionGeneratorSaml1();
        this.sag2 = new SamlAssertionGeneratorSaml2();
    }

    /**
     * Create and return the SAML Authentication Statement assertion The SAML assertion
     * is signed by assertion signer in this Assertion Generator.
     *
     * @param subjectStatement the subject the statement is about
     * @param options the options
     * @return the holder of key assertion for the
     * @throws SignatureException   on signature related error
     * @throws CertificateException on certificate error
     */
    public Document createAssertion(SubjectStatement subjectStatement, Options options)
            throws SignatureException, CertificateException, UnrecoverableKeyException {
        final String caDn = assertionSigner.getCertificateChain()[0].getSubjectDN().getName();

        Document doc = options.getVersion()==Options.VERSION_1 ?
                sag1.createStatementDocument(subjectStatement, options, caDn) :
                sag2.createStatementDocument(subjectStatement, options, caDn);

        if (options.isSignAssertion()) signAssertion(
                options,
                doc,
                assertionSigner.getPrivate(),
                assertionSigner.getCertificateChain(),
                options.getIssuerKeyInfoType());
        return doc;
    }

    /**
     * Create and return the SAML Authentication Statement assertion The SAML assertion
     * is signed by assertion signer in this Assertion Generator.
     *
     * @param statements the Subject Statement(s) to include in the assertion
     * @param options the options
     * @return the holder of key assertion for the
     * @throws SignatureException   on signature related error
     * @throws CertificateException on certificate error
     */
    public Document createAssertion(SubjectStatement[] statements, Options options)
            throws SignatureException, CertificateException, UnrecoverableKeyException {
        final String caDn = assertionSigner.getCertificateChain()[0].getSubjectDN().getName();

        Document doc = options.getVersion()==Options.VERSION_1 ?
                sag1.createStatementDocument(statements, options, caDn) :
                sag2.createStatementDocument(statements, options, caDn);

        if (options.isSignAssertion()) signAssertion(
                options,
                doc,
                assertionSigner.getPrivate(),
                assertionSigner.getCertificateChain(),
                options.getIssuerKeyInfoType());
        return doc;
    }

    /**
     * Create and attach the SAML Authentication Statement to the assertion The SAML assertion
     * is signed by assertion signer in this Assertion Generator.
     *
     * @param document the soap message the subject the statement is about
     * @param subject  the subject the statement is about
     * @param options  the options
     * @return the document representing the authentication assertion
     * @throws SignatureException   on signature related error
     * @throws CertificateException on certificate error
     */
    public Document attachStatement(Document document, SubjectStatement subject, Options options)
            throws SignatureException, CertificateException, UnrecoverableKeyException {
        final String caDn = assertionSigner.getCertificateChain()[0].getSubjectDN().getName();

        Document doc = options.getVersion()==Options.VERSION_1 ?
                sag1.createStatementDocument(subject, options, caDn) :
                sag2.createStatementDocument(subject, options, caDn);

        // sign only if requested and if the confirmation is holder of key.
        // according to WSS SAML interop scenarios the sender vouches is not signed
        if (options.isSignAssertion() && subject.isConfirmationHolderOfKey()) {
            signAssertion(options,
                    doc,
                    assertionSigner.getPrivate(),
                    assertionSigner.getCertificateChain(),
                    options.getIssuerKeyInfoType());
        }
        attachAssertion(document, doc, options);
        return doc;
    }

    /**
     *
     */
    protected void attachAssertion(Document soapMessage, Document assertionDoc, Options options)
      throws SignatureException, CertificateException {

        try {
            Element bodyElement = SoapUtil.getBodyElement(soapMessage);
            if (bodyElement == null) {
                throw new MessageNotSoapException();
            }
            WssDecorator wssDecorator = new WssDecoratorImpl();
            DecorationRequirements dr = new DecorationRequirements();
            final Set<Element> elementsToSign = dr.getElementsToSign();
            if (options.isProofOfPosessionRequired()) {
                final SignerInfo attestingEntity = options.getAttestingEntity();
                if (attestingEntity == null) {
                    throw new IllegalArgumentException("Proof Of possession required, without attesting entity keys");
                }
                elementsToSign.add(bodyElement);
                dr.setSenderSamlToken(SamlAssertion.newInstance(assertionDoc.getDocumentElement()));
                dr.setSenderMessageSigningPrivateKey(attestingEntity.getPrivate());
                dr.setSenderMessageSigningCertificate(attestingEntity.getCertificateChain()[0]);
            }
            dr.setSecurityHeaderActor(options.getSecurityHeaderActor());
            wssDecorator.decorateMessage(new Message(soapMessage), dr);
        } catch (SignatureException e) {
            throw e;
        } catch (CertificateException e) {
            throw e;
        } catch (DecoratorException e) {
            throw new SignatureException("Error signing the SAML ticket: " + ExceptionUtils.getMessage(e), e);
        } catch (MessageNotSoapException e) {
            throw new SignatureException("Error signing the SAML ticket: " + ExceptionUtils.getMessage(e), e);
        } catch (InvalidDocumentFormatException e) {
            throw new SignatureException("Error signing the SAML ticket: " + ExceptionUtils.getMessage(e), e);
        } catch (IOException e) {
            throw new SignatureException("Error signing the SAML ticket: " + ExceptionUtils.getMessage(e), e);
        } catch (GeneralSecurityException e) {
            throw new SignatureException("Error signing the SAML ticket: " + ExceptionUtils.getMessage(e), e);
        } catch (SAXException e) {
            throw new SignatureException("Error signing the SAML ticket: " + ExceptionUtils.getMessage(e), e);
        }
    }


    public static void signAssertion(final Options options,
                                     final Document assertionDoc,
                                     final PrivateKey signingKey,
                                     final X509Certificate[] signingCertChain,
                                     final KeyInfoInclusionType keyInfoType)
            throws SignatureException
    {
        SupportedSignatureMethods signaturemethod = DsigUtil.getSignatureMethod(signingKey);

        // Create signature template and populate with appropriate transforms. Reference is to SOAP Envelope
        TemplateGenerator template = new TemplateGenerator(assertionDoc,
                                                           SupportedDigestMethods.fromAlias(signaturemethod.getDigestAlgorithmName()).getIdentifier(),
                                                           Canonicalizer.EXCLUSIVE,
                                                           signaturemethod.getAlgorithmIdentifier());

        String idAttr = options.getVersion()==Options.VERSION_1 ?
                SamlConstants.ATTR_ASSERTION_ID:
                SamlConstants.ATTR_SAML2_ASSERTION_ID;

        final String id = assertionDoc.getDocumentElement().getAttribute(idAttr);
        template.setPrefix("ds");
        template.setIndentation(false);
        Reference ref = template.createReference("#" + id);
        ref.addTransform(Transform.ENVELOPED);
        ref.addTransform(Transform.C14N_EXCLUSIVE);
        template.addReference(ref);

        SignatureContext context = new SignatureContext();
        context.setAlgorithmFactory(new WssProcessorAlgorithmFactory());
        context.setEntityResolver( XmlUtil.getXss4jEntityResolver());
        context.setIDResolver(new IDResolver() {
            @Override
            public Element resolveID(Document document, String s) {
                if (id.equals(s))
                    return assertionDoc.getDocumentElement();
                else
                    throw new IllegalArgumentException("I don't know how to find " + s);
            }
        });

        final Element signatureElement = template.getSignatureElement();
        // Ensure that CanonicalizationMethod has required c14n subelement
        Element signedInfoElement = template.getSignedInfoElement();
        Element c14nMethod = DomUtils.findFirstChildElementByName(signedInfoElement,
                                                                 SoapConstants.DIGSIG_URI,
                                                                 "CanonicalizationMethod");
        DsigUtil.addInclusiveNamespacesToElement(c14nMethod);

        // Ensure that any Transform has required c14n subelement
        NodeList transforms = signedInfoElement.getElementsByTagNameNS(signedInfoElement.getNamespaceURI(), "Transform");
        for (int i = 0; i < transforms.getLength(); ++i)
            if (Transform.C14N_EXCLUSIVE.equals(((Element)transforms.item(i)).getAttribute("Algorithm")))
                DsigUtil.addInclusiveNamespacesToElement((Element)transforms.item(i));

        if (options.getVersion()==Options.VERSION_1) {
            assertionDoc.getDocumentElement().appendChild(signatureElement);
        }
        else {
            try {
                Element docElement = assertionDoc.getDocumentElement();
                Element sigSibling = DomUtils.findOnlyOneChildElementByName(docElement,
                        SamlConstants.NS_SAML2,
                        SamlConstants.ELEMENT_ISSUER);
                if (sigSibling == null)
                    throw new IllegalArgumentException("Invalid SAML Assertion (no Issuer)");

                docElement.insertBefore(signatureElement, DomUtils.findNextElementSibling(sigSibling));
            }
            catch(TooManyChildElementsException tmcee) {
                throw new IllegalArgumentException("Invalid SAML Assertion (multiple Issuers)");
            }
        }

        KeyInfo keyInfo = new KeyInfo();
        Element keyInfoElement;
        switch(keyInfoType) {
            case STR_THUMBPRINT:
                keyInfoElement = keyInfo.getKeyInfoElement(assertionDoc, nullIndentConfig);
                // Replace cert with STR?
                try {
                    String thumb = CertUtils.getThumbprintSHA1(signingCertChain[0]);
                    KeyInfoDetails.makeKeyId(thumb, true, SoapConstants.VALUETYPE_X509_THUMB_SHA1).
                            populateExistingKeyInfoElement(new NamespaceFactory(), keyInfoElement);
                } catch (Exception e) {
                    throw new SignatureException(e);
                }
                break;
            case STR_SKI:
                keyInfoElement = keyInfo.getKeyInfoElement(assertionDoc, nullIndentConfig);
                // Replace cert with STR?
                try {
                    String thumb = CertUtils.getSki(signingCertChain[0]);
                    KeyInfoDetails.makeKeyId(thumb, true, SoapConstants.VALUETYPE_SKI).
                            populateExistingKeyInfoElement(new NamespaceFactory(), keyInfoElement);
                } catch (Exception e) {
                    throw new SignatureException(e);
                }
                break;
            case CERT:
                KeyInfo.X509Data x509 = new KeyInfo.X509Data();
                x509.setCertificate(signingCertChain[0]);
                x509.setParameters(signingCertChain[0], false, false, true);
                keyInfo.setX509Data(new KeyInfo.X509Data[]{x509});
                keyInfoElement = keyInfo.getKeyInfoElement(assertionDoc, nullIndentConfig);
                break;
            case NONE:
            default:
                throw new IllegalArgumentException("KeyInfoType must be CERT, STR_THUMBPRINT or STR_SKI");
        }

        keyInfoElement.setAttributeNS(DomUtils.XMLNS_NS, "xmlns", SoapConstants.DIGSIG_URI);
        signatureElement.appendChild(keyInfoElement);

        try {
            context.sign(signatureElement, signingKey);
        } catch (XSignatureException e) {
            DsigUtil.repairXSignatureException(e);
            throw new SignatureException(e.getMessage(), e);
        }
    }

    public static String generateAssertionId(String prefix) {
        byte[] disambig = new byte[16];
        random.nextBytes(disambig);
        return (prefix != null ? prefix : DEFAULT_PREFIX) + "-" + HexUtils.hexDump(disambig);
    }

    public static class Options {
        public static final int VERSION_1 = 1;
        public static final int VERSION_2 = 2;

        /**
         * Get the not before seconds.
         *
         * <p>A value of -1 means the attribute should be omitted.</p>
         *
         * @return The offset from the current time in seconds.
         */
        public int getNotBeforeSeconds() {
            return notBeforeSeconds;
        }

        public void setNotBeforeSeconds(int notBeforeSeconds) {
            this.notBeforeSeconds = notBeforeSeconds;
        }

        /**
         * Get the not on or after seconds.
         *
         * <p>A value of -1 means the attribute should be omitted.</p>
         *
         * @return The offset from the current time in seconds.
         */
        public int getNotAfterSeconds() {
            return notAfterSeconds;
        }

        public void setNotAfterSeconds(int notAfterSeconds) {
            this.notAfterSeconds = notAfterSeconds;
        }

        public boolean isProofOfPosessionRequired() {
            return proofOfPosessionRequired;
        }

        public void setProofOfPosessionRequired(boolean proofOfPosessionRequired) {
            this.proofOfPosessionRequired = proofOfPosessionRequired;
        }

        public boolean isSignAssertion() {
            return signAssertion;
        }

        public void setSignAssertion(boolean signAssertion) {
            this.signAssertion = signAssertion;
        }

        public InetAddress getClientAddress() {
            return clientAddress;
        }

        public void setClientAddress(InetAddress clientAddress) {
            this.clientAddress = clientAddress;
        }

        public boolean isClientAddressDNS() {
            return clientAddressDNS;
        }

        public void setClientAddressDNS( final boolean clientAddressDNS ) {
            this.clientAddressDNS = clientAddressDNS;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public SignerInfo getAttestingEntity() {
            return attestingEntity;
        }

        public void setAttestingEntity(SignerInfo attestingEntity) {
            this.attestingEntity = attestingEntity;
        }

        public int getVersion() {
            return samlVersion;
        }

        public void setVersion(int samlVersion) {
            this.samlVersion = samlVersion;
        }

        public String getSecurityHeaderActor() {
            return securityHeaderActor;
        }

        public void setSecurityHeaderActor(String securityHeaderActor) {
            this.securityHeaderActor = securityHeaderActor;
        }

        public KeyInfoInclusionType getIssuerKeyInfoType() {
            return issuerKeyInfoType;
        }

        public void setIssuerKeyInfoType(KeyInfoInclusionType issuerKeyInfoType) {
            this.issuerKeyInfoType = issuerKeyInfoType;
        }

        public String getAudienceRestriction() {
            return audienceRestriction;
        }

        public void setAudienceRestriction(String audienceRestriction) {
            this.audienceRestriction = audienceRestriction;
        }

        public String getSubjectConfirmationDataRecipient() {
            return subjectConfirmationDataRecipient;
        }

        public void setSubjectConfirmationDataRecipient( final String subjectConfirmationDataRecipient ) {
            this.subjectConfirmationDataRecipient = subjectConfirmationDataRecipient;
        }

        public String getSubjectConfirmationDataAddress() {
            return subjectConfirmationDataAddress;
        }

        public void setSubjectConfirmationDataAddress( final String subjectConfirmationDataAddress ) {
            this.subjectConfirmationDataAddress = subjectConfirmationDataAddress;
        }

        public String getSubjectConfirmationDataInResponseTo() {
            return subjectConfirmationDataInResponseTo;
        }

        public void setSubjectConfirmationDataInResponseTo( final String subjectConfirmationDataInResponseTo ) {
            this.subjectConfirmationDataInResponseTo = subjectConfirmationDataInResponseTo;
        }

        /**
         * Get the not before seconds for the subject confirmation data.
         *
         * <p>A value of -1 means the attribute should be omitted.</p>
         *
         * @return The offset from the current time in seconds.
         */
        public int getSubjectConfirmationDataNotBeforeSecondsInPast() {
            return subjectConfirmationDataNotBeforeSecondsInPast;
        }

        public void setSubjectConfirmationDataNotBeforeSecondsInPast( final int subjectConfirmationDataNotBeforeSecondsInPast ) {
            this.subjectConfirmationDataNotBeforeSecondsInPast = subjectConfirmationDataNotBeforeSecondsInPast;
        }

        /**
         * Get the not on or after seconds for the subject confirmation data.
         *
         * <p>A value of -1 means the attribute should be omitted.</p>
         *
         * @return The offset from the current time in seconds.
         */
        public int getSubjectConfirmationDataNotOnOrAfterExpirySeconds() {
            return subjectConfirmationDataNotOnOrAfterExpirySeconds;
        }

        public void setSubjectConfirmationDataNotOnOrAfterExpirySeconds( final int subjectConfirmationDataNotOnOrAfterExpirySeconds ) {
            this.subjectConfirmationDataNotOnOrAfterExpirySeconds = subjectConfirmationDataNotOnOrAfterExpirySeconds;
        }

        private KeyInfoInclusionType issuerKeyInfoType = KeyInfoInclusionType.CERT;
        private boolean proofOfPosessionRequired = true;
        private int notBeforeSeconds = DEFAULT_NOT_BEFORE_SECONDS;
        private int notAfterSeconds = DEFAULT_NOT_AFTER_SECONDS;
        private InetAddress clientAddress;
        private boolean clientAddressDNS = ConfigFactory.getBooleanProperty( SUBJECT_ENABLE_DNS_SYSTEM_PROPERTY, false );
        private boolean signAssertion = true;
        private SignerInfo attestingEntity;
        private String id = null;
        private int samlVersion = VERSION_1;
        private String securityHeaderActor = null;
        private String audienceRestriction = null;
        private String subjectConfirmationDataRecipient;
        private String subjectConfirmationDataAddress;
        private String subjectConfirmationDataInResponseTo;
        private int subjectConfirmationDataNotBeforeSecondsInPast = -1;
        private int subjectConfirmationDataNotOnOrAfterExpirySeconds = -1;
    }

    static final int DEFAULT_NOT_BEFORE_SECONDS = 120;
    static final int DEFAULT_NOT_AFTER_SECONDS = 300;
}
