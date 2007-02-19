package com.l7tech.common.security.saml;

import com.ibm.xml.dsig.*;
import com.l7tech.common.security.xml.DsigUtil;
import com.l7tech.common.security.xml.KeyInfoDetails;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.util.*;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.common.xml.TooManyChildElementsException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
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
    static final TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
    private static final SecureRandom random = new SecureRandom();
    private final SignerInfo assertionSigner;
    private final SamlAssertionGeneratorSaml1 sag1;
    private final SamlAssertionGeneratorSaml2 sag2;

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
     * @param subject the subject the statement is about
     * @param options the options
     * @return the holder of key assertion for the
     * @throws SignatureException   on signature related error
     * @throws CertificateException on certificate error
     */
    public Document createAssertion(SubjectStatement subject, Options options)
      throws SignatureException, CertificateException {
        final String caDn = assertionSigner.getCertificateChain()[0].getSubjectDN().getName();

        Document doc = options.getVersion()==Options.VERSION_1 ?
                sag1.createStatementDocument(subject, options, caDn) :
                sag2.createStatementDocument(subject, options, caDn);

        if (options.isSignAssertion()) signAssertion(
                options,
                doc,
                assertionSigner.getPrivate(),
                assertionSigner.getCertificateChain(),
                options.isUseThumbprintForSignature());
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
      throws SignatureException, CertificateException {
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
                    options.isUseThumbprintForSignature());
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
            final Set elementsToSign = dr.getElementsToSign();
            if (options.isProofOfPosessionRequired()) {
                final SignerInfo attestingEntity = options.getAttestingEntity();
                if (attestingEntity == null) {
                    throw new IllegalArgumentException("Proof Of posession required, without attesting entity keys");
                }
                elementsToSign.add(bodyElement);
                dr.setSenderSamlToken(assertionDoc.getDocumentElement());
                dr.setSenderMessageSigningPrivateKey(attestingEntity.getPrivate());
                dr.setSenderMessageSigningCertificate(attestingEntity.getCertificateChain()[0]);
            }
            wssDecorator.decorateMessage(soapMessage, dr);
        } catch (Throwable e) {
            SignatureException ex = new SignatureException("error signing the saml ticket");
            ex.initCause(e);
            throw ex;
        }
    }


    public static void signAssertion(final Options options,
                                     final Document assertionDoc,
                                     final PrivateKey signingKey,
                                     final X509Certificate[] signingCertChain,
                                     final boolean useThumbprintForSignature)
            throws SignatureException
    {
        TemplateGenerator template = new TemplateGenerator(assertionDoc, XSignature.SHA1,
                                                           Canonicalizer.EXCLUSIVE, SignatureMethod.RSA);

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
        context.setEntityResolver(XmlUtil.getXss4jEntityResolver());
        context.setIDResolver(new IDResolver() {
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
        Element c14nMethod = XmlUtil.findFirstChildElementByName(signedInfoElement,
                                                                 SoapUtil.DIGSIG_URI,
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
                Element sigSibling = XmlUtil.findOnlyOneChildElementByName(docElement,
                        SamlConstants.NS_SAML2,
                        SamlConstants.ELEMENT_ISSUER);
                if (sigSibling == null)
                    throw new IllegalArgumentException("Invalid SAML Assertion (no Issuer)");

                docElement.insertBefore(signatureElement, XmlUtil.findNextElementSibling(sigSibling));
            }
            catch(TooManyChildElementsException tmcee) {
                throw new IllegalArgumentException("Invalid SAML Assertion (multiple Issuers)");
            }
        }

        KeyInfo keyInfo = new KeyInfo();
        Element keyInfoElement;
        if (useThumbprintForSignature) {
            keyInfoElement = keyInfo.getKeyInfoElement(assertionDoc);
            // Replace cert with STR?
            try {
                String thumb = CertUtils.getThumbprintSHA1(signingCertChain[0]);
                KeyInfoDetails.makeKeyId(thumb, true, SoapUtil.VALUETYPE_X509_THUMB_SHA1).
                        populateExistingKeyInfoElement(new NamespaceFactory(), keyInfoElement);
            } catch (Exception e) {
                throw new SignatureException(e);
            }
        } else {
            KeyInfo.X509Data x509 = new KeyInfo.X509Data();
            x509.setCertificate(signingCertChain[0]);
            x509.setParameters(signingCertChain[0], false, false, true);
            keyInfo.setX509Data(new KeyInfo.X509Data[]{x509});
            keyInfoElement = keyInfo.getKeyInfoElement(assertionDoc);
        }

        keyInfoElement.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns", SoapUtil.DIGSIG_URI);
        signatureElement.appendChild(keyInfoElement);

        try {
            context.sign(signatureElement, signingKey);
        } catch (XSignatureException e) {
            throw new SignatureException(e.getMessage());
        }
    }

    public static String generateAssertionId(String prefix) {
        if (prefix == null) prefix = "SamlAssertion";
        byte[] disambig = new byte[16];
        random.nextBytes(disambig);
        return prefix + "-" + HexUtils.hexDump(disambig);
    }

    public static class Options {
        public static int VERSION_1 = 1;
        public static int VERSION_2 = 2;

        public int getExpiryMinutes() {
            return expiryMinutes;
        }

        public void setExpiryMinutes(int expiryMinutes) {
            this.expiryMinutes = expiryMinutes;
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

        public boolean isUseThumbprintForSignature() {
            return useThumbprintForSignature;
        }

        public void setUseThumbprintForSignature(boolean useThumbprintForSignature) {
            this.useThumbprintForSignature = useThumbprintForSignature;
        }

        public int getVersion() {
            return samlVersion;
        }

        public void setVersion(int samlVersion) {
            this.samlVersion = samlVersion;
        }

        private boolean useThumbprintForSignature = false;
        private boolean proofOfPosessionRequired = true;
        private int expiryMinutes = DEFAULT_EXPIRY_MINUTES;
        private InetAddress clientAddress;
        private boolean signAssertion = true;
        private SignerInfo attestingEntity;
        private String id = null;
        private int samlVersion = VERSION_1;
    }

    static final int DEFAULT_EXPIRY_MINUTES = 5;
}
