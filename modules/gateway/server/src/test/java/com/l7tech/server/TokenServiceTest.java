/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
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
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.security.xml.processor.ProcessorException;
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
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class TokenServiceTest extends TestCase {
    private static Logger log = Logger.getLogger(TokenServiceTest.class.getName());
    static ApplicationContext applicationContext = null;

    public TokenServiceTest(String name) {
        super(name);
    }

    public static Test suite() {
         final TestSuite suite = new TestSuite(TokenServiceTest.class);
         TestSetup wrapper = new TestSetup(suite) {

             protected void setUp() throws Exception {
                 applicationContext = ApplicationContexts.getTestApplicationContext();
             }

             protected void tearDown() throws Exception {
                 ;
             }
         };
         return wrapper;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private TokenServiceClient tokenServiceClient = new TokenServiceClient(WsTrustConfigFactory.getDefaultWsTrustConfig());
    public void testTokenServiceClient() throws Exception {
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
            public AssertionStatus respondToSecurityTokenRequest(PolicyEnforcementContext context, CredentialsAuthenticator authenticator, boolean useThumbprintForSamlSignature, boolean useThumbprintForSamlSubject) throws InvalidDocumentFormatException, TokenServiceException, ProcessorException, GeneralSecurityException, AuthenticationException {
                setApplicationContext(TokenServiceTest.applicationContext);
                return super.respondToSecurityTokenRequest(context, authenticator, useThumbprintForSamlSignature, useThumbprintForSamlSubject);
            }
        };

        final TokenServiceImpl.CredentialsAuthenticator authenticator = new TokenServiceImpl.CredentialsAuthenticator() {

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
        assertEquals(status, AssertionStatus.NONE);

        Document responseMsg = response.getXmlKnob().getDocumentWritable();

        log.info("Decorated response (reformatted): " + XmlUtil.nodeToFormattedString(responseMsg));

        Object responseObj = tokenServiceClient.parseSignedRequestSecurityTokenResponse(responseMsg,
                                                                   TestDocuments.getDotNetServerCertificate(),
                                                                   TestDocuments.getDotNetServerPrivateKey(),
                                                                   TestDocuments.getDotNetServerCertificate());

        assertTrue(responseObj instanceof TokenServiceClient.SecureConversationSession);
        TokenServiceClient.SecureConversationSession session = (TokenServiceClient.SecureConversationSession)responseObj;
        log.info("Got session! Session id=" + session.getSessionId());
        log.info("Session expiry date=" + session.getExpiryDate());
        log.info("Session shared secret=" + HexUtils.hexDump(session.getSharedSecret()));
    }

    public void testTokenServiceClientSaml() throws Exception {
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
        assertEquals(status, AssertionStatus.NONE);

        Document responseMsg = response.getXmlKnob().getDocumentWritable();

        log.info("Decorated response (reformatted): " + XmlUtil.nodeToFormattedString(responseMsg));

        Object responseObj = tokenServiceClient.parseSignedRequestSecurityTokenResponse(responseMsg,
                                                                                  subjectCertificate,
                                                                                  subjectPrivateKey,
                                                                                  issuerCertificate);
        assertTrue("Token obtained must be SAML", responseObj instanceof SamlAssertion);
        SamlAssertion token = (SamlAssertion)responseObj;
        assertTrue("Obtained saml token must be signed", token.hasEmbeddedIssuerSignature());
        assertTrue("Obtained saml token must be holder-of-key", token.isHolderOfKey());
        assertTrue("Obtained saml token must have non-null assertion ID", token.getAssertionId() != null);
        assertTrue("Obtained saml token must have non-empty assertion ID", token.getAssertionId().length() > 0);
        assertTrue("Obtained saml token must have subject certificate", token.getSubjectCertificate() != null);
        assertTrue("Obtained saml token must have issuer certificate", token.getIssuerCertificate() != null);
        assertTrue("Obtained saml token must have embedded issuer certificate", token.hasEmbeddedIssuerSignature());
        assertEquals("subject certificate must match our client certificate",
                     token.getSubjectCertificate(),
                     subjectCertificate);
        assertEquals("issuer certificate must match the expected issuer certificate",
                     token.getIssuerCertificate(),
                     issuerCertificate);
        token.verifyEmbeddedIssuerSignature();
    }

    private HttpServletRequest getFakeServletRequest() {
        MockHttpServletRequest mhsr = new MockHttpServletRequest();
        mhsr.setMethod("POST");
        return mhsr;
    }

    public void testCreateFimRst() throws Exception {

        UsernameToken usernameToken = new UsernameTokenImpl("testuser", "passw0rd".toCharArray());
        Element utElm = usernameToken.asElement();
        SoapUtil.setWsuId(utElm, SoapUtil.WSU_NAMESPACE, "UsernameToken-1");

        // TODO after FIM interop use AppliesTo like http://l7tech.com/services/TokenServiceTest instead
        Document rstDoc = tokenServiceClient.createRequestSecurityTokenMessage(null,
                                                                                    WsTrustRequestType.VALIDATE,
                                                                                    usernameToken,
                                                                                    "http://samlpart.com/sso", null);
        assertNotNull(rstDoc);

        // TODO check this somehow, other than uncommenting below line and eyeballing the diff
        //InputStream fimIs = getClass().getClassLoader().getResourceAsStream("com/l7tech/example/resources/tivoli/FIM_RST.xml");
        //final String origRst = XmlUtil.nodeToFormattedString(XmlUtil.parse(fimIs));
        //String rst = XmlUtil.nodeToFormattedString(rstDoc);
        //assertEquals(rst, origRst);
    }

    public void testParseFimRstr() throws Exception {
        InputStream respIs = TokenServiceTest.class.getResourceAsStream("tivoliFIM_RSTR.xml");
        assertNotNull( "Message resource input", respIs );
        final Document rstr = XmlUtil.parse(respIs);

        Object got = tokenServiceClient.parseUnsignedRequestSecurityTokenResponse(rstr);

        SamlAssertion saml = (SamlAssertion)got;
        assertEquals(saml.getNameIdentifierFormat(), SamlConstants.NAMEIDENTIFIER_EMAIL);
        assertEquals(saml.getNameIdentifierValue(), "testloser");

        assertFalse(saml.isHolderOfKey());
        assertFalse(saml.isSenderVouches());
        assertNull(saml.getConfirmationMethod());
        saml.verifyEmbeddedIssuerSignature();

        log.info("Issuer cert = " + saml.getIssuerCertificate());
        log.info("Subject cert = " + saml.getSubjectCertificate());
        log.info("Got SAML assertion (reformatted): " + XmlUtil.nodeToFormattedString(saml.asElement()));
    }
}