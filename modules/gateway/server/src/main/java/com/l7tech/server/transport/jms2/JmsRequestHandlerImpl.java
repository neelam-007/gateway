package com.l7tech.server.transport.jms2;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.message.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.audit.MessageSummaryAuditFactory;
import com.l7tech.server.cluster.ClusterMaster;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.transport.jms.*;
import com.l7tech.server.util.EventChannel;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.xml.sax.SAXException;

import javax.jms.*;
import javax.jms.Message;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.message.JmsKnob.HEADER_TYPE_JMS_HEADER;
import static com.l7tech.message.JmsKnob.HEADER_TYPE_JMS_PROPERTY;

/**
 * The JmsRequestHandler is responsible for processing inbound Jms request messages and providing
 * the appropriate response (or error handling).  This is the place in the JMS subsystem that hooks
 * into the SSG via the MessageProcessor.
 *
 * This class is for use from a single thread.
 *
 * Note: this class is largely unchanged from the original Jms implementation.
 */
public class JmsRequestHandlerImpl implements JmsRequestHandler {

    private static final Logger _logger = Logger.getLogger(JmsRequestHandlerImpl.class.getName());
    private static final boolean topicMasterOnly = ConfigFactory.getBooleanProperty( "com.l7tech.server.transport.jms.topicMasterOnly", true );
    private static final long DEFAULT_MESSAGE_MAX_BYTES = 2621440L;

    private final Config config;
    private final MessageProcessor messageProcessor;
    private final StashManagerFactory stashManagerFactory;
    private final ApplicationEventPublisher messageProcessingEventChannel;
    private final ClusterMaster clusterMaster;

    private AuditContextFactory auditContextFactory;
    private MessageSummaryAuditFactory messageSummaryAuditFactory;
    private MessageProducer responseProducer;

    public JmsRequestHandlerImpl( final ApplicationContext ctx ) {
        if (ctx == null) {
            throw new IllegalArgumentException("Spring Context is required");
        }
        config = ctx.getBean("serverConfig", Config.class);
        messageProcessor = ctx.getBean("messageProcessor", MessageProcessor.class);
        stashManagerFactory = ctx.getBean("stashManagerFactory", StashManagerFactory.class);
        messageProcessingEventChannel = ctx.getBean("messageProcessingEventChannel", EventChannel.class);
        clusterMaster = ctx.getBean("clusterMaster", ClusterMaster.class);
        auditContextFactory = ctx.getBean("auditContextFactory", AuditContextFactory.class);
        messageSummaryAuditFactory = ctx.getBean("messageSummaryAuditFactory", MessageSummaryAuditFactory.class);
    }

