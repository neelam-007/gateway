package com.l7tech.server.transport.email;

import com.l7tech.gateway.common.transport.email.EmailAdmin;
import com.l7tech.gateway.common.transport.email.EmailMessage;
import com.l7tech.gateway.common.transport.email.EmailTestException;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.Config;

/**
 * Implementation of the Email Admin interface
 */
public class EmailAdminImpl implements EmailAdmin {

    private Config config;

    public EmailAdminImpl(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void testSendEmail(String toAddr, String ccAddr, String bccAddr, String fromAddr, String subject, String host,
                                 int port, String base64Message, EmailAlertAssertion.Protocol protocol, boolean authenticate,
                                 String authUsername, String authPassword) throws EmailTestException {

        long connectTimeout = 25000;
        long readTimeout = 25000;

        EmailConfig emailConfig = new EmailConfig(authenticate, authUsername, authPassword, host, port, protocol, connectTimeout, readTimeout);
        EmailMessage emailMessage = new EmailMessage(bccAddr, ccAddr, fromAddr, toAddr, base64Message, subject);
        EmailUtils.sendTestMessage(emailMessage, emailConfig);
    }

    @Override
    public void testSendEmail(EmailAlertAssertion eaa) throws EmailTestException {
        long connectTimeout = 25000;
        long readTimeout = 25000;
        int port = EmailAlertAssertion.DEFAULT_PORT;

        try{
            port = Integer.parseInt(eaa.getSmtpPort());
        }catch(NumberFormatException nfe){
            port = EmailAlertAssertion.DEFAULT_PORT;
        }

        EmailConfig emailConfig = new EmailConfig(eaa.isAuthenticate(), eaa.getAuthUsername(), eaa.getAuthPassword(),
                eaa.getSmtpHost(), port, eaa.getProtocol(), connectTimeout, readTimeout);

        EmailMessage emailMessage = new EmailMessage(eaa.getTargetBCCEmailAddress(), eaa.getTargetCCEmailAddress(),
                eaa.getSourceEmailAddress(), eaa.getTargetEmailAddress(), eaa.messageString(), eaa.getSubject());

        EmailUtils.sendTestMessage(emailMessage, emailConfig);
    }

    private static final long DEFAULT_MAX_SIZE = 5242880L;

    @Override
    public long getXmlMaxBytes() {
        return config.getLongProperty(ServerConfigParams.PARAM_EMAIL_MESSAGE_MAX_BYTES, DEFAULT_MAX_SIZE);
    }
}
