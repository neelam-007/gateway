package com.l7tech.server.transport.jms2;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.message.JmsKnob;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.XmlKnob;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.cluster.ClusterMaster;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.transport.jms.BytesMessageInputStream;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.util.EventChannel;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.xml.sax.SAXException;

import javax.jms.*;
import javax.naming.NamingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


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

        AssertionStatus status = AssertionStatus.UNDEFINED;
        boolean responseSuccess = false;
        boolean messageTooLarge = false;
        Properties props = endpointCfg.getConnection().properties();
        try {
            if ( topicMasterOnly && !endpointCfg.isQueue() && !clusterMaster.isMaster() ) {
                status = AssertionStatus.NONE;
                responseSuccess = true;
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
                final Goid[] hardwiredserviceGoidHolder = new Goid[]{PublishedService.DEFAULT_GOID};
                try {
                    String tmp = props.getProperty(JmsConnection.PROP_IS_HARDWIRED_SERVICE);
                    if (tmp != null) {
                        if (Boolean.parseBoolean(tmp)) {
                            tmp = props.getProperty(JmsConnection.PROP_HARDWIRED_SERVICE_ID);
                            hardwiredserviceGoidHolder[0] = GoidUpgradeMapper.mapId(EntityType.SERVICE, tmp);
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
                        return hardwiredserviceGoidHolder[0];
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

                PolicyEnforcementContext context = null;
                String faultMessage = null;
                String faultCode = null;

                try {
                    final boolean replyExpected;
                    final Destination replyToDest = jmsRequest.getJMSReplyTo();
                    replyExpected = replyToDest != null || jmsRequest.getJMSCorrelationID() != null;

                    context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null, replyExpected);

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
                        } catch ( PolicyVersionException pve ) {
                            String msg1 = "Request referred to an outdated version of policy";
                            _logger.log( Level.INFO, msg1 );
                            faultMessage = msg1;
                            faultCode = SoapUtil.FC_CLIENT;
                        } catch ( Throwable t ) {
                            _logger.warning("Exception while processing JMS message: " + ExceptionUtils.getMessage(t));
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
                        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
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

                        responseSuccess = sendResponse( jmsRequest, jmsResponse, bag, endpointCfg );
                    } else { // is stealth mode
                        responseSuccess = true;
                    }
                } catch (IOException e) {
                    throw new JmsRuntimeException(e);
                } catch (JMSException e) {
                    throw new JmsRuntimeException("Couldn't acknowledge message!", e);
                } finally {
                    ResourceUtils.closeQuietly(context);
                }
            } catch (IOException e) {
                throw new RuntimeException(e); // can't happen
            }
        } finally {
            if ( transacted ) {
                boolean handledAnyFailure;
                handledAnyFailure = status == AssertionStatus.NONE || bag.getMessageProducer() != null && sendFailureRequest(jmsRequest, bag.getMessageProducer());

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
                } catch (JMSException e) {
                    _logger.log( Level.FINE, "Error closing response producer: " + JmsUtil.getJMSErrorMessage(e), ExceptionUtils.getDebugException(e) );
                }
            }
        }
    }

    private ContentTypeHeader getContentType(Message jmsRequest, Properties props)
            throws JmsRuntimeException, JMSException, IOException {
        ContentTypeHeader ctype;
        String requestCtype = null;

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
                                  final JmsEndpointConfig endpointCfg ) {
        boolean sent = false;
        try {
            final Destination jmsReplyDest = endpointCfg.getResponseDestination(jmsRequestMsg, bag.getJndiContext());
            if ( jmsReplyDest == null ) {
                _logger.fine( "No response will be sent!" );
            } else {
                _logger.fine( "Sending response to " + jmsReplyDest );

                // bug #5415 - we will close the MessageProducer only after a transaction is committed
                final Session session = bag.getSession();
                responseProducer = JmsUtil.createMessageProducer( session, jmsReplyDest );

                final String newCorrId = endpointCfg.getEndpoint().isUseMessageIdForCorrelation() ?
                        jmsRequestMsg.getJMSMessageID() :
                        jmsRequestMsg.getJMSCorrelationID();
                jmsResponseMsg.setJMSCorrelationID(newCorrId);
                responseProducer.send( jmsResponseMsg, jmsRequestMsg.getJMSDeliveryMode(), jmsRequestMsg.getJMSPriority(), jmsRequestMsg.getJMSExpiration() );
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
