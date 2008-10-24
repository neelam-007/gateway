/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.server.policy.assertion.alert;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.transport.http.SslClientSocketFactory;
import com.l7tech.server.ServerConfig;
import org.springframework.context.ApplicationContext;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.AuthenticationFailedException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;
import java.net.ConnectException;
import java.net.Socket;
import java.net.InetAddress;

/**
 * Server side implementation of assertion that sends an email alert.
 */
public class ServerEmailAlertAssertion extends AbstractServerAssertion<EmailAlertAssertion> {
    private static final Logger logger = Logger.getLogger(ServerEmailAlertAssertion.class.getName());

    private final Auditor auditor;
    private final Map<String, String> propertyMap;
    private final InternetAddress[] toAddresses;
    private final InternetAddress[] ccAddresses;
    private final InternetAddress[] bccAddresses;
    private final InternetAddress[] recipients;
    private final InternetAddress fromAddress;

    private static final Map<Map<String,String>, Session> sessionCache = new WeakHashMap<Map<String,String>, Session>();
    private Session session = null;
    private final String[] varsUsed;
    private static final String SOCKET_FACTORY_CLASSNAME = SslClientSocketFactory.class.getName();

    public ServerEmailAlertAssertion(EmailAlertAssertion ass, ApplicationContext spring) {
        super(ass);
        auditor = new Auditor(this, spring, logger);

        ServerConfig config = (ServerConfig) spring.getBean( "serverConfig", ServerConfig.class );
        long connectTimeout = config.getTimeUnitPropertyCached( "ioMailConnectTimeout", 60000, 30000 );
        long readTimeout = config.getTimeUnitPropertyCached( "ioMailReadTimeout", 60000, 30000 );
        propertyMap = buildProperties( ass, connectTimeout, readTimeout );

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
     * Test the assertion.
     * /
    public Collection<String> test()  {
        Collection<String> messages = new ArrayList<String>();

        Properties properties = new Properties();
        properties.putAll( propertyMap );
        Session session = Session.getInstance(properties, null);
        String body = assertion.getBase64message();

        if (toAddresses == null) {
            messages.add("Invalid to address.");
        } else if (fromAddress == null) {
            messages.add("Invalid from address.");
        } else {
            try {
                sendMessage( session, body );
            } catch (AuthenticationFailedException e) {
                messages.add("Authentication failed.");
            } catch (MessagingException e) {
                if ( ExceptionUtils.causedBy( e, ConnectException.class ) ) {
                    messages.add("Connection failed.");
                } else if ( ExceptionUtils.causedBy( e, SSLHandshakeException.class ) ) {
                    messages.add("SSL connection failed.");
                } else {
                    messages.add( "An error occurred '" + ExceptionUtils.getMessage(e) + "'");            
                }
            }
        }

        return messages;
    }*/

    /**
     * Build immutable properties for this assertion 
     */
    private Map<String,String> buildProperties( final EmailAlertAssertion emailAlertAssertion,
                                                final long connectTimeout,
                                                final long readTimeout ) {
        EmailAlertAssertion.Protocol protocol = assertion.getProtocol();
        String protoVal = protocol==EmailAlertAssertion.Protocol.SSL ? "smtps" : "smtp";

        Map<String,String> props = new HashMap<String,String>();
        props.put("mail.from", emailAlertAssertion.getSourceEmailAddress());

        // Transport config
        props.put("mail.transport.protocol", protoVal);
        props.put("mail." + protoVal + ".sendpartial", "true");
        props.put("mail." + protoVal + ".connectiontimeout", Long.toString(connectTimeout));
        props.put("mail." + protoVal + ".timeout", Long.toString(readTimeout));
        props.put("mail." + protoVal + ".host", emailAlertAssertion.getSmtpHost());
        props.put("mail." + protoVal + ".port", Integer.toString(emailAlertAssertion.getSmtpPort()));
        props.put("mail." + protoVal + ".fallback", "false");

        // SSL Config
        if ( protocol == EmailAlertAssertion.Protocol.STARTTLS ) {
            props.put("mail." + protoVal + ".socketFactory.class", StartTlsSocketFactory.class.getName());
            props.put("mail." + protoVal + ".starttls.enable", "true");
        } else if ( protocol == EmailAlertAssertion.Protocol.SSL ) {
            props.put("mail." + protoVal + ".socketFactory.class", SOCKET_FACTORY_CLASSNAME);
        }

        if( assertion.isAuthenticate() ) {
            props.put("mail." + protoVal + ".auth", "true");
        }

        return Collections.unmodifiableMap( props );
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
            session = sessionCache.get(propertyMap);
            if (session == null) {
                // create properties for session instantiation
                Properties properties = new Properties();
                properties.putAll( propertyMap );
                session = Session.getInstance(properties, null);

                // store using immutable property map as key
                sessionCache.put(propertyMap, session);
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
            final String body = ExpandVariables.process(assertion.messageString(), context.getVariableMap(varsUsed, auditor), auditor);

            sendMessage( session, body );

            auditor.logAndAudit(AssertionMessages.EMAILALERT_MESSAGE_SENT);
            return AssertionStatus.NONE;
        } catch (AuthenticationFailedException e) {
            auditor.logAndAudit(AssertionMessages.EMAILALERT_AUTH_FAIL, null, ExceptionUtils.getDebugException(e) );
            return AssertionStatus.FAILED;
        } catch (MessagingException e) {
            if ( ExceptionUtils.causedBy( e, ConnectException.class ) ) {
                auditor.logAndAudit(AssertionMessages.EMAILALERT_CONNECT_FAIL, null, ExceptionUtils.getDebugException(e) );                
            } else if ( ExceptionUtils.causedBy( e, SSLHandshakeException.class ) ) {
                auditor.logAndAudit(AssertionMessages.EMAILALERT_SSL_FAIL, null, ExceptionUtils.getDebugException(e) );
            } else {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[] {"Unable to send email: " + e.getMessage()}, ExceptionUtils.getDebugException(e));
            }
            return AssertionStatus.FAILED;
        }
    }

    /**
     * Send the message. 
     */
    private void sendMessage( final Session session, final String body ) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.addRecipients(javax.mail.Message.RecipientType.TO, toAddresses);
        message.addRecipients(javax.mail.Message.RecipientType.CC, ccAddresses);
        message.addRecipients(javax.mail.Message.RecipientType.BCC, bccAddresses);
        message.setFrom(fromAddress);
        message.setSentDate(new java.util.Date());
        message.setSubject(assertion.getSubject());
        message.setText(body);
        message.saveChanges();

        Transport tr = session.getTransport(assertion.getProtocol() == EmailAlertAssertion.Protocol.SSL ? "smtps" : "smtp");
        tr.connect(assertion.getSmtpHost(), assertion.getSmtpPort(), assertion.getAuthUsername(), assertion.getAuthPassword());
        tr.sendMessage(message, recipients);
    }

    /**
     * This is a hack. For StartTLS, the initial connection must be unencrypted, but after the STARTTLS command
     * is sent, the socket must be recreated as an SSL socket. Unfortunately both cases must be covered using
     * the same SocketFactory.
     */
    public static class StartTlsSocketFactory extends SSLSocketFactory {
        private SSLSocketFactory sslFactory = SslClientSocketFactory.getDefault();
        private static StartTlsSocketFactory singleton = new StartTlsSocketFactory();

        public static synchronized SSLSocketFactory getDefault() {
            if(singleton == null) {
                singleton = new StartTlsSocketFactory();
            }
            return singleton;
        }

        public String[] getDefaultCipherSuites() {
            return sslFactory.getDefaultCipherSuites();
        }

        public String[] getSupportedCipherSuites() {
            return sslFactory.getSupportedCipherSuites();
        }

        /**
         * Wrap existing socket with SSL 
         */
        public Socket createSocket(Socket socket, String string, int i, boolean b) throws IOException {
            return sslFactory.createSocket(socket, string, i, b);
        }

        @Override
        public Socket createSocket() throws IOException {
            return new Socket();
        }

        public Socket createSocket(String string, int i) throws IOException {
            return new Socket(string, i);
        }

        public Socket createSocket(String string, int i, InetAddress inetAddress, int i1) throws IOException {
            return new Socket(string, i, inetAddress, i1);
        }

        public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
            return new Socket(inetAddress, i);
        }

        public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
            return new Socket(inetAddress, i, inetAddress1, i1);
        }
    }
}
