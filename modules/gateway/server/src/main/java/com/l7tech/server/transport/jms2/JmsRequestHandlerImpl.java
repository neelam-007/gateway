/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.transport.jms2;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.IOUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.message.JmsKnob;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.transport.jms.BytesMessageInputStream;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
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

/**
 * The JmsRequestHandler is responsible for processing inbound Jms request messages and prvoiding
 * the appropriate response (or error handling).  This is the place in the JMS subsystem that hooks
 * into the SSG via the MessageProcessor.
 *
 * Note: this class is largely unchanged from the original Jms implementation.
 */
public class JmsRequestHandlerImpl implements JmsRequestHandler {

    private MessageProcessor messageProcessor;
    private AuditContext auditContext;
    private SoapFaultManager soapFaultManager;
    private ClusterPropertyCache clusterPropertyCache;
    private StashManagerFactory stashManagerFactory;
    private MessageProducer responseProducer;

    public JmsRequestHandlerImpl(ApplicationContext ctx) {

//        this.springContext = ctx;
        if (ctx == null) {
            throw new IllegalArgumentException("Spring Context is required");
        }
        messageProcessor = (MessageProcessor) ctx.getBean("messageProcessor", MessageProcessor.class);
        auditContext = (AuditContext) ctx.getBean("auditContext", AuditContext.class);
        soapFaultManager = (SoapFaultManager)ctx.getBean("soapFaultManager", SoapFaultManager.class);
        clusterPropertyCache = (ClusterPropertyCache)ctx.getBean("clusterPropertyCache", ClusterPropertyCache.class);
        stashManagerFactory = (StashManagerFactory) ctx.getBean("stashManagerFactory", StashManagerFactory.class);
    }


    /**
     * Handle an incoming JMS SOAP request.  Also takes care of sending the reply if appropriate.
     *
     * @param endpointCfg The Jms endpoint configuration that this handler operates on
     * @param bag The JMS context
     * @param transacted True is the session is transactional (so commit when done)
     * @param failureQueue The queue for failed messages (may be null)
     * @param jmsRequest The request message to process
     * @throws com.l7tech.server.transport.jms.JmsRuntimeException if an error occurs
     */
    public void onMessage( final JmsEndpointConfig endpointCfg,
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
                    requestStream = new ByteArrayInputStream(((TextMessage)jmsRequest).getText().getBytes("UTF-8"));
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
                    handleInvalidMessageType(endpointCfg, jmsRequest);
                    // not reached
                    ctype = null;
                    requestStream = null;
                }

