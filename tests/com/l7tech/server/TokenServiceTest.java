/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.message.Message;
import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.token.UsernameToken;
import com.l7tech.common.security.token.UsernameTokenImpl;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.proxy.util.TokenServiceClient;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.security.PrivateKey;
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

    public void testTokenServiceClient() throws Exception {
        Document requestMsg = TokenServiceClient.createRequestSecurityTokenMessage(TestDocuments.getDotNetServerCertificate(),
                                                                    TestDocuments.getDotNetServerPrivateKey(),
                                                                    SecurityTokenType.WSSC,
                                                                    TokenServiceClient.RequestType.ISSUE,
                                                                    null, null, null);
        log.info("Decorated token request (reformatted): " + XmlUtil.nodeToFormattedString(requestMsg));

        final TokenService service = new TokenServiceImpl(TestDocuments.getDotNetServerPrivateKey(),
                                                      TestDocuments.getDotNetServerCertificate(),
                                                      (ServerPolicyFactory)applicationContext.getBean("policyFactory"));

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

        service.respondToSecurityTokenRequest(context, authenticator);

        Document responseMsg = response.getXmlKnob().getDocumentWritable();

        log.info("Decorated response (reformatted): " + XmlUtil.nodeToFormattedString(responseMsg));

        Object responseObj = TokenServiceClient.parseSignedRequestSecurityTokenResponse(responseMsg,
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
        Document requestMsg = TokenServiceClient.createRequestSecurityTokenMessage(
                subjectCertificate,
                subjectPrivateKey,
                SecurityTokenType.SAML_AUTHENTICATION,
                TokenServiceClient.RequestType.ISSUE,
                null, null, null);
        requestMsg.getDocumentElement().setAttribute("xmlns:saml", SamlConstants.NS_SAML);
        log.info("Decorated token request (reformatted): " + XmlUtil.nodeToFormattedString(requestMsg));

        final TokenService service = new TokenServiceImpl(issuerPrivateKey,
                                                          issuerCertificate,
                                                          (ServerPolicyFactory)applicationContext.getBean("policyFactory"));

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

        final PolicyEnforcementContext context = new PolicyEnforcementContext(request, response, getFakeServletRequest(), null);

        service.respondToSecurityTokenRequest(context, authenticator);

        Document responseMsg = response.getXmlKnob().getDocumentWritable();

        log.info("Decorated response (reformatted): " + XmlUtil.nodeToFormattedString(responseMsg));

        Object responseObj = TokenServiceClient.parseSignedRequestSecurityTokenResponse(responseMsg,
                                                                                  subjectCertificate,
                                                                                  subjectPrivateKey,
                                                                                  issuerCertificate);
        assertTrue("Token obtained must be SAML", responseObj instanceof SamlAssertion);
        SamlAssertion token = (SamlAssertion)responseObj;
        assertTrue("Obtained saml token must be signed", token.isSigned());
        assertTrue("Obtained saml token must be holder-of-key", token.isHolderOfKey());
        assertTrue("Obtained saml token must have non-null assertion ID", token.getAssertionId() != null);
        assertTrue("Obtained saml token must have non-empty assertion ID", token.getAssertionId().length() > 0);
        assertTrue("Obtained saml token must have subject certificate", token.getSubjectCertificate() != null);
        assertTrue("Obtained saml token must have issuer certificate", token.getIssuerCertificate() != null);
        assertEquals("subject certificate must match our client certificate",
                     token.getSubjectCertificate(),
                     subjectCertificate);
        assertEquals("issuer certificate must match the expected issuer certificate",
                     token.getIssuerCertificate(),
                     issuerCertificate);
        token.verifyIssuerSignature();
    }

    private HttpServletRequest getFakeServletRequest() {
        return new MockHttpServletRequest();
    }

    public void testCreateFimRst() throws Exception {

        UsernameToken usernameToken = new UsernameTokenImpl("testuser", "passw0rd".toCharArray());

        // TODO after FIM interop use AppliesTo like http://l7tech.com/services/TokenServiceTest instead
        Document rstDoc = TokenServiceClient.createRequestSecurityTokenMessage(null,
                                                                                    TokenServiceClient.RequestType.VALIDATE,
                                                                                    usernameToken,
                                                                                    "http://samlpart.com/sso");
        assertNotNull(rstDoc);

        // TODO check this somehow, other than uncommenting below line and eyeballing the diff
//        InputStream fimIs = getClass().getClassLoader().getResourceAsStream("com/l7tech/example/resources/tivoli/FIM_RST.xml");
//        final String origRst = XmlUtil.nodeToFormattedString(XmlUtil.parse(fimIs));
//        String rst = XmlUtil.nodeToFormattedString(rstDoc);
//        assertEquals(rst, origRst);
    }

    public void testParseFimRstr() throws Exception {
        InputStream respIs = getClass().getClassLoader().getResourceAsStream("com/l7tech/example/resources/tivoli/tivoliFIM_RSTR.xml");
        final Document rstr = XmlUtil.parse(respIs);

        Object got = TokenServiceClient.parseUnsignedRequestSecurityTokenResponse(rstr);

        SamlAssertion saml = (SamlAssertion)got;
        assertEquals(saml.getNameIdentifierFormat(), SamlConstants.NAMEIDENTIFIER_EMAIL);
        assertEquals(saml.getNameIdentifierValue(), "testloser");

        assertFalse(saml.isHolderOfKey());
        assertFalse(saml.isSenderVouches());
        assertNull(saml.getConfirmationMethod());
        saml.verifyIssuerSignature();

        log.info("Issuer cert = " + saml.getIssuerCertificate());
        log.info("Subject cert = " + saml.getSubjectCertificate());
        log.info("Got SAML assertion (reformatted): " + XmlUtil.nodeToFormattedString(saml.asElement()));
    }
}