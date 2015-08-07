package com.l7tech.security.saml;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.TestKeys;
import com.l7tech.common.io.CertUtils;
import com.l7tech.security.token.http.TlsClientCertToken;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.security.xml.processor.X509BinarySecurityTokenImpl;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecoratorImpl;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.saml.SamlAssertion;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.FileOutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

public class SignedSamlTest {
    
    @Before
    public void setUp() throws Exception {
        caPrivateKey = TestDocuments.getEttkServerPrivateKey();
        caCertChain = new X509Certificate[]{TestDocuments.getEttkServerCertificate()};
        caPublicKey = caCertChain[0].getPublicKey();

        clientPrivateKey = TestDocuments.getEttkClientPrivateKey();
        clientCertChain = new X509Certificate[]{TestDocuments.getEttkClientCertificate()};
        clientPublicKey = clientCertChain[0].getPublicKey();
    }

    @Test
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

    private Document getUnsignedHolderOfKeyAssertion(String id, boolean useThumbprintForSubject, int samlVersion) throws CertificateException, SignatureException, UnrecoverableKeyException {
        X509BinarySecurityTokenImpl token = new X509BinarySecurityTokenImpl(clientCertChain[0], null);
        token.onPossessionProved();
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(token, RequireWssX509Cert.class);
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

    private Document getUnsignedHolderOfKeyAssertionTwoStatements(String id, boolean useThumbprintForSubject, int samlVersion) throws CertificateException, SignatureException, UnrecoverableKeyException {
        X509BinarySecurityTokenImpl token = new X509BinarySecurityTokenImpl(clientCertChain[0], null);
        token.onPossessionProved();
        LoginCredentials creds = LoginCredentials.makeLoginCredentials(token, RequireWssX509Cert.class);
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

        final SamlAssertionGenerator.Options opts = new SamlAssertionGenerator.Options();
        opts.setId(assertionId);
        opts.setSignAssertion(true);
        opts.setVersion(samlVersion);
        SamlAssertionGenerator.signAssertion(opts, assertionDoc, caPrivateKey, caCertChain, useThumbprintForSignature ? KeyInfoInclusionType.STR_THUMBPRINT : KeyInfoInclusionType.CERT);
        return assertionDoc;
    }

    @Test
    public void testRequestSignedWithSamlToken() throws Exception {
        Document req = getRequestSignedWithSamlToken(false, false, false, 1);
        log.info("Signed request using saml token: " + XmlUtil.nodeToFormattedString(req));
    }

    @Test
    public void testRequestSignedWithSamlTokenWithThumbprint() throws Exception {
        Document req = getRequestSignedWithSamlToken(true, true, false, 1);
        log.info("Signed request using saml token: " + XmlUtil.nodeToFormattedString(req));
    }


    @Test
    public void testRequestSignedWithSamlTokenTwoStatements() throws Exception {
        Document req = getRequestSignedWithSamlToken(false, false, true, 2);
        final FileOutputStream fos = new FileOutputStream("/tmp/saml2statements.xml");
        XmlUtil.nodeToOutputStream(req, fos);
        fos.close();
        log.info("Signed request using saml token: " + XmlUtil.nodeToFormattedString(req));
    }

    @Test
    public void testRequestWithSenderVouchesToken() throws Exception {
        Document req = getRequestWithSenderVouchesToken();
        log.info("Request including sender vouches token: " + XmlUtil.nodeToFormattedString(req));
    }

    @Test
    public void testSignedRequestWithSenderVouchesToken() throws Exception {
        Document req = getSignedRequestWithSenderVouchesToken();
        log.info("Request signed with a sender vouches token: " + XmlUtil.nodeToFormattedString(req));
    }

    public Document getRequestWithSenderVouchesToken() throws Exception {
        Document request = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);

        SamlAssertionGenerator ag = new SamlAssertionGenerator(new SignerInfo(caPrivateKey, caCertChain));
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setNotAfterSeconds(300);
        samlOptions.setProofOfPosessionRequired(false);
        SubjectStatement statement =
          SubjectStatement.createAuthenticationStatement(LoginCredentials.makeLoginCredentials(new TlsClientCertToken(clientCertChain[0]), SslAssertion.class),
                                                         SubjectStatement.HOLDER_OF_KEY, KeyInfoInclusionType.CERT, NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);
        ag.attachStatement(request, statement, samlOptions);
        return request;
    }

    @Test
    public void testAttachRequest_SKISubjectConfirmationSpecifiedButCertContainsNoSKI_CertificateExceptionThrown() throws Exception {
        Document request = TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT);

        Pair<X509Certificate, PrivateKey> testCertAndKey = TestKeys.getCertAndKey("DSA_1024_NO_SKI");

        SamlAssertionGenerator ag = new SamlAssertionGenerator(new SignerInfo(testCertAndKey.right,
                new X509Certificate[] {testCertAndKey.left}));

        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setNotAfterSeconds(300);
        samlOptions.setProofOfPosessionRequired(false);
        samlOptions.setIssuerKeyInfoType(KeyInfoInclusionType.STR_SKI);
        samlOptions.setVersion(1);

        SubjectStatement statement =
                SubjectStatement.createAuthenticationStatement(LoginCredentials.
                        makeLoginCredentials(new TlsClientCertToken(testCertAndKey.left), SslAssertion.class),
                SubjectStatement.HOLDER_OF_KEY, KeyInfoInclusionType.STR_SKI,
                NameIdentifierInclusionType.FROM_CREDS, null, null, null, null);

        try {
            ag.attachStatement(request, statement, samlOptions);
            fail("Expected CertificateException");
        } catch (CertificateException e) {
            assertEquals("Unable to create SKI reference: no SKI available for certificate [cn=test_dsa_1024]",
                    e.getMessage());
        }
    }

    public Document getSignedRequestWithSenderVouchesToken() throws Exception {
        // Make a SAML assertion
        SamlAssertionGenerator.Options samlOptions = new SamlAssertionGenerator.Options();
        samlOptions.setNotAfterSeconds(300);
        samlOptions.setProofOfPosessionRequired(false);
        samlOptions.setId("TestAssertionId-001");
        final LoginCredentials credentials = LoginCredentials.makeLoginCredentials(new TlsClientCertToken(clientCertChain[0]), SslAssertion.class);
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
        req.setSignTimestamp(true);
        req.getElementsToSign().add(body);
        req.setSenderSamlToken(SamlAssertion.newInstance(samlsvAssertion.getDocumentElement()), true);
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
        req.setSignTimestamp(true);
        req.getElementsToSign().add(body);
        SecurityTokenResolver resolver = new SimpleSecurityTokenResolver(caCertChain[0]);
        req.setSenderSamlToken(SamlAssertion.newInstance(samlAssertion, resolver), true);
        req.setSenderMessageSigningPrivateKey(clientPrivateKey);
        new WssDecoratorImpl().decorateMessage(new Message(request), req);

        return request;
    }

    private PrivateKey caPrivateKey;
    private PublicKey caPublicKey;
    private X509Certificate[] caCertChain;

    private PrivateKey clientPrivateKey;
    private PublicKey clientPublicKey;
    private X509Certificate[] clientCertChain;
    private Logger log = Logger.getLogger(getClass().getName());
}