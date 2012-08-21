package com.l7tech.server.policy.assertion.alert;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.transport.email.EmailUtils;
import com.l7tech.server.transport.http.AnonymousSslClientHostnameAwareSocketFactory;
import com.l7tech.server.transport.http.SslClientHostnameAwareSocketFactory;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.*;

/**
 * Server side implementation of assertion that sends an email alert.
 */
public class ServerEmailAlertAssertion extends AbstractServerAssertion<EmailAlertAssertion> {
    private final Config config;

    private static final Map<Map<String, String>, Session> sessionCache = new WeakHashMap<Map<String, String>, Session>();

    // We only support SSL without client cert, but allow configuration of SSL default key just in case.
    private static final String PROP_SSL_DEFAULT_KEY = "com.l7tech.server.policy.emailalert.useDefaultSsl";
    private static final boolean SSL_DEFAULT_KEY = ConfigFactory.getBooleanProperty( PROP_SSL_DEFAULT_KEY, false );
    private static final String SOCKET_FACTORY_CLASSNAME = SSL_DEFAULT_KEY ?
            SslClientHostnameAwareSocketFactory.class.getName() :
            AnonymousSslClientHostnameAwareSocketFactory.class.getName();

    public ServerEmailAlertAssertion(EmailAlertAssertion ass, ApplicationContext spring) {
        super(ass);
        config = spring.getBean("serverConfig", Config.class);
    }

    /*
     * Build immutable properties for this assertion
     */
    private Map<String, String> buildProperties(final long connectTimeout,
                                                final long readTimeout,
                                                final String port,
                                                final String host,
                                                final String fromAddress) {
        EmailAlertAssertion.Protocol protocol = assertion.getProtocol();
        String protoVal = protocol == EmailAlertAssertion.Protocol.SSL ? "smtps" : "smtp";
        Map<String, String> props = new HashMap<String, String>();
        props.put("mail.from", fromAddress);

        // Transport config
        props.put("mail.transport.protocol", protoVal);
        props.put("mail." + protoVal + ".sendpartial", "true");
        props.put("mail." + protoVal + ".connectiontimeout", Long.toString(connectTimeout));
        props.put("mail." + protoVal + ".timeout", Long.toString(readTimeout));
        props.put("mail." + protoVal + ".host", host);
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
     * @param propertyMap map of the variables
     * @return a session, either new or reusing one from another ServerEmailAlertAssertion with compatible settings.
     */
    @SuppressWarnings({"UseOfPropertiesAsHashtable"})
    private Session getSession(Map<String, String> propertyMap) {
        synchronized (sessionCache) {
            Session session = sessionCache.get(propertyMap);
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

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        InternetAddress[] toAddresses;
        InternetAddress[] ccAddresses;
        InternetAddress[] bccAddresses;
        InternetAddress[] recipients;
        InternetAddress fromAddress = null;
        InternetAddress[] fromAddresses;
        String host;
        String userName;
        String pwd;
        String subject;
        String portNum;
        String[] varsUsed = assertion.getVariablesUsed();
        Map<String, Object> variableMap = context.getVariableMap(varsUsed, getAudit());
        long connectTimeout = config.getTimeUnitProperty( "ioMailConnectTimeout", 60000L );
        long readTimeout = config.getTimeUnitProperty( "ioMailReadTimeout", 60000L );

        //start putting together the lists of email addresses and other variables to send email
        //and replace the context variables
        toAddresses = assembleAddresses(ExpandVariables.process(assertion.getTargetEmailAddress(), variableMap, getAudit()), "destination email address");
        ccAddresses = assembleAddresses(ExpandVariables.process(assertion.getTargetCCEmailAddress(), variableMap, getAudit()), "cc email address");
        bccAddresses = assembleAddresses(ExpandVariables.process(assertion.getTargetBCCEmailAddress(), variableMap, getAudit()), "bcc email address");
        fromAddresses = assembleAddresses(ExpandVariables.process(assertion.getSourceEmailAddress(), variableMap, getAudit()), "from email address");
        if(fromAddresses!=null&&fromAddresses.length>0)
                fromAddress = fromAddresses[0];

        host = assertion.getSmtpHost();
        portNum = assertion.getSmtpPort();
        userName = assertion.getAuthUsername();
        pwd = assertion.getAuthPassword();
        subject = assertion.getSubject();

        host = ExpandVariables.process(host, variableMap, getAudit());
        portNum = ExpandVariables.process(portNum, variableMap, getAudit());
        if(assertion.isAuthenticate())
        {
            userName = ExpandVariables.process(userName, variableMap, getAudit());
            if(assertion.isContextVarPassword())
                pwd = ExpandVariables.process(pwd, variableMap, getAudit());
        }
        subject = ExpandVariables.process(subject, variableMap, getAudit());

        //get the recipients populated
        recipients = amalgamateAddresses(toAddresses, ccAddresses, bccAddresses);

        //check if smtp port is valid.  This port could be a context var that needs to be filled in.
        //the port num might be a context var.  get the real value
        if (portNum != null && !portNum.equals("")) {
            boolean isOk = portNumberOk(portNum);
            if(!isOk){
             //bad port number.
              logAndAudit( AssertionMessages.EMAILALERT_BAD_PORT );
              return AssertionStatus.FAILED;
            }
        }
        //port number is ok, let's keep an int version
        int portNumberInt = Integer.parseInt(portNum);

        if (toAddresses == null) {
            //no to addresses!
            logAndAudit( AssertionMessages.EMAILALERT_BAD_TO_ADDR );
            return AssertionStatus.FAILED;
        }

        if (fromAddress == null) {
            //no from address!
            logAndAudit( AssertionMessages.EMAILALERT_BAD_FROM_ADDR );
            return AssertionStatus.FAILED;
        }

        //check that the host name is not a context var
        if(host==null || host.equals("")){
            //host is null or empty, fail the assertion
            logAndAudit( AssertionMessages.EMAILALERT_BAD_HOST );
            return AssertionStatus.FAILED;
        }

        if(assertion.isAuthenticate())
        {
            if(userName==null || userName.equals("")){
                logAndAudit( AssertionMessages.EMAILALERT_BAD_USER );
                return AssertionStatus.FAILED;
            }

            if(pwd==null || pwd.equals("")){
                logAndAudit( AssertionMessages.EMAILALERT_BAD_PWD );
                return AssertionStatus.FAILED;
            }
        }

        if(assertion.isTestBean()){
            //This is a test, set all the expanded values back on the bean so the TestCase will not fail
            assertion.setSubject(subject);
            assertion.setSmtpPort(portNum);
            assertion.setSmtpHost(host);
            assertion.setAuthUsername(userName);
            assertion.setAuthPassword(pwd);
            assertion.setSourceEmailAddress(fromAddress.getAddress());

            //concat all the to, bcc, cc email addresses to a single string and set
            StringBuffer sb = new StringBuffer();
            for(int i=0; i < toAddresses.length; i++){
                sb.append(toAddresses[i].getAddress());
                if(i<toAddresses.length-1)
                    sb.append(",");
            }
            assertion.setTargetEmailAddress(sb.toString());
            sb = new StringBuffer();
            for(int i=0; i < ccAddresses.length; i++){
                sb.append(ccAddresses[i].getAddress());
                if(i<ccAddresses.length-1)
                    sb.append(",");
            }
            assertion.setTargetCCEmailAddress(sb.toString());
            sb = new StringBuffer();
            for(int i=0; i < bccAddresses.length; i++){
                sb.append(bccAddresses[i].getAddress());
                if(i<bccAddresses.length-1)
                    sb.append(",");
            }
            assertion.setTargetBCCEmailAddress(sb.toString());
        }

        try {
            final Map<String, String> propertyMap = buildProperties(connectTimeout, readTimeout, portNum, host, fromAddress.getAddress());
            final Session session = getSession(propertyMap);
            final String body = ExpandVariables.process(assertion.messageString(), context.getVariableMap(varsUsed, getAudit()), getAudit());

            sendMessage(session, body, host, toAddresses, ccAddresses, bccAddresses, userName, pwd, recipients, fromAddress, subject, portNumberInt);

            logAndAudit( AssertionMessages.EMAILALERT_MESSAGE_SENT );
            return AssertionStatus.NONE;
        } catch (AuthenticationFailedException e) {
            logAndAudit( AssertionMessages.EMAILALERT_AUTH_FAIL, null, ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.FAILED;
        } catch (MessagingException e) {
            if (ExceptionUtils.causedBy(e, ConnectException.class)) {
                logAndAudit( AssertionMessages.EMAILALERT_CONNECT_FAIL, null, ExceptionUtils.getDebugException( e ) );
            } else if (ExceptionUtils.causedBy(e, SSLHandshakeException.class)) {
                logAndAudit( AssertionMessages.EMAILALERT_SSL_FAIL, null, ExceptionUtils.getDebugException( e ) );
            } else if ("[EOF]".equals(e.getMessage())) {
                logAndAudit( AssertionMessages.EMAILALERT_AUTH_FAIL, null, ExceptionUtils.getDebugException( e ) );
            } else {
                logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[]{ "Unable to send email: " + e.getMessage() }, ExceptionUtils.getDebugException( e ) );
            }
            return AssertionStatus.FAILED;
        } catch (Exception e) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{ "Unexpected error sending email: " + e.getMessage() }, ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.FAILED;
        }
    }

    /*
     * Send the message.
     */
    private void sendMessage(final Session session, final String body,
                             String host,
                             InternetAddress[] toAddresses, InternetAddress[] ccAddresses, InternetAddress[] bccAddresses,
                             String userName, String pwd, InternetAddress[] recipients, InternetAddress fromAddress,
                             String subject, int portNumber) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.addRecipients(javax.mail.Message.RecipientType.TO, toAddresses);
        message.addRecipients(javax.mail.Message.RecipientType.CC, ccAddresses);
        message.addRecipients(javax.mail.Message.RecipientType.BCC, bccAddresses);
        message.setFrom(fromAddress);
        message.setSentDate(new java.util.Date());
        message.setSubject(subject);
        message.setText(body);
        message.saveChanges();

        Transport tr = getTransport(session, assertion.getProtocol() == EmailAlertAssertion.Protocol.SSL ? "smtps" : "smtp");
        tr.connect(host, portNumber, userName, pwd);
        tr.sendMessage(message, recipients);
    }

    protected Transport getTransport(Session session, String protocol) throws NoSuchProviderException {
        return session.getTransport(protocol);
    }


    private InternetAddress[] assembleAddresses(String addresses, String addressName) {
        InternetAddress[] addr = null;
        if(addresses!=null){
            try {
                addr = InternetAddress.parse(addresses);
            } catch (AddressException e) {
                logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "Unable to compose email addresses in ServerEmailAlertAssertion: invalid " + addressName }, ExceptionUtils.getDebugException( e ) );
                addr = null;
            }
        }
        return addr;
    }

    /*
     * amalgamate three arrays of InternetAddress[] types to a single list
     *
     * @return a single InternetAddress[] array with contents of the to, cc, and bcc address arrays 
     */
    private InternetAddress[] amalgamateAddresses(InternetAddress[] toAddresses, InternetAddress[] ccAddresses, InternetAddress[] bccAddresses){
        ArrayList<InternetAddress> list = new ArrayList<InternetAddress>();
        //add the to addresses
        if(toAddresses!=null)
            list.addAll(Arrays.asList(toAddresses));
        //add the cc addresses
        if(ccAddresses!=null)
            list.addAll(Arrays.asList(ccAddresses));
        //add the bcc addresses
        if(bccAddresses!=null)
            list.addAll(Arrays.asList(bccAddresses));
        return list.toArray(new InternetAddress[list.size()]);
    }

    /**
     * check that the port number string is ok
     *
     * @param portNumber the string value of the port number to send the email on
     * @return true if it's ok, false if it's bad
     */
    private boolean portNumberOk(String portNumber){
       try {
          int port = Integer.parseInt(portNumber);
          if (port < 0 || port > 65535){
            //invalid port number log and fail assertion.
            return false;
          }
       }catch (NumberFormatException nfe) {
            return false;
       }
       return true;
    }


}