    /**
     * Handle an incoming JMS SOAP request.  Also takes care of sending the reply if appropriate.
     *
     * @param endpointCfg The Jms endpoint configuration that this handler operates on
     * @param bag The JMS context
     * @param transacted True is the session is transactional (so commit when done)
     * @param jmsRequest The request message to process
     * @throws com.l7tech.server.transport.jms.JmsRuntimeException if an error occurs
     */
    @Override
    public void onMessage( final JmsEndpointConfig endpointCfg,
                           final JmsBag bag,
                           final boolean transacted,
                           final Message jmsRequest ) throws JmsRuntimeException {
        final Message jmsResponse;
        final InputStream requestStream;
        final ContentTypeHeader ctype;
        final Map<String, Object> reqJmsMsgProps;
        final Map<String, String> reqJmsMsgHeaders;
        final String soapAction;

        final AssertionStatus status[] = { AssertionStatus.UNDEFINED };
        final boolean responseSuccess[] = { false };
        final boolean messageTooLarge[] = { false };
        Properties props = endpointCfg.getConnection().properties();

        HeadersKnob requestMessageHeadersKnob = new HeadersKnobSupport();

        try {
            if ( topicMasterOnly && !endpointCfg.isQueue() && !clusterMaster.isMaster() ) {
                status[0] = AssertionStatus.NONE;
                responseSuccess[0] = true;
                _logger.fine( "Not processing message from topic (node is not master)" );
                return;
            }

            try {
                // Init content and type
                long size = 0;

                ctype = getContentType(jmsRequest, props);

                if ( jmsRequest instanceof TextMessage ) {
                    size = ((TextMessage)jmsRequest).getText().length() * 2;
                    requestStream = new ByteArrayInputStream(((TextMessage)jmsRequest).getText().getBytes(Charsets.UTF8));
                } else if ( jmsRequest instanceof BytesMessage ) {
                    size = ((BytesMessage)jmsRequest).getBodyLength();
                    requestStream = new BytesMessageInputStream((BytesMessage)jmsRequest);
                } else {
                    // not reached;
                    handleInvalidMessageType(endpointCfg, jmsRequest);
                    requestStream = null;
                }

                // enforce size restriction
                long sizeLimit;
                if(endpointCfg.getEndpoint().getRequestMaxSize()<0L)
                {
                    long clusterPropValue = config.getLongProperty(ServerConfigParams.PARAM_JMS_MESSAGE_MAX_BYTES, DEFAULT_MESSAGE_MAX_BYTES);
                    if(clusterPropValue >= 0L ){
                        sizeLimit = clusterPropValue;
                    }else{
                        sizeLimit = com.l7tech.message.Message.getMaxBytes();
                    }
                }else{
                    sizeLimit = endpointCfg.getEndpoint().getRequestMaxSize();
                }

                if ( sizeLimit > 0 && size > sizeLimit ) {
                    messageTooLarge[0] = true;
                }

                // Copy the request JMS message properties into the request JmsKnob and HeadersKnob
                final Map<String, Object> msgProps = new HashMap<>();
                for (Enumeration e = jmsRequest.getPropertyNames(); e.hasMoreElements() ;) {
                    final String name = (String)e.nextElement();
                    final Object value = jmsRequest.getObjectProperty(name);
                    msgProps.put(name, value);
                    requestMessageHeadersKnob.addHeader(name, value, HEADER_TYPE_JMS_PROPERTY);
                }

                reqJmsMsgProps = Collections.unmodifiableMap(msgProps);
                reqJmsMsgHeaders = Collections.unmodifiableMap(JmsUtil.getJmsHeaders(jmsRequest));

                // Gets the JMS message property to use as SOAPAction, if present.
                String soapActionValue = null;
                final String jmsMsgPropWithSoapAction = endpointCfg.getConnection().properties().getProperty(JmsConnection.JMS_MSG_PROP_WITH_SOAPACTION);
                if (jmsMsgPropWithSoapAction != null) {
                    soapActionValue = (String)reqJmsMsgProps.get(jmsMsgPropWithSoapAction);
                    if (_logger.isLoggable(Level.FINER))
                    _logger.finer("Found JMS message property to use as SOAPAction value: " + jmsMsgPropWithSoapAction + "=" + soapActionValue);
                }
                soapAction = soapActionValue;
            } catch (IOException | JMSException ioe) {
                throw new JmsRuntimeException("Error processing request message", ioe);
            }

            try {
                jmsResponse = buildMessageFromTemplate(bag, endpointCfg, jmsRequest);
            } catch (JMSException e) {
                throw new JmsRuntimeException("Couldn't create response message!", e);
            }

            try {
                final Goid[] hardwiredServiceGoidHolder = new Goid[]{PublishedService.DEFAULT_GOID};
                try {
                    String tmp = props.getProperty(JmsConnection.PROP_IS_HARDWIRED_SERVICE);
                    if (tmp != null) {
                        if (Boolean.parseBoolean(tmp)) {
                            tmp = props.getProperty(JmsConnection.PROP_HARDWIRED_SERVICE_ID);
                            hardwiredServiceGoidHolder[0] = GoidUpgradeMapper.mapId(EntityType.SERVICE, tmp);
                        }
                    }
                } catch (Exception e) {
                    _logger.log(Level.WARNING, "Error processing hardwired service", e);
                }

                com.l7tech.message.Message request = new com.l7tech.message.Message();
                request.initialize(stashManagerFactory.createStashManager(), ctype, requestStream, 0);
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
                    public Goid getServiceGoid() {
                        return hardwiredServiceGoidHolder[0];
                    }

                    @Override
                    public String[] getHeaderValues(final String name) {
                        final String headerValue = reqJmsMsgHeaders.get(name);
                        if (headerValue != null) {
                            return new String[]{headerValue};
                        } else {
                            return new String[0];
                        }
                    }

                    @Override
                    public String[] getHeaderNames() {
                        return reqJmsMsgHeaders.keySet().toArray(new String[reqJmsMsgHeaders.size()]);
                    }
                });

                // attach the HeadersKnob
                request.attachKnob(HeadersKnob.class, requestMessageHeadersKnob);

                final boolean replyExpected;

                try {
                    final Destination replyToDest = jmsRequest.getJMSReplyTo();
                    replyExpected = replyToDest != null || jmsRequest.getJMSCorrelationID() != null;
                } catch (JMSException e) {
                    throw new JmsRuntimeException(e);
                }

                final PolicyEnforcementContext context =
                        PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null, replyExpected);

