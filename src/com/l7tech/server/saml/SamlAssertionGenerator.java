package com.l7tech.server.saml;

import com.ibm.xml.dsig.*;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.WssDecorator;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.MessageNotSoapException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.w3.x2000.x09.xmldsig.KeyInfoType;
import org.w3.x2000.x09.xmldsig.X509DataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.*;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

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

    /**
     * Instantiate the <code>SamlAssertionGenerator</code> with the assertion
     * signer (Issuing Authority). If assertion signer is null no assertion signing
     * will be performed
     *
     * @param assertionSigner the assertion signer signer
     */
    public SamlAssertionGenerator(SignerInfo assertionSigner) {
        this.assertionSigner = assertionSigner;
    }

    /**
     * Create and return the SAML Authentication Statement assertion The SAML assertion
     * is signed by assertion signer in this Assertion Generator.
     *
     * @param subject the subject the statement is about
     * @param options the options
     * @return the holder of key assertion for the
     * @throws IOException          on io error
     * @throws SignatureException   on signature related error
     * @throws SAXException         on xml parsing error
     * @throws CertificateException on certificate error
     */
    public Document createAssertion(SubjectStatement subject, Options options)
      throws IOException, SignatureException, SAXException, CertificateException {

        Document doc = assertionToDocument(createStatementType(subject, options));
        if (options.isSignAssertion()) signAssertion(doc, assertionSigner.getPrivate(), assertionSigner.getCertificateChain());
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
     * @throws IOException          on io error
     * @throws SignatureException   on signature related error
     * @throws SAXException         on xml parsing error
     * @throws CertificateException on certificate error
     */
    public Document attachStatement(Document document, SubjectStatement subject, Options options)
      throws IOException, SignatureException, SAXException, CertificateException {
        Document doc = assertionToDocument(createStatementType(subject, options));
        // sign only if requested and if the confirmation is holder of key.
        // according to WSS SAML interop scenarios the sender vouches is not signed
        if (options.isSignAssertion() && subject.isConfirmationHolderOfKey()) {
            signAssertion(doc, assertionSigner.getPrivate(), assertionSigner.getCertificateChain());
        }
        attachAssertion(document, doc, options);
        return doc;
    }

    /**
     * Create the statement depending on the SubjectStatement subclass and populating the subject
     *
     * @param subjectStatement the subject statament subclass (authenticatiom, authorization, attribute
     *                         statement
     * @param options          the options, with expiry minutes
     * @return the assertion type containing the requested statement
     * @throws IOException          on io error
     * @throws SignatureException
     * @throws SAXException
     * @throws CertificateException
     */
    protected AssertionType createStatementType(SubjectStatement subjectStatement, Options options)
      throws IOException, SignatureException, SAXException, CertificateException {
        Calendar now = Calendar.getInstance(utcTimeZone);
        AssertionType assertionType = getGenericAssertion(now, options.getExpiryMinutes(),
                                                          options.getId() != null ? options.getId() : generateAssertionId(null));
        SubjectStatementAbstractType subjectStatementAbstractType = null;

        if (subjectStatement instanceof AuthenticationStatement) {
            AuthenticationStatement as = (AuthenticationStatement)subjectStatement;
            AuthenticationStatementType authStatement = assertionType.addNewAuthenticationStatement();
            authStatement.setAuthenticationMethod(as.getAuthenticationMethod());

            InetAddress clientAddress = options.getClientAddress();
            if (clientAddress != null) {
                final SubjectLocalityType subjectLocality = authStatement.addNewSubjectLocality();
                subjectLocality.setIPAddress(clientAddress.getHostAddress());
                subjectLocality.setDNSAddress(clientAddress.getCanonicalHostName());
            }
            subjectStatementAbstractType = authStatement;
        } else if (subjectStatement instanceof AuthorizationStatement) {
            AuthorizationStatement as = (AuthorizationStatement)subjectStatement;
            AuthorizationDecisionStatementType atzStatement = assertionType.addNewAuthorizationDecisionStatement();
            atzStatement.setResource(as.getResource());
            if (as.getAction() != null) {
                ActionType actionType = atzStatement.addNewAction();
                actionType.setStringValue(as.getAction());
                if (as.getActionNamespace() != null) {
                    actionType.setNamespace(as.getActionNamespace());
                }
            }
            subjectStatementAbstractType = atzStatement;

        } else if (subjectStatement instanceof AttributeStatement) {
            AttributeStatement as = (AttributeStatement)subjectStatement;
            AttributeStatementType attStatement = assertionType.addNewAttributeStatement();
            AttributeStatement.Attribute[] attributes = as.getAttributes();
            for (int i = 0; i < attributes.length; i++) {
                AttributeStatement.Attribute attribute = attributes[i];
                AttributeType attributeType = attStatement.addNewAttribute();
                attributeType.setAttributeName(attribute.getName());
                if (attribute.getNameSpace() != null) {
                    attributeType.setAttributeNamespace(attribute.getNameSpace());
                }
                XmlObject attributeValue = attributeType.addNewAttributeValue();
                attributeValue.set(XmlObject.Factory.newValue(attribute.getValue()));
            }
            subjectStatementAbstractType = attStatement;
        } else {
            throw new IllegalArgumentException("Unknown statement class " + subjectStatement.getClass());
        }

        populateSubjectStatement(subjectStatementAbstractType, subjectStatement);
        return assertionType;
    }

    /**
     * Populate the subject statement assertion properties such as subject name, name qualifier
     *
     * @param subjectStatementAbstractType the subject statement abstract type
     * @param subjectStatement
     * @throws CertificateEncodingException on certificate error
     */
    protected void populateSubjectStatement(SubjectStatementAbstractType subjectStatementAbstractType,
                                            SubjectStatement subjectStatement) throws CertificateEncodingException {

        SubjectType subjectStatementType = subjectStatementAbstractType.addNewSubject();
        NameIdentifierType nameIdentifierType = subjectStatementType.addNewNameIdentifier();
        nameIdentifierType.setStringValue(subjectStatement.getName());
        if (subjectStatement.getNameFormat() != null) {
            nameIdentifierType.setFormat(subjectStatement.getNameFormat());
        }
        if (subjectStatement.getNameQualifier() != null) {
            nameIdentifierType.setNameQualifier(subjectStatement.getNameQualifier());
        }

        SubjectConfirmationType subjectConfirmation = subjectStatementType.addNewSubjectConfirmation();
        subjectConfirmation.addConfirmationMethod(subjectStatement.getConfirmationMethod());

        final Object keyInfo = subjectStatement.getKeyInfo();
        if (keyInfo == null || !(keyInfo instanceof X509Certificate)) {
            return;
        }
        KeyInfoType keyInfoType = subjectConfirmation.addNewKeyInfo();
        X509DataType x509Data = keyInfoType.addNewX509Data();
        x509Data.addX509Certificate(((X509Certificate)keyInfo).getEncoded());
    }

    /**
     * @param options
     */
    protected void attachAssertion(Document soapMessage, Document assertionDoc, Options options)
      throws IOException, SAXException, SignatureException, CertificateException {

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
                dr.setSenderPrivateKey(attestingEntity.getPrivate());
                dr.setSenderCertificate(attestingEntity.getCertificateChain()[0]);
            }
            wssDecorator.decorateMessage(soapMessage, dr);
        } catch (Throwable e) {
            SignatureException ex = new SignatureException("error signing the saml ticket");
            ex.initCause(e);
            throw ex;
        }
    }


    public static void signAssertion(final Document assertionDoc, PrivateKey signingKey,
                                     X509Certificate[] signingCertChain) throws SignatureException {
        TemplateGenerator template = new TemplateGenerator(assertionDoc, XSignature.SHA1,
                                                           Canonicalizer.EXCLUSIVE, SignatureMethod.RSA);
        final String id = assertionDoc.getDocumentElement().getAttribute(SamlConstants.ATTR_ASSERTION_ID);
        template.setPrefix("ds");
        Reference ref = template.createReference("#" + id);
        ref.addTransform(Transform.ENVELOPED);
        ref.addTransform(Transform.C14N_EXCLUSIVE);
        template.addReference(ref);

        SignatureContext context = new SignatureContext();
        context.setIDResolver(new IDResolver() {
            public Element resolveID(Document document, String s) {
                if (id.equals(s))
                    return assertionDoc.getDocumentElement();
                else
                    throw new IllegalArgumentException("I don't know how to find " + s);
            }
        });

        final Element signatureElement = template.getSignatureElement();
        assertionDoc.getDocumentElement().appendChild(signatureElement);
        KeyInfo keyInfo = new KeyInfo();
        KeyInfo.X509Data x509 = new KeyInfo.X509Data();
        x509.setCertificate(signingCertChain[0]);
        x509.setParameters(signingCertChain[0], false, false, true);
        keyInfo.setX509Data(new KeyInfo.X509Data[]{x509});

        final Element keyInfoElement = keyInfo.getKeyInfoElement(assertionDoc);
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

    protected AssertionType getGenericAssertion(Calendar now, int expiryMinutes, String assertionId) {
        final String caDn = assertionSigner.getCertificateChain()[0].getSubjectDN().getName();
        Map caMap = CertUtils.dnToAttributeMap(caDn);
        String caCn = (String)((List)caMap.get("CN")).get(0);

        AssertionType assertion = AssertionType.Factory.newInstance();

        assertion.setMinorVersion(BigInteger.ONE);
        assertion.setMajorVersion(BigInteger.ONE);
        if (assertionId == null)
            assertion.setAssertionID(generateAssertionId(null));
        else
            assertion.setAssertionID(assertionId);
        assertion.setIssuer(caCn);
        assertion.setIssueInstant(now);

        ConditionsType ct = ConditionsType.Factory.newInstance();
        Calendar calendar = Calendar.getInstance(utcTimeZone);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        ct.setNotBefore(calendar);
        Calendar c2 = (Calendar)calendar.clone();
        c2.roll(Calendar.MINUTE, expiryMinutes);
        ct.setNotOnOrAfter(c2);
        assertion.setConditions(ct);
        return assertion;
    }

    protected static Document assertionToDocument(AssertionType assertion)
      throws IOException, SAXException {
        AssertionDocument assertionDocument = AssertionDocument.Factory.newInstance();
        StringWriter sw = new StringWriter();
        assertionDocument.setAssertion(assertion);

        XmlOptions xo = new XmlOptions();
        Map namespaces = new HashMap();
        namespaces.put(SamlConstants.NS_SAML, SamlConstants.NS_SAML_PREFIX);
        xo.setSaveSuggestedPrefixes(namespaces);
        assertionDocument.save(sw, xo);
        return XmlUtil.stringToDocument(sw.toString());
    }


    public static class Options {
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

        boolean proofOfPosessionRequired = true;
        int expiryMinutes = DEFAULT_EXPIRY_MINUTES;
        InetAddress clientAddress;
        boolean signAssertion = true;
        SignerInfo attestingEntity;
        String id = null;
    }

    static final int DEFAULT_EXPIRY_MINUTES = 5;
}
