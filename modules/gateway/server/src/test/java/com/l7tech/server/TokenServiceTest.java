/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.TestDocuments;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernameToken;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.wstrust.TokenServiceClient;
import com.l7tech.security.wstrust.WsTrustConfigFactory;
import com.l7tech.security.wstrust.WsTrustConfig;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.security.xml.processor.BadSecurityContextException;
import com.l7tech.util.HexUtils;
import com.l7tech.xml.WsTrustRequestType;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.audit.AuditContextStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.SOAPConstants;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class TokenServiceTest {
    private static Logger log = Logger.getLogger(TokenServiceTest.class.getName());
    private static ApplicationContext applicationContext = null;

    @BeforeClass
    public static void init() {
        applicationContext = ApplicationContexts.getTestApplicationContext();
    }

    @Test
    public void testTokenServiceClient() throws Exception {
        doTestTokenServiceClient( WsTrustConfigFactory.getDefaultWsTrustConfig() );
    }

    @Test
    public void testTokenServiceClientSoap12() throws Exception {
        WsTrustConfig config = WsTrustConfigFactory.getDefaultWsTrustConfig();
        config.setSoapNs( SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE );
        doTestTokenServiceClient( config );
    }

    private void doTestTokenServiceClient( final WsTrustConfig wsTrustConfig ) throws Exception {
        final TokenServiceClient tokenServiceClient = new TokenServiceClient(wsTrustConfig);
        
        Document requestMsg = tokenServiceClient.createRequestSecurityTokenMessage(TestDocuments.getDotNetServerCertificate(),
                                                                    TestDocuments.getDotNetServerPrivateKey(),
                                                                    SecurityTokenType.WSSC_CONTEXT,
                                                                    WsTrustRequestType.ISSUE,
                                                                    null, null, null, null);
        log.info("Decorated token request (reformatted): " + XmlUtil.nodeToFormattedString(requestMsg));

        SecurityTokenResolver testTokenResolver = (SecurityTokenResolver)applicationContext.getBean("securityTokenResolver");

        ((SimpleSecurityTokenResolver)testTokenResolver).addCerts( new X509Certificate[]{TestDocuments.getDotNetServerCertificate()} );

        final TokenService service = new TokenServiceImpl(new TestDefaultKey(),
                                                      (ServerPolicyFactory)applicationContext.getBean("policyFactory"),
                                                      testTokenResolver)
        {
            @Override
            public AssertionStatus respondToSecurityTokenRequest(PolicyEnforcementContext context, CredentialsAuthenticator authenticator, boolean useThumbprintForSamlSignature, boolean useThumbprintForSamlSubject) throws InvalidDocumentFormatException, TokenServiceException, ProcessorException, BadSecurityContextException, GeneralSecurityException, AuthenticationException {
                setApplicationContext(applicationContext);
                return super.respondToSecurityTokenRequest(context, authenticator, useThumbprintForSamlSignature, useThumbprintForSamlSubject);
            }
        };

        final TokenServiceImpl.CredentialsAuthenticator authenticator = new TokenServiceImpl.CredentialsAuthenticator() {

            @Override
            public User authenticate(LoginCredentials creds) {
                UserBean user = new UserBean();
                user.setLogin("john");
                user.setSubjectDn("cn=john");
                return user;
            }
        };
        final Message response = new Message();
        final Message request = new Message();
        request.initialize(requestMsg);
        final PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setAuditContext(new AuditContextStub());

        AssertionStatus status = service.respondToSecurityTokenRequest(context, authenticator, false, false);
        Assert.assertEquals(status, AssertionStatus.NONE);

        Document responseMsg = response.getXmlKnob().getDocumentWritable();

        log.info("Decorated response (reformatted): " + XmlUtil.nodeToFormattedString(responseMsg));

        Object responseObj = tokenServiceClient.parseSignedRequestSecurityTokenResponse(responseMsg,
                                                                   TestDocuments.getDotNetServerCertificate(),
                                                                   TestDocuments.getDotNetServerPrivateKey(),
                                                                   TestDocuments.getDotNetServerCertificate());

        Assert.assertTrue(responseObj instanceof TokenServiceClient.SecureConversationSession);
        TokenServiceClient.SecureConversationSession session = (TokenServiceClient.SecureConversationSession)responseObj;
        log.info("Got session! Session id=" + session.getSessionId());
        log.info("Session expiry date=" + session.getExpiryDate());
        log.info("Session shared secret=" + HexUtils.hexDump(session.getSharedSecret()));
    }

    @Test
    public void testTokenServiceClientSaml() throws Exception {
        final TokenServiceClient tokenServiceClient = new TokenServiceClient(WsTrustConfigFactory.getDefaultWsTrustConfig());
        final X509Certificate subjectCertificate = TestDocuments.getDotNetServerCertificate();
        final PrivateKey subjectPrivateKey = TestDocuments.getDotNetServerPrivateKey();
        final X509Certificate issuerCertificate = TestDocuments.getDotNetServerCertificate();
        final PrivateKey issuerPrivateKey = TestDocuments.getDotNetServerPrivateKey();
        Document requestMsg = tokenServiceClient.createRequestSecurityTokenMessage(
                subjectCertificate,
                subjectPrivateKey,
                SecurityTokenType.SAML_ASSERTION,
                WsTrustRequestType.ISSUE,
                null, null, null, null);
        requestMsg.getDocumentElement().setAttribute("xmlns:saml", SamlConstants.NS_SAML);
        log.info("Decorated token request (reformatted): " + XmlUtil.nodeToFormattedString(requestMsg));

        final TokenService service = new TokenServiceImpl(new TestDefaultKey(issuerCertificate, issuerPrivateKey),
                                                          (ServerPolicyFactory)applicationContext.getBean("policyFactory"),
                                                          (SecurityTokenResolver)applicationContext.getBean("securityTokenResolver"));

        final TokenServiceImpl.CredentialsAuthenticator authenticator = new TokenServiceImpl.CredentialsAuthenticator() {

            @Override
            public User authenticate(LoginCredentials creds) {
                UserBean user = new UserBean();
                user.setLogin("john");
                user.setSubjectDn("cn=john");
                return user;
            }
        };

        final Message response = new Message();
        final Message request = new Message();
        request.initialize(requestMsg);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(getFakeServletRequest()));

        final PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setAuditContext(new AuditContextStub());
        AssertionStatus status = service.respondToSecurityTokenRequest(context, authenticator, false, false);
        Assert.assertEquals(status, AssertionStatus.NONE);

        Document responseMsg = response.getXmlKnob().getDocumentWritable();

        log.info("Decorated response (reformatted): " + XmlUtil.nodeToFormattedString(responseMsg));

        Object responseObj = tokenServiceClient.parseSignedRequestSecurityTokenResponse(responseMsg,
                                                                                  subjectCertificate,
                                                                                  subjectPrivateKey,
                                                                                  issuerCertificate);
        Assert.assertTrue("Token obtained must be SAML", responseObj instanceof SamlAssertion);
        SamlAssertion token = (SamlAssertion)responseObj;
        Assert.assertTrue("Obtained saml token must be signed", token.hasEmbeddedIssuerSignature());
        Assert.assertTrue("Obtained saml token must be holder-of-key", token.isHolderOfKey());
        Assert.assertTrue("Obtained saml token must have non-null assertion ID", token.getAssertionId() != null);
        Assert.assertTrue("Obtained saml token must have non-empty assertion ID", token.getAssertionId().length() > 0);
        Assert.assertTrue("Obtained saml token must have subject certificate", token.getSubjectCertificate() != null);
        Assert.assertTrue("Obtained saml token must have issuer certificate", token.getIssuerCertificate() != null);
        Assert.assertTrue("Obtained saml token must have embedded issuer certificate", token.hasEmbeddedIssuerSignature());
        Assert.assertEquals("subject certificate must match our client certificate",
                     token.getSubjectCertificate(),
                     subjectCertificate);
        Assert.assertEquals("issuer certificate must match the expected issuer certificate",
                     token.getIssuerCertificate(),
                     issuerCertificate);
        token.verifyEmbeddedIssuerSignature();
    }

    private HttpServletRequest getFakeServletRequest() {
        MockHttpServletRequest mhsr = new MockHttpServletRequest();
        mhsr.setMethod("POST");
        return mhsr;
    }

    @Test
    public void testCreateFimRst() throws Exception {

        final TokenServiceClient tokenServiceClient = new TokenServiceClient(WsTrustConfigFactory.getDefaultWsTrustConfig());

        UsernameToken usernameToken = new UsernameTokenImpl("testuser", "passw0rd".toCharArray());
        Element utElm = usernameToken.asElement();
        SoapUtil.setWsuId(utElm, SoapUtil.WSU_NAMESPACE, "UsernameToken-1");

        // TODO after FIM interop use AppliesTo like http://l7tech.com/services/TokenServiceTest instead
        Document rstDoc = tokenServiceClient.createRequestSecurityTokenMessage(null,
                                                                                    WsTrustRequestType.VALIDATE,
                                                                                    usernameToken,
                                                                                    "http://samlpart.com/sso", null);
        Assert.assertNotNull(rstDoc);

        // TODO check this somehow, other than uncommenting below line and eyeballing the diff
        //InputStream fimIs = getClass().getClassLoader().getResourceAsStream("com/l7tech/example/resources/tivoli/FIM_RST.xml");
        //final String origRst = XmlUtil.nodeToFormattedString(XmlUtil.parse(fimIs));
        //String rst = XmlUtil.nodeToFormattedString(rstDoc);
        //assertEquals(rst, origRst);
    }

    @Test
    public void testParseFimRstr() throws Exception {
        final TokenServiceClient tokenServiceClient = new TokenServiceClient(WsTrustConfigFactory.getDefaultWsTrustConfig());

        InputStream respIs = TokenServiceTest.class.getResourceAsStream("tivoliFIM_RSTR.xml");
        Assert.assertNotNull( "Message resource input", respIs );
        final Document rstr = XmlUtil.parse(respIs);

        Object got = tokenServiceClient.parseUnsignedRequestSecurityTokenResponse(rstr);

        SamlAssertion saml = (SamlAssertion)got;
        Assert.assertEquals(saml.getNameIdentifierFormat(), SamlConstants.NAMEIDENTIFIER_EMAIL);
        Assert.assertEquals(saml.getNameIdentifierValue(), "testloser");

        Assert.assertFalse(saml.isHolderOfKey());
        Assert.assertFalse(saml.isSenderVouches());
        Assert.assertNull(saml.getConfirmationMethod());
        saml.verifyEmbeddedIssuerSignature();

        log.info("Issuer cert = " + saml.getIssuerCertificate());
        log.info("Subject cert = " + saml.getSubjectCertificate());
        log.info("Got SAML assertion (reformatted): " + XmlUtil.nodeToFormattedString(saml.asElement()));
    }

    @Test
    public void testParseNPEingSamlAssertion() throws Exception {
        Document doc = XmlUtil.stringToDocument(NPE_SAML_SIGNED);
        SamlAssertion.newInstance(doc.getDocumentElement(), null);
    }

    public static final String NPE_SAML_SIGNED =
            "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" AssertionID=\"SamlAssertion-04669ba89981a43d9f1ef27407f62b29\" IssueInstant=\"2008-08-15T11:57:44.308Z\" Issuer=\"demo.l7tech.com\" MajorVersion=\"1\" MinorVersion=\"1\"><saml:Conditions NotBefore=\"2008-08-15T11:55:00.000Z\" NotOnOrAfter=\"2008-08-15T12:02:44.310Z\"><saml:AudienceRestrictionCondition><saml:Audience></saml:Audience></saml:AudienceRestrictionCondition></saml:Conditions><saml:AuthenticationStatement AuthenticationInstant=\"2008-08-15T11:57:44.308Z\" AuthenticationMethod=\"urn:oasis:names:tc:SAML:1.0:am:unspecified\"><saml:Subject><saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\" NameQualifier=\"\">jmacdonald</saml:NameIdentifier><saml:SubjectConfirmation><saml:ConfirmationMethod xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"></saml:ConfirmationMethod></saml:SubjectConfirmation></saml:Subject><saml:SubjectLocality DNSAddress=\"192.168.50.1\" IPAddress=\"192.168.50.1\"></saml:SubjectLocality></saml:AuthenticationStatement><ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:CanonicalizationMethod><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"></ds:SignatureMethod><ds:Reference URI=\"#SamlAssertion-04669ba89981a43d9f1ef27407f62b29\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"></ds:Transform><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"></ds:Transform></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"></ds:DigestMethod><ds:DigestValue>8KWwM1PD5GFegHMYXb7jPt56w9c=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>CoM0thAdHp0tYw6TA+c9mMLgEt9zrA37kP8W6jTiC0TOlLKO74ex4/Z16zkmBO4iyKfdNspkfQSylLKB1juudAiR2GjbyOxdJBNZ+mql+WXZ1qYtIqebmD335SALH7Lv1KNZfTrUAgqM4JjAwDvV2fW+oqCj9bB+M5jpc9e0xhs=</ds:SignatureValue><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><X509Data><X509SubjectName>CN=demo.l7tech.com</X509SubjectName><X509Certificate>MIICFjCCAX+gAwIBAgIIEehwp3YEAfAwDQYJKoZIhvcNAQEFBQAwHzEdMBsGA1UEAwwUcm9vdC5kZW1vLmw3dGVjaC5jb20wHhcNMDgwODEzMTg0MDU4WhcNMTAwODEzMTg1MDU4WjAaMRgwFgYDVQQDDA9kZW1vLmw3dGVjaC5jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALXxhzUtQYudpgVxFtVmDcJjmYqzbA1+1qhHMrKY2d1HcV2VbvDXxlkdh8z0r9+JUlnjyWnmEs/S4mVyR6boc/BGcGQaGJKH1G0qxdupBGFoJziZc5QlDx/tF/tDpcXG/y/I8ejFLULOktxzZoHslqcszqvhR3VCn5Gx1R/MR4J/AgMBAAGjYDBeMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgXgMB0GA1UdDgQWBBQXVpc1Whi1VSbSML1yDHLMGxIMyjAfBgNVHSMEGDAWgBRgDclv9bZ249ThrtWuCSThya2V1zANBgkqhkiG9w0BAQUFAAOBgQARU1wHdLge8wpMTrQmUnMxRKbOx/COy3xv5zGsoxDRTtP+tSWyx7ipKtlnGyusq2RHB6SKwgKZg+DRMt3f6eBGeRr4PyEJ9OL6GbIOu18y489fZ0BJLdRDdogD2DPy89znPXl0uYrgva3/0aV111Wlqfo2RMPsdYbnCeEIdcfQ0w==</X509Certificate></X509Data></KeyInfo></ds:Signature></saml:Assertion>";
}