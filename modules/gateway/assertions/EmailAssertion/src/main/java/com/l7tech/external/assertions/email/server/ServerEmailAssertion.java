package com.l7tech.external.assertions.email.server;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.external.assertions.email.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.transport.email.EmailUtils;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.net.ssl.SSLHandshakeException;
import javax.activation.DataSource;
import java.io.IOException;
import java.net.ConnectException;
import java.util.*;
import java.util.logging.Level;

import static com.l7tech.external.assertions.email.EmailAttachmentException.*;
import static com.l7tech.external.assertions.email.EmailExceptionHelper.newAttachmentException;
import static com.l7tech.external.assertions.email.EmailMessage.EmailMessageBuilder;

/**
 * Server side implementation of assertion that sends an email.
 */
public class ServerEmailAssertion extends AbstractServerAssertion<EmailAssertion> {
    private final Config config;
    private final EmailSender emailSender;

    /* By default, the size limit on attachments is 2621440 bytes. */
    private static final long DEFAULT_ATTACHMENT_SIZE_LIMIT = 2621440;
    private static final String EMAIL_ATTACHMENT_MAX_SIZE = "email.attachments.maxSize";

    public ServerEmailAssertion(EmailAssertion ass, ApplicationContext spring) {
        this(ass, spring, EmailSender.getInstance());
    }

