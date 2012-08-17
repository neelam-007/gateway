package com.l7tech.server.transport.email;

import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.server.transport.http.SslClientHostnameAwareSocketFactory;
import com.l7tech.server.transport.http.AnonymousSslClientHostnameAwareSocketFactory;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.common.transport.email.EmailMessage;
import com.l7tech.gateway.common.transport.email.EmailTestException;

import javax.net.ssl.SSLSocketFactory;
import javax.mail.Session;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.AuthenticationFailedException;
import javax.mail.internet.MimeMessage;
import java.util.*;
import java.util.logging.Logger;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

/**
 * An email utility class
 *
 * User: dlee
 * Date: Nov 18, 2008
 */
public class EmailUtils {

    // We only support SSL without client cert, but allow configuration of SSL default key just in case.
    private static final String PROP_SSL_DEFAULT_KEY = "com.l7tech.server.policy.emailalert.useDefaultSsl";
    private static final boolean SSL_DEFAULT_KEY = ConfigFactory.getBooleanProperty( PROP_SSL_DEFAULT_KEY, false );
    private static final String SOCKET_FACTORY_CLASSNAME = SSL_DEFAULT_KEY ?
            SslClientHostnameAwareSocketFactory.class.getName() :
            AnonymousSslClientHostnameAwareSocketFactory.class.getName();
    private static final Logger logger = Logger.getLogger(EmailUtils.class.getName());

    private static Map<String,String> buildProperties(final EmailConfig emailConfig, final EmailMessage emailMessage) {

        String protoVal = emailConfig.getProtocol() == EmailAlertAssertion.Protocol.SSL ? "smtps" : "smtp";

        Map<String,String> props = new HashMap<String,String>();
        props.put("mail.from", emailMessage.getFromAddress().toString());

        // Transport config
        props.put("mail.transport.protocol", protoVal);
        props.put("mail." + protoVal + ".sendpartial", "true");
        props.put("mail." + protoVal + ".connectiontimeout", Long.toString(emailConfig.getConnectionTimeout()));
        props.put("mail." + protoVal + ".timeout", Long.toString(emailConfig.getReadTimeout()));
        props.put("mail." + protoVal + ".host", emailConfig.getHost());
        props.put("mail." + protoVal + ".port", Integer.toString(emailConfig.getPort()));
        props.put("mail." + protoVal + ".fallback", "false");

        // SSL Config
        if ( emailConfig.getProtocol() == EmailAlertAssertion.Protocol.STARTTLS ) {
            props.put("mail." + protoVal + ".socketFactory.class", StartTlsSocketFactory.class.getName());
            props.put("mail." + protoVal + ".socketFactory.fallback", "false");
            props.put("mail." + protoVal + ".starttls.enable", "true");
        } else if ( emailConfig.getProtocol() == EmailAlertAssertion.Protocol.SSL ) {
            props.put("mail." + protoVal + ".socketFactory.class", SOCKET_FACTORY_CLASSNAME);
            props.put("mail." + protoVal + ".socketFactory.fallback", "false");
        }

        if( emailConfig.isAuthenticate() ) {
            props.put("mail." + protoVal + ".auth", "true");
        }

        return Collections.unmodifiableMap( props );
    }

    /**
     * Find a shared session that uses the same SMTP host, SMTP port, and mail From address.
     *
     * @return a session, either new or reusing one from another ServerEmailAlertAssertion with compatible settings.
     */
    @SuppressWarnings({ "UseOfPropertiesAsHashtable" })
    private static Session getSession(final Map<String, String> propertyMap) {
        // create properties for session instantiation
        Properties properties = new Properties();
        properties.putAll( propertyMap );
        return Session.getInstance(properties, null);
    }

    /**
     * Sends an email message based on the configuration and email message content.
     *
     * @param emailMessage  Email message
     * @param emailConfig   Email configuration
     * @throws EmailTestException   Failed to send a test email message
     */
    public static void sendTestMessage(final EmailMessage emailMessage, final EmailConfig emailConfig) throws EmailTestException {
        try {
            Map<String, String> propertyMap = buildProperties(emailConfig, emailMessage);
            Session session = getSession(propertyMap);
            sendMessage(session, emailMessage, emailConfig);
        } catch (MessagingException me) {
            if (ExceptionUtils.causedBy(me, AuthenticationFailedException.class)) {
                logger.fine("Failed to send test email message: Authentication failed");
                throw new EmailTestException("Authentication failed.");
            } else if ("[EOF]".equals(me.getMessage())) {
                logger.fine("Failed to send test email message: Client does not have permission to Send As this sender '" + emailMessage.getFromAddress() + "'");
                throw new EmailTestException("Client does not have permission to Send As this sender '" + emailMessage.getFromAddress() + "'");
            } else {
                logger.fine("Failed to send test email message: " + me.getMessage());
                if ( ExceptionUtils.causedBy(me,IOException.class)) {
                    IOException cause = ExceptionUtils.getCauseIfCausedBy( me, IOException.class );
                    throw new EmailTestException(me.getMessage() + " due to " + ExceptionUtils.getMessage( cause ));
                } else {
                    throw new EmailTestException(me.getMessage());
                }
            }
        } catch (Exception e) {
            String msg = "Unexpected error sending email: " + e.getMessage();
            logger.fine(msg);
            throw new EmailTestException(msg);
        }
    }

    /**
     * Send the message.
     */
    private static void sendMessage(final Session session, final EmailMessage emailMessage, final EmailConfig emailConfig) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.addRecipients(javax.mail.Message.RecipientType.TO, emailMessage.getToAddresses());
        message.addRecipients(javax.mail.Message.RecipientType.CC, emailMessage.getCcAddresses());
        message.addRecipients(javax.mail.Message.RecipientType.BCC, emailMessage.getBccAddresses());
        message.setFrom(emailMessage.getFromAddress());
        message.setSentDate(new java.util.Date());
        message.setSubject(emailMessage.getSubject());
        message.setText(emailMessage.getMessage());
        message.saveChanges();

        Transport tr = session.getTransport(emailConfig.getProtocol() == EmailAlertAssertion.Protocol.SSL ? "smtps" : "smtp");
        tr.connect(emailConfig.getHost(), emailConfig.getPort(), emailConfig.getAuthUsername(), emailConfig.getAuthPassword());
        tr.sendMessage(message, emailMessage.getAllRecipients());
    }

    /**
     * This is a hack. For StartTLS, the initial connection must be unencrypted, but after the STARTTLS command
     * is sent, the socket must be recreated as an SSL socket. Unfortunately both cases must be covered using
     * the same SocketFactory.
     */
    public static class StartTlsSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory sslFactory = SSL_DEFAULT_KEY ?
                SslClientHostnameAwareSocketFactory.getDefault() :
                AnonymousSslClientHostnameAwareSocketFactory.getDefault();
        private static final StartTlsSocketFactory singleton = new StartTlsSocketFactory();

        public static synchronized SSLSocketFactory getDefault() {
            return singleton;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return sslFactory.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return sslFactory.getSupportedCipherSuites();
        }

        /**
         * Wrap existing socket with SSL
         */
        @Override
        public Socket createSocket(Socket socket, String string, int i, boolean b) throws IOException {
            return sslFactory.createSocket(socket, string, i, b);
        }

        @Override
        public Socket createSocket() throws IOException {
            return new Socket();
        }

        @Override
        public Socket createSocket(String string, int i) throws IOException {
            return new Socket(string, i);
        }

        @Override
        public Socket createSocket(String string, int i, InetAddress inetAddress, int i1) throws IOException {
            return new Socket(string, i, inetAddress, i1);
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
            return new Socket(inetAddress, i);
        }

        @Override
        public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
            return new Socket(inetAddress, i, inetAddress1, i1);
        }
    }
}
