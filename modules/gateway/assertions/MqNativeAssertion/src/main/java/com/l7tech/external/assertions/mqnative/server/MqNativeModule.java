package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.*;
import com.ibm.mq.headers.MQDataException;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.external.assertions.mqnative.server.MqNativeClient.ClientBag;
import com.l7tech.external.assertions.mqnative.server.decorator.*;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.message.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
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
import com.l7tech.util.Functions.UnaryThrows;
import com.l7tech.xml.soap.SoapFaultUtils;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ibm.mq.constants.MQConstants.*;
import static com.l7tech.external.assertions.mqnative.MqNativeConstants.MQ_LISTENER_MAX_CONCURRENT_CONNECTIONS_PROPERTY;
import static com.l7tech.external.assertions.mqnative.MqNativeConstants.QUEUE_OPEN_OPTIONS_INBOUND_FAILURE_QUEUE;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_AUTOMATIC;
import static com.l7tech.external.assertions.mqnative.server.MqNativeUtils.*;
import static com.l7tech.external.assertions.mqnative.server.MqNativeUtils.buildMqNativeKnob;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.server.GatewayFeatureSets.SERVICE_MQNATIVE_MESSAGE_INPUT;
import static com.l7tech.util.CollectionUtils.caseInsensitiveSet;
import static com.l7tech.util.ConfigFactory.getBooleanProperty;
import static com.l7tech.util.ConfigFactory.getTimeUnitProperty;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;
import static com.l7tech.util.JdkLoggerConfigurator.debugState;

/**
 * MQ native listener module (aka boot process).
 */
public class MqNativeModule extends ActiveTransportModule implements ApplicationListener {
    static final int DEFAULT_MESSAGE_MAX_BYTES = 2621440;
    static final int DEFAULT_LISTENER_MAX_CONCURRENT_CONNECTIONS = 1000;

    private static final Logger logger = Logger.getLogger(MqNativeModule.class.getName());
    private static final Set<String> SUPPORTED_TYPES = caseInsensitiveSet( ACTIVE_CONNECTOR_TYPE_MQ_NATIVE );
    private static final String PROP_ENABLE_POOLING = "com.l7tech.external.assertions.mqnative.server.enablePooling";
    private static final String PROP_SOCKET_CONNECT_TIMEOUT = "com.l7tech.external.assertions.mqnative.server.socketConnectTimeout";
    private static final String PROP_ENABLE_MQ_LOGGING = "com.l7tech.external.assertions.mqnative.server.enableMqLogging";
    private static final String SYSPROP_MQ_SOCKET_CONNECT_TIMEOUT = "com.ibm.mq.tuning.socketConnectTimeout";

    private final Map<Goid, Set<MqNativeListener>> activeListeners = new ConcurrentHashMap<Goid, Set<MqNativeListener>> ();
    private final ThreadPoolBean threadPoolBean;

    static {
        final long connectTimeout = getTimeUnitProperty( PROP_SOCKET_CONNECT_TIMEOUT, 30000L);
        if ( SyspropUtil.getString( SYSPROP_MQ_SOCKET_CONNECT_TIMEOUT, null ) == null ) {
            logger.config( "Setting MQ socket timeout to " + connectTimeout + "ms" );
            SyspropUtil.setProperty( SYSPROP_MQ_SOCKET_CONNECT_TIMEOUT, String.valueOf(connectTimeout) );
        }

        if ( !getBooleanProperty( PROP_ENABLE_MQ_LOGGING, debugState() ) ) {
            MQException.log = null; // This is part of the public API ...
        } else {
            // excluded log message when polling an empty queue.  i.e. excluded "MQJE001: Completion Code 2, Reason 2033"
            MQException.logExclude(MQRC_NO_MSG_AVAILABLE);
        }
    }

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
        super("MQ native active connector module", Component.GW_MQ_NATIVE_RECV, logger, SERVICE_MQNATIVE_MESSAGE_INPUT);
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
        // initialize the default connection pool
        if ( connectionPoolToken == null && serverConfig.getBooleanProperty( PROP_ENABLE_POOLING, false ) ) {
            connectionPoolToken = new MQPoolToken();
            MQEnvironment.addConnectionPoolToken(connectionPoolToken);
        }

        super.doStart();
        if (gatewayState.isReadyForMessages()) {
            try {
                threadPoolBean.start();
                startInitialListeners();
            } catch (FindException e) {
                auditError(SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE, "Unable to access initial MQ native listener(s): " + getMessage( e ), e);
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
                        addConnector( connector.getReadOnlyCopy() );
                    } catch ( Exception e ) {
                        auditError(connector.getType(), "Unable to start MQ native active connector " + connector.getName() + ": " + getMessage( e ), e);
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
        for (final Set<MqNativeListener> listenerSet : activeListeners.values()) {
            for (final MqNativeListener listener : listenerSet) {
                logger.info("Stopping MQ native receiver '" + listener.toString() + "'");
                listener.stop();
            }
        }
        for (final Set<MqNativeListener> listenerSet : activeListeners.values()) {
            for (final MqNativeListener listener : listenerSet) {
                logger.info("Waiting for MQ native receiver to stop '" + listener.toString() + "'");
                listener.ensureStopped();
            }
            listenerSet.clear();
        }

        activeListeners.clear();
        threadPoolBean.shutdown();

        if (connectionPoolToken != null) {
            MQEnvironment.removeConnectionPoolToken(connectionPoolToken);
            connectionPoolToken = null;
        }
    }

    @Override
    protected boolean isValidConnectorConfig( @NotNull final SsgActiveConnector ssgActiveConnector ) {
        return ssgActiveConnector.getBooleanProperty( PROPERTIES_KEY_IS_INBOUND );
    }

    @Override
    protected void addConnector( @NotNull final SsgActiveConnector ssgActiveConnector ) throws ListenerException {
        int numberOfListenersToCreate =  ssgActiveConnector.getIntegerProperty(PROPERTIES_KEY_NUMBER_OF_SAC_TO_CREATE, 1);
        int maxListenersAllowed = serverConfig.getIntProperty(MQ_LISTENER_MAX_CONCURRENT_CONNECTIONS_PROPERTY, DEFAULT_LISTENER_MAX_CONCURRENT_CONNECTIONS);
        if (numberOfListenersToCreate > maxListenersAllowed) {
            logger.log(Level.INFO, "Overriding connection concurrency configured for " + ssgActiveConnector.getName() + " to: " + maxListenersAllowed +
                    ", configured: " + numberOfListenersToCreate + ", maximum allowed: " + maxListenersAllowed  +  ".");
            numberOfListenersToCreate = maxListenersAllowed;
        }

        Set<MqNativeListener> listenerSet = new HashSet<MqNativeListener>( numberOfListenersToCreate );
        activeListeners.put( ssgActiveConnector.getGoid(), listenerSet );
        for ( int i = 1; i <= numberOfListenersToCreate; i++ ) {
            MqNativeListener newListener = null;
            try {
                final int concurrentId = numberOfListenersToCreate > 1 ? i : 0;
                newListener = newMqNativeListener(ssgActiveConnector, concurrentId);
                newListener.start();
                listenerSet.add( newListener );
            } catch ( LifecycleException e ) {
                auditError(ssgActiveConnector.getType(), "Exception while initializing MQ native listener " + newListener.getDisplayName() + ": " + getMessage( e ), getDebugException( e ));
            } catch ( MqNativeConfigException e ) {
                auditError(ssgActiveConnector.getType(), "Exception while initializing MQ native listener " + ssgActiveConnector.getName() + ": " + getMessage( e ), getDebugException( e ));
            }
        }
    }

    private MqNativeListener newMqNativeListener(final SsgActiveConnector ssgActiveConnector, final Integer concurrentId) throws MqNativeConfigException {
        return new MqNativeListener( ssgActiveConnector, concurrentId, getApplicationContext(), securePasswordManager, serverConfig ) {
            @Override
            void handleMessage( final MQMessage queueMessage ) throws MqNativeException {
                try {
                    final Future<MqNativeException> result = threadPoolBean.submitTask( new Callable<MqNativeException>(){
                        @Override
                        public MqNativeException call() {
                            try {
                                if ( !isTransactional( ssgActiveConnector ) ) {
                                    // commit now - on-take / AUTOMATIC
                                    commitWork( mqNativeClient );
                                }
                                handleMessageForConnector( ssgActiveConnector, mqNativeClient, queueMessage );
                            } catch ( MqNativeException e ) {
                                return e;
                            } catch ( Exception e ) {
                                return new MqNativeException( e );
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
                    auditError("Error handling message, thread pool is shutdown.", getDebugException( e ));
                } catch ( ExecutionException e ) {
                    auditError("Error handling message: "+ getMessage( e ), getDebugException( e ));
                } catch ( RejectedExecutionException e ) {
                    try {
                        rollbackWork( mqNativeClient );
                    } catch ( MQException e1 ) {
                        throw new MqNativeException( "Error rolling back work for rejected execution", e1 );
                    } catch ( MqNativeConfigException e1 ) {
                        throw new MqNativeException( "Error rolling back work for rejected execution", e1 );
                    }
                    throw e;
                }
            }

            @Override
            final void auditError( final String message, @Nullable final Throwable exception ) {
                MqNativeModule.this.auditError( ssgActiveConnector.getType(), message, exception);
            }
        };
    }

    @Override
    protected void removeConnector( Goid goid ) {
        Set<MqNativeListener> listenerSet = activeListeners.remove(goid);
        if  (listenerSet != null) {
            for (MqNativeListener listener : listenerSet) {
                listener.stop();
            }
        }
    }

    @Override
    protected Set<String> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    /**
     * Commit outstanding work for client.
     *
     * @param mqNativeClient  The MQ native client to access the MQ server
     * @throws MQException if an error occurs
     * @throws MqNativeConfigException if a config error occurs
     */
    private void commitWork( @NotNull final MqNativeClient mqNativeClient ) throws MQException, MqNativeConfigException {
        mqNativeClient.doWork( new UnaryThrows<Void,ClientBag,MQException>() {
            @Override
            public Void call( final ClientBag bag ) throws MQException {
                bag.getQueueManager().commit();
                return null;
            }
        }, false );
    }

    /**
     * Commit outstanding work for client.
     *
     * @param mqNativeClient  The MQ native client to access the MQ server
     * @throws MQException if an error occurs
     * @throws MqNativeConfigException if a config error occurs
     */
    private void rollbackWork( @NotNull final MqNativeClient mqNativeClient ) throws MQException, MqNativeConfigException {
        mqNativeClient.doWork( new UnaryThrows<Void, ClientBag, MQException>() {
            @Override
            public Void call( final ClientBag bag ) throws MQException {
                bag.getQueueManager().backout();
                return null;
            }
        }, false );
    }

    /**
     * Handle an incoming message.  Also takes care of sending the reply if appropriate.
     *
     * @param connector The MQ native listener configuration that this handler operates on
     * @param mqNativeClient  The MQ native client to access the MQ server
     * @param mqRequestMessage The request message to process
     * @throws MqNativeException if an error occurs
     */
    public void handleMessageForConnector( @NotNull final SsgActiveConnector connector,
                                           @NotNull final MqNativeClient mqNativeClient,
                                           @NotNull final MQMessage mqRequestMessage) throws MqNativeException, MqNativeConfigException {
        final ContentTypeHeader ctype;
        final Pair<byte[], byte[]> mqRequestHeaderPayload;
        final long requestSizeLimit;
        boolean messageTooLarge = false;
        boolean responseSuccess = false;
        try {
            // get the content type
            String contentTypeValue = connector.getProperty(PROPERTIES_KEY_OVERRIDE_CONTENT_TYPE);
            // If the content type is not specified, it will be set as the default type, "text/xml".
            if (contentTypeValue == null || contentTypeValue.trim().length() == 0) {
                contentTypeValue = ContentTypeHeader.XML_DEFAULT.getFullValue();
            }
            ctype = ContentTypeHeader.parseValue(contentTypeValue);

            // parse the headers and payload from request mq message
            mqRequestHeaderPayload = parseHeaderPayload(mqRequestMessage); // message payload in memory

            // enforce size restriction
            final int size = mqRequestHeaderPayload.right.length;
            requestSizeLimit = connector.getLongProperty(
                PROPERTIES_KEY_REQUEST_SIZE_LIMIT, // prop value
                serverConfig.getLongProperty(ServerConfigParams.PARAM_IO_MQ_MESSAGE_MAX_BYTES, DEFAULT_MESSAGE_MAX_BYTES) // default value
            );

            if ( requestSizeLimit > 0 && size > requestSizeLimit ) {
                messageTooLarge = true;
            }
        } catch (IOException e) {
            throw new MqNativeException("Error processing request message.  " + getMessage( e ), e);
        } catch (MQDataException e) {
            throw new MqNativeException("Error processing request message.  " + getMessage( e ), e);
        }

        PolicyEnforcementContext context = null;
        String faultMessage = null;
        String faultCode = null;
        AssertionStatus status = AssertionStatus.UNDEFINED;
        try {
            // convert the payload into an input stream
            final InputStream requestStream = new ByteArrayInputStream(mqRequestHeaderPayload.right);

            Message gatewayRequestMessage = new Message();
            gatewayRequestMessage.initialize(stashManagerFactory.createStashManager(), ctype, requestStream, requestSizeLimit);

            // Gets the MQ message property to use as SOAPAction, if present.
            final String soapActionValue = null;
            final String soapActionProp = connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_INBOUND_SOAP_ACTION );
            if (connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_INBOUND_IS_SOAP_ACTION_USED ) && !StringUtils.isEmpty(soapActionProp)) {
                // get SOAP action from custom MQ property -- TBD
            }

            MqMessageProxy mqMessage = new MqMessageProxy(mqRequestMessage);
            gatewayRequestMessage.attachKnob(buildMqNativeKnob(soapActionValue, mqMessage), true, MqNativeKnob.class, OutboundHeadersKnob.class);

            final Goid hardwiredServiceGoid = connector.getHardwiredServiceGoid();
            if ( hardwiredServiceGoid != null ) {
                gatewayRequestMessage.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
            }

            final boolean replyExpected = MqNativeReplyType.REPLY_NONE !=
                    connector.getEnumProperty( PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class );


            Message gatewayResponseMessage = new Message();
            gatewayResponseMessage.attachKnob(buildMqNativeKnob(new MqMessageProxy(new MQMessage())), true, MqNativeKnob.class, OutboundHeadersKnob.class ); // TODO jwilliams: see here for preservable knob attachment
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(gatewayRequestMessage, gatewayResponseMessage, replyExpected);

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
                            responseStream = contextResponse.getMimeKnob().getFirstPart().getInputStream(false);
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
                final byte[] responsePayload;
                try {
                    IOUtils.copyStream(responseStream, baos);
                    responsePayload = baos.toByteArray();
                } finally {
                    baos.close();
                }

                MqNativeKnob mqNativeKnob = gatewayResponseMessage.getKnob(MqNativeKnob.class);
                MQMessage mqResponseMessage = new MQMessage();
                mqResponseMessage = new PassThroughDecorator(mqResponseMessage, (MqMessageProxy) mqNativeKnob.getMessage(),
                        gatewayResponseMessage.getKnob(OutboundHeadersKnob.class), null, context, getAudit());
                mqResponseMessage = new DescriptorDecorator((MqMessageDecorator)mqResponseMessage);
                mqResponseMessage = new PropertyDecorator((MqMessageDecorator)mqResponseMessage);
                mqResponseMessage = new HeaderDecorator((MqMessageDecorator)mqResponseMessage);
                mqResponseMessage = ((MqMessageDecorator) mqResponseMessage).decorate();

                if (responsePayload != null && responsePayload.length > 0) {
                    mqResponseMessage.write(responsePayload);
                }

                long startResp = System.currentTimeMillis();
                responseSuccess = sendResponse( mqRequestMessage, mqResponseMessage, connector, mqNativeClient );
                logger.log(Level.INFO, "Send response took {0} milliseconds; listener {1}",
                        new Object[] {(System.currentTimeMillis() - startResp), connector.getName()});
            } else { // is stealth mode
                responseSuccess = true;
            }
        } catch (IOException e) {
            throw new MqNativeException(e);
        } catch(MQDataException e) {
            throw new MqNativeException(e);
        } catch(MQException e) {
            throw new MqNativeException(e);
        } finally {
            ResourceUtils.closeQuietly(context);

            if ( isTransactional( connector ) ) {
                boolean handledAnyFailure = status == AssertionStatus.NONE || postMessageToFailureQueue(mqRequestMessage, connector, mqNativeClient);

                if ( responseSuccess && handledAnyFailure ) {
                    try {
                        logger.log( Level.FINE, "Committing MQ transaction." );
                        commitWork( mqNativeClient );
                    } catch (Exception e) {
                        logger.log( Level.WARNING, "Error performing MQ commit.", e );
                    }
                } else {
                    try {
                        logger.log( Level.INFO, "Back out MQ transaction." );
                        rollbackWork( mqNativeClient );
                    } catch (Exception e) {
                        logger.log( Level.WARNING, "Error performing MQ back out.", e );
                    }
                }
            }
        }
    }

    boolean sendResponse( final MQMessage requestMessage,
                                  final MQMessage responseMessage,
                                  final SsgActiveConnector connector,
                                  final MqNativeClient mqNativeClient ) {
        boolean success = false;

        final boolean transactional = isTransactional(connector);
        final boolean allowReconnect = !transactional; // allow reconnect for reply if not transactional
        final MqNativeReplyType replyType = connector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class);
        final MQPutMessageOptions replyOptions = new MQPutMessageOptions();
        replyOptions.options = MQPMO_NEW_MSG_ID |
                ( transactional ? MQPMO_SYNCPOINT : MQPMO_NO_SYNCPOINT );
        if (isOpenForSetAllContext()) {
            replyOptions.options |= MQPMO_SET_ALL_CONTEXT;
        }
        switch(replyType) {
            case REPLY_NONE:
                logger.fine( "No response will be sent!" );
                success = true;
                break;
            case REPLY_AUTOMATIC:
                final String replyToQueueName = requestMessage.replyToQueueName;
                if (replyToQueueName == null || StringUtils.isEmpty(replyToQueueName.trim())) {
                    logger.log(Level.WARNING, "Inbound listener configured with \"REPLY_AUTOMATIC\", but MQ request message does not contain a replyToQueueName");
                } else {
                    try {
                        mqNativeClient.doWork( new UnaryThrows<Void, ClientBag, MQException>() {
                            @Override
                            public Void call( final ClientBag clientBag ) throws MQException {
                                MQQueue replyToQueue = null;
                                try {
                                    replyToQueue = clientBag.getQueueManager().accessQueue(replyToQueueName, getTempOutboundPutMessageOption());
                                    logger.log(Level.FINER, "Sending response to {0} for request seqNum: {1}", new Object[] { replyToQueueName, requestMessage.messageSequenceNumber });
                                    setResponseCorrelationId(connector, requestMessage, responseMessage);
                                    replyToQueue.put( responseMessage, replyOptions );
                                    logger.finer( "Sent response to " + replyToQueue );
                                } finally {
                                    MqNativeUtils.closeQuietly( replyToQueue );
                                }
                                return null;
                            }
                        }, allowReconnect );
                    } catch ( MQException e ) {
                        logger.log( Level.WARNING, "Error sending MQ response: " + getMessage(e), ExceptionUtils.getDebugException(e) );
                    } catch ( MqNativeConfigException e ) {
                        logger.log( Level.WARNING, "Error sending MQ response: " + getMessage(e), ExceptionUtils.getDebugException(e) );
                    }
                }
                success = true;
                break;
            case REPLY_SPECIFIED_QUEUE:
                success = true;
                try {
                    mqNativeClient.doWork( new UnaryThrows<Void, ClientBag, MQException>() {
                        @Override
                        public Void call( final ClientBag clientBag ) throws MQException {
                            final MQQueue specifiedReplyQueue = clientBag.getSpecifiedReplyQueue();
                            if ( specifiedReplyQueue != null ) {
                                setResponseCorrelationId( connector, requestMessage, responseMessage );
                                specifiedReplyQueue.put( responseMessage, replyOptions );
                                logger.finer( "Sent response to " + specifiedReplyQueue );
                            } else {
                                logger.log( Level.WARNING, "Error sending MQ response: specified queue not available" );
                            }
                            return null;
                        }
                    }, allowReconnect );
                } catch ( MQException e ) {
                    success = false;
                    logger.log( Level.WARNING, "Error sending MQ response: " + getMessage(e), ExceptionUtils.getDebugException(e) );
                } catch ( MqNativeConfigException e ) {
                    success = false;
                    logger.log( Level.WARNING, "Error sending MQ response: " + getMessage(e), ExceptionUtils.getDebugException(e) );
                }
                break;
            default:
                logger.log( Level.WARNING, "Configuration exception while sending response.  Bad state - unknown MQ native replyType = " + replyType);
                break;
        }

        return success;
    }

    private void setResponseCorrelationId(final SsgActiveConnector connector, final MQMessage requestMessage, final MQMessage responseMessage) {
        if (connector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_COPY_CORRELATION_ID_FROM_REQUEST)) {
            logger.finer( "reply correlationId = request correlationId" );
            responseMessage.correlationId = requestMessage.correlationId;
        } else {
            logger.finer( "reply correlationId = request messageId" );
            responseMessage.correlationId = requestMessage.messageId;
        }
    }

    private boolean postMessageToFailureQueue( final MQMessage requestMessage,
                                               final SsgActiveConnector connector,
                                               final MqNativeClient mqNativeClient ) {
        boolean posted = false;
        final String failedQueueName = connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_FAILED_QUEUE_NAME);
        if ( connector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_IS_FAILED_QUEUE_USED) && !StringUtils.isEmpty(failedQueueName) ) {
            try {
                mqNativeClient.doWork( new UnaryThrows<Void, ClientBag, MQException>() {
                    @Override
                    public Void call( final ClientBag clientBag ) throws MQException {
                        MQQueue failedQueue = null;
                        try {
                            failedQueue = clientBag.getQueueManager().accessQueue( failedQueueName, QUEUE_OPEN_OPTIONS_INBOUND_FAILURE_QUEUE );
                            final MQPutMessageOptions pmo = new MQPutMessageOptions();
                            pmo.options = MQPMO_SYNCPOINT;
                            failedQueue.put(requestMessage, pmo);
                            logger.log( Level.FINE, "Message sent to failure queue");
                        } finally {
                            closeQuietly( failedQueue );
                        }
                        return null;
                    }
                }, false );
                posted = true;
            } catch (MQException e) {
                logger.log( Level.WARNING, "Error sending message to failure queue: " + getMessage(e), getDebugException(e));
            } catch ( MqNativeConfigException e ) {
                logger.log( Level.WARNING, "Error sending message to failure queue: " + getMessage(e), getDebugException(e));
            }
        }
        return posted;
    }

    void setMessageProcessor(final MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    void setServerConfig(final ServerConfig serverConfig){
        this.serverConfig = serverConfig;
    }

    void setStashManagerFactory(final StashManagerFactory stashManagerFactory){
        this.stashManagerFactory = stashManagerFactory;
    }

    void setMessageProcessingEventChannel(final ApplicationEventPublisher messageProcessingEventChannel){
        this.messageProcessingEventChannel = messageProcessingEventChannel;
    }

    void setSecurePasswordManager(final SecurePasswordManager securePasswordManager) {
        this.securePasswordManager = securePasswordManager;
    }

    Map<Goid, Set<MqNativeListener>> getActiveListeners() {
        return activeListeners;
    }
}