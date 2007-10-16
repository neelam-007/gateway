package com.l7tech.common.security.saml;

import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.common.security.xml.SecurityActor;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.FileOutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
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
    public SignedSamlTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the SignedSamlTest <code>TestCase</code>
     */
    public static Test suite() {
        return new TestSuite(SignedSamlTest.class);
    }

    public void setUp() throws Exception {
        caPrivateKey = TestDocuments.getEttkServerPrivateKey();
        caCertChain = new X509Certificate[]{TestDocuments.getEttkServerCertificate()};
        caPublicKey = caCertChain[0].getPublicKey();

        clientPrivateKey = TestDocuments.getEttkClientPrivateKey();
        clientCertChain = new X509Certificate[]{TestDocuments.getEttkClientCertificate()};
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

    private Document getUnsignedHolderOfKeyAssertion(String id, boolean useThumbprintForSubject, int samlVersion) throws CertificateException, SignatureException {
        LoginCredentials creds = new LoginCredentials(null, null, CredentialFormat.CLIENTCERT, RequestWssX509Cert.class, null, clientCertChain[0]);
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddressUtil.getLocalHost());
        samlOptions.setSignAssertion(false);
        samlOptions.setVersion(samlVersion);
        SignerInfo si = new SignerInfo(caPrivateKey, caCertChain);
        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(creds, SubjectStatement.HOLDER_OF_KEY, useThumbprintForSubject ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null);
        SamlAssertionGenerator generator = new SamlAssertionGenerator(si);
        samlOptions.setId(id);
        return generator.createAssertion(subjectStatement, samlOptions);
    }

    private Document getUnsignedHolderOfKeyAssertionTwoStatements(String id, boolean useThumbprintForSubject, int samlVersion) throws CertificateException, SignatureException {
        LoginCredentials creds = new LoginCredentials(null, null, CredentialFormat.CLIENTCERT, RequestWssX509Cert.class, null, clientCertChain[0]);
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setClientAddress(InetAddressUtil.getLocalHost());
        samlOptions.setVersion(samlVersion);
        samlOptions.setSignAssertion(false);
        SignerInfo si = new SignerInfo(caPrivateKey, caCertChain);

        final KeyInfoInclusionType keyInfoType = useThumbprintForSubject ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT;
        SubjectStatement authnStatement = SubjectStatement.createAuthenticationStatement(creds, SubjectStatement.HOLDER_OF_KEY, keyInfoType, NameIdentifierInclusionType.FROM_CREDS, null, null);
        SubjectStatement attrStatement = SubjectStatement.createAttributeStatement(creds, SubjectStatement.HOLDER_OF_KEY, "foo", "bar", "baz", keyInfoType, NameIdentifierInclusionType.FROM_CREDS, null, null);
        SamlAssertionGenerator generator = new SamlAssertionGenerator(si);
        samlOptions.setId(id);
        return generator.createAssertion(new SubjectStatement[] { authnStatement, attrStatement }, samlOptions);
    }

    public Document getSignedHolderOfKey(final String assertionId,
                                         boolean useThumbprintForSignature,
                                         boolean useThumbprintForSubject,
                                         boolean useTwoStatements,
                                         int samlVersion)
            throws Exception
    {
        final Document assertionDoc = useTwoStatements ?
                getUnsignedHolderOfKeyAssertionTwoStatements(assertionId, useThumbprintForSubject, samlVersion) :
                getUnsignedHolderOfKeyAssertion(assertionId, useThumbprintForSubject, samlVersion);

        String s2 = XmlUtil.nodeToFormattedString(assertionDoc);
        System.out.println("Before signing: " + s2);
        final SamlAssertionGenerator.Options opts = new SamlAssertionGenerator.Options();
        opts.setId(assertionId);
        opts.setSignAssertion(true);
        opts.setVersion(samlVersion);
        SamlAssertionGenerator.signAssertion(opts, assertionDoc, caPrivateKey, caCertChain, useThumbprintForSignature ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT);
        return assertionDoc;
    }

    public void testRequestSignedWithSamlToken() throws Exception {
        Document req = getRequestSignedWithSamlToken(false, false, false, 1);
        log.info("Signed request using saml token: " + XmlUtil.nodeToFormattedString(req));
    }

    public void testRequestSignedWithSamlTokenWithThumbprint() throws Exception {
        Document req = getRequestSignedWithSamlToken(true, true, false, 1);
        log.info("Signed request using saml token: " + XmlUtil.nodeToFormattedString(req));
    }


    public void testRequestSignedWithSamlTokenTwoStatements() throws Exception {
        Document req = getRequestSignedWithSamlToken(false, false, true, 2);
        final FileOutputStream fos = new FileOutputStream("/tmp/saml2statements.xml");
        XmlUtil.nodeToOutputStream(req, fos);
        fos.close();
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

        SamlAssertionGenerator ag = new SamlAssertionGenerator(new SignerInfo(caPrivateKey, caCertChain));
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setExpiryMinutes(5);
        samlOptions.setProofOfPosessionRequired(false);
        SubjectStatement statement =
          SubjectStatement.createAuthenticationStatement(LoginCredentials.makeCertificateCredentials(clientCertChain[0],
                                                                                                     RequestWssX509Cert.class),
                                                         SubjectStatement.HOLDER_OF_KEY, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null);
        ag.attachStatement(request, statement, samlOptions);
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
        req.setSignTimestamp();
        req.getElementsToSign().add(body);
        req.setSenderMessageSigningCertificate(caCertChain[0]);
        req.setSenderMessageSigningPrivateKey(caPrivateKey);
        new WssDecoratorImpl().decorateMessage(request, req);

        // hack message so original signature refers to the saml token instead of the BST
        Element security = SoapUtil.getSecurityElement(request, SecurityActor.L7ACTOR.getValue());
        if (security == null) {
            security = SoapUtil.getSecurityElement(request);
        }
        assertNotNull(security);
        Element bst = XmlUtil.findOnlyOneChildElementByName(security, security.getNamespaceURI(), "BinarySecurityToken");
        assertNotNull(bst);
        String bstId = bst.getAttributeNS(SoapUtil.WSU_NAMESPACE, "Id");
        assertNotNull(bstId);
        assertTrue(bstId.length() > 0);

        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setExpiryMinutes(5);
        samlOptions.setProofOfPosessionRequired(false);
        samlOptions.setId(bstId);
        final LoginCredentials credentials = LoginCredentials.makeCertificateCredentials(clientCertChain[0], getClass());
        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(credentials, SubjectStatement.SENDER_VOUCHES, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null);
        SamlAssertionGenerator generator = new SamlAssertionGenerator(new SignerInfo(caPrivateKey, caCertChain));
        samlOptions.setId(bstId);
        Document samlsvAssertion = generator.createAssertion(subjectStatement, samlOptions);

        Node importedNode = request.importNode(samlsvAssertion.getDocumentElement(), true);
        security.replaceChild(importedNode, bst);
        return request;
    }

    public Document getRequestSignedWithSamlToken(boolean useThumbprintForSignature, boolean useThumbprintForSubject, boolean useTwoStatements, int samlVersion) throws Exception {
        Document request = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        assertNotNull(request);
        Element body = XmlUtil.findOnlyOneChildElementByName(request.getDocumentElement(),
                                                             request.getDocumentElement().getNamespaceURI(),
                                                             "Body");
        assertNotNull(body);

        DecorationRequirements req = new DecorationRequirements();
        req.setSignTimestamp();
        req.getElementsToSign().add(body);
        req.setSenderMessageSigningCertificate(clientCertChain[0]);
        req.setSenderMessageSigningPrivateKey(clientPrivateKey);
        new WssDecoratorImpl().decorateMessage(request, req);

        // Hand-hack the decorated message, replacing the BST with the saml:assertion
        Element security = SoapUtil.getSecurityElement(request, SecurityActor.L7ACTOR.getValue());
        if (security == null) {
            security = SoapUtil.getSecurityElement(request);
        }
        assertNotNull(security);
        Element bst = XmlUtil.findOnlyOneChildElementByName(security, security.getNamespaceURI(), "BinarySecurityToken");
        assertNotNull(bst);
        String bstId = bst.getAttributeNS(SoapUtil.WSU_NAMESPACE, "Id");
        assertNotNull(bstId);
        assertTrue(bstId.length() > 0);

        // Create saml assertion using the same ID
        Document assertionDoc = getSignedHolderOfKey(bstId, useThumbprintForSignature, useThumbprintForSubject, useTwoStatements, samlVersion);
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
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }

    private PrivateKey caPrivateKey;
    private PublicKey caPublicKey;
    private X509Certificate[] caCertChain;

    private PrivateKey clientPrivateKey;
    private PublicKey clientPublicKey;
    private X509Certificate[] clientCertChain;
    private Logger log = Logger.getLogger(getClass().getName());
}