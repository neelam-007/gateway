package com.l7tech.common.security.saml;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.server.saml.HolderOfKeyHelper;
import com.l7tech.server.saml.SamlAssertionGenerator;
import com.l7tech.server.saml.SamlAssertionHelper;
import com.l7tech.server.saml.SenderVouchesHelper;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class SignedSamlTest extends TestCase {
    /**
     * test <code>SignedSamlTest</code> constructor
     */
    public SignedSamlTest( String name ) {
        super( name );
    }

    /**
     * create the <code>TestSuite</code> for the SignedSamlTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite( SignedSamlTest.class );
        return suite;
    }

    public void setUp() throws Exception {
        caPrivateKey = TestDocuments.getEttkServerPrivateKey();
        caCertChain = new X509Certificate[] { TestDocuments.getEttkServerCertificate() };
        caPublicKey = caCertChain[0].getPublicKey();

        clientPrivateKey = TestDocuments.getEttkClientPrivateKey();
        clientCertChain = new X509Certificate[] { TestDocuments.getEttkClientCertificate() };
        clientPublicKey = clientCertChain[0].getPublicKey();
    }

    public void testKeys() throws Exception {
        System.out.println("CA private key: " + caPrivateKey);
        System.out.println("CA public key: " + caPublicKey);
        System.out.println("CA cert: " + caCertChain[0]);

        System.out.println("Client private key: " + clientPrivateKey);
        System.out.println("Client public key: " + clientPublicKey);
        System.out.println("Client cert: " + clientCertChain[0]);
    }

    private Document getUnsignedHolderOfKeyAssertion(String id) throws IOException, SAXException, CertificateException {
        LoginCredentials creds = new LoginCredentials(null, null, CredentialFormat.CLIENTCERT, RequestWssX509Cert.class, null, clientCertChain[0]);
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddress.getLocalHost());
        Document doc = new DocumentImpl();
        SignerInfo si = new SignerInfo(caPrivateKey, caCertChain);
        HolderOfKeyHelper hh = new HolderOfKeyHelper(doc, samlOptions, creds, si);
        Document samlDoc = hh.createAssertion(id);
        return samlDoc;
    }

    public Document getSignedHolderOfKey(final String assertionId) throws Exception {
        final Document assertionDoc = getUnsignedHolderOfKeyAssertion(assertionId);

        String s2 = XmlUtil.nodeToFormattedString(assertionDoc);
        System.out.println("Before signing: " + s2);

        SamlAssertionHelper.signAssertion(assertionDoc, caPrivateKey, caCertChain);

        return assertionDoc;
    }

    public void testRequestSignedWithSamlToken() throws Exception {
        Document req = getRequestSignedWithSamlToken();
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
        Document request = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);

        SamlAssertionGenerator ag = new SamlAssertionGenerator();
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setExpiryMinutes(5);
        samlOptions.setSignEnvelope(false);
        ag.attachSenderVouches(request,
                               new SignerInfo(caPrivateKey, caCertChain),
                               LoginCredentials.makeCertificateCredentials(clientCertChain[0], getClass()),
                               //LoginCredentials.makePasswordCredentials("john", "ilovesheep".toCharArray(), getClass()),
                               samlOptions);
        return request;
    }

    public Document getSignedRequestWithSenderVouchesToken() throws Exception {
        Document request = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        assertNotNull(request);
        Element body = XmlUtil.findOnlyOneChildElementByName(request.getDocumentElement(),
                                                             request.getDocumentElement().getNamespaceURI(),
                                                             "Body");
        assertNotNull(body);
        // in this case, the request is actually signed by the issuer
        DecorationRequirements req = new DecorationRequirements();
        req.setSignTimestamp(true);
        req.getElementsToSign().add(body);
        req.setSenderCertificate(caCertChain[0]);
        req.setSenderPrivateKey(caPrivateKey);
        new WssDecoratorImpl().decorateMessage(request, req);

        // hack message so original signature refers to the saml token instead of the BST
        Element security = SoapUtil.getSecurityElement(request);
        assertNotNull(security);
        Element bst = XmlUtil.findOnlyOneChildElementByName(security, security.getNamespaceURI(), "BinarySecurityToken");
        assertNotNull(bst);
        String bstId = bst.getAttributeNS(SoapUtil.WSU_NAMESPACE, "Id");
        assertNotNull(bstId);
        assertTrue(bstId.length() > 0);

        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setExpiryMinutes(5);
        samlOptions.setSignEnvelope(false);
        samlOptions.setId(bstId);

        SenderVouchesHelper svh = new SenderVouchesHelper(request, samlOptions, LoginCredentials.makeCertificateCredentials(clientCertChain[0], getClass()), new SignerInfo(caPrivateKey, caCertChain));
        Document samlsvAssertion = svh.createSignedAssertion(bstId);

        Node importedNode = request.importNode(samlsvAssertion.getDocumentElement(), true);
        security.replaceChild(importedNode, bst);
        return request;
    }

    public Document getRequestSignedWithSamlToken() throws Exception {
        Document request = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        assertNotNull(request);
        Element body = XmlUtil.findOnlyOneChildElementByName(request.getDocumentElement(),
                                                             request.getDocumentElement().getNamespaceURI(),
                                                             "Body");
        assertNotNull(body);

        DecorationRequirements req = new DecorationRequirements();
        req.setSignTimestamp(true);
        req.getElementsToSign().add(body);
        req.setSenderCertificate(clientCertChain[0]);
        req.setSenderPrivateKey(clientPrivateKey);
        new WssDecoratorImpl().decorateMessage(request, req);

        // Hand-hack the decorated message, replacing the BST with the saml:assertion
        Element security = SoapUtil.getSecurityElement(request);
        assertNotNull(security);
        Element bst = XmlUtil.findOnlyOneChildElementByName(security, security.getNamespaceURI(), "BinarySecurityToken");
        assertNotNull(bst);
        String bstId = bst.getAttributeNS(SoapUtil.WSU_NAMESPACE, "Id");
        assertNotNull(bstId);
        assertTrue(bstId.length() > 0);

        // Create saml assertion using the same ID
        Document assertionDoc = getSignedHolderOfKey(bstId);
        assertNotNull(assertionDoc);
        Element samlAssertion = assertionDoc.getDocumentElement();
        assertNotNull(samlAssertion);

        Node importedNode = request.importNode(samlAssertion, true);
        security.replaceChild(importedNode, bst);

        return request;
    }

    /**
     * Test <code>SignedSamlTest</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
    }

    private PrivateKey caPrivateKey;
    private PublicKey caPublicKey;
    private X509Certificate[] caCertChain;

    private PrivateKey clientPrivateKey;
    private PublicKey clientPublicKey;
    private X509Certificate[] clientCertChain;
    private Logger log = Logger.getLogger(getClass().getName());
}