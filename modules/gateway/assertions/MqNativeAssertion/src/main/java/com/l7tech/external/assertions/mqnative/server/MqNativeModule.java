package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.message.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.*;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.ActiveTransportModule;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_AUTOMATIC;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.message.Message.getMaxBytes;
import static com.l7tech.server.GatewayFeatureSets.SERVICE_MQNATIVE_MESSAGE_INPUT;
import static com.l7tech.util.CollectionUtils.caseInsensitiveSet;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;

/**
 * MQ native listener module (aka boot process).
 */
public class MqNativeModule extends ActiveTransportModule implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(MqNativeModule.class.getName());
    private static final int DEFAULT_MESSAGE_MAX_BYTES = 2621440;
    private static final Set<String> SUPPORTED_TYPES = caseInsensitiveSet( ACTIVE_CONNECTOR_TYPE_MQ_NATIVE );

    private final Map<Long, MqNativeListener> activeListeners = new ConcurrentHashMap<Long, MqNativeListener> ();
    private final ThreadPoolBean threadPoolBean;

    @Inject
    private GatewayState gatewayState;
    @Inject
    private SecurePasswordManager securePasswordManager;
    @Inject
    private MessageProcessor messageProcessor;
    @Inject
    private StashManagerFactory stashManagerFactory;
    @Inject
    private ApplicationEventPublisher messageProcessingEventChannel;
    @Inject
    private ServerConfig serverConfig;

    private MQPoolToken connectionPoolToken;

    /**
     * Single constructor for MQNative boot process.
     *
     * @param threadPoolBean listener thread pool
     */
    public MqNativeModule(@NotNull final ThreadPoolBean threadPoolBean) {
        super("MQ native active connector module", logger, SERVICE_MQNATIVE_MESSAGE_INPUT);
        this.threadPoolBean = threadPoolBean;
    }

    @Override
    protected boolean isInitialized() {
        return !threadPoolBean.isShutdown();
    }

    /**
     * Starts {@link MqNativeListener} using
     * configuration in {@link com.l7tech.gateway.common.transport.SsgActiveConnector}.
     */
    @Override
    protected void doStart() throws LifecycleException {
        super.doStart();
        if (gatewayState.isReadyForMessages()) {
            try {
                // excluded log message when polling an empty queue.  i.e. excluded "MQJE001: Completion Code 2, Reason 2033"
				MQException.logExclude(MQException.MQRC_NO_MSG_AVAILABLE);

                threadPoolBean.start();
                startInitialListeners();
            } catch (FindException e) {
                logger.log(Level.SEVERE, "Unable to access initial MQ native listener(s): " + getMessage( e ), getDebugException( e ));
            }
        }
    }

    /**
     * Starts all configured listeners.
     *
     * @throws com.l7tech.objectmodel.FindException when problems occur during subsystem startup
     */
    private void startInitialListeners() throws FindException {
        final boolean wasSystem = AuditContextUtils.isSystem();
        try {
            AuditContextUtils.setSystem(true);
            final Collection<SsgActiveConnector> connectors = ssgActiveConnectorManager.findSsgActiveConnectorsByType( ACTIVE_CONNECTOR_TYPE_MQ_NATIVE );
            for ( final SsgActiveConnector connector : connectors ) {
                if ( connector.isEnabled() && connectorIsOwnedByThisModule( connector ) && isValidConnectorConfig( connector ) ) {
                    try {
                        // initialize the default connection pool
                        if (connectionPoolToken == null) {
                            connectionPoolToken = new MQPoolToken();
                            MQEnvironment.addConnectionPoolToken(connectionPoolToken);
                        }
                        addConnector( connector.getReadOnlyCopy() );
                    } catch ( Exception e ) {
                        logger.log(Level.WARNING, "Unable to start MQ native active connector " + connector.getName() +
                                        ": " + getMessage( e ), e);
                    }
                }
            }
        } finally {
            AuditContextUtils.setSystem(wasSystem);
        }
    }

    /**
     * Attempts to stop all running MQ native listeners.
     */
    @Override
    protected void doStop() {
        for (final MqNativeListener listener : activeListeners.values()) {
            logger.info("Stopping MQ native receiver '" + listener.toString() + "'");
            listener.stop();
        }
        for (final MqNativeListener listener : activeListeners.values()) {
            logger.info("Waiting for MQ native receiver to stop '" + listener.toString() + "'");
            listener.ensureStopped();
        }

        activeListeners.clear();
        threadPoolBean.shutdown();

        if (connectionPoolToken != null) {
            MQEnvironment.removeConnectionPoolToken(connectionPoolToken);
        }
    }

    @Override
    protected boolean isValidConnectorConfig( @NotNull final SsgActiveConnector ssgActiveConnector ) {
        return ssgActiveConnector.getBooleanProperty( PROPERTIES_KEY_IS_INBOUND );
    }

    @Override
    protected void addConnector( @NotNull final SsgActiveConnector ssgActiveConnector ) throws ListenerException {
        MqNativeListener newListener = null;
        try {
            newListener = new MqNativeListener( ssgActiveConnector, getApplicationContext(), securePasswordManager ) {
                @Override
                void handleMessage( final MQMessage queueMessage ) throws MqNativeException {
                    try {
                        final Future<MqNativeException> result = threadPoolBean.submitTask( new Callable<MqNativeException>(){
                            @Override
                            public MqNativeException call() {
                                try {
                                    acknowledgeMessage( MqNativeUtils.isTransactional(ssgActiveConnector), mqNativeClient, queueMessage.messageId );
                                    handleMessageForConnector( ssgActiveConnector, mqNativeClient, queueMessage );
                                } catch ( MqNativeException e ) {
                                    return e;
                                } catch ( Exception e ) {
                                    return new MqNativeException(e);
                                }
                                return null;
                            }
                        } );
                        final MqNativeException exception = result.get();
                        if ( exception != null ) {
                            throw exception;
                        }
                    } catch ( InterruptedException e ) {
                        Thread.currentThread().interrupt();
                    } catch ( ThreadPool.ThreadPoolShutDownException e ) {
                        logger.log( Level.WARNING, "Error handling message, thread pool is shutdown.", getDebugException( e ) );
                    } catch ( ExecutionException e ) {
                        logger.log( Level.WARNING, "Error handling message: " + getMessage( e ), getDebugException( e ) );
                    }
                }
            };
            newListener.setErrorSleepTime(serverConfig.getProperty(MQ_CONNECT_ERROR_SLEEP_PROPERTY));
            newListener.start();
            activeListeners.put( ssgActiveConnector.getOid(), newListener );
        } catch (LifecycleException e) {
            logger.log( Level.WARNING,
                    "Exception while initializing MQ native listener " + newListener.getDisplayName() + ": " + getMessage( e ),
                    getDebugException( e ) );
        } catch ( MqNativeConfigException e ) {
            logger.log( Level.WARNING,
                    "Exception while initializing MQ native listener " + ssgActiveConnector.getName() + ": " + getMessage( e ),
                    getDebugException( e ) );
        }
    }

    @Override
    protected void removeConnector( long oid ) {
        final MqNativeListener listener = activeListeners.remove( oid );
        if  ( listener != null  ) {
            listener.stop();
        }
    }

    @Override
    protected Set<String> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    /**
     * Acknowledge current queue message based on given acknowledgement type.
     * @param isTransactional The acknowledgement type configured for this listener
     * @param mqNativeClient  The MQ native client to access the MQ server
     * @param messageId The request message id currently being processed
     * @throws MQException if an error occurs
     */
    public void acknowledgeMessage(boolean isTransactional,
                                   @NotNull final MqNativeClient mqNativeClient,
                                   @NotNull final byte[] messageId) throws MQException {
        MQMessage pop = new MQMessage();
        pop.messageId = messageId;
        mqNativeClient.getTargetQueue().get(pop, mqNativeClient.getAcknowledgeOptions(isTransactional));
    }

    /**
     * Handle an incoming message.  Also takes care of sending the reply if appropriate.
     *
     * @param connector The MQ native listener configuration that this handler operates on
     * @param mqNativeClient  The MQ native client to access the MQ server
     * @param requestMessage The request message to process
     * @throws MqNativeException if an error occurs
     */
    public void handleMessageForConnector( @NotNull final SsgActiveConnector connector,
                                           @NotNull final MqNativeClient mqNativeClient,
                                           @NotNull final MQMessage requestMessage) throws MqNativeException {
        final ContentTypeHeader ctype;
        final Pair<byte[], byte[]> parsedRequest;
        final byte[] mqHeader;
        boolean messageTooLarge = false;
        boolean responseSuccess = false;
        try {
            // get the content type
            ctype = ContentTypeHeader.parseValue( connector.getProperty( PROPERTIES_KEY_OVERRIDE_CONTENT_TYPE ) );

            // parse the request message
            parsedRequest = MqNativeUtils.parseHeader(requestMessage); // TODO (TL) need help changing; this is reading the whole message into memory
            mqHeader = parsedRequest.left;

            // enforce size restriction
            final int size = parsedRequest.right.length;
            final long sizeLimit = connector.getLongProperty(
                PROPERTIES_KEY_MQ_NATIVE_INBOUND_MQ_MESSAGE_MAX_BYTES,                                                    // prop value
                serverConfig.getLongProperty(ServerConfigParams.PARAM_IO_MQ_MESSAGE_MAX_BYTES, DEFAULT_MESSAGE_MAX_BYTES) // default value
            );

            if ( sizeLimit > 0 && size > sizeLimit ) {
                messageTooLarge = true;
            }
        } catch (IOException ioe) {
            throw new MqNativeException("Error processing request message.  " + getMessage( ioe ), ioe);
        }

        PolicyEnforcementContext context = null;
        String faultMessage = null;
        String faultCode = null;
        AssertionStatus status = AssertionStatus.UNDEFINED;
        try {
            // convert the payload into an input stream
            InputStream requestStream;
            if (connector.getBooleanProperty(MQ_PROPERTY_B64ENCODE_MSG)) {
                final String b64msg = HexUtils.encodeBase64(parsedRequest.right);
                requestStream = new ByteArrayInputStream( MessageFormat.format(XML_WRAPPER_TEMPLATE, b64msg, "true").getBytes() );

            } else if (connector.getBooleanProperty(MQ_PROPERTY_WRAP_MSG)) {
                requestStream = new ByteArrayInputStream( MessageFormat.format(XML_WRAPPER_TEMPLATE, new String(parsedRequest.right), "false").getBytes() );

            } else {
                requestStream = new ByteArrayInputStream(parsedRequest.right);
            }

            final long requestSizeLimit = connector.getLongProperty( PROPERTIES_KEY_REQUEST_SIZE_LIMIT, getMaxBytes() );
            Message request = new Message();
            request.initialize(stashManagerFactory.createStashManager(), ctype, requestStream, requestSizeLimit);

            // Gets the MQ message property to use as SOAPAction, if present.
            String soapActionValue = null;
            final String soapActionProp = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_INBOUND_SOAP_ACTION );
            if (connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_INBOUND_IS_SOAP_ACTION_USED ) && !StringUtils.isEmpty(soapActionProp)) {
                // get SOAP action from custom MQ property -- TBD
            }
            final String soapAction = soapActionValue;

            request.attachKnob(MqNativeKnob.class, MqNativeUtils.buildMqNativeKnob( soapAction, mqHeader ));

            final Long hardwiredServiceOid = connector.getHardwiredServiceOid();
            if ( hardwiredServiceOid != null ) {
                request.attachKnob(HasServiceOid.class, new HasServiceOidImpl(hardwiredServiceOid));
            }

            final boolean replyExpected = MqNativeReplyType.REPLY_NONE !=
                    connector.getEnumProperty( PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class );
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null, replyExpected);
            boolean stealthMode = false;
            InputStream responseStream = null;
            if ( !messageTooLarge ) {
                try {
                    status = messageProcessor.processMessage(context);

                    context.setPolicyResult(status);
                    logger.log(Level.FINEST, "Policy resulted in status " + status);

                    Message contextResponse = context.getResponse();
                    if (contextResponse.getKnob(XmlKnob.class) != null || contextResponse.getKnob(MimeKnob.class) != null) {
                        // if the policy is not successful AND the stealth flag is on, drop connection
                        if (status != AssertionStatus.NONE && context.isStealthResponseMode()) {
                            logger.log(Level.INFO, "Policy returned error and stealth mode is set.  Not sending response message.");
                            stealthMode = true;
                        } else {
                            // add more detailed diagnosis message
                            if (!contextResponse.isXml()) {
                                logger.log(Level.INFO, "Response message is non-XML, the ContentType is: {0}", context.getRequest().getMimeKnob().getOuterContentType());
                            }
                            responseStream = contextResponse.getMimeKnob().getEntireMessageBodyAsInputStream();
                        }
                    } else {
                        logger.log(Level.FINER, "No response received");
                        responseStream = null;
                    }
                } catch ( PolicyVersionException pve ) {
                    String msg1 = "Request referred to an outdated version of policy";
                    logger.log( Level.INFO, msg1 );
                    faultMessage = msg1;
                    faultCode = SoapUtil.FC_CLIENT;
                } catch ( Throwable t ) {
                    logger.warning("Exception while processing message via MQ native: " + getMessage( t ));
                    faultMessage = t.getMessage();
                    if ( faultMessage == null ) faultMessage = t.toString();
                }
            } else {
                String msg1 = "Message too large";
                logger.log( Level.INFO, msg1 );
                faultMessage = msg1;
                faultCode = SoapUtil.FC_CLIENT;
            }

            if ( responseStream == null ) {
                if (context.isStealthResponseMode()) {
                    logger.info("No response data available and stealth mode is set. " + "Not sending response message.");
                    stealthMode = true;
                } else {
                    if ( faultMessage == null ) {
                        faultMessage = status.getMessage();
                    }
                    try {
                        String faultXml = SoapFaultUtils.generateSoapFaultXml(
                                (context.getService() != null) ? context.getService().getSoapVersion() : SoapVersion.UNKNOWN,
                                faultCode == null ? SoapUtil.FC_SERVER : faultCode,
                                faultMessage, null, "");

                        responseStream = new ByteArrayInputStream(faultXml.getBytes( Charsets.UTF8));

                        if (faultXml != null) {
                            messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                        }
                    } catch (SAXException e) {
                        throw new MqNativeException(e);
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

                final MQMessage responseMessage = MqNativeUtils.buildMqMessage(connector);
                if (responseBytes != null && responseBytes.length > 0) {
                    // if a MQRFH2 header is needed, it should be done here
                    MqNativeKnob mqNativeKnob = context.getResponse().getKnob(MqNativeKnob.class);
                    if (mqNativeKnob != null && mqNativeKnob.getMessageHeaderLength() > 0) {
                        responseMessage.write(mqNativeKnob.getMessageHeaderBytes());
                    }
                    responseMessage.write(responseBytes);
                }

                long startResp = System.currentTimeMillis();
                responseSuccess = sendResponse( requestMessage, responseMessage, connector, mqNativeClient );
                logger.log(Level.INFO, "Send response took {0} milliseconds; listener {1}", new Object[] {(System.currentTimeMillis() - startResp), connector.getName()});
            } else { // is stealth mode
                responseSuccess = true;
            }
        } catch (IOException e) {
            throw new MqNativeException(e);
        } finally {
            ResourceUtils.closeQuietly(context);

            if ( MqNativeUtils.isTransactional(connector) ) {
                boolean handledAnyFailure = status == AssertionStatus.NONE || postMessageToFailureQueue(requestMessage, connector, mqNativeClient);

                if ( responseSuccess && handledAnyFailure ) {
                    try {
                        logger.log( Level.INFO, "Committing MQ transaction." );
                        mqNativeClient.getQueueManager().commit();
                    } catch (Exception e) {
                        logger.log( Level.WARNING, "Error performing MQ commit.", e );
                    }
                } else {
                    try {
                        logger.log( Level.INFO, "Back out MQ transaction." );
                        mqNativeClient.getQueueManager().backout();
                    } catch (Exception e) {
                        logger.log( Level.WARNING, "Error performing MQ back out.", e );
                    }
                }
            }
        }
    }

    private boolean sendResponse(MQMessage requestMessage, MQMessage responseMessage, SsgActiveConnector connector, MqNativeClient mqNativeClient ) {
        boolean sent = false;

        MqNativeReplyType replyType = connector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class);
        MQPutMessageOptions replyOptions = mqNativeClient.getReplyOptions(replyType, MqNativeUtils.isTransactional(connector));
        switch(replyType) {
            case REPLY_NONE:
                logger.info("No response will be sent!");
                break;
            case REPLY_AUTOMATIC:
                String replyToQueueName = requestMessage.replyToQueueName;
                if (replyToQueueName == null || StringUtils.isEmpty(replyToQueueName.trim())) {
                    logger.log(Level.WARNING, "Inbound listener configured with \"REPLY_AUTOMATIC\", but MQ request message does not contain a replyToQueueName");
                } else {
                    MQQueue replyToQueue = null;
                    try {
                        replyToQueue = mqNativeClient.getQueueManager().accessQueue(replyToQueueName, MQC.MQOO_OUTPUT);
                        logger.log(Level.FINER, "Sending response to {0} for request seqNum: {1}", new Object[] { replyToQueueName, requestMessage.messageSequenceNumber });
                        setResponseCorrelationId(connector, requestMessage, responseMessage);
                        replyToQueue.put( responseMessage, replyOptions );
                        logger.finer( "Sent response to " + replyToQueue );
                        sent = true;
                    } catch ( MQException e ) {
                        logger.log( Level.WARNING, "Caught MQ exception while sending response", ExceptionUtils.getDebugException(e) );
                    } finally {
                        if (replyToQueue != null) {
                            try {
                                replyToQueue.close();
                            } catch (MQException e) {
                                logger.log(Level.WARNING, "Error closing reply queue: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                            }
                        }
                    }
                }
                break;
            case REPLY_SPECIFIED_QUEUE:
                try {
                    MQQueue specifiedReplyQueue = mqNativeClient.getSpecifiedReplyQueue();
                    setResponseCorrelationId(connector, requestMessage, responseMessage);
                    specifiedReplyQueue.put( responseMessage, replyOptions );
                    logger.finer( "Sent response to " + specifiedReplyQueue );
                    sent = true;
                } catch ( MQException e ) {
                    logger.log( Level.WARNING, "Caught MQ exception while sending response", ExceptionUtils.getDebugException(e) );
                }
                break;
            default:
                logger.log( Level.WARNING, "Configuration exception while sending response.  Bad state - unknown MQ native replyType = " + replyType);
                break;
        }

        return sent;
    }

    private void setResponseCorrelationId(final SsgActiveConnector connector, final MQMessage requestMessage, final MQMessage responseMessage) {
        if (connector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_COPY_CORRELATION_ID_FROM_REQUEST)) {
            logger.info( "reply correlationId = request correlationId" );
            responseMessage.correlationId = requestMessage.correlationId;
        } else {
            logger.info( "reply correlationId = request messageId" );
            responseMessage.correlationId = requestMessage.messageId;
        }
    }

    private boolean postMessageToFailureQueue(final MQMessage requestMessage, SsgActiveConnector connector, MqNativeClient mqNativeClient) {
        boolean posted = false;
        String failedQueueName = connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_FAILED_QUEUE_NAME);
        if ( MqNativeUtils.isTransactional(connector) && !StringUtils.isEmpty(failedQueueName)) {
            MQQueue failedQueue = null;
            try {
                failedQueue = mqNativeClient.getQueueManager().accessQueue( failedQueueName, MQC.MQOO_OUTPUT );
                MQPutMessageOptions pmo = new MQPutMessageOptions();
                pmo.options = MQC.MQPMO_NONE;
                failedQueue.put(requestMessage, pmo);
                logger.log( Level.INFO, "Message sent to failure queue");
                posted = true;
            } catch (MQException mex) {
                logger.log( Level.WARNING, "Error sending message to failure queue", mex);
            } finally {
                if (failedQueue != null) {
                    try {
                        failedQueue.close();
                    } catch (MQException e) {
                        logger.log(Level.WARNING, "Error closing failure queue: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    }
                }
            }
        }
        return posted;
    }
}

