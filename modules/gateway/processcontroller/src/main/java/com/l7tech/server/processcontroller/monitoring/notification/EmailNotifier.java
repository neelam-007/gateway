package com.l7tech.server.processcontroller.monitoring.notification;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.management.api.monitoring.NotificationAttempt;
import com.l7tech.server.management.config.monitoring.AuthInfo;
import com.l7tech.server.management.config.monitoring.EmailNotificationRule;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.transport.email.EmailUtils;
import com.l7tech.server.transport.http.SslClientSocketFactory;
import com.l7tech.server.processcontroller.monitoring.InOut;
import com.l7tech.util.ExceptionUtils;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 *
 */
class EmailNotifier extends Notifier<EmailNotificationRule> {
    private static final Logger logger = Logger.getLogger(EmailNotifier.class.getName());

    private final Audit auditor;
    private final Map<String, String> propertyMap;
    private final InternetAddress[] toAddresses;
    private final InternetAddress[] ccAddresses;
    private final InternetAddress[] bccAddresses;
    private final InternetAddress[] recipients;
    private final InternetAddress fromAddress;

    private static final Map<Map<String,String>, Session> sessionCache = new WeakHashMap<Map<String,String>, Session>();
    private Session session = null;

    private static final String SOCKET_FACTORY_CLASSNAME = SslClientSocketFactory.class.getName();

    public EmailNotifier(EmailNotificationRule rule) {
        super(rule);
        auditor = new LoggingAudit(logger);

        long connectTimeout = ConfigFactory.getLongProperty( "com.l7tech.server.processcontroller.monitoring.ioMailConnectTimeout", 60000 );
        long readTimeout = ConfigFactory.getLongProperty( "com.l7tech.server.processcontroller.monitoringioMailReadTimeout", 60000 );
        propertyMap = buildProperties( rule, connectTimeout, readTimeout );

        try {
            toAddresses = parseAddresses(rule.getTo());
            ccAddresses = parseAddresses(rule.getCc());
            bccAddresses = parseAddresses(rule.getBcc());
        } catch (AddressException e) {
            throw new IllegalArgumentException(e);
        }

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
            fromaddr = new InternetAddress(rule.getFrom());
        } catch (AddressException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    "Unable to initialize EmailAlertAssert: invalid destination email address: " + ExceptionUtils.getMessage(e));
            fromaddr = null;
        }
        fromAddress = fromaddr;
    }

    private static InternetAddress[] parseAddresses(Collection<String> addresses) throws AddressException {
        Collection<InternetAddress> ret = new ArrayList<InternetAddress>();
        for (String addr : addresses) {
            ret.addAll(Arrays.asList(InternetAddress.parse(addr)));
        }
        return ret.toArray(new InternetAddress[ret.size()]);
    }

    private static Map<String,String> buildProperties(final EmailNotificationRule rule,
                                                      final long connectTimeout,
                                                      final long readTimeout)
    {
        final EmailNotificationRule.CryptoType cryptoType = rule.getCryptoType();
        String protoVal = cryptoType == EmailNotificationRule.CryptoType.SSL ? "smtps" : "smtp";

        Map<String,String> props = new HashMap<String,String>();
        String from = rule.getFrom();
        props.put("mail.from", from == null ? "Layer 7 Process Controller <l7pc@NOMAILBOX>" : from);

        // Transport config
        props.put("mail.transport.protocol", protoVal);
        props.put("mail." + protoVal + ".sendpartial", "true");
        props.put("mail." + protoVal + ".connectiontimeout", Long.toString(connectTimeout));
        props.put("mail." + protoVal + ".timeout", Long.toString(readTimeout));
        String smtpHost = rule.getSmtpHost();
        props.put("mail." + protoVal + ".host", smtpHost == null ? InetAddressUtil.getLocalHostAddress() : smtpHost);
        props.put("mail." + protoVal + ".port", Integer.toString(rule.getPort()));
        props.put("mail." + protoVal + ".fallback", "false");

        // SSL Config
        if ( cryptoType == EmailNotificationRule.CryptoType.STARTTLS ) {
            props.put("mail." + protoVal + ".socketFactory.class", EmailUtils.StartTlsSocketFactory.class.getName());
            props.put("mail." + protoVal + ".starttls.enable", "true");
        } else if ( cryptoType == EmailNotificationRule.CryptoType.SSL ) {
            props.put("mail." + protoVal + ".socketFactory.class", SOCKET_FACTORY_CLASSNAME);
        }

        if( rule.getAuthInfo() != null) {
            props.put("mail." + protoVal + ".auth", "true");
        }

        return Collections.unmodifiableMap( props );
    }

    /**
     * Find a shared session that uses the same SMTP host, SMTP port, and mail From address.
     *
     * @return a session, either new or reusing one from another EmailNotifier with compatible settings.
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

    private void sendMessage( final Session session, final String body ) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.addRecipients(javax.mail.Message.RecipientType.TO, toAddresses);
        message.addRecipients(javax.mail.Message.RecipientType.CC, ccAddresses);
        message.addRecipients(javax.mail.Message.RecipientType.BCC, bccAddresses);
        message.setFrom(fromAddress);
        message.setSentDate(new java.util.Date());
        message.setSubject( sanitizeSubject( rule.getSubject() ) );
        message.setText(body);
        message.saveChanges();

        Transport tr = session.getTransport(rule.getCryptoType() == EmailNotificationRule.CryptoType.SSL ? "smtps" : "smtp");
        final AuthInfo authInfo = rule.getAuthInfo();
        final String username = authInfo == null ? null : authInfo.getUsername();
        final String pass = authInfo == null ? null : new String(authInfo.getPassword());
        tr.connect(rule.getSmtpHost(), rule.getPort(), username, pass);
        tr.sendMessage(message, recipients);
    }

    private String sanitizeSubject( String subject ) {
        // Remove all carriage returns and line feeds from subject
        return subject == null ? "" : subject.replace( "\015", "" ).replace( "\012", "" );
    }

    @Override
    public NotificationAttempt.StatusType doNotification(Long timestamp, InOut inOut, Object value, Trigger trigger) throws IOException {
        try {
            final Session session = getSession();
            final String body = ExpandVariables.process(rule.getText(), getMonitoringVariables(trigger, inOut, value), auditor);

            sendMessage( session, body );
            return NotificationAttempt.StatusType.SENT;
        } catch (AuthenticationFailedException e) {
            throw new IOException(e);
        } catch (MessagingException e) {
            throw new IOException(e);
        }
    }
}
