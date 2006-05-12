/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.audit.AuditContext;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.message.JmsKnob;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.jms.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
class JmsRequestHandler {
    final private ApplicationContext springContext;
    final private MessageProcessor messageProcessor;
    final private AuditContext auditContext;
    final private SoapFaultManager soapFaultManager;

    public JmsRequestHandler(ApplicationContext ctx) {
        this.springContext = ctx;
        if (ctx == null) {
            throw new IllegalArgumentException("Spring Context is required");
        }
        messageProcessor = (MessageProcessor) ctx.getBean("messageProcessor", MessageProcessor.class);
        auditContext = (AuditContext) ctx.getBean("auditContext", AuditContext.class);
        soapFaultManager = (SoapFaultManager)ctx.getBean("soapFaultManager", SoapFaultManager.class);
    }

    /**
     * Handle an incoming JMS SOAP request.  Also takes care of sending the reply if appropriate.
     * @param receiver
     * @param bag
     * @param jmsRequest
     */
    public void onMessage( JmsReceiver receiver, final JmsBag bag, final javax.jms.Message jmsRequest ) throws JmsRuntimeException {

        Message jmsResponse = null;

        try {
            InputStream requestStream = null;
            ContentTypeHeader ctype = ContentTypeHeader.XML_DEFAULT;
            if ( jmsRequest instanceof TextMessage ) {
                requestStream = new ByteArrayInputStream(((TextMessage)jmsRequest).getText().getBytes("UTF-8"));
                jmsResponse = bag.getSession().createBytesMessage();

            } else if ( jmsRequest instanceof BytesMessage ) {
                requestStream = new BytesMessageInputStream((BytesMessage)jmsRequest);
                String requestCtype = jmsRequest.getStringProperty("Content-Type");
                if (requestCtype != null)
                    ctype = ContentTypeHeader.parseValue(requestCtype);
                jmsResponse = bag.getSession().createBytesMessage();

            } else {
                String msg = "Received message of unsupported type " + jmsRequest.getClass().getName() +
                             " on " + receiver.getInboundRequestEndpoint().getDestinationName() +
                             ".  Only TextMessage and BytesMessage are supported";
                _logger.warning( msg );
                throw new JmsRuntimeException( msg );
            }

            com.l7tech.common.message.Message request = new com.l7tech.common.message.Message();
            request.initialize(StashManagerFactory.createStashManager(), ctype, requestStream );
            request.attachJmsKnob(new JmsKnob() {
                public boolean isBytesMessage() {
                    return jmsRequest instanceof BytesMessage;
                }
            });

            final PolicyEnforcementContext context = new PolicyEnforcementContext(request,
                                                                                  new com.l7tech.common.message.Message());
            AssertionStatus status = AssertionStatus.UNDEFINED;

            String faultMessage = null;
            String faultCode = null;

            try {
                context.setAuditContext(auditContext);
                context.setSoapFaultManager(soapFaultManager);

                // WebSphere MQ doesn't like this with AUTO_ACKNOWLEDGE
                // jmsRequest.acknowledge(); // TODO parameterize acknowledge semantics?

                if(jmsRequest.getJMSReplyTo() != null || jmsRequest.getJMSCorrelationID() != null) {
                    context.setReplyExpected(true);
                } else {
                    context.setReplyExpected(false);
                }

                boolean stealthMode = false;
                InputStream responseStream = null;
                try {
                    status = messageProcessor.processMessage(context);
                    _logger.finest("Policy resulted in status " + status);
                    if (context.getResponse().getKnob(XmlKnob.class) != null ||
                        context.getResponse().getKnob(MimeKnob.class) != null) {
                        // if the policy is not successful AND the stealth flag is on, drop connection
                        if (status != AssertionStatus.NONE && context.isStealthResponseMode()) {
                            _logger.info("Policy returned error and stealth mode is set. " +
                                        "Not sending response message.");
                            stealthMode = true;
                        }
                        else {
                            responseStream = new ByteArrayInputStream(XmlUtil.nodeToString(context.getResponse().getXmlKnob().getDocumentReadOnly()).getBytes());
                        }
                    }
                    else {
                        _logger.finer("No response received");
                        responseStream = null;
                    }
                } catch ( PolicyVersionException pve ) {
                    String msg1 = "Request referred to an outdated version of policy";
                    _logger.log( Level.INFO, msg1 );
                    faultMessage = msg1;
                    faultCode = SoapFaultUtils.FC_CLIENT;
                } catch ( Throwable t ) {
                    _logger.log( Level.WARNING, "Exception while processing JMS message", t );
                    faultMessage = t.getMessage();
                    if ( faultMessage == null ) faultMessage = t.toString();
                }

                if ( responseStream == null ) {
                    if (context.isStealthResponseMode()) {
                        _logger.info("No response data available and stealth mode is set. " +
                                    "Not sending response message.");
                        stealthMode = true;
                    }
                    else {
                        if ( faultMessage == null ) faultMessage = status.getMessage();
                        try {
                            String faultXml = SoapFaultUtils.generateSoapFaultXml(
                                    faultCode == null ? SoapFaultUtils.FC_SERVER : faultCode,
                                    faultMessage, null, "");

                            responseStream = new ByteArrayInputStream(faultXml.getBytes("UTF-8"));

                            if (faultXml != null)
                                springContext.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                        } catch (SAXException e) {
                            throw new JmsRuntimeException(e);
                        }
                    }
                }

                if (!stealthMode) {
                    BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream();
                    final byte[] responseBytes;
                    try {
                        HexUtils.copyStream(responseStream, baos);
                        responseBytes = baos.toByteArray();
                    } finally {
                        baos.close();
                    }

                    JmsKnob requestJmsKnob = (JmsKnob)context.getRequest().getKnob(JmsKnob.class);
                    if (requestJmsKnob == null)
                        throw new JmsRuntimeException("Request wasn't a JMS message");
                    if (requestJmsKnob.isBytesMessage()) {
                        BytesMessage bresp = (BytesMessage)jmsResponse;
                        bresp.writeBytes(responseBytes);
                    } else if ( jmsResponse instanceof TextMessage ) {
                        TextMessage tresp = (TextMessage)jmsResponse;
                        tresp.setText(new String(responseBytes, JmsUtil.DEFAULT_ENCODING));
                    } else {
                        throw new JmsRuntimeException( "Can't send a " + jmsResponse.getClass().getName() +
                                                       ". Only BytesMessage and TextMessage are supported" );
                    }

                    sendResponse( jmsRequest, jmsResponse, bag, receiver, status );
                }
            } catch (IOException e) {
                _logger.log( Level.WARNING, e.toString(), e );
            } catch (JMSException e) {
                _logger.log( Level.WARNING, "Couldn't acknowledge message!", e );
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
        } catch (JMSException e) {
            _logger.log(Level.WARNING, "Couldn't create response message!", e);
        } catch (NoSuchPartException e) {
            throw new RuntimeException(e); // can't happen
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private void sendResponse(Message jmsRequestMsg, Message jmsResponseMsg, JmsBag bag, JmsReceiver receiver, AssertionStatus status ) {
        try {
            Destination jmsReplyDest = receiver.getOutboundResponseDestination( jmsRequestMsg, jmsResponseMsg );
            if ( status != AssertionStatus.NONE ) {
                // TODO send response to failure endpoint if defined
            }

            if ( jmsReplyDest == null ) {
                _logger.fine( "No response will be sent!" );
            } else {
                _logger.fine( "Sending response to " + jmsReplyDest );
                MessageProducer producer = null;
                try {
                    Session session = bag.getSession();
                    if ( session instanceof QueueSession ) {
                        producer = ((QueueSession)session).createSender( (Queue)jmsReplyDest );
                    } else if ( session instanceof TopicSession ) {
                        producer = ((TopicSession)session).createPublisher( (Topic)jmsReplyDest );
                    } else {
                        producer = session.createProducer( jmsReplyDest );
                    }
                    jmsResponseMsg.setJMSCorrelationID( jmsRequestMsg.getJMSCorrelationID() );
                    producer.send( jmsResponseMsg );
                    _logger.fine( "Sent response to " + jmsReplyDest );
                } finally {
                    if ( producer != null ) producer.close();
                }
            }
        } catch ( JMSException e ) {
            _logger.log( Level.WARNING, "Caught JMS exception while sending response", e );
        }
    }

    private static final Logger _logger = Logger.getLogger(JmsRequestHandler.class.getName());
}
