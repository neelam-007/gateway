package com.l7tech.server.transport.email;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.EmailKnob;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.util.EventChannel;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.gateway.common.transport.email.EmailListener;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles email messages received from an email listener. Stealth mode is always set, since it is
 * currently not possible to return a response. The message is passed off to the message handler.
 */
public class EmailHandlerImpl implements EmailHandler {
    private MessageProcessor messageProcessor;
    private AuditContext auditContext;
    private StashManagerFactory stashManagerFactory;
    private EventChannel messageProcessingEventChannel;
    
    public EmailHandlerImpl(ApplicationContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Spring Context is required");
        }
        messageProcessor = (MessageProcessor) ctx.getBean("messageProcessor", MessageProcessor.class);
        auditContext = (AuditContext) ctx.getBean("auditContext", AuditContext.class);
        stashManagerFactory = (StashManagerFactory) ctx.getBean("stashManagerFactory", StashManagerFactory.class);
        messageProcessingEventChannel = (EventChannel) ctx.getBean("messageProcessingEventChannel", EventChannel.class);
    }

    @Override
    public void onMessage( final EmailListenerConfig emailListenerCfg,
                           final MimeMessage message )
            throws EmailListenerRuntimeException
    {
        final MimeMessage emailResponse;
        final InputStream requestStream;
        final ContentTypeHeader ctype;
        final Map<String, Object> reqEmailMsgProps;
        final String soapAction;

        AssertionStatus status = AssertionStatus.UNDEFINED;
        boolean responseSuccess = false;
        boolean messageTooLarge = false;
        try {
            // Init content and type
            long size = 0;
            String contentTypeValue = message.getContentType();
            if(contentTypeValue == null) {
                ctype = ContentTypeHeader.XML_DEFAULT;
            } else {
                ctype = ContentTypeHeader.parseValue(contentTypeValue);
            }
            size = message.getSize();
            requestStream = message.getInputStream();

            // enforce size restriction
            int sizeLimit = emailListenerCfg.getMaxMessageSize();
            if ( sizeLimit > 0 && size > sizeLimit ) {
                messageTooLarge = true;
            }

            // Copies the request JMS message properties into the request JmsKnob.
            final Map<String, Object> msgProps = new HashMap<String, Object>();
            for(Enumeration e = message.getAllHeaders(); e.hasMoreElements();) {
                Header header = (Header)e.nextElement();
                msgProps.put(header.getName(), header.getValue());
            }
            reqEmailMsgProps = Collections.unmodifiableMap(msgProps);

            // Gets the JMS message property to use as SOAPAction, if present.
            String soapActionValue = null;
            soapActionValue = (String)msgProps.get(SoapUtil.SOAPACTION);
            if(soapActionValue == null) {
                soapActionValue = ctype.getParam("action");
                if(soapActionValue != null && _logger.isLoggable(Level.FINER))
                    _logger.finer("Found email header to use for SOAPAction value: action=" + soapActionValue);
            } else {
                if(_logger.isLoggable(Level.FINER))
                    _logger.finer("Found email header to use for SOAPAction value: SOAPAction=" + soapActionValue);
            }
            soapAction = soapActionValue;
        } catch (IOException ioe) {
            throw new EmailListenerRuntimeException("Error processing request message", ioe);
        } catch (MessagingException me) {
            throw new EmailListenerRuntimeException("Error processing request message", me);
        }

        emailResponse = buildMessageFromTemplate(emailListenerCfg, message);

        try {
            final long[] hardWiredServiceOidHolder = new long[]{0};
            try {
                Properties props = emailListenerCfg.getEmailListener().properties();
                String tmp = props.getProperty(EmailListener.PROP_IS_HARDWIRED_SERVICE);
                if (tmp != null) {
                    if (Boolean.parseBoolean(tmp)) {
                        tmp = props.getProperty( EmailListener.PROP_HARDWIRED_SERVICE_ID);
                        hardWiredServiceOidHolder[0] = Long.parseLong(tmp);
                    }
                }
            } catch (Exception e) {
                _logger.log(Level.WARNING, "Error processing hardwired service", e);
            }

            com.l7tech.message.Message request = new com.l7tech.message.Message();
            request.initialize(stashManagerFactory.createStashManager(), ctype, requestStream );
            request.attachEmailKnob(new EmailKnob() {
                @Override
                public Map<String, Object> getEmailMsgPropMap() {
                    return reqEmailMsgProps;
                }
                @Override
                public String getSoapAction() {
                    return soapAction;
                }

                @Override
                public long getServiceOid() {
                    return hardWiredServiceOidHolder[0];
                }
            });

            final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null, false);

            String faultMessage = null;
            String faultCode = null;

            try {
                boolean stealthMode = false;
                InputStream responseStream = null;
                if ( !messageTooLarge ) {
                    try {
                        status = messageProcessor.processMessage(context);
                        context.setPolicyResult(status);
                        _logger.finest("Policy resulted in status " + status);
                        if (context.getResponse().getKnob(XmlKnob.class) != null ||
                                context.getResponse().getKnob(MimeKnob.class) != null) {
                            // if the policy is not successful AND the stealth flag is on, drop connection
                            if (status != AssertionStatus.NONE && context.isStealthResponseMode()) {
                                _logger.info("Policy returned error and stealth mode is set. " +
                                        "Not sending response message.");
                                stealthMode = true;
                            } else {
                                responseStream = new ByteArrayInputStream(XmlUtil.nodeToString(context.getResponse().getXmlKnob().getDocumentReadOnly()).getBytes());
                            }
                        } else {
                            _logger.finer("No response received");
                            responseStream = null;
                        }
                    } catch ( PolicyVersionException pve ) {
                        String msg1 = "Request referred to an outdated version of policy";
                        _logger.log( Level.INFO, msg1 );
                        faultMessage = msg1;
                        faultCode = "Client";
                    } catch ( Throwable t ) {
                        _logger.log( Level.WARNING, "Exception while processing email message: {0}", ExceptionUtils.getMessage(t));
                        faultMessage = t.getMessage();
                        if ( faultMessage == null ) faultMessage = t.toString();
                    }
                } else {
                    String msg1 = "Request message too large";
                    _logger.log( Level.INFO, msg1 );
                    faultMessage = msg1;
                    faultCode = "Client";
                }

                if ( responseStream == null ) {
                    if (context.isStealthResponseMode()) {
                        _logger.info("No response data available and stealth mode is set. " +
                                "Not sending response message.");
                        stealthMode = true;
                    } else {
                        if ( faultMessage == null ) faultMessage = status.getMessage();
                        try {
                            String faultXml = SoapFaultUtils.generateSoapFaultXml(
                                    (context.getService() != null) ? context.getService().getSoapVersion() : SoapVersion.UNKNOWN,
                                    faultCode == null ? "Server" : faultCode,
                                    faultMessage, null, "");

                            responseStream = new ByteArrayInputStream(faultXml.getBytes("UTF-8"));

                            if (faultXml != null) {
                                messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                            }
                        } catch (SAXException e) {
                            throw new EmailListenerRuntimeException(e);
                        }
                    }
                }

                responseSuccess = true;
            } catch (IOException e) {
                throw new EmailListenerRuntimeException(e);
            } finally {
                try {
                    auditContext.flush();
                }
                finally {
                    if (context != null) {
                        try {
                            context.close();
                        } catch (Throwable t) {
                            _logger.log(Level.SEVERE, "soapRequest cleanup threw", t);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private MimeMessage buildMessageFromTemplate(EmailListenerConfig emailListenerCfg, MimeMessage template) {
        return null;
    }

    private static final Logger _logger = Logger.getLogger(EmailHandlerImpl.class.getName());
}
