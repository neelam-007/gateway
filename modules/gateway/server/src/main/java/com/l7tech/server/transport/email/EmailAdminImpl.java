package com.l7tech.server.transport.email;

import com.l7tech.gateway.common.transport.email.EmailAdmin;
import com.l7tech.gateway.common.transport.email.EmailMessage;
import com.l7tech.gateway.common.transport.email.EmailTestException;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.server.ServerConfig;

/**
 * Implementation of the Email Admin interface
 */
public class EmailAdminImpl implements EmailAdmin {
    private final ServerConfig serverConfig;

    public EmailAdminImpl(ServerConfig serverConfig) {
        if (serverConfig == null) throw new IllegalArgumentException("Server config cannot be null");
        this.serverConfig = serverConfig;
    }

    public void testSendEmail(String toAddr, String ccAddr, String bccAddr, String fromAddr, String subject, String host,
                                 int port, String base64Message, EmailAlertAssertion.Protocol protocol, boolean authenticate,
                                 String authUsername, String authPassword) throws EmailTestException {

        long connectTimeout = 25000;
        long readTimeout = 25000;

        EmailConfig emailConfig = new EmailConfig(authenticate, authUsername, authPassword, host, port, protocol, connectTimeout, readTimeout);
        EmailMessage emailMessage = new EmailMessage(bccAddr, ccAddr, fromAddr, toAddr, base64Message, subject);
        EmailUtils.sendTestMessage(emailMessage, emailConfig);
    }

    public void testSendEmail(EmailAlertAssertion eaa) throws EmailTestException {
        long connectTimeout = 25000;
        long readTimeout = 25000;

        EmailConfig emailConfig = new EmailConfig(eaa.isAuthenticate(), eaa.getAuthUsername(), eaa.getAuthPassword(),
                eaa.getSmtpHost(), eaa.getSmtpPort(), eaa.getProtocol(), connectTimeout, readTimeout);

        EmailMessage emailMessage = new EmailMessage(eaa.getTargetBCCEmailAddress(), eaa.getTargetCCEmailAddress(),
                eaa.getSourceEmailAddress(), eaa.getTargetEmailAddress(), eaa.messageString(), eaa.getSubject());

        EmailUtils.sendTestMessage(emailMessage, emailConfig);
    }
}