                try {
                    auditContextFactory.doWithNewAuditContext( new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            String faultMessage = null;
                            String faultCode = null;
                            boolean stealthMode = false;
                            InputStream responseStream = null;

                            if (!messageTooLarge[0]) {
                                try {
                                    status[0] = messageProcessor.processMessageNoAudit(context);
                                    context.setPolicyResult(status[0]);
                                    _logger.finest("Policy resulted in status " + status[0]);
                                    if (context.getResponse().getKnob(XmlKnob.class) != null ||
                                            context.getResponse().getKnob(MimeKnob.class) != null) {
                                        // if the policy is not successful AND the stealth flag is on, drop connection
                                        if (status[0] != AssertionStatus.NONE && context.isStealthResponseMode()) {
                                            _logger.info("Policy returned error and stealth mode is set. " +
                                                    "Not sending response message.");
                                            stealthMode = true;
                                        } else {
                                            // add more detailed diagnosis message
                                            if (!context.getResponse().isXml()) {
                                                _logger.log(Level.INFO, "Response message is non-XML, the ContentType is: {0}", context.getRequest().getMimeKnob().getOuterContentType());
                                            }
                                            responseStream = context.getResponse().getMimeKnob().getFirstPart().getInputStream(false);
                                        }
                                    } else {
                                        _logger.finer("No response received");
                                        responseStream = null;
                                    }
                                } catch (PolicyVersionException pve) {
                                    String msg1 = "Request referred to an outdated version of policy";
                                    _logger.log(Level.INFO, msg1);
                                    faultMessage = msg1;
                                    faultCode = SoapUtil.FC_CLIENT;
                                } catch (Throwable t) {
                                    _logger.log(Level.WARNING, "Exception while processing JMS message: " + ExceptionUtils.getMessage(t),
                                            ExceptionUtils.getDebugException(t));
                                    faultMessage = t.getMessage();
                                    if (faultMessage == null) faultMessage = t.toString();
                                }
                            } else {
                                String msg1 = "Request message too large";
                                _logger.log(Level.INFO, msg1);
                                faultMessage = msg1;
                                faultCode = SoapUtil.FC_CLIENT;
                            }

                            if (responseStream == null) {
                                if (context.isStealthResponseMode()) {
                                    _logger.info("No response data available and stealth mode is set. " +
                                            "Not sending response message.");
                                    stealthMode = true;
                                } else {
                                    if (faultMessage == null) faultMessage = status[0].getMessage();
                                    try {
                                        String faultXml = SoapFaultUtils.generateSoapFaultXml(
                                                (context.getService() != null)
                                                        ? context.getService().getSoapVersion() : SoapVersion.UNKNOWN,
                                                faultCode == null ? SoapUtil.FC_SERVER : faultCode,
                                                faultMessage, null, "");

                                        responseStream = new ByteArrayInputStream(faultXml.getBytes(Charsets.UTF8));

                                        if (faultXml != null) {
                                            messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                                        }
                                    } catch (SAXException e) {
                                        throw new JmsRuntimeException(e);
                                    }
                                }
                            }

                            if (!stealthMode) {
                                final byte[] responseBytes;

                                try (PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream()) {
                                    IOUtils.copyStream(responseStream, baos);
                                    responseBytes = baos.toByteArray();
                                }

                                if (jmsResponse instanceof BytesMessage) {
                                    BytesMessage bresp = (BytesMessage) jmsResponse;
                                    bresp.writeBytes(responseBytes);
                                } else if (jmsResponse instanceof TextMessage) {
                                    TextMessage tresp = (TextMessage) jmsResponse;
                                    tresp.setText(new String(responseBytes, JmsUtil.DEFAULT_ENCODING));
                                } else {
                                    throw new JmsRuntimeException("Can't send a " + jmsResponse.getClass().getName() +
                                            ". Only BytesMessage and TextMessage are supported");
                                }

                                // Copy the JMS Property headers from the response HeadersKnob to the response JMS message.
                                // Propagation rules have already been enforced in the knob by the JMS routing assertion.

                                responseSuccess[0] = sendResponse(jmsRequest, jmsResponse, bag, endpointCfg, context);
                            } else { // is stealth mode
                                responseSuccess[0] = true;
                            }

                            return null;
                        }
                    }, new Functions.Nullary<AuditRecord>() {
                        @Override
                        public AuditRecord call() {
                            AssertionStatus s = status[0] == null ? AssertionStatus.UNDEFINED : status[0];
                            return messageSummaryAuditFactory.makeEvent( context, s );
                        }
                    });
                } catch (JMSException e) {
                    throw new JmsRuntimeException("Couldn't acknowledge message!", e);
                } catch (Exception e) {
                    throw new JmsRuntimeException(e);
                } finally {
                    ResourceUtils.closeQuietly(context);
                }
            } catch (IOException e) {
                throw new RuntimeException(e); // can't happen
            }
        } finally {
            if ( transacted ) {
                boolean handledAnyFailure;
                handledAnyFailure = status[0] == AssertionStatus.NONE
                        || bag.getMessageProducer() != null && sendFailureRequest(jmsRequest, bag.getMessageProducer());

                Session session = bag.getSession();
                if ( responseSuccess[0] && handledAnyFailure ) {
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
                } catch (JMSException e) {
                    _logger.log( Level.FINE, "Error closing response producer: " + JmsUtil.getJMSErrorMessage(e), ExceptionUtils.getDebugException(e) );
                }
            }
        }
    }

    private ContentTypeHeader getContentType(Message jmsRequest, Properties props)
            throws JmsRuntimeException, JMSException, IOException {
        ContentTypeHeader ctype;
        String requestCtype;

        if (jmsRequest instanceof TextMessage) return ContentTypeHeader.XML_DEFAULT;

        String source = props.getProperty(JmsConnection.PROP_CONTENT_TYPE_SOURCE);
        String val = props.getProperty(JmsConnection.PROP_CONTENT_TYPE_VAL);

        if ( (null == source) || "".equals(source) ) {
            _logger.warning ("no content type specified for this message, attempting to find one using Content-Type property");
            requestCtype = jmsRequest.getStringProperty("Content-Type");
            if (requestCtype != null) {
                _logger.info("found a content type of " + requestCtype);
                ctype = ContentTypeHeader.create(requestCtype);
            } else {
                _logger.info("Didn't find a content type. Using " + ContentTypeHeader.XML_DEFAULT.toString());
                ctype = ContentTypeHeader.XML_DEFAULT;
            }
        } else {
            if (JmsConnection.CONTENT_TYPE_SOURCE_HEADER.equals(source)) {
                requestCtype = jmsRequest.getStringProperty(val);
                // more informative diagnosis message
                if (requestCtype == null || requestCtype.isEmpty()) {
                    throw new JmsRuntimeException("Expected ContentType JMS property not set: " + val);
                }
            } else {
                requestCtype = val;
            }
            ctype = ContentTypeHeader.create(requestCtype);

            // log warning for unrecognized content type
            if (ctype != null && !ctype.matches("text", "xml") && !ctype.matches("text", "plain") && !ctype.matches("application", "*")) {
                _logger.log(Level.WARNING, "ContentType from JMS property not recognized, may cause policy to fail: {0}", ctype.toString());
            }
        }
        return ctype;
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

    private boolean sendResponse( final Message jmsRequestMsg,
                                  final Message jmsResponseMsg,
                                  final JmsBag bag,
                                  final JmsEndpointConfig endpointCfg,
                                  final PolicyEnforcementContext context) {
        boolean sent = false;
        try {
            final Context jndiContext = bag.getJndiContext();
            final Destination jmsReplyDest = endpointCfg.getResponseDestination(jmsRequestMsg, jndiContext);
            if ( jmsReplyDest == null ) {
                _logger.fine( "No response will be sent!" );
            } else {
                _logger.fine( "Sending response to " + jmsReplyDest );

                // bug #5415 - we will close the MessageProducer only after a transaction is committed
                final Session session = bag.getSession();
                responseProducer = getMessageProducer(jmsReplyDest, session);

                jmsResponseMsg.setJMSDeliveryMode(jmsRequestMsg.getJMSDeliveryMode());
                jmsResponseMsg.setJMSPriority(jmsRequestMsg.getJMSPriority());
                jmsResponseMsg.setJMSExpiration(jmsRequestMsg.getJMSExpiration());
                final String newCorrId = endpointCfg.getEndpoint().isUseMessageIdForCorrelation() ?
                        jmsRequestMsg.getJMSMessageID() :
                        jmsRequestMsg.getJMSCorrelationID();
                jmsResponseMsg.setJMSCorrelationID(newCorrId);

                final HeadersKnob headersKnob = context.getResponse().getKnob(HeadersKnob.class);

                if (headersKnob != null) {
                    //set JMS properties
                    Collection<Header> properties = headersKnob.getHeaders(HEADER_TYPE_JMS_PROPERTY);
                    for (Header property : properties) {
                        jmsResponseMsg.setObjectProperty(property.getKey(), property.getValue());
                    }
                    //set JMS headers
                    Collection<Header> headers = headersKnob.getHeaders(HEADER_TYPE_JMS_HEADER);
                    for(Header header : headers) {
                        if(JmsUtil.isJmsHeader(header.getKey())) { //JMS headers are set differently
                            //Set JMS Header defined in the context that might override the default value
                            Object value;

                            if((header.getKey().equals(JmsUtil.JMS_REPLY_TO) || header.getKey().equals(JmsUtil.JMS_DESTINATION)) && header.getValue() instanceof String) {
                                value = JmsUtil.cast( jndiContext.lookup((String)header.getValue()), Destination.class);
                            }
                            else {
                                value = header.getValue();
                            }
                            JmsUtil.setJmsHeader(jmsResponseMsg, new Pair<>(header.getKey(), value));
                        }
                        else {
                            _logger.log(Level.WARNING, "JMS Header \"" + header.getKey() + "\" is not supported.");
                        }
                    }
                }

                long timeToLive = jmsResponseMsg.getJMSExpiration() > 0 ? jmsResponseMsg.getJMSExpiration() - System.currentTimeMillis() : 0;
                if(timeToLive < 0) {
                    _logger.log(Level.WARNING, "Unable to send JMS message: JMS message expired");
                    throw new JmsMessageExpiredException();
                }

                responseProducer.send( jmsResponseMsg, jmsResponseMsg.getJMSDeliveryMode(), jmsResponseMsg.getJMSPriority(), timeToLive);
                _logger.fine( "Sent response to " + jmsReplyDest );
            }
            sent = true;
        } catch ( JMSException e ) {
            final JMSException logException = JmsUtil.isCausedByExpectedJMSException( e ) ? null : e;
            _logger.log( Level.WARNING, "Error sending response: " + JmsUtil.getJMSErrorMessage(e), logException );
        } catch (NamingException e ) {
            final NamingException logException = JmsUtil.isCausedByExpectedNamingException( e ) ? null : e;
            _logger.log(Level.WARNING, "Error trying to lookup the destination endpoint from preset reply-to queue name: " + JmsUtil.getJNDIErrorMessage( e ), logException );
        }
        return sent;
    }

    protected MessageProducer getMessageProducer(Destination jmsReplyDest, Session session) throws JMSException {
        return JmsUtil.createMessageProducer(session, jmsReplyDest);
    }

    private boolean sendFailureRequest( final Message message,
                                        final MessageProducer producer ) {
        boolean sent = false;

        try {
            producer.send(message);
            sent = true;
        } catch (JMSException e) {
            final Throwable logException = JmsUtil.isCausedByExpectedJMSException( e ) ? null : e;
            _logger.log( Level.WARNING, "Error sending request to failure destination: " + JmsUtil.getJMSErrorMessage(e), logException);
        }

        return sent;
    }
}
