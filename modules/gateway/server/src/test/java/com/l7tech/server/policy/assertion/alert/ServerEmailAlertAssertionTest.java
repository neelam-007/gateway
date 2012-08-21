package com.l7tech.server.policy.assertion.alert;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.boot.GatewayPermissiveLoggingSecurityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.CollectionUtils;
import com.sun.mail.smtp.SMTPTransport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


/**
 * Date: Sep 23, 2010
 * Time: 3:14:30 PM
 * @author grduck
 * @author jwilliams
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerEmailAlertAssertionTest {

    @Spy
    private SMTPTransport smtpTransport = new SMTPTransport(Session.getInstance(new Properties(), null), null);

    private ServerEmailAlertAssertion spyServer;

    @Autowired
    private ApplicationContext applicationContext;
    private PolicyEnforcementContext policyContext;
    private EmailAlertAssertion assertion;
    private ServerEmailAlertAssertion serverAssertion;

    private TestAudit testAudit;

    private SecurityManager originalSecurityManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        testAudit = new TestAudit();
        assertion = new EmailAlertAssertion();
        serverAssertion = new ServerEmailAlertAssertion(assertion, applicationContext);
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        setDefaultTestData();

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new GatewayPermissiveLoggingSecurityManager());

        myMocks();
    }

    @After
    public void tearDown() throws Exception {
        System.setSecurityManager(originalSecurityManager);
    }

    private void myMocks() throws Exception {
        spyServer = spy(serverAssertion);

        doReturn(smtpTransport).when(spyServer).getTransport((Session) anyObject(), anyString());
        doNothing().when(smtpTransport).sendMessage((javax.mail.Message) any(), (Address[]) any());
    }

    @Test
    @BugNumber(12847)
    public void testCheckRequestWithSpaceBeforeWildcardInHost_AssertionFailed() throws Exception {
        policyContext.setVariable("host", " *.example.com");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
        assertTrue(testAudit.isAuditPresentContaining("Unexpected error sending email"));
    }

    @Test
    public void testCheckRequestWithOutOfRangePortNumber_AssertionFailed() throws Exception {
        policyContext.setVariable("port", "66666");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAILALERT_BAD_PORT));
    }

    @Test
    public void testCheckRequestWithNonNumericPortNumber_AssertionFailed() throws Exception {
        policyContext.setVariable("port", "aksjldf");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAILALERT_BAD_PORT));
    }

    @Test
    public void testCheckRequestWithBadToAddress_AssertionFailed() throws Exception {
        policyContext.setVariable("toAddress", "@ak#sjl(df");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAILALERT_BAD_TO_ADDR));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
    }

    @Test
    public void testCheckRequestWithBadFromAddress_AssertionFailed() throws Exception {
        policyContext.setVariable("fromAddress", "@ak#sjl(df");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAILALERT_BAD_FROM_ADDR));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
    }

    @Test
    public void testCheckRequestWithBadHost_AssertionFailed() throws Exception {
        policyContext.setVariable("host", "");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAILALERT_BAD_HOST));
    }

    @Test
    public void testCheckRequestWithAuthenticateAndNoUser_AssertionFailed() throws Exception {
        policyContext.setVariable("user", "");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAILALERT_BAD_USER));
    }

    @Test
    public void testCheckRequestWithAuthenticateAndNoPassword_AssertionFailed() throws Exception {
        policyContext.setVariable("pwd", "");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAILALERT_BAD_PWD));
    }

    @Test
    public void testCheckRequestWithAuthenticationFailedException_AssertionFailed() throws Exception {
        doThrow(new AuthenticationFailedException()).when(smtpTransport)
                .connect(anyString(), anyInt(), anyString(), anyString());

        final AssertionStatus assertionStatus = spyServer.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAILALERT_AUTH_FAIL));
    }

    @Test
    public void testCheckRequestWithMessagingException_AssertionFailed() throws Exception {
        doThrow(new MessagingException()).when(smtpTransport).connect(anyString(), anyInt(), anyString(), anyString());

        final AssertionStatus assertionStatus = spyServer.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
    }

    @Test
    public void testCheckRequestWithMessagingExceptionByConnectException_AssertionFailed() throws Exception {
        doThrow(new MessagingException(null, new ConnectException())).when(smtpTransport)
                .connect(anyString(), anyInt(), anyString(), anyString());

        final AssertionStatus assertionStatus = spyServer.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAILALERT_CONNECT_FAIL));
    }

    @Test
    public void testCheckRequestWithMessagingExceptionBySSLHandshakeException_AssertionFailed() throws Exception {
        doThrow(new MessagingException(null, new SSLHandshakeException(null))).when(smtpTransport)
                .connect(anyString(), anyInt(), anyString(), anyString());

        final AssertionStatus assertionStatus = spyServer.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAILALERT_SSL_FAIL));
    }

    @Test
    public void testCheckRequestWithMessagingExceptionByAuthenticationFailure_AssertionFailed() throws Exception {
        doThrow(new MessagingException("[EOF]")).when(smtpTransport)
                .connect(anyString(), anyInt(), anyString(), anyString());

        final AssertionStatus assertionStatus = spyServer.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAILALERT_AUTH_FAIL));
    }

    @Test
    @BugNumber(8681)
    public void testCheckRequestWithContextVariables_Success() throws Exception {
        doNothing().when(smtpTransport).connect(anyString(), anyInt(), anyString(), anyString());

        assertion.setIsTestBean(true);
        assertTrue(containsContextVar(assertion.getSmtpHost()));

        AssertionStatus status = null;

        try {
            status = spyServer.checkRequest(policyContext);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        verify(smtpTransport, times(1)).connect(anyString(), anyInt(), anyString(), anyString());
        verify(smtpTransport, times(1)).sendMessage((javax.mail.Message) any(), (Address[]) any());

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAILALERT_MESSAGE_SENT));

        // confirm that the context variables have been replaced.
        assertFalse(containsContextVar(assertion.getSmtpHost()));
        assertFalse(containsContextVar(assertion.getSmtpPort()));
        assertFalse(containsContextVar(assertion.getAuthUsername()));
        assertFalse(containsContextVar(assertion.getAuthPassword()));
        assertFalse(containsContextVar(assertion.getSubject()));
        assertFalse(containsContextVar(assertion.getSourceEmailAddress()));
        assertFalse(containsContextVar(assertion.getTargetBCCEmailAddress()));
        assertFalse(containsContextVar(assertion.getTargetCCEmailAddress()));
        assertFalse(containsContextVar(assertion.getTargetEmailAddress()));
    }

    private boolean containsContextVar(String s){
        return s.matches(".*\\$\\{.*");
    }

    /**
     * Create default assertion & server test data to work with.
     */
    private void setDefaultTestData() {
        assertion.setSmtpHost("${host}");
        assertion.setSmtpPort("${port}");
        assertion.setAuthenticate(true);
        assertion.setAuthPassword("${pwd}");
        assertion.setContextVarPassword(true);
        assertion.setAuthUsername("${user}");
        assertion.setBase64message("Message with ${var}.");
        assertion.setTargetEmailAddress("${toAddress}");
        assertion.setTargetCCEmailAddress("${ccAddress}");
        assertion.setTargetBCCEmailAddress("${bccAddress}");
        assertion.setSourceEmailAddress("${fromAddress}");
        assertion.setSubject("${subject}");

        policyContext.setVariable("host", "mail.example.com");
        policyContext.setVariable("port", "25");
        policyContext.setVariable("pwd", "password!");
        policyContext.setVariable("user", "jsmith");
        policyContext.setVariable("var", "this is the value of var");
        policyContext.setVariable("toAddress", "toAddress1@email.com,toAddress2@email.com");
        policyContext.setVariable("ccAddress", "ccAddress2@email.com,ccAddress@email2.com");
        policyContext.setVariable("bccAddress", "bccAddress1@email.com,bccAddress2@email.com");
        policyContext.setVariable("fromAddress", "fromAddress@email.com");
        policyContext.setVariable("subject", "This is the subject");
    }
}
