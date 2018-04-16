package com.l7tech.external.assertions.email.server;

import com.l7tech.external.assertions.email.*;
import com.l7tech.server.transport.email.EmailUtils;

import javax.activation.DataHandler;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.activation.DataSource;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

/**
 * This singleton class is used for sending email messages. It is responsible for creating MIME message, connect to
 * SMTP server and send the message. It is capable to sending UTF-8 encoded HTML or PLAIN text content.
 */
public class EmailSender {

    private static final Map<EmailConfig, Session> sessionCache = new WeakHashMap<>();

    private EmailSender() {}

    /**
     * This helper class is for lazy initialization of EmailSender singleton.
     */
    private static class EmailSenderLazyInitializer {
        private EmailSenderLazyInitializer() {}

        private static final EmailSender INSTANCE = new EmailSender();
    }

    /**
     * Returns the Singleton instance of this EmailSender class.
     * @return
     */
    public static EmailSender getInstance() {
        return EmailSenderLazyInitializer.INSTANCE;
    }

    /**
     * Prepares and sends the email.
     * @param emailConfig containing SMTP connection properties
     * @param emailMessage containing Email content and attachments
     * @throws MessagingException if there was an error sending the email
     */
    public void send(final EmailConfig emailConfig, final EmailMessage emailMessage) throws
            MessagingException {
        Session session = getSession(emailConfig);
        MimeMessage message = buildMimeMessage(getMimeMessage(session), emailMessage);

        Transport transport = getTransport(session, emailConfig);
        transport.connect(emailConfig.getHost(), emailConfig.getPort(), emailConfig.getAuthUsername(), emailConfig
                .getAuthPassword());
        transport.sendMessage(message, message.getAllRecipients());
    }

    /**
     * This method creates the MIME message based on the info specified in EmailMessage object.
     * @param message Mime Message instance to be build.
     * @param emailMessage EmailMessage object containing information regarding content of the email
     * @return MimeMessage
     * @throws MessagingException
     */
    private MimeMessage buildMimeMessage(final MimeMessage message, final EmailMessage emailMessage) throws
            MessagingException {
        message.addRecipients(javax.mail.Message.RecipientType.TO, emailMessage.getToAddresses());
        message.addRecipients(javax.mail.Message.RecipientType.CC, emailMessage.getCcAddresses());
        message.addRecipients(javax.mail.Message.RecipientType.BCC, emailMessage.getBccAddresses());
        message.setFrom(emailMessage.getFromAddress());
        message.setSentDate(new java.util.Date());
        message.setSubject( EmailUtils.sanitizeSubject( emailMessage.getSubject() ) );

        buildContentOrParts(message, emailMessage);

        message.saveChanges();
        return message;
    }

    /**
     * Builds content if no attachments are provided or Parts if attachments are configured.
     * @param message The MimeMessage containing the email content like body and attachments if any.
     * @param emailMessage The EmailMessage object holding info regarding the email content like message body
     *                     and attachments if any.
     * @throws MessagingException
     */
    private void buildContentOrParts(final MimeMessage message, final EmailMessage emailMessage) throws MessagingException {
        if(!emailMessage.getAttachmentDataSources().isEmpty()) {
            buildMultipartMessage(message, emailMessage);
        } else {
            message.setContent(emailMessage.getMessage(), emailMessage.getFormat().getContentType());
        }
    }


    /**
     * This method creates the MIME message using MimeBodyPart based on the info specified in EmailMessage object.
     * This method has been introduced to build mime message from text content and attachments in EmailMessage object.
     * @param message Mime Message instance to be build.
     * @param emailMessage EmailMessage object containing information regarding content of the email
     * @throws MessagingException if there was and error creating the MimeMessage
     */
    private void buildMultipartMessage(final MimeMessage message, final EmailMessage emailMessage) throws MessagingException {
        final MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(emailMessage.getMessage(), emailMessage.getFormat().getContentType());
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        for (DataSource attachment : emailMessage.getAttachmentDataSources()) {
            final MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.setFileName(attachment.getName());
            attachmentPart.setDataHandler(new DataHandler(attachment));
            multipart.addBodyPart(attachmentPart);
        }
        message.setContent(multipart);
    }

    private MimeMessage getMimeMessage(final Session session) {
        return new MimeMessage(session);
    }

    private Transport getTransport(final Session session, final EmailConfig emailConfig) throws MessagingException{
        return session.getTransport(emailConfig.getProtocol() == EmailProtocol.SSL ? "smtps" : "smtp");
    }

    /**
     * Returns the Session instance from the Cache. Creates a new session if session not found and update the cache
     * with the new instance.
     * @param config Key for the Session cache.
     * @return Session instance from the cache.
     */
    private Session getSession(final EmailConfig config) {
        synchronized (sessionCache) {
            return sessionCache.computeIfAbsent(config, k -> Session.getInstance(getSessionProperties(config), null));
        }
    }

    /**
     * Returns Properties for creating new session based on email config.
     * @param emailConfig
     * @return Properties for creating session
     */
    private Properties getSessionProperties(final EmailConfig emailConfig) {
        final Properties props = new Properties();
        final String protoVal = emailConfig.getProtocol() == EmailProtocol.SSL ? "smtps" : "smtp";
        final String mailProtoPrefix = "mail." + protoVal;
        props.put("mail.transport.protocol", protoVal);
        props.put(mailProtoPrefix + ".sendpartial", "true");
        props.put(mailProtoPrefix + ".connectiontimeout", Long.toString(emailConfig.getConnectionTimeout()));
        props.put(mailProtoPrefix + ".timeout", Long.toString(emailConfig.getReadTimeout()));
        props.put(mailProtoPrefix + ".host", emailConfig.getHost());
        props.put(mailProtoPrefix + ".port", Integer.toString(emailConfig.getPort()));
        props.put(mailProtoPrefix + ".fallback", Boolean.FALSE.toString());

        // SSL Config
        if ( emailConfig.getProtocol() == EmailProtocol.STARTTLS ) {
            props.put(mailProtoPrefix + ".starttls.enable", Boolean.TRUE.toString());
            if(!EmailUtils.getUseDefaultSslProperty()) {
                props.put(mailProtoPrefix + ".socketFactory.class", EmailUtils.getStartTlsSocketFactoryClassName());
                props.put(mailProtoPrefix + ".socketFactory.fallback", Boolean.FALSE.toString());
                props.put(mailProtoPrefix + ".ssl.protocols", "TLSv1 TLSv1.1 TLSv1.2");
            }
        } else if ( emailConfig.getProtocol() == EmailProtocol.SSL ) {
            props.put(mailProtoPrefix + ".socketFactory.class", EmailUtils.getSslSocketFactoryClassName());
            if(!EmailUtils.getUseDefaultSslProperty()) {
                props.put(mailProtoPrefix + ".socketFactory.fallback", "false");
                props.put(mailProtoPrefix + ".ssl.protocols", "TLSv1 TLSv1.1 TLSv1.2");
            }
        }

        if( emailConfig.isAuthenticate() ) {
            props.put(mailProtoPrefix + ".auth", "true");
        }
        return props;
    }
}
