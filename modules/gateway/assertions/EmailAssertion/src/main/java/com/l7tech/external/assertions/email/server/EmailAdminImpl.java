package com.l7tech.external.assertions.email.server;

import com.l7tech.external.assertions.email.EmailAdmin;
import com.l7tech.external.assertions.email.EmailConfig;
import com.l7tech.external.assertions.email.EmailMessage;

import javax.mail.MessagingException;

/**
 * Implementation of the Email Admin interface
 */
public class EmailAdminImpl implements EmailAdmin {

    private EmailSender emailSender;

    public EmailAdminImpl() {
        this(EmailSender.getInstance());
    }

    public EmailAdminImpl(final EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void sendTestEmail(final EmailMessage emailMessage, final EmailConfig emailConfig) throws MessagingException {
        emailSender.send(emailConfig, emailMessage);
    }

}