                // enforce size restriction
                int sizeLimit = endpointCfg.getMaxMessageSize();
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
                final String jmsMsgPropWithSoapAction = (String)endpointCfg.getConnection().properties().get(JmsConnection.JMS_MSG_PROP_WITH_SOAPACTION);
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
                jmsResponse = buildMessageFromTemplate(bag, endpointCfg, jmsRequest);
            } catch (JMSException e) {
                throw new JmsRuntimeException("Couldn't create response message!", e);
            }

            try {
                com.l7tech.message.Message request = new com.l7tech.message.Message();
                request.initialize(stashManagerFactory.createStashManager(), ctype, requestStream );
                request.attachJmsKnob(new JmsKnob() {
                    public boolean isBytesMessage() {
                        return jmsRequest instanceof BytesMessage;
                    }
                    public Map<String, Object> getJmsMsgPropMap() {
                        return reqJmsMsgProps;
                    }
                    public String getSoapAction() {
                        return soapAction;
                    }
                });

                final PolicyEnforcementContext context = new PolicyEnforcementContext(request,
                                                                                      new com.l7tech.message.Message());

                try {
                    Properties props = endpointCfg.getConnection().properties();
                    String tmp = props.getProperty(JmsConnection.PROP_IS_HARDWIRED_SERVICE);
                    if (tmp != null) {
                        if (Boolean.parseBoolean(tmp)) {
                            tmp = props.getProperty(JmsConnection.PROP_HARDWIRED_SERVICE_ID);
                            long hardwiredserviceid = Long.parseLong(tmp);
                            context.setHardwiredService(hardwiredserviceid);
                        }
                    }
                } catch (Exception e) {
                    _logger.log(Level.WARNING, "problem testing for hardwired service", e);
                }

                String faultMessage = null;
                String faultCode = null;

                try {
                    context.setAuditContext(auditContext);
                    context.setSoapFaultManager(soapFaultManager);
                    context.setClusterPropertyCache(clusterPropertyCache);

                    final Destination replyToDest = jmsRequest.getJMSReplyTo();
                    if (replyToDest != null || jmsRequest.getJMSCorrelationID() != null) {
                        context.setReplyExpected(true);
                    } else {
                        context.setReplyExpected(false);
                    }

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
                            faultCode = SoapUtil.FC_CLIENT;
                        } catch ( Throwable t ) {
                            _logger.log( Level.WARNING, "Exception while processing JMS message", t );
                            faultMessage = t.getMessage();
                            if ( faultMessage == null ) faultMessage = t.toString();
                        }
                    } else {
                        String msg1 = "Request message too large";
                        _logger.log( Level.INFO, msg1 );
                        faultMessage = msg1;
                        faultCode = SoapUtil.FC_CLIENT;
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
                                        faultCode == null ? SoapUtil.FC_SERVER : faultCode,
                                        faultMessage, null, "");

                                responseStream = new ByteArrayInputStream(faultXml.getBytes("UTF-8"));

                                if (faultXml != null) {
                                    endpointCfg.getApplicationContext().publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                                }
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
                        final JmsKnob jmsResponseKnob = (JmsKnob)context.getResponse().getKnob(JmsKnob.class);
                        if (jmsResponseKnob != null) {
                            final Map<String, Object> respJmsMsgProps = jmsResponseKnob.getJmsMsgPropMap();
                            for (String name : respJmsMsgProps.keySet()) {
                                jmsResponse.setObjectProperty(name, respJmsMsgProps.get(name));
                            }
                        }

                        responseSuccess = sendResponse( jmsRequest, jmsResponse, bag, endpointCfg, status );
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

            // bug #5415 - we will close the MessageProducer only after a transaction is committed
            if (responseProducer != null) {
                try {
                    responseProducer.close();
                } catch (JMSException jex) {
                    // ignore at this point
                } 
            }
        }
    }

    private void handleInvalidMessageType(JmsEndpointConfig endpointCfg, Message message) throws JmsRuntimeException {
        String msg = "Received message of unsupported type " + message.getClass().getName() +
                     " on " + endpointCfg.getEndpoint().getDestinationName() +
                     ".  Only TextMessage and BytesMessage are supported";
        _logger.warning( msg );
        throw new JmsRuntimeException( msg );
    }

    private Message buildMessageFromTemplate(JmsBag bag, JmsEndpointConfig endpointCfg, Message template)
            throws JMSException, JmsRuntimeException {
        Message message = null;
        if ( template instanceof TextMessage ) {
            message = bag.getSession().createTextMessage();
        } else if ( template instanceof BytesMessage ) {
            message = bag.getSession().createBytesMessage();
        } else {
            handleInvalidMessageType(endpointCfg, template);
        }
        return message;
    }

    private boolean sendResponse(Message jmsRequestMsg, Message jmsResponseMsg, JmsBag bag, JmsEndpointConfig endpointCfg, AssertionStatus status ) {
        boolean sent = false;
        try {
            Destination jmsReplyDest = endpointCfg.getResponseDestination(jmsRequestMsg, bag);
            if ( status != AssertionStatus.NONE ) {
                // TODO send response to failure endpoint if defined
            }

            if ( jmsReplyDest == null ) {
                _logger.fine( "No response will be sent!" );
            } else {
                _logger.fine( "Sending response to " + jmsReplyDest );

                // bug #5415 - we will close the MessageProducer only after a transaction is committed
                Session session = bag.getSession();
                if ( session instanceof QueueSession ) {
                    responseProducer = ((QueueSession)session).createSender( (Queue)jmsReplyDest );
                } else if ( session instanceof TopicSession ) {
                    responseProducer = ((TopicSession)session).createPublisher( (Topic)jmsReplyDest );
                } else {
                    responseProducer = session.createProducer( jmsReplyDest );
                }

                final String newCorrId = endpointCfg.getEndpoint().isUseMessageIdForCorrelation() ?
                        jmsRequestMsg.getJMSMessageID() :
                        jmsRequestMsg.getJMSCorrelationID();
                jmsResponseMsg.setJMSCorrelationID(newCorrId);
                responseProducer.send( jmsResponseMsg );
                _logger.fine( "Sent response to " + jmsReplyDest );
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

    private static final Logger _logger = Logger.getLogger(JmsRequestHandlerImpl.class.getName());


    /* New setters for spring initialization */

    public void setMessageProcessor(MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    public void setAuditContext(AuditContext auditContext) {
        this.auditContext = auditContext;
    }

    public void setSoapFaultManager(SoapFaultManager soapFaultManager) {
        this.soapFaultManager = soapFaultManager;
    }

    public void setStashManagerFactory(StashManagerFactory stashManagerFactory) {
        this.stashManagerFactory = stashManagerFactory;
    }
}