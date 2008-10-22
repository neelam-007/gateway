package com.l7tech.security.saml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.util.DomUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.FileOutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

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

    @Override
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
        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(creds, SubjectStatement.HOLDER_OF_KEY, useThumbprintForSubject ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
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
        SubjectStatement authnStatement = SubjectStatement.createAuthenticationStatement(creds, SubjectStatement.HOLDER_OF_KEY, keyInfoType, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        SubjectStatement attrStatement = SubjectStatement.createAttributeStatement(creds, SubjectStatement.HOLDER_OF_KEY, "foo", "bar", "baz", keyInfoType, NameIdentifierInclusionType.FROM_CREDS, null, null, null);
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
                                                         SubjectStatement.HOLDER_OF_KEY, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        ag.attachStatement(request, statement, samlOptions);
        return request;
    }

    public Document getSignedRequestWithSenderVouchesToken() throws Exception {
        // Make a SAML assertion
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setExpiryMinutes(5);
        samlOptions.setProofOfPosessionRequired(false);
        samlOptions.setId("TestAssertionId-001");
        final LoginCredentials credentials = LoginCredentials.makeCertificateCredentials(clientCertChain[0], getClass());
        SubjectStatement subjectStatement = SubjectStatement.createAuthenticationStatement(credentials, SubjectStatement.SENDER_VOUCHES, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        SamlAssertionGenerator generator = new SamlAssertionGenerator(new SignerInfo(caPrivateKey, caCertChain));
        Document samlsvAssertion = generator.createAssertion(subjectStatement, samlOptions);

        // Decorate a message with it
        Document request = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        assertNotNull(request);
        Element body = DomUtils.findOnlyOneChildElementByName(request.getDocumentElement(),
                         request.getDocumentElement().getNamespaceURI(),
                         "Body");
        assertNotNull(body);
        // in this case, the request is actually signed by the issuer
        DecorationRequirements req = new DecorationRequirements();
        req.setSignTimestamp();
        req.getElementsToSign().add(body);
        req.setSenderSamlToken(samlsvAssertion.getDocumentElement(), true);
        req.setSenderMessageSigningPrivateKey(caPrivateKey);
        new WssDecoratorImpl().decorateMessage(new Message(request), req);

        return request;
    }

    public Document getRequestSignedWithSamlToken(boolean useThumbprintForSignature, boolean useThumbprintForSubject, boolean useTwoStatements, int samlVersion) throws Exception {
        Document request = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);
        assertNotNull(request);
        Element body = DomUtils.findOnlyOneChildElementByName(request.getDocumentElement(),
                                                             request.getDocumentElement().getNamespaceURI(),
                                                             "Body");
        assertNotNull(body);

        // Create saml assertion using the same ID
        Document assertionDoc = getSignedHolderOfKey("TestAssertionId-001", useThumbprintForSignature, useThumbprintForSubject, useTwoStatements, samlVersion);
        assertNotNull(assertionDoc);
        Element samlAssertion = assertionDoc.getDocumentElement();
        assertNotNull(samlAssertion);

        DecorationRequirements req = new DecorationRequirements();
        req.setSignTimestamp();
        req.getElementsToSign().add(body);
        req.setSenderSamlToken(samlAssertion, true);
        req.setSenderMessageSigningPrivateKey(clientPrivateKey);
        new WssDecoratorImpl().decorateMessage(new Message(request), req);

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