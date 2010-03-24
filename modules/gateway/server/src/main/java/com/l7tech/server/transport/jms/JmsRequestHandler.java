/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.message.JmsKnob;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.util.EventChannel;
import com.l7tech.util.BufferPoolByteArrayOutputStream;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.xml.sax.SAXException;

import javax.jms.*;
import javax.jms.Queue;
import javax.naming.NamingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class JmsRequestHandler {
    final private MessageProcessor messageProcessor;
    final private AuditContext auditContext;
    final private StashManagerFactory stashManagerFactory;
    final private ApplicationEventPublisher messageProcessingEventChannel;

    public JmsRequestHandler(ApplicationContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Spring Context is required");
        }
        messageProcessor = (MessageProcessor) ctx.getBean("messageProcessor", MessageProcessor.class);
        auditContext = (AuditContext) ctx.getBean("auditContext", AuditContext.class);
        stashManagerFactory = (StashManagerFactory) ctx.getBean("stashManagerFactory", StashManagerFactory.class);
        messageProcessingEventChannel = (ApplicationEventPublisher) ctx.getBean("messageProcessingEventChannel", EventChannel.class);
    }

    /**
     * Handle an incoming JMS SOAP request.  Also takes care of sending the reply if appropriate.
     *
     * @param receiver The calling receiver
     * @param bag The JMS context
     * @param transacted True is the session is transactional (so commit when done)
     * @param failureQueue The queue for failed messages (may be null)
     * @param jmsRequest The request message to process
     * @throws JmsRuntimeException if an error occurs
     */
    public void onMessage( final JmsReceiver receiver,
                           final JmsBag bag,
                           final boolean transacted,
                           final QueueSender failureQueue,
                           final Message jmsRequest ) throws JmsRuntimeException {

        final Message jmsResponse;
        final InputStream requestStream;
        final ContentTypeHeader ctype;
        final Map<String, Object> reqJmsMsgProps;
        final String soapAction;

        AssertionStatus status = AssertionStatus.UNDEFINED;
        boolean responseSuccess = false;
        boolean messageTooLarge = false;
        try {
            try {
                // Init content and type
                long size = 0;
                if ( jmsRequest instanceof TextMessage ) {
                    size = ((TextMessage)jmsRequest).getText().length() * 2;
                    requestStream = new ByteArrayInputStream(((TextMessage)jmsRequest).getText().getBytes(Charsets.UTF8));
                    ctype = ContentTypeHeader.XML_DEFAULT;
                } else if ( jmsRequest instanceof BytesMessage ) {
                    size = ((BytesMessage)jmsRequest).getBodyLength();
                    requestStream = new BytesMessageInputStream((BytesMessage)jmsRequest);
                    String requestCtype = jmsRequest.getStringProperty("Content-Type");
                    if (requestCtype != null)
                        ctype = ContentTypeHeader.parseValue(requestCtype);
                    else
                        ctype = ContentTypeHeader.XML_DEFAULT;
                } else {
                    handleInvalidMessageType(receiver, jmsRequest);
                    // not reached
                    ctype = null;
                    requestStream = null;
                }

                // enforce size restriction
                int sizeLimit = receiver.getMessageMaxSize();
                if ( sizeLimit > 0 && size > sizeLimit ) {
                    messageTooLarge = true;
                }

                // Copies the request JMS message properties into the request JmsKnob.
                final Map<String, Object> msgProps = new HashMap<String, Object>();
                for (Enumeration e = jmsRequest.getPropertyNames(); e.hasMoreElements() ;) {
                    final String name = (String)e.nextElement();
                    final Object value = jmsRequest.getObjectProperty(name);
                    msgProps.put(name, value);
                }
                reqJmsMsgProps = Collections.unmodifiableMap(msgProps);

                // Gets the JMS message property to use as SOAPAction, if present.
                String soapActionValue = null;
                final String jmsMsgPropWithSoapAction = receiver.getConnection().properties().getProperty(JmsConnection.JMS_MSG_PROP_WITH_SOAPACTION);
                if (jmsMsgPropWithSoapAction != null) {
                    soapActionValue = (String)reqJmsMsgProps.get(jmsMsgPropWithSoapAction);
                    if (_logger.isLoggable(Level.FINER))
                    _logger.finer("Found JMS message property to use as SOAPAction value: " + jmsMsgPropWithSoapAction + "=" + soapActionValue);
                }
                soapAction = soapActionValue;
            } catch (IOException ioe) {
                throw new JmsRuntimeException("Error processing request message", ioe);
            } catch (JMSException jmse) {
                throw new JmsRuntimeException("Error processing request message", jmse);
            }

            try {
                jmsResponse = buildMessageFromTemplate(bag, receiver, jmsRequest);
            } catch (JMSException e) {
                throw new JmsRuntimeException("Couldn't create response message!", e);
            }

            try {
                final long[] hardwiredserviceOidHolder = new long[]{0};
                try {
                    Properties props = receiver.getConnection().properties();
                    String tmp = props.getProperty(JmsConnection.PROP_IS_HARDWIRED_SERVICE);
                    if (tmp != null) {
                        if (Boolean.parseBoolean(tmp)) {
                            tmp = props.getProperty(JmsConnection.PROP_HARDWIRED_SERVICE_ID);
                            hardwiredserviceOidHolder[0] = Long.parseLong(tmp);
                        }
                    }
                } catch (Exception e) {
                    _logger.log(Level.WARNING, "Error processing hardwired service", e);
                }

                com.l7tech.message.Message request = new com.l7tech.message.Message();
                request.initialize(stashManagerFactory.createStashManager(), ctype, requestStream );
                request.attachJmsKnob(new JmsKnob() {
                    @Override
                    public boolean isBytesMessage() {
                        return jmsRequest instanceof BytesMessage;
                    }
                    @Override
                    public Map<String, Object> getJmsMsgPropMap() {
                        return reqJmsMsgProps;
                    }
                    @Override
                    public String getSoapAction() {
                        return soapAction;
                    }
                    @Override
                    public long getServiceOid() {
                        return hardwiredserviceOidHolder[0];
                    }
                });

                PolicyEnforcementContext context = null;
                String faultMessage = null;
                String faultCode = null;

                try {
                    final boolean replyExpected;
                    final Destination replyToDest = jmsRequest.getJMSReplyTo();
                    replyExpected = replyToDest != null || jmsRequest.getJMSCorrelationID() != null;

                    context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(
                            request,
                            null,
                            replyExpected );

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
                            faultCode = SoapConstants.FC_CLIENT;
                        } catch ( Throwable t ) {
                            _logger.log( Level.WARNING, "Exception while processing JMS message", t );
                            faultMessage = t.getMessage();
                            if ( faultMessage == null ) faultMessage = t.toString();
                        }
                    } else {
                        String msg1 = "Request message too large";
                        _logger.log( Level.INFO, msg1 );
                        faultMessage = msg1;
                        faultCode = SoapConstants.FC_CLIENT;
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
                                        faultCode == null ? SoapConstants.FC_SERVER : faultCode,
                                        faultMessage, null, "");

                                responseStream = new ByteArrayInputStream(faultXml.getBytes(Charsets.UTF8));

                                if (faultXml != null)
                                    messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                            } catch (SAXException e) {
                                throw new JmsRuntimeException(e);
                            }
                        }
                    }

                    if (!stealthMode) {
                        BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream();
                        final byte[] responseBytes;
                        try {
                            IOUtils.copyStream(responseStream, baos);
                            responseBytes = baos.toByteArray();
                        } finally {
                            baos.close();
                        }

                        if (jmsResponse instanceof BytesMessage) {
                            BytesMessage bresp = (BytesMessage)jmsResponse;
                            bresp.writeBytes(responseBytes);
                        } else if ( jmsResponse instanceof TextMessage ) {
                            TextMessage tresp = (TextMessage)jmsResponse;
                            tresp.setText(new String(responseBytes, JmsUtil.DEFAULT_ENCODING));
                        } else {
                            throw new JmsRuntimeException( "Can't send a " + jmsResponse.getClass().getName() +
                                                           ". Only BytesMessage and TextMessage are supported" );
                        }

                        // Copies the JMS message properties from the response JmsKnob to the response JMS message.
                        // Propagation rules has already been enforced in the knob by the JMS routing assertion.
                        final JmsKnob jmsResponseKnob = context.getResponse().getKnob(JmsKnob.class);
                        if (jmsResponseKnob != null) {
                            final Map<String, Object> respJmsMsgProps = jmsResponseKnob.getJmsMsgPropMap();
                            for (String name : respJmsMsgProps.keySet()) {
                                jmsResponse.setObjectProperty(name, respJmsMsgProps.get(name));
                            }
                        }

                        responseSuccess = sendResponse( jmsRequest, jmsResponse, bag, receiver, status );
                    } else { // is stealth mode
                        responseSuccess = true;
                    }
                } catch (IOException e) {
                    throw new JmsRuntimeException(e);
                } catch (JMSException e) {
                    throw new JmsRuntimeException("Couldn't acknowledge message!", e);
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
        } finally {
            if ( transacted ) {
                boolean handledAnyFailure;
                handledAnyFailure = status == AssertionStatus.NONE || failureQueue != null && postMessageToFailureQueue(jmsRequest, failureQueue);

                Session session = bag.getSession();
                if ( responseSuccess && handledAnyFailure ) {
                    try {
                        _logger.log( Level.FINE, "Committing JMS session." );
                        session.commit();
                    } catch (Exception e) {
                        _logger.log( Level.WARNING, "Error committing JMS session.", e );
                    }
                } else {
                    try {
                        _logger.log( Level.FINE, "Rolling back JMS session." );
                        session.rollback();
                    } catch (Exception e) {
                        _logger.log( Level.WARNING, "Error during JMS session rollback.", e );
                    }
                }
            }
        }
    }

    private void handleInvalidMessageType(JmsReceiver receiver, Message message) throws JmsRuntimeException {
        String msg = "Received message of unsupported type " + message.getClass().getName() +
                     " on " + receiver.getInboundRequestEndpoint().getDestinationName() +
                     ".  Only TextMessage and BytesMessage are supported";
        _logger.warning( msg );
        throw new JmsRuntimeException( msg );
    }

    private Message buildMessageFromTemplate(JmsBag bag, JmsReceiver receiver, Message template)
            throws JMSException, JmsRuntimeException {
        Message message = null;
        if ( template instanceof TextMessage ) {
            message = bag.getSession().createTextMessage();
        } else if ( template instanceof BytesMessage ) {
            message = bag.getSession().createBytesMessage();
        } else {
            handleInvalidMessageType(receiver, template);
        }
        return message;
    }

    private boolean sendResponse(Message jmsRequestMsg, Message jmsResponseMsg, JmsBag bag, JmsReceiver receiver, AssertionStatus status ) {
        boolean sent = false;
        try {
            Destination jmsReplyDest = receiver.getOutboundResponseDestination(jmsRequestMsg, bag);
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

                    final String newCorrId = receiver.getInboundRequestEndpoint().isUseMessageIdForCorrelation() ?
                            jmsRequestMsg.getJMSMessageID() :
                            jmsRequestMsg.getJMSCorrelationID();
                    jmsResponseMsg.setJMSCorrelationID(newCorrId);
                    producer.send( jmsResponseMsg );
                    _logger.fine( "Sent response to " + jmsReplyDest );
                } finally {
                    if ( producer != null ) producer.close();
                }
            }
            sent = true;
        } catch ( JMSException e ) {
            _logger.log( Level.WARNING, "Caught JMS exception while sending response", e );
        } catch (NamingException e ) {
            _logger.log(Level.WARNING, "Error trying to lookup the destination endpoint from preset reply-to queue name", e );
        }
        return sent;
    }

    private boolean postMessageToFailureQueue(Message message, QueueSender sender) {
        boolean posted = false;

        try {
            sender.send(message);
            posted = true;
        } catch (JMSException jmse) {
            _logger.log( Level.WARNING, "Error sending message to failure queue", jmse);
        }

        return posted;
    }

    private static final Logger _logger = Logger.getLogger(JmsRequestHandler.class.getName());
}
