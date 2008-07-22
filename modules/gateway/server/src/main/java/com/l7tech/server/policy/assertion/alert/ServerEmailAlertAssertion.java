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
import org.springframework.context.ApplicationContext;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.*;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import java.security.GeneralSecurityException;
import java.net.Socket;
import java.net.InetAddress;

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
    private static final String SOCKET_FACTORY_CLASSNAME = SslClientSocketFactory.class.getName();

    public ServerEmailAlertAssertion(EmailAlertAssertion ass, ApplicationContext spring) {
        super(ass);
        auditor = new Auditor(this, spring, logger);

        props = new Properties();
        props.setProperty("mail.from", ass.getSourceEmailAddress());
        props.setProperty("mail.stmp.sendpartial", "true");
        EmailAlertAssertion.Protocol proto = assertion.getProtocol();
        String propPrefix = "mail.smtp";
        if (proto == EmailAlertAssertion.Protocol.SSL) {
            props.setProperty("mail.smtps.socketFactory.fallback", "false");
            props.setProperty("mail.smtps.socketFactory.class", SOCKET_FACTORY_CLASSNAME);
            propPrefix = "mail.smtps";
        } else if (proto == EmailAlertAssertion.Protocol.STARTTLS) {
            props.put("mail.smtp.starttls.enable", "true");
            // TODO should I set a socket factory here?
            props.setProperty("mail.smtp.socketFactory.class", StartTlsSocketFactory.class.getName());
        }

        props.setProperty(propPrefix + ".host", ass.getSmtpHost());
        props.setProperty(propPrefix + ".port", Integer.toString(ass.getSmtpPort()));

        if(assertion.isAuthenticate()) {
            props.setProperty(propPrefix + ".auth", "true");
        }

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

            Transport tr = session.getTransport(assertion.getProtocol() == EmailAlertAssertion.Protocol.SSL ? "smtps" : "smtp");
            tr.connect(assertion.getSmtpHost(), assertion.getSmtpPort(), assertion.getAuthUsername(), assertion.getAuthPassword());
            tr.sendMessage(message, recipients);
        } catch (MessagingException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Unable to send email: " + e.getMessage()}, e);
            return AssertionStatus.FAILED;
        }
        auditor.logAndAudit(AssertionMessages.EMAILALERT_MESSAGE_SENT);
        return AssertionStatus.NONE;
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

        @Override
        public Socket createSocket() throws IOException {
            return new Socket();
        }

        public Socket createSocket(Socket socket, String string, int i, boolean b) throws IOException {
            return sslFactory.createSocket(socket, string, i, b);
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