    public ServerEmailAssertion(EmailAssertion ass, ApplicationContext spring, EmailSender emailSender) {
        super(ass);
        config = spring.getBean("serverConfig", Config.class);
        this.emailSender = emailSender;
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        InternetAddress[] toAddresses;
        InternetAddress[] ccAddresses;
        InternetAddress[] bccAddresses;
        InternetAddress fromAddress = null;
        InternetAddress[] fromAddresses;
        String host;
        String userName;
        String pwd;
        String subject;
        String portNum;
        String[] varsUsed = assertion.getVariablesUsed();
        Map<String, Object> variableMap = context.getVariableMap(varsUsed, getAudit());
        long connectTimeout = config.getTimeUnitProperty( "ioMailConnectTimeout", 60000L );
        long readTimeout = config.getTimeUnitProperty( "ioMailReadTimeout", 60000L );

        //start putting together the lists of email addresses and other variables to send email
        //and replace the context variables
        toAddresses = assembleAddresses(ExpandVariables.process(assertion.getTargetEmailAddress(), variableMap, getAudit()), "destination email address");
        ccAddresses = assembleAddresses(ExpandVariables.process(assertion.getTargetCCEmailAddress(), variableMap, getAudit()), "cc email address");
        bccAddresses = assembleAddresses(ExpandVariables.process(assertion.getTargetBCCEmailAddress(), variableMap, getAudit()), "bcc email address");
        fromAddresses = assembleAddresses(ExpandVariables.process(assertion.getSourceEmailAddress(), variableMap, getAudit()), "from email address");
        if(fromAddresses!=null&&fromAddresses.length>0)
                fromAddress = fromAddresses[0];

        host = assertion.getSmtpHost();
        portNum = assertion.getSmtpPort();
        userName = assertion.getAuthUsername();
        pwd = assertion.getAuthPassword();
        subject = assertion.getSubject();

        host = ExpandVariables.process(host, variableMap, getAudit());
        portNum = ExpandVariables.process(portNum, variableMap, getAudit());
        if(assertion.isAuthenticate())
        {
            userName = ExpandVariables.process(userName, variableMap, getAudit());
            if(assertion.isContextVarPassword())
                pwd = ExpandVariables.process(pwd, variableMap, getAudit());
        }
        subject = ExpandVariables.process(subject, variableMap, getAudit());

        //check if smtp port is valid.  This port could be a context var that needs to be filled in.
        //the port num might be a context var.  get the real value
        if (portNum != null && !portNum.equals("")) {
            boolean isOk = portNumberOk(portNum);
            if(!isOk){
             //bad port number.
              logAndAudit( AssertionMessages.EMAIL_BAD_PORT);
              return AssertionStatus.FAILED;
            }
        }
        //port number is ok, let's keep an int version
        int portNumberInt = Integer.parseInt(portNum);

        if (toAddresses == null) {
            //no to addresses!
            logAndAudit( AssertionMessages.EMAIL_BAD_TO_ADDR);
            return AssertionStatus.FAILED;
        }

        if (fromAddress == null) {
            //no from address!
            logAndAudit( AssertionMessages.EMAIL_BAD_FROM_ADDR);
            return AssertionStatus.FAILED;
        }

        //check that the host name is not a context var
        if(host==null || host.equals("")){
            //host is null or empty, fail the assertion
            logAndAudit( AssertionMessages.EMAIL_BAD_HOST);
            return AssertionStatus.FAILED;
        }

        if(assertion.isAuthenticate())
        {
            if(userName==null || userName.equals("")){
                logAndAudit( AssertionMessages.EMAIL_BAD_USER);
                return AssertionStatus.FAILED;
            }

            if(pwd==null || pwd.equals("")){
                logAndAudit( AssertionMessages.EMAIL_BAD_PWD);
                return AssertionStatus.FAILED;
            }
        }

        if(assertion.isTestBean()){
            //This is a test, set all the expanded values back on the bean so the TestCase will not fail
            assertion.setSubject( EmailUtils.sanitizeSubject( subject ) );
            assertion.setSmtpPort(portNum);
            assertion.setSmtpHost(host);
            assertion.setAuthUsername(userName);
            assertion.setAuthPassword(pwd);
            assertion.setSourceEmailAddress(fromAddress.getAddress());

            //concat all the to, bcc, cc email addresses to a single string and set
            StringBuilder sb = new StringBuilder();
            for(int i=0; i < toAddresses.length; i++){
                sb.append(toAddresses[i].getAddress());
                if(i<toAddresses.length-1)
                    sb.append(",");
            }
            assertion.setTargetEmailAddress(sb.toString());
            sb = new StringBuilder();
            for(int i=0; i < ccAddresses.length; i++){
                sb.append(ccAddresses[i].getAddress());
                if(i<ccAddresses.length-1)
                    sb.append(",");
            }
            assertion.setTargetCCEmailAddress(sb.toString());
            sb = new StringBuilder();
            for(int i=0; i < bccAddresses.length; i++){
                sb.append(bccAddresses[i].getAddress());
                if(i<bccAddresses.length-1)
                    sb.append(",");
            }
            assertion.setTargetBCCEmailAddress(sb.toString());
        }

        try {
            final String body = ExpandVariables.process(assertion.messageString(), context.getVariableMap(varsUsed, getAudit()), getAudit());
            final List<DataSource> attachmentDataSources = prepareAttachmentDataSources(variableMap, assertion.getAttachments());
            final EmailConfig emailConfig = new EmailConfig(assertion.isAuthenticate(), userName, pwd, host, portNumberInt,
                    assertion.getProtocol(), connectTimeout, readTimeout);
            final EmailMessage emailMessage = new EmailMessageBuilder(fromAddress, toAddresses, body, subject)
                    .setFormat(assertion.getFormat())
                    .setCcAddresses(ccAddresses)
                    .setBccAddresses(bccAddresses)
                    .setAttachmentDataSources(attachmentDataSources)
                    .build();

            emailSender.send(emailConfig, emailMessage);
            logAndAudit( AssertionMessages.EMAIL_MESSAGE_SENT, emailMessage.getFormat().getDescription(),
                    String.valueOf(emailMessage.getAttachmentDataSources().size()));
            return AssertionStatus.NONE;
        } catch (MessagingException e) {
            if (ExceptionUtils.causedBy(e, AuthenticationFailedException.class)) {
                logAndAudit( AssertionMessages.EMAIL_AUTH_FAIL, null, ExceptionUtils.getDebugException( e ) );
            } else if (ExceptionUtils.causedBy(e, ConnectException.class)) {
                logAndAudit( AssertionMessages.EMAIL_CONNECT_FAIL, null, ExceptionUtils.getDebugException( e ) );
            } else if (ExceptionUtils.causedBy(e, SSLHandshakeException.class)) {
                logAndAudit( AssertionMessages.EMAIL_SSL_FAIL, null, ExceptionUtils.getDebugException( e ) );
            } else if ("[EOF]".equals(e.getMessage())) {
                logAndAudit( AssertionMessages.EMAIL_AUTH_FAIL, null, ExceptionUtils.getDebugException( e ) );
            } else {
                logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[]{ "Unable to send email: " + e.getMessage() }, ExceptionUtils.getDebugException( e ) );
            }

            return AssertionStatus.FAILED;
        } catch (EmailAttachmentException e) {
            logAndAudit(AssertionMessages.EMAIL_ATTACHMENT_INVALID, e.getMessage());
            return AssertionStatus.FAILED;
        } catch (ByteLimitInputStream.DataSizeLimitExceededException e) {
            logAndAudit(AssertionMessages.EMAIL_ATTACHMENT_INVALID, ATTACHMENT_SIZE_EXCEEDS);
            return AssertionStatus.FAILED;
        } catch (Exception e) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{ "Unexpected error sending email: " + e.getMessage() }, ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.FAILED;
        }
    }

    private InternetAddress[] assembleAddresses(String addresses, String addressName) {
        InternetAddress[] addr = null;
        if(addresses!=null){
            try {
                addr = InternetAddress.parse(addresses);
            } catch (AddressException e) {
                logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "Unable to compose email addresses in ServerEmailAssertion: invalid " + addressName }, ExceptionUtils.getDebugException( e ) );
                addr = null;
            }
        }
        return addr;
    }

    /**
     * check that the port number string is ok
     *
     * @param portNumber the string value of the port number to send the email on
     * @return true if it's ok, false if it's bad
     */
    private boolean portNumberOk(String portNumber){
       try {
          int port = Integer.parseInt(portNumber);
          if (port < 0 || port > 65535){
            //invalid port number log and fail assertion.
            return false;
          }
       }catch (NumberFormatException nfe) {
            return false;
       }
       return true;
    }

    /**
     * This method fetches the value of the context variables and constructs EmailAttachmentDataSource out of it.
     * @param variableMap
     * @param emailAttachments
     * @return list of valid EmailAttachmentDataSource
     * @throws EmailAttachmentException
     * @throws IOException
     */
    private List<DataSource> prepareAttachmentDataSources(
            final Map<String, Object> variableMap,
            final List<EmailAttachment> emailAttachments) throws EmailAttachmentException, IOException {
        final List<DataSource> attachmentDataSources = new ArrayList<>();
        final long maxAttachmentSize = config.getLongProperty(EMAIL_ATTACHMENT_MAX_SIZE, DEFAULT_ATTACHMENT_SIZE_LIMIT);

        for (EmailAttachment attachment : emailAttachments) {
            final Object source = getAttachmentSource(variableMap, attachment);

            if (attachment.isMimePartVariable()) {
                attachmentDataSources.addAll(EmailAttachmentDataSourceHelper.getAttachmentDataSources(attachment, source, maxAttachmentSize));
            } else {
                final String name = getAttachmentName(variableMap, attachment);
                attachmentDataSources.add(EmailAttachmentDataSourceHelper.getAttachmentDataSource(attachment, source, name, maxAttachmentSize));
            }
        }

        verifyAttachmentDataSources(attachmentDataSources);

        return attachmentDataSources;
    }

    private String getAttachmentName(final Map<String, Object> variableMap, EmailAttachment attachment) throws EmailAttachmentException {
        if (StringUtils.isBlank(attachment.getName())) {
            throw newAttachmentException(INVALID_ATTACHMENT_NAME, attachment.getName());
        }

        return ExpandVariables.process(attachment.getName(), variableMap, getAudit());
    }

    private Object getAttachmentSource(final Map<String, Object> variableMap, EmailAttachment attachment) throws EmailAttachmentException {
        final Object source = variableMap.get(attachment.getSourceVariable());

        if (source == null) {
            throw newAttachmentException(UNDEFINED_ATTACHMENT_SOURCE_VARIABLE, attachment.getSourceVariable());
        }

        return source;
    }

    /**
     * This method validates attachments if the name, context variable and the attachment are valid or not
     * @param attachments
     * @throws EmailAttachmentException
     */
    private void verifyAttachmentDataSources(List<DataSource> attachments) throws EmailAttachmentException {
        final Set<String> names = new HashSet<>();

        for (DataSource attachment : attachments) {
            if (names.contains(attachment.getName())) {
                throw newAttachmentException(DUPLICATE_ATTACHMENT_NAME, attachment.getName());
            }

            logger.log(Level.FINE, "Including the attachment: " + attachment.getName());
            names.add(attachment.getName());
        }
    }
}
