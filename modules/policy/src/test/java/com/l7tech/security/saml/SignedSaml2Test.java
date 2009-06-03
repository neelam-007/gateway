package com.l7tech.security.saml;

import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.TestDocuments;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 *
 */
public class SignedSaml2Test extends TestCase {

    /**
     * test <code>SignedSamlTest</code> constructor
     */
    public SignedSaml2Test(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the SignedSaml2Test <code>TestCase</code>
     */
    public static Test suite() {
        return new TestSuite(SignedSaml2Test.class);
    }

    @Override
    public void setUp() throws Exception {
        caPrivateKey =  TestDocuments.getEttkServerPrivateKey();
        caCertChain =  new X509Certificate[]{TestDocuments.getEttkServerCertificate()};
        caPublicKey = caCertChain[0].getPublicKey();

        clientPrivateKey =  TestDocuments.getEttkClientPrivateKey();
        clientCertChain =  new X509Certificate[]{TestDocuments.getEttkClientCertificate()};
        clientPublicKey = clientCertChain[0].getPublicKey();
    }

    public void testKeys() throws Exception {
        System.out.println("CA private key: " + caPrivateKey);
        System.out.println("CA public key: " + caPublicKey);
        System.out.println("CA cert: " + caCertChain[0]);
        System.out.println("CA PEM:\n" + CertUtils.encodeAsPEM(caCertChain[0]));

        System.out.println("Client private key: " + clientPrivateKey);
        System.out.println("Client public key: " + clientPublicKey);
        System.out.println("Client cert: " + clientCertChain[0]);
        System.out.println("Client PEM:\n" + CertUtils.encodeAsPEM(clientCertChain[0]));
    }

    private Document getUnsignedHolderOfKeyAssertion(String id, boolean useThumbprintForSubject) throws CertificateException, SignatureException {
        LoginCredentials creds = new LoginCredentials(null, null, CredentialFormat.CLIENTCERT, RequireWssX509Cert.class, null, clientCertChain[0]);
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddressUtil.getLocalHost());
        samlOptions.setSignAssertion(false);
        samlOptions.setVersion(2);
        SignerInfo si = new SignerInfo(caPrivateKey, caCertChain);
        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(creds, SubjectStatement.HOLDER_OF_KEY, useThumbprintForSubject ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        SamlAssertionGenerator generator = new SamlAssertionGenerator(si);
        samlOptions.setId(id);
        return generator.createAssertion(subjectStatement, samlOptions);
    }

    public Document getSignedHolderOfKey(final String assertionId, boolean useThumbprintForSignature, boolean useThumbprintForSubject) throws Exception {
        final Document assertionDoc = getUnsignedHolderOfKeyAssertion(assertionId, useThumbprintForSubject);

        String s2 = XmlUtil.nodeToFormattedString(assertionDoc);
        System.out.println("Before signing: " + s2);
        SamlAssertionGenerator.Options options = new SamlAssertionGenerator.Options();
        options.setVersion(2);
        SamlAssertionGenerator.signAssertion(options, assertionDoc, caPrivateKey, caCertChain, useThumbprintForSignature ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT);
        return assertionDoc;
    }

    public void testRequestSignedWithSamlToken() throws Exception {
        Document req = getRequestSignedWithSamlToken(false, false);
        log.info("Signed request using saml token: " + XmlUtil.nodeToFormattedString(req));

        Element assertionElement = (Element) req.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Assertion").item(0);
        SamlAssertion assertion  = SamlAssertion.newInstance(assertionElement);
        assertion.verifyEmbeddedIssuerSignature();
    }

    public void testRequestSignedWithSamlTokenWithThumbprint() throws Exception {
        Document req = getRequestSignedWithSamlToken(true, true);
        log.info("Signed request using saml token: " + XmlUtil.nodeToFormattedString(req));
    }

    public void testRequestWithSenderVouchesToken() throws Exception {
        Document req = getRequestWithSenderVouchesToken();
        log.info("Request including sender vouches token: " + XmlUtil.nodeToFormattedString(req));
    }

    public void testSignedRequestWithSenderVouchesToken() throws Exception {
        Document req = getSignedRequestWithSenderVouchesToken();
        log.info("Request signed with a sender vouches token: " + XmlUtil.nodeToFormattedString(req));
    }

    public Document getRequestWithSenderVouchesToken() throws Exception {
        Document request =  TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);

        SamlAssertionGenerator ag = new SamlAssertionGenerator(new SignerInfo(caPrivateKey, caCertChain));
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setVersion(2);
        samlOptions.setExpiryMinutes(5);
        samlOptions.setProofOfPosessionRequired(false);
        SubjectStatement statement =
          SubjectStatement.createAuthenticationStatement(LoginCredentials.makeCertificateCredentials(clientCertChain[0],
                                                                                                     RequireWssX509Cert.class),
                                                         SubjectStatement.HOLDER_OF_KEY, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        ag.attachStatement(request, statement, samlOptions);
        return request;
    }

    public Document getSignedRequestWithSenderVouchesToken() throws Exception {
        Document request =  TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        assertNotNull(request);
        Element body = DomUtils.findOnlyOneChildElementByName(request.getDocumentElement(),
                                                             request.getDocumentElement().getNamespaceURI(),
                                                             "Body");
        assertNotNull(body);
        // in this case, the request is actually signed by the issuer
        DecorationRequirements req = new DecorationRequirements();
        req.setSignTimestamp();
        req.getElementsToSign().add(body);
        req.setSenderMessageSigningCertificate(caCertChain[0]);
        req.setSenderMessageSigningPrivateKey(caPrivateKey);
        new WssDecoratorImpl().decorateMessage(new Message(request), req);

        // hack message so original signature refers to the saml token instead of the BST
        Element security = SoapUtil.getSecurityElementForL7(request);
        if (security == null) {
            security = SoapUtil.getSecurityElement(request);
        }
        assertNotNull(security);
        Element bst = DomUtils.findOnlyOneChildElementByName(security, security.getNamespaceURI(), "BinarySecurityToken");
        assertNotNull(bst);
        String bstId = bst.getAttributeNS(SoapUtil.WSU_NAMESPACE, "Id");
        assertNotNull(bstId);
        assertTrue(bstId.length() > 0);

        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setVersion(2);
        samlOptions.setExpiryMinutes(5);
        samlOptions.setProofOfPosessionRequired(false);
        samlOptions.setId(bstId);
        final LoginCredentials credentials = LoginCredentials.makeCertificateCredentials(clientCertChain[0], RequireWssSaml.class);
        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(credentials, SubjectStatement.SENDER_VOUCHES, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        SamlAssertionGenerator generator = new SamlAssertionGenerator(new SignerInfo(caPrivateKey, caCertChain));
        samlOptions.setId(bstId);
        Document samlsvAssertion = generator.createAssertion(subjectStatement, samlOptions);

        Node importedNode = request.importNode(samlsvAssertion.getDocumentElement(), true);
        security.replaceChild(importedNode, bst);
        return request;
    }

    public Document getRequestSignedWithSamlToken(boolean useThumbprintForSignature, boolean useThumbprintForSubject) throws Exception {
        Document request =  TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        assertNotNull(request);
        Element body = DomUtils.findOnlyOneChildElementByName(request.getDocumentElement(),
                                                             request.getDocumentElement().getNamespaceURI(),
                                                             "Body");
        assertNotNull(body);

        DecorationRequirements req = new DecorationRequirements();
        req.setSignTimestamp();
        req.getElementsToSign().add(body);
        req.setSenderMessageSigningCertificate(clientCertChain[0]);
        req.setSenderMessageSigningPrivateKey(clientPrivateKey);
        new WssDecoratorImpl().decorateMessage(new Message(request), req);

        // Hand-hack the decorated message, replacing the BST with the saml:assertion
        Element security = SoapUtil.getSecurityElementForL7(request);
        if (security == null) {
            security = SoapUtil.getSecurityElement(request);
        }
        assertNotNull(security);
        Element bst = DomUtils.findOnlyOneChildElementByName(security, security.getNamespaceURI(), "BinarySecurityToken");
        assertNotNull(bst);
        String bstId = bst.getAttributeNS(SoapUtil.WSU_NAMESPACE, "Id");
        assertNotNull(bstId);
        assertTrue(bstId.length() > 0);

        // Create saml assertion using the same ID
        Document assertionDoc = getSignedHolderOfKey(bstId, useThumbprintForSignature, useThumbprintForSubject);
        assertNotNull(assertionDoc);
        Element samlAssertion = assertionDoc.getDocumentElement();
        assertNotNull(samlAssertion);

        Node importedNode = request.importNode(samlAssertion, true);
        security.replaceChild(importedNode, bst);

        return request;
    }

    /**
     * Test <code>SignedSaml2Test</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(SignedSaml2Test.suite());
    }

    private PrivateKey caPrivateKey;
    private PublicKey caPublicKey;
    private X509Certificate[] caCertChain;

    private PrivateKey clientPrivateKey;
    private PublicKey clientPublicKey;
    private X509Certificate[] clientCertChain;
    private Logger log = Logger.getLogger(getClass().getName());
}
