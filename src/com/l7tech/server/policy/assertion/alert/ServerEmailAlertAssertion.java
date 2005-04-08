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
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.logging.Logger;

/**
 * Server side implementation of assertion that sends an email alert.
 */
public class ServerEmailAlertAssertion implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerEmailAlertAssertion.class.getName());

    private final Auditor auditor;
    private final EmailAlertAssertion ass;
    private final Properties props;
    private final InternetAddress address;
    private final InternetAddress fromAddress;

    private static Map sessionCache = new WeakHashMap();
    private Session session = null;

    /**
     * Find a shared session that uses the same SMTP host, SMTP port, and mail From address.
     *
     * @return a session, either new or reusing one from another ServerEmailAlertAssertion with compatible settings.
     */
    private Session getSession() {
        if (session != null)
            return session;

        synchronized (sessionCache) {
            session = (Session)sessionCache.get(props);
            if (session == null) {
                session = Session.getInstance(props, null);
                sessionCache.put(props, session);
            }
            return session;
        }
    }

    public ServerEmailAlertAssertion(EmailAlertAssertion ass, ApplicationContext applicationContext) {
        auditor = new Auditor(this, applicationContext, logger);
        this.ass = ass;

        props = new Properties();
        props.setProperty("mail.host", ass.getSmtpHost());
        props.setProperty("mail.from", ass.getSourceEmailAddress());
        props.setProperty("mail.smtp.port", Integer.toString(ass.getSmtpPort()));

        InternetAddress addr;
        try {
            addr = new InternetAddress(ass.getTargetEmailAddress());
        } catch (AddressException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Unable to initialize EmailAlertAssert: invalid destination email address"}, e);
            addr = null;
        }
        address = addr;

        InternetAddress fromaddr;
        try {
            fromaddr = new InternetAddress(ass.getSourceEmailAddress());
        } catch (AddressException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Unable to initialize EmailAlertAssert: invalid destination email address"}, e);
            fromaddr = null;
        }
        fromAddress = fromaddr;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (address == null) {
            auditor.logAndAudit(AssertionMessages.EMAILALERT_BAD_TO_ADDR);
            return AssertionStatus.FAILED;
        }

        if (fromAddress == null) {
            auditor.logAndAudit(AssertionMessages.EMAILALERT_BAD_FROM_ADDR);
            return AssertionStatus.FAILED;
        }

        try {
            MimeMessage message = new MimeMessage(getSession());
            message.addRecipient(javax.mail.Message.RecipientType.TO, address);
            message.setFrom(fromAddress);
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
