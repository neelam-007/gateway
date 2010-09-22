/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.server.policy.assertion.alert;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.transport.http.SslClientHostnameAwareSocketFactory;
import com.l7tech.server.transport.http.AnonymousSslClientHostnameAwareSocketFactory;
import com.l7tech.server.transport.email.EmailUtils;
import com.l7tech.server.ServerConfig;
import org.springframework.context.ApplicationContext;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.AuthenticationFailedException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Logger;
import java.net.ConnectException;

/**
 * Server side implementation of assertion that sends an email alert.
 */
public class ServerEmailAlertAssertion extends AbstractServerAssertion<EmailAlertAssertion> {
    private static final Logger logger = Logger.getLogger(ServerEmailAlertAssertion.class.getName());

    private final Auditor auditor;
//    private final Map<String, String> propertyMap;
    private final InternetAddress[] toAddresses;
    private final InternetAddress[] ccAddresses;
    private final InternetAddress[] bccAddresses;
    private final InternetAddress[] recipients;
    private final InternetAddress fromAddress;
    private static final Map<Map<String, String>, Session> sessionCache = new WeakHashMap<Map<String, String>, Session>();
    private Session session = null;
    private final String[] varsUsed;

    // We only support SSL without client cert, but allow configuration of SSL default key just in case.
    private static final String PROP_SSL_DEFAULT_KEY = "com.l7tech.server.policy.emailalert.useDefaultSsl";
    private static final boolean SSL_DEFAULT_KEY = SyspropUtil.getBoolean(PROP_SSL_DEFAULT_KEY, false);
    private static final String SOCKET_FACTORY_CLASSNAME = SSL_DEFAULT_KEY ?
            SslClientHostnameAwareSocketFactory.class.getName() :
            AnonymousSslClientHostnameAwareSocketFactory.class.getName();

    public ServerEmailAlertAssertion(EmailAlertAssertion ass, ApplicationContext spring) {
        super(ass);
        auditor = new Auditor(this, spring, logger);

        ServerConfig config = spring.getBean("serverConfig", ServerConfig.class);
        long connectTimeout = config.getTimeUnitPropertyCached("ioMailConnectTimeout", 60000, 30000);
        long readTimeout = config.getTimeUnitPropertyCached("ioMailReadTimeout", 60000, 30000);
        ass.setConnectTimeout(connectTimeout);
        ass.setReadTimeout(readTimeout);
        //I'm moving this to be initialized in the first run of checkRequest.
        //I'm doing this because the port property being set could be a context variable that has to be expanded,
        //also the host name could be a context var.
//        propertyMap = buildProperties(ass, connectTimeout, readTimeout);


        InternetAddress[] addr;
        try {
            addr = InternetAddress.parse(ass.getTargetEmailAddress());
        } catch (AddressException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Unable to initialize EmailAlertAssert: invalid destination email address"}, ExceptionUtils.getDebugException(e));
            addr = null;
        }
        toAddresses = addr;

        try {
            addr = InternetAddress.parse(ass.getTargetCCEmailAddress());
        } catch (AddressException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"Unable to initialize EmailAlertAssert: invalid CC email address"}, ExceptionUtils.getDebugException(e));
            addr = null;
        }
        ccAddresses = addr;

        try {
            addr = InternetAddress.parse(ass.getTargetBCCEmailAddress());
        } catch (AddressException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[]{"Unable to initialize EmailAlertAssert: invalid BCC email address"}, ExceptionUtils.getDebugException(e));
            addr = null;
        }
        bccAddresses = addr;


        int size = 0;
        if (toAddresses != null) size += toAddresses.length;
        if (ccAddresses != null) size += ccAddresses.length;
        if (bccAddresses != null) size += bccAddresses.length;
        recipients = new InternetAddress[size];

        int i = 0;
        if (toAddresses != null) {
            for (InternetAddress address : toAddresses) {
                recipients[i++] = address;
            }
        }
        if (ccAddresses != null) {
            for (InternetAddress address : ccAddresses) {
                recipients[i++] = address;
            }
        }
        if (bccAddresses != null) {
            for (InternetAddress address : bccAddresses) {
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
     * Build immutable properties for this assertion
     */
    private Map<String, String> buildProperties(final EmailAlertAssertion emailAlertAssertion,
                                                final long connectTimeout,
                                                final long readTimeout,
                                                final String port,
                                                final String host) {
        EmailAlertAssertion.Protocol protocol = assertion.getProtocol();
        String protoVal = protocol == EmailAlertAssertion.Protocol.SSL ? "smtps" : "smtp";

        Map<String, String> props = new HashMap<String, String>();
        props.put("mail.from", emailAlertAssertion.getSourceEmailAddress());

        // Transport config
        props.put("mail.transport.protocol", protoVal);
        props.put("mail." + protoVal + ".sendpartial", "true");
        props.put("mail." + protoVal + ".connectiontimeout", Long.toString(connectTimeout));
        props.put("mail." + protoVal + ".timeout", Long.toString(readTimeout));
        //if the host name has a context var, this needs to be replaced before putting the prop.
        //Same as port number
        props.put("mail." + protoVal + ".host", host);
//        props.put("mail." + protoVal + ".port", Integer.toString(emailAlertAssertion.getSmtpPort()));
        
        props.put("mail." + protoVal + ".port", port);
        props.put("mail." + protoVal + ".fallback", "false");

        // SSL Config
        if (protocol == EmailAlertAssertion.Protocol.STARTTLS) {
            props.put("mail." + protoVal + ".socketFactory.class", EmailUtils.StartTlsSocketFactory.class.getName());
            props.put("mail." + protoVal + ".socketFactory.fallback", "false");
            props.put("mail." + protoVal + ".starttls.enable", "true");
        } else if (protocol == EmailAlertAssertion.Protocol.SSL) {
            props.put("mail." + protoVal + ".socketFactory.class", SOCKET_FACTORY_CLASSNAME);
            props.put("mail." + protoVal + ".socketFactory.fallback", "false");
        }

        if (assertion.isAuthenticate()) {
            props.put("mail." + protoVal + ".auth", "true");
        }

        return Collections.unmodifiableMap(props);
    }

    /**
     * Find a shared session that uses the same SMTP host, SMTP port, and mail From address.
     *
     * @return a session, either new or reusing one from another ServerEmailAlertAssertion with compatible settings.
     */
    @SuppressWarnings({"UseOfPropertiesAsHashtable"})
    private Session getSession(Map<String, String> propertyMap) {
        if (session != null)
            return session;

        synchronized (sessionCache) {
            session = sessionCache.get(propertyMap);
            if (session == null) {
                // create properties for session instantiation
                Properties properties = new Properties();
                properties.putAll(propertyMap);
                session = Session.getInstance(properties, null);

                // store using immutable property map as key
                sessionCache.put(propertyMap, session);
            }
            return session;
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {



        //check if smtp port is valid.  This port could be a context var that needs to be filled in.
        String portNum = assertion.getSmtpPort();
        if (assertion.portHasVars()) {
            //the port num is a context var.  get the real value
            String tempNum = ExpandVariables.process(portNum, context.getVariableMap(varsUsed, auditor), auditor);
            if (tempNum != null) {
                //check if it is a valid port number;
                try {
                    int port = Integer.parseInt(tempNum);
                    if (port < 0 || port > 65535) throw new IllegalArgumentException();
                    portNum = tempNum;
                    //tempNum is ok, use that num to send the message
                    //ie: this.portNum is ok.
                } catch (NumberFormatException nfe) {
                    //port was not a number dumbass!
                    //fail the assertion
                    auditor.logAndAudit(AssertionMessages.EMAILALERT_BAD_PORT);
                    return AssertionStatus.FAILED;
                }
            }
        }else{
            //port number is not a context var just make sure it is a good number.
            try {
                int port = Integer.parseInt(portNum);
                if (port < 0 || port > 65535){
                    //bad port range, fail assertion
                    auditor.logAndAudit(AssertionMessages.EMAILALERT_BAD_PORT);
                    return AssertionStatus.FAILED;
                }

            } catch (NumberFormatException nfe) {
                //port was not a number dumbass!
                //fail the assertion
                auditor.logAndAudit(AssertionMessages.EMAILALERT_BAD_PORT);
                return AssertionStatus.FAILED;
            }
        }

        if (toAddresses == null) {
            auditor.logAndAudit(AssertionMessages.EMAILALERT_BAD_TO_ADDR);
            return AssertionStatus.FAILED;
        }

        if (fromAddress == null) {
            auditor.logAndAudit(AssertionMessages.EMAILALERT_BAD_FROM_ADDR);
            return AssertionStatus.FAILED;
        }

        //check that the host name is not a context var
        String host = assertion.getSmtpHost();
        if (assertion.hostHasVars()) {
            String ias = ExpandVariables.process(host, context.getVariableMap(varsUsed, auditor), auditor);
            if (!ias.equals(host)) {
                host = ias;
            }
        }

        try {
            final Map<String, String> propertyMap = buildProperties(assertion, assertion.getConnectTimeout(), assertion.getReadTimeout(), portNum, host);
            final Session session = getSession(propertyMap);
            final String body = ExpandVariables.process(assertion.messageString(), context.getVariableMap(varsUsed, auditor), auditor);

            sendMessage(session, body, context);

            auditor.logAndAudit(AssertionMessages.EMAILALERT_MESSAGE_SENT);
            return AssertionStatus.NONE;
        } catch (AuthenticationFailedException e) {
            auditor.logAndAudit(AssertionMessages.EMAILALERT_AUTH_FAIL, null, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        } catch (MessagingException e) {
            if (ExceptionUtils.causedBy(e, ConnectException.class)) {
                auditor.logAndAudit(AssertionMessages.EMAILALERT_CONNECT_FAIL, null, ExceptionUtils.getDebugException(e));
            } else if (ExceptionUtils.causedBy(e, SSLHandshakeException.class)) {
                auditor.logAndAudit(AssertionMessages.EMAILALERT_SSL_FAIL, null, ExceptionUtils.getDebugException(e));
            } else if ("[EOF]".equals(e.getMessage())) {
                auditor.logAndAudit(AssertionMessages.EMAILALERT_AUTH_FAIL, null, ExceptionUtils.getDebugException(e));
            } else {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[]{"Unable to send email: " + e.getMessage()}, ExceptionUtils.getDebugException(e));
            }
            return AssertionStatus.FAILED;
        }
    }

    /**
     * Send the message.
     */
    private void sendMessage(final Session session, final String body, PolicyEnforcementContext context) throws MessagingException {
        String host = assertion.getSmtpHost();
        String userName = assertion.getAuthUsername();
        String pd = assertion.getAuthPassword();
//        String portNumber = assertion.getSmtpPort();


        //before sending, check that the to addresses don't have a context var in them
        //(only process ones that have context vars in 'em)
        if (assertion.toHasVars())
            for (int i = 0; i < toAddresses.length; i++) {
                InternetAddress ia = toAddresses[i];
                String ias = ExpandVariables.process(ia.getAddress(), context.getVariableMap(varsUsed, auditor), auditor);
                if (!ias.equals(ia.getAddress())) {
                    toAddresses[i].setAddress(ias);
                }
            }
        //and then the cc field.....
        if (assertion.ccHasVars())
            for (int i = 0; i < ccAddresses.length; i++) {
                InternetAddress ia = ccAddresses[i];
                String ias = ExpandVariables.process(ia.getAddress(), context.getVariableMap(varsUsed, auditor), auditor);
                if (!ias.equals(ia.getAddress())) {
                    ccAddresses[i].setAddress(ias);
                }
            }
        //and then the bcc field.....
        if (assertion.bccHasVars())
            for (int i = 0; i < bccAddresses.length; i++) {
                InternetAddress ia = bccAddresses[i];
                String ias = ExpandVariables.process(ia.getAddress(), context.getVariableMap(varsUsed, auditor), auditor);
                if (!ias.equals(ia.getAddress())) {
                    bccAddresses[i].setAddress(ias);
                }
            }
        //and then the from field....
        if (assertion.fromHasVars()) {
            String ias = ExpandVariables.process(fromAddress.getAddress(), context.getVariableMap(varsUsed, auditor), auditor);
            if (!ias.equals(fromAddress.getAddress())) {
                fromAddress.setAddress(ias);
            }
        }

        //check that the host name is not a context var
        if (assertion.hostHasVars()) {
            String ias = ExpandVariables.process(host, context.getVariableMap(varsUsed, auditor), auditor);
            if (!ias.equals(host)) {
                host = ias;
            }
        }

        //check the username has no context vars
        if (assertion.userNameHasVars()) {
            String ias = ExpandVariables.process(userName, context.getVariableMap(varsUsed, auditor), auditor);
            if (!ias.equals(userName)) {
                userName = ias;
            }
        }

        if (assertion.pwdHasVars()) {
            String ias = ExpandVariables.process(pd, context.getVariableMap(varsUsed, auditor), auditor);
            if (!ias.equals(pd)) {
                pd = ias;
            }
        }

        //get the subject from the assertion and fill in any context vars that may exist
        String subject = assertion.getSubject();
        //replace any context vars in the subject
        if (assertion.subjectHasVars()) {
            String ias = ExpandVariables.process(subject, context.getVariableMap(varsUsed, auditor), auditor);
            if (!ias.equals(subject)) {
                subject = ias;
            }
        }

        String portNum = assertion.getSmtpPort();
        //replace the context var if there is one with the real value
        if (assertion.portHasVars()) {
            String ias = ExpandVariables.process(portNum, context.getVariableMap(varsUsed, auditor), auditor);
            if (!ias.equals(portNum)) {
                portNum = ias;
            }
        }

        int portNumber = Integer.parseInt(portNum);

        MimeMessage message = new MimeMessage(session);
        message.addRecipients(javax.mail.Message.RecipientType.TO, toAddresses);
        message.addRecipients(javax.mail.Message.RecipientType.CC, ccAddresses);
        message.addRecipients(javax.mail.Message.RecipientType.BCC, bccAddresses);
        message.setFrom(fromAddress);
        message.setSentDate(new java.util.Date());
        message.setSubject(subject);
        message.setText(body);
        message.saveChanges();

        Transport tr = session.getTransport(assertion.getProtocol() == EmailAlertAssertion.Protocol.SSL ? "smtps" : "smtp");
        tr.connect(host, portNumber, userName, pd);
        tr.sendMessage(message, recipients);
    }
}
