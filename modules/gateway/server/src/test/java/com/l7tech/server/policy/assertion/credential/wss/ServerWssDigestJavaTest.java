package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ConfigFactory;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ServerWssDigestJavaTest {

    private static Document requestDocument;

    private TestAudit audit;
    private WssDigest assertion;
    private ServerWssDigest serverAssertion;
    private Message request;
    private PolicyEnforcementContext pec;

    @BeforeClass
    public static void init() throws Exception {
        requestDocument = XmlUtil.stringAsDocument("<?xml version='1.0' encoding='UTF-8'?><S:Envelope xml" +
                "ns:S=\"http://www.w3.org/2003/05/soap-envelope\"><S:Header><wsse:Security xmlns:wsse=\"http://docs.o" +
                "asis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" S:mustUnderstand=\"true\"><ws" +
                "se:UsernameToken xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-util" +
                "ity-1.0.xsd\" wsu:Id=\"unt_jH5NZ9r24lIs1049\"><wsse:Username>Alice</wsse:Username><wsse:Password Typ" +
                "e=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDiges" +
                "t\">jlRIlWrSTh0O8I7AgmGX35vcr6Q=</wsse:Password><wsse:Nonce EncodingType=\"http://docs.oasis-open.or" +
                "g/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">NLu+blE91TuBXeCRdQts4X5vBGpP" +
                "/XNq+1PcZSjcQSA=</wsse:Nonce><wsu:Created>2008-11-11T01:52:36Z</wsu:Created></wsse:UsernameToken></w" +
                "sse:Security></S:Header><S:Body><EchoRequest xmlns=\"http://example.com/ws/2008/09/securitypolicy\">" +
                "Test A2113 From Oracle Weblogic Server</EchoRequest></S:Body></S:Envelope>");
    }

    @Before
    public void setUp() throws Exception {
        audit = new TestAudit();

        assertion = new WssDigest();

        // Ensure all test variables get marked as used.  Actual test will override the bean requirements later.
        assertion.setRequiredUsername("${username1}${username2}");
        assertion.setRequiredPassword("${password1}${password2}");
        assertion.setRequireNonce(true);
        assertion.setRequireTimestamp(true);

        serverAssertion = new ServerWssDigest(assertion);

        request = new Message(requestDocument);

        pec = PolicyEnforcementContextFactory
                .createPolicyEnforcementContext(request, new Message());

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                        .put("auditFactory", audit.factory())
                        .put("config", ConfigFactory.getCachedConfig())
                        .put("securityTokenResolver", new SimpleSecurityTokenResolver())
                        .unmodifiableMap()
        );

        pec.setVariable("username1", "Alice");
        pec.setVariable("username2", "blahblah");
        pec.setVariable("password1", "ecilA");
        pec.setVariable("password2", "wkljhdasdds");
    }

    /**
     * fail if request is not SOAP
     */
    @Test
    public void testCheckRequest_RequestNotSoap_NotSoapAuditedAndAssertionNotApplicable() throws Exception {
        request.initialize(XmlUtil.createEmptyDocument("blah", "ns", "urn:blah"));

        assertEquals(AssertionStatus.NOT_APPLICABLE, serverAssertion.checkRequest(pec));

        assertTrue(audit.isAuditPresent(AssertionMessages.REQUEST_NOT_SOAP));
    }

    /**
     * fail if request does not contain a security header
     */
    @Test
    public void testCheckRequest_NoSecurityHeaderInRequest_NoSecurityAuditedAndAssertionFailsOnAuthRequired() throws Exception {
        request.initialize(SoapUtil.createSoapEnvelopeAndGetBody(SoapVersion.SOAP_1_1).getOwnerDocument());

        assertEquals(AssertionStatus.AUTH_REQUIRED, serverAssertion.checkRequest(pec));

        assertTrue(audit.isAuditPresent(AssertionMessages.REQUESTWSS_NO_SECURITY));
    }

    /**
     * succeed if valid username and password are present
     */
    @Test
    public void testCheckRequest_ValidCredentialsSpecified_UserDetailInfoAudited() throws Exception {
        assertion.setRequiredUsername("Alice");
        assertion.setRequiredPassword("ecilA");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.USERDETAIL_INFO));
    }

    /**
     * fail if request username does not match
     */
    @Test
    public void testCheckRequest_InvalidUsernameSpecified_UserDetailInfoAuditedAndAssertionFalsified() throws Exception {
        assertion.setRequiredUsername("qwerasdf");
        assertion.setRequiredPassword("ecilA");

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(pec));

        assertEquals(2, audit.getAuditCount());
        assertTrue(audit.isAuditPresentWithParameters(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                "Ignoring UsernameToken that does not contain a matching username"));
        assertTrue(audit.isAuditPresentWithParameters(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                "No conforming WSS Digest token was found in request"));
    }

    /**
     * fail if request password does not match
     */
    @Test
    public void testCheckRequest_InvalidPasswordSpecified_UserDetailInfoAuditedAndAssertionFalsified() throws Exception {
        assertion.setRequiredUsername("Alice");
        assertion.setRequiredPassword("qwerasdf");

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(pec));

        assertEquals(2, audit.getAuditCount());
        assertTrue(audit.isAuditPresentWithParameters(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                "UsernameToken digest value does not match the expected value"));
        assertTrue(audit.isAuditPresentWithParameters(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                "No conforming WSS Digest token was found in request"));
    }

    /**
     * succeed if valid username and password are present using context variables
     */
    @Test
    public void testCheckRequest_ValidCredentialsSpecifiedInContextVariables_UserDetailInfoAudited() throws Exception {
        assertion.setRequiredUsername("${username1}");
        assertion.setRequiredPassword("${password1}");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.USERDETAIL_INFO));
    }

    /**
     * fail if request username does not match using context variables
     */
    @Test
    public void testCheckRequest_InvalidUsernameSpecifiedInContextVariable_WarningAuditedAndAssertionFalsified() throws Exception {
        assertion.setRequiredUsername("${username2}");
        assertion.setRequiredPassword("ecilA");

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(pec));

        assertEquals(2, audit.getAuditCount());
        assertTrue(audit.isAuditPresentWithParameters(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                "Ignoring UsernameToken that does not contain a matching username"));
        assertTrue(audit.isAuditPresentWithParameters(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                "No conforming WSS Digest token was found in request"));
    }

    /**
     * fail if request password does not match using context variables
     */
    @Test
    public void testCheckRequest_InvalidPasswordSpecifiedInContextVariable_WarningAuditedAndAssertionFalsified() throws Exception {
        assertion.setRequiredUsername("Alice");
        assertion.setRequiredPassword("${password2}");

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(pec));

        assertEquals(2, audit.getAuditCount());
        assertTrue(audit.isAuditPresentWithParameters(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                "UsernameToken digest value does not match the expected value"));
        assertTrue(audit.isAuditPresentWithParameters(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                "No conforming WSS Digest token was found in request"));
    }

    /**
     * pass if assertion recipient context is non-local
     */
    @Test
    public void testCheckRequest_NonLocalRecipientContext_NothingToValidateAudited() throws Exception {
        assertion.setRecipientContext(new XmlSecurityRecipientContext("nonLocalRecipient", null));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        assertEquals(1, audit.getAuditCount());
        assertTrue(audit.isAuditPresent(AssertionMessages.REQUESTWSS_NOT_FOR_US));
    }

    /**
     * fail if no required password specified
     */
    @Test
    public void testCheckRequest_NoRequiredPasswordSpecified_AssertionStatusExceptionThrownForServerError() throws Exception {
        assertion.setRequiredUsername("Alice");
        assertion.setRequiredPassword(null);

        try {
            serverAssertion.checkRequest(pec);
            fail("Expected AssertionStatusException.");
        } catch (AssertionStatusException e) {
            assertEquals(AssertionStatus.SERVER_ERROR, e.getAssertionStatus());
            assertEquals("WssDigest assertion has no password configured", e.getMessage());
        }

        assertEquals(0, audit.getAuditCount());
    }
}