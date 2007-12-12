/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.server.policy.assertion.alert;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
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
public class ServerEmailAlertAssertion extends AbstractServerAssertion<EmailAlertAssertion> {
    private static final Logger logger = Logger.getLogger(ServerEmailAlertAssertion.class.getName());

    private final Auditor auditor;
    private final Properties props;
    private final InternetAddress[] toAddresses;
    private final InternetAddress[] ccAddresses;
    private final InternetAddress[] bccAddresses;
    private final InternetAddress[] recipients;
    private final InternetAddress fromAddress;

    private static final Map<Properties, Session> sessionCache = new WeakHashMap<Properties, Session>();
    private Session session = null;
    private final String[] varsUsed;

    public ServerEmailAlertAssertion(EmailAlertAssertion ass, ApplicationContext spring) {
        super(ass);
        auditor = new Auditor(this, spring, logger);

        props = new Properties();
        props.setProperty("mail.from", ass.getSourceEmailAddress());
        props.setProperty("mail.smtp.host", ass.getSmtpHost());
        props.setProperty("mail.smtp.port", Integer.toString(ass.getSmtpPort()));
        props.setProperty("mail.stmp.sendpartial", "true");

        InternetAddress[] addr;
        try {
            addr = InternetAddress.parse(ass.getTargetEmailAddress());
        } catch (AddressException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Unable to initialize EmailAlertAssert: invalid destination email address"}, e);
            addr = null;
        }
        toAddresses = addr;

        try {
            addr = InternetAddress.parse(ass.getTargetCCEmailAddress());
        } catch (AddressException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] {"Unable to initialize EmailAlertAssert: invalid CC email address"}, e);
            addr = null;
        }
        ccAddresses = addr;

        try {
            addr = InternetAddress.parse(ass.getTargetBCCEmailAddress());
        } catch (AddressException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] {"Unable to initialize EmailAlertAssert: invalid BCC email address"}, e);
            addr = null;
        }
        bccAddresses = addr;

        int size = 0;
        if(toAddresses != null) size += toAddresses.length;
        if(ccAddresses != null) size += ccAddresses.length;
        if(bccAddresses != null) size += bccAddresses.length;
        recipients = new InternetAddress[size];

        int i = 0;
        if(toAddresses != null) {
            for(InternetAddress address : toAddresses) {
                recipients[i++] = address;
            }
        }
        if(ccAddresses != null) {
            for(InternetAddress address : ccAddresses) {
                recipients[i++] = address;
            }
        }
        if(bccAddresses != null) {
            for(InternetAddress address : bccAddresses) {
                recipients[i++] = address;
            }
        }

        InternetAddress fromaddr;
        try {
            fromaddr = new InternetAddress(ass.getSourceEmailAddress());
        } catch (AddressException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    "Unable to initialize EmailAlertAssert: invalid destination email address: " + ExceptionUtils.getMessage(e));
            fromaddr = null;
        }
        fromAddress = fromaddr;
        varsUsed = ass.getVariablesUsed();
    }

    /**
     * Find a shared session that uses the same SMTP host, SMTP port, and mail From address.
     *
     * @return a session, either new or reusing one from another ServerEmailAlertAssertion with compatible settings.
     */
    private Session getSession() {
        if (session != null)
            return session;

        synchronized (sessionCache) {
            session = sessionCache.get(props);
            if (session == null) {
                session = Session.getInstance(props, null);
                sessionCache.put(props, session);
            }
            return session;
        }
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (toAddresses == null) {
            auditor.logAndAudit(AssertionMessages.EMAILALERT_BAD_TO_ADDR);
            return AssertionStatus.FAILED;
        }

        if (fromAddress == null) {
            auditor.logAndAudit(AssertionMessages.EMAILALERT_BAD_FROM_ADDR);
            return AssertionStatus.FAILED;
        }

        try {
            final Session session = getSession();

            MimeMessage message = new MimeMessage(session);
            message.addRecipients(javax.mail.Message.RecipientType.TO, toAddresses);
            message.addRecipients(javax.mail.Message.RecipientType.CC, ccAddresses);
            message.addRecipients(javax.mail.Message.RecipientType.BCC, bccAddresses);
            message.setFrom(fromAddress);
            message.setSentDate(new java.util.Date());
            message.setSubject(assertion.getSubject());
            message.setText(ExpandVariables.process(assertion.messageString(), context.getVariableMap(varsUsed, auditor), auditor));
            message.saveChanges();

            Transport tr = session.getTransport("smtp");
            tr.connect(assertion.getSmtpHost(), assertion.getSmtpPort(), null, null);
            tr.sendMessage(message, recipients);
        } catch (MessagingException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Unable to send email: " + e.getMessage()}, e);
            return AssertionStatus.FAILED;
        }
        auditor.logAndAudit(AssertionMessages.EMAILALERT_MESSAGE_SENT);
        return AssertionStatus.NONE;
    }
}
