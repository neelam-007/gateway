package com.l7tech.external.assertions.email.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.email.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.boot.GatewayPermissiveLoggingSecurityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.CollectionUtils;

import com.l7tech.util.HexUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.SSLHandshakeException;
import java.io.ByteArrayInputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import static com.l7tech.external.assertions.email.EmailAttachmentException.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Date: Sep 23, 2010
 * Time: 3:14:30 PM
 * @author grduck
 * @author jwilliams
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerEmailAssertionTest {

    @Spy
    private EmailSender sender =  EmailSender.getInstance();
    private ServerEmailAssertion spyServer;

    @Autowired
    private ApplicationContext applicationContext;
    private PolicyEnforcementContext policyContext;
    private EmailAssertion assertion;
    private ServerEmailAssertion serverAssertion;
    private ServerConfigStub serverConfig;
    private TestAudit testAudit;

    private SecurityManager originalSecurityManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        serverConfig = applicationContext.getBean("serverConfig", ServerConfigStub.class);
        serverConfig.putProperty("email.attachments.maxSize", "40");
        testAudit = new TestAudit();
        assertion = new EmailAssertion();
        serverAssertion = new ServerEmailAssertion(assertion, applicationContext, sender);
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
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_BAD_PORT));
    }

    @Test
    public void testCheckRequestWithNonNumericPortNumber_AssertionFailed() throws Exception {
        policyContext.setVariable("port", "aksjldf");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_BAD_PORT));
    }

    @Test
    public void testCheckRequestWithBadToAddress_AssertionFailed() throws Exception {
        policyContext.setVariable("toAddress", "@ak#sjl(df");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_BAD_TO_ADDR));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
    }

    @Test
    public void testCheckRequestWithBadFromAddress_AssertionFailed() throws Exception {
        policyContext.setVariable("fromAddress", "@ak#sjl(df");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_BAD_FROM_ADDR));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
    }

    @Test
    public void testCheckRequestWithBadHost_AssertionFailed() throws Exception {
        policyContext.setVariable("host", "");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_BAD_HOST));
    }

    @Test
    public void testCheckRequestWithAuthenticateAndNoUser_AssertionFailed() throws Exception {
        policyContext.setVariable("user", "");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_BAD_USER));
    }

    @Test
    public void testCheckRequestWithAuthenticateAndNoPassword_AssertionFailed() throws Exception {
        policyContext.setVariable("pwd", "");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_BAD_PWD));
    }

    @Test
    public void testCheckRequestWithAuthenticationFailedException_AssertionFailed() throws Exception {
        doThrow(new AuthenticationFailedException()).when(sender).send((EmailConfig) any(),
                (EmailMessage) any());

        final AssertionStatus assertionStatus = spyServer.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_AUTH_FAIL));
    }

    @Test
    public void testCheckRequestWithMessagingException_AssertionFailed() throws Exception {
        doThrow(new MessagingException()).when(sender).send((EmailConfig) any(), (EmailMessage) any());

        final AssertionStatus assertionStatus = spyServer.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
    }

    @Test
    public void testCheckRequestWithMessagingExceptionByConnectException_AssertionFailed() throws Exception {
        doThrow(new MessagingException(null, new ConnectException())).when(sender).send((EmailConfig) any(),
                (EmailMessage) any());

        final AssertionStatus assertionStatus = spyServer.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_CONNECT_FAIL));
    }

    @Test
    public void testCheckRequestWithMessagingExceptionBySSLHandshakeException_AssertionFailed() throws Exception {
        doThrow(new MessagingException(null, new SSLHandshakeException(null))).when(sender).send((EmailConfig) any(),
                (EmailMessage) any());

        final AssertionStatus assertionStatus = spyServer.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_SSL_FAIL));
    }

    @Test
    public void testCheckRequestWithMessagingExceptionByAuthenticationFailure_AssertionFailed() throws Exception {
        doThrow(new MessagingException("[EOF]")).when(sender).send((EmailConfig) any(), (EmailMessage) any());

        final AssertionStatus assertionStatus = spyServer.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_AUTH_FAIL));
    }

    @Test
    @BugNumber(8681)
    public void testCheckRequestWithContextVariables_Success() throws Exception {
        doNothing().when(sender).send((EmailConfig) any(), (EmailMessage) any());

        assertion.setIsTestBean(true);
        assertTrue(containsContextVar(assertion.getSmtpHost()));

        AssertionStatus status = null;

        try {
            status = spyServer.checkRequest(policyContext);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        verify(sender, times(1)).send((EmailConfig) any(), (EmailMessage) any());

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_MESSAGE_SENT));

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

    @Test
    public void testEmailSentSuccessfully() throws Exception {
        doNothing().when(sender).send((EmailConfig) any(), (EmailMessage) any());

        assertion.setBase64message(HexUtils.encodeBase64("Hello".getBytes()));
        assertion.setSmtpPort("25");
        assertion.setSmtpHost("mail");
        assertion.setSourceEmailAddress("L7SSG@NOMAILBOX");
        assertion.setTargetEmailAddress("ssgautotest@layer7tech.com");
        assertion.setFormat(EmailFormat.PLAIN_TEXT);

        EmailAttachment attachment = new EmailAttachment("Hello.txt", "textContent", false);
        List<EmailAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(attachment);
        assertion.setAttachments(attachmentList);

        Message message = new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new
                ByteArrayInputStream("This is the attachment content.".getBytes()));
        policyContext.setVariable("textContent", message);

        AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_MESSAGE_SENT));
    }

    @Test
    public void testEmail_DuplicateNameError() throws Exception {
        doNothing().when(sender).send((EmailConfig) any(), (EmailMessage) any());

        assertion.setBase64message(HexUtils.encodeBase64("Hello".getBytes()));
        assertion.setSmtpPort("25");
        assertion.setSmtpHost("mail");
        assertion.setSourceEmailAddress("L7SSG@NOMAILBOX");
        assertion.setTargetEmailAddress("ssgautotest@layer7tech.com");
        assertion.setFormat(EmailFormat.PLAIN_TEXT);

        EmailAttachment attachment = new EmailAttachment("text.txt", "textContent", false);
        EmailAttachment attachment2 = new EmailAttachment("text.txt", "textContent2", false);
        List<EmailAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(attachment);
        attachmentList.add(attachment2);
        assertion.setAttachments(attachmentList);

        Message message = new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new
                ByteArrayInputStream("This is the attachment content.".getBytes()));
        Message message2 = new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new
                ByteArrayInputStream("This is the attachment 2 content.".getBytes()));
        policyContext.setVariable("textContent", message);
        policyContext.setVariable("textContent2", message2);
        AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_ATTACHMENT_INVALID));
        assertTrue(testAudit.isAuditPresentContaining(DUPLICATE_ATTACHMENT_NAME));
    }

    @Test
    public void testEmail_AttachmentNameError() throws Exception {
        doNothing().when(sender).send((EmailConfig) any(), (EmailMessage) any());

        assertion.setBase64message(HexUtils.encodeBase64("Hello".getBytes()));
        assertion.setSmtpPort("25");
        assertion.setSmtpHost("mail");
        assertion.setSourceEmailAddress("L7SSG@NOMAILBOX");
        assertion.setTargetEmailAddress("ssgautotest@layer7tech.com");
        assertion.setFormat(EmailFormat.PLAIN_TEXT);

        EmailAttachment attachment = new EmailAttachment(null, "textContent", false);
        List<EmailAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(attachment);
        assertion.setAttachments(attachmentList);

        Message message = new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new
                ByteArrayInputStream("This is the attachment content.".getBytes()));
        policyContext.setVariable("textContent", message);
        AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_ATTACHMENT_INVALID));
        assertTrue(testAudit.isAuditPresentContaining(INVALID_ATTACHMENT_NAME));
    }

    @Test
    public void testEmail_UndefinedAttachmentSourceVariableError() throws Exception {
        doNothing().when(sender).send((EmailConfig) any(), (EmailMessage) any());

        assertion.setBase64message(HexUtils.encodeBase64("Hello".getBytes()));
        assertion.setSmtpPort("25");
        assertion.setSmtpHost("mail");
        assertion.setSourceEmailAddress("L7SSG@NOMAILBOX");
        assertion.setTargetEmailAddress("ssgautotest@layer7tech.com");
        assertion.setFormat(EmailFormat.PLAIN_TEXT);

        EmailAttachment attachment = new EmailAttachment("text.txt", "textContent", false);
        List<EmailAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(attachment);
        assertion.setAttachments(attachmentList);

        AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_ATTACHMENT_INVALID));
        assertTrue(testAudit.isAuditPresentContaining(UNDEFINED_ATTACHMENT_SOURCE_VARIABLE));
    }

    @Test
    public void testEmail_AttachmentMaxSizeExceedsError() throws Exception {
        /**
         * Reading the input stream using data source to mock the
         * runtime behaviour of sending email to validate the attachment size.
         */
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final EmailMessage message = invocationOnMock.getArgumentAt(1, EmailMessage.class);
                final ByteArrayDataSource dataSource = new ByteArrayDataSource(
                        message.getAttachmentDataSources().get(0).getInputStream(), "test");
                return null;
            }
        }).when(sender).send((EmailConfig) any(), (EmailMessage) any());

        assertion.setBase64message(HexUtils.encodeBase64("Hello".getBytes()));
        assertion.setSmtpPort("25");
        assertion.setSmtpHost("mail");
        assertion.setSourceEmailAddress("L7SSG@NOMAILBOX");
        assertion.setTargetEmailAddress("ssgautotest@layer7tech.com");
        assertion.setFormat(EmailFormat.PLAIN_TEXT);

        Message message = new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new
                ByteArrayInputStream("This is the attachment content which is used to validate the maxSize of the attachment.".getBytes()));

        policyContext.setVariable("textContent", message);
        EmailAttachment attachment = new EmailAttachment("text.txt", "textContent", false);
        List<EmailAttachment> attachmentList = new ArrayList<>();
        attachmentList.add(attachment);
        assertion.setAttachments(attachmentList);

        AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMAIL_ATTACHMENT_INVALID));
        assertTrue(testAudit.isAuditPresentContaining(ATTACHMENT_SIZE_EXCEEDS));
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
