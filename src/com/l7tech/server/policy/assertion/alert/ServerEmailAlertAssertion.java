/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.alert;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Server side implementation of assertion that sends an email alert.
 */
public class ServerEmailAlertAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerEmailAlertAssertion.class.getName());
    private static final String MAIL_FROM = "L7SSG@NOMAILBOX"; // TODO how do I get my own hostname?

    private final ApplicationContext applicationContext;
    private final Auditor auditor;
    private final EmailAlertAssertion ass;
    private final Properties props;
    private final InternetAddress address;

    public ServerEmailAlertAssertion(EmailAlertAssertion ass, ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        auditor = new Auditor(this, applicationContext, logger);
        this.ass = ass;

        props = new Properties();
        props.setProperty("mail.host", ass.getSmtpHost());
        props.setProperty("mail.from", MAIL_FROM);
        props.setProperty("mail.smtp.port", Integer.toString(ass.getSmtpPort()));

        try {
            address = new InternetAddress(ass.getTargetEmailAddress());
        } catch (AddressException e) {
            throw (IllegalArgumentException)new IllegalArgumentException("Invalid email address").initCause(e);
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {


            Session session = Session.getDefaultInstance(props, null);
            MimeMessage message = new MimeMessage(session);
            message.addRecipient(javax.mail.Message.RecipientType.TO, address);
            message.setSubject(ass.getSubject());
            message.setText(ass.getMessage());
            Transport.send(message);
        } catch (MessagingException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Unable to send email: " + e.getMessage()}, e);
            return AssertionStatus.FAILED;
        }
        auditor.logAndAudit(AssertionMessages.EMAILALERT_MESSAGE_SENT);
        return AssertionStatus.NONE;
    }
}
