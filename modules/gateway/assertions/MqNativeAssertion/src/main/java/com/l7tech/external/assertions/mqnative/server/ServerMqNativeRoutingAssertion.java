package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.mqnative.MqNativeDynamicProperties;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.external.assertions.mqnative.MqNativeRoutingAssertion;
import com.l7tech.external.assertions.mqnative.server.MqNativeResourceManager.MqTaskCallback;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.message.Message;
import com.l7tech.message.MessageRole;
import com.l7tech.message.MimeKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.SsgActiveConnectorManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.*;
import com.l7tech.util.Functions.Unary;
import com.l7tech.util.Functions.UnaryThrows;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.ibm.mq.constants.MQConstants.*;
import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_AUTOMATIC;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_SPECIFIED_QUEUE;
import static com.l7tech.external.assertions.mqnative.server.MqNativeUtils.*;
import static com.l7tech.gateway.common.audit.AssertionMessages.*;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.ACTIVE_CONNECTOR_TYPE_MQ_NATIVE;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.PROPERTIES_KEY_IS_INBOUND;
import static com.l7tech.message.Message.getMaxBytes;
import static com.l7tech.objectmodel.EntityUtil.name;
import static com.l7tech.server.ServerConfigParams.PARAM_IO_MQ_MESSAGE_MAX_BYTES;
import static com.l7tech.util.ArrayUtils.contains;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Option.some;
import static java.util.Collections.unmodifiableMap;

/**
 * Server side implementation of the MqNativeRoutingAssertion.
 *
 * @author vchan - tactical code on which this implementation is based.
 */
public class ServerMqNativeRoutingAssertion extends ServerRoutingAssertion<MqNativeRoutingAssertion> {
    private static final String PROP_RETRY_DELAY = "com.l7tech.external.assertions.mqnative.server.routingRetryDelay";
    private static final String PROP_MAX_OOPS = "com.l7tech.external.assertions.mqnative.server.routingMaxRetries";
    private static final int MAX_OOPSES = 5;
    private static final long RETRY_DELAY = 1000L;
    private static final long DEFAULT_MESSAGE_MAX_BYTES = 2621440L;
    private static final String completionCodeString = "mq.completion.code";
    private static final String reasonCodeString = "mq.reason.code";

    @Inject
    private Config config;
    @Inject
    private StashManagerFactory stashManagerFactory;
    @Inject
    private SecurePasswordManager securePasswordManager;
    @Inject
    private SsgActiveConnectorManager ssgActiveConnectorManager;
    private ApplicationEventProxy applicationEventProxy;
    private MqNativeResourceManager mqNativeResourceManager;
    private MqNativeSsgActiveConnectorInvalidator invalidator = new MqNativeSsgActiveConnectorInvalidator(this);
    private AtomicInteger connectionAttempts = new AtomicInteger(0);
    private final AtomicBoolean needsUpdate = new AtomicBoolean(false);
    private MqNativeEndpointConfig endpointConfig;
    private final Object endpointConfigSync = new Object();

    public ServerMqNativeRoutingAssertion( final MqNativeRoutingAssertion data,
                                           final ApplicationContext applicationContext ) {
        super(data, applicationContext);
    }

    @Inject
    protected final void setApplicationEventProxy( final ApplicationEventProxy applicationEventProxy ) {
        this.applicationEventProxy = applicationEventProxy;
        mqNativeResourceManager = MqNativeResourceManager.getInstance( config, applicationEventProxy );
        applicationEventProxy.addApplicationListener( invalidator );
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException {

        context.setRoutingStatus(RoutingStatus.ATTEMPTED);

        final com.l7tech.message.Message targetMessage;
        final TargetMessageType targetMessageType;
        try {
            targetMessage = context.getTargetMessage(assertion.getRequestTarget());
            targetMessageType = assertion.getRequestTarget().getTarget();
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);

        }
        if ( assertion.isPutToQueue() && !isValidRequest(targetMessage, targetMessageType) ) {
            logAndAudit(AssertionMessages.MQ_ROUTING_REQUEST_TOO_LARGE);
            return AssertionStatus.BAD_REQUEST;
        }

        MqNativeEndpointConfig cfg = null;
        boolean iSetAConnectionAttempt = false;

        try {
            // DELETE CURRENT SECURITY HEADER IF NECESSARY
            handleProcessedSecurityHeader(targetMessage);

            if (markedForUpdate()) {
                try {
                    logger.info("MQ information needs update, closing session (if open).");

                    resetEndpointInfo();
                } finally {
                    markUpdate(false);
                }
            }

            final long retryDelay = config.getTimeUnitProperty( PROP_RETRY_DELAY, RETRY_DELAY );
            final int maxOopses = config.getIntProperty( PROP_MAX_OOPS, MAX_OOPSES );
            int oopses = 0;

            final Option<MqNativeDynamicProperties> preProcessingMqDynamicProperties = optional( assertion.getDynamicMqRoutingProperties() );
            final Option<MqNativeDynamicProperties> mqDynamicProperties = expandMqDynamicPropertiesVariables( context, preProcessingMqDynamicProperties );
            cfg = getEndpointConfig( mqDynamicProperties );
            // Message send retry loop.
            // Once the message is sent, there are no more retries
            while ( true ) {
                final MqRoutingCallback mqrc = new MqRoutingCallback(context, cfg);
                try {
                    mqNativeResourceManager.doWithMqResources( cfg, mqrc );
                    mqrc.doException();
                    break; // no error
                } catch (MqNativeRuntimeException mqre) {
                    if ( mqrc.isMessageSentOrReceived() ) {
                        throw mqre.getCause() != null ? mqre.getCause() : mqre;
                    }

                    if(oopses==0){
                        int attempts = connectionAttempts.incrementAndGet();
                        iSetAConnectionAttempt = true ;
                        if(attempts>=10){
                            //10 failed attempts made to make a connection, fail!
                            if ( mqre.getCause() instanceof MQException ) {
                                logger.log(Level.WARNING, "At least 10 connections failed trying to connect to MQ.  MQ server is not available.  Falsifying assertion and returning completion code 2059.", getDebugExceptionForExpectedReasonCode((MQException)mqre.getCause()));
                            }  else {
                                logger.log(Level.WARNING, "At least 10 connections failed trying to connect to MQ.  MQ server is not available.  Falsifying assertion and returning completion code 2059.", mqre);
                            }
                            context.setVariable(ServerMqNativeRoutingAssertion.completionCodeString, MQCC_FAILED);
                            context.setVariable(ServerMqNativeRoutingAssertion.reasonCodeString, MQRC_Q_MGR_NOT_AVAILABLE);
                            return AssertionStatus.FALSIFIED;
                        }
                    }

                    if (++oopses < maxOopses) {
                        logAndAudit(MQ_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(retryDelay)}, getDebugException( mqre ));
                        mqNativeResourceManager.invalidate(cfg);
                        sleep( retryDelay );
                    } else {
                        logAndAudit(MQ_ROUTING_CANT_CONNECT_NOMORETRIES, String.valueOf(maxOopses));
                        // Catcher will log/audit the stack trace
                        throw mqre.getCause() != null ? mqre.getCause() : mqre;
                    }
                } catch (MQException e) {
                    if ( mqrc.isMessageSentOrReceived() ) throw e;

                    if(oopses==0){
                        int attempts = connectionAttempts.incrementAndGet();
                        iSetAConnectionAttempt = true;
                        if(attempts>=10){
                            //10 failed attempts made to make a connection, fail!
                            logger.log(Level.WARNING, "At least 10 connections failed trying to connect to MQ.  MQ server is not available.  Falsifying assertion.", getDebugExceptionForExpectedReasonCode(e));
                            context.setVariable(ServerMqNativeRoutingAssertion.completionCodeString, e.completionCode);
                            context.setVariable(ServerMqNativeRoutingAssertion.reasonCodeString, e.reasonCode);
                            return AssertionStatus.FALSIFIED;
                        }
                    }
                    if (++oopses < maxOopses) {
                        logAndAudit(MQ_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(retryDelay)}, getDebugExceptionForExpectedReasonCode(e) );
                        mqNativeResourceManager.invalidate( cfg );
                        sleep( retryDelay );
                    } else {
                        logAndAudit(MQ_ROUTING_CANT_CONNECT_NOMORETRIES, String.valueOf(maxOopses));
                        //Create context variables for MQ exception completion and response codes here.
                        context.setVariable(ServerMqNativeRoutingAssertion.completionCodeString, e.completionCode);
                        context.setVariable(ServerMqNativeRoutingAssertion.reasonCodeString, e.reasonCode);
                        // Catcher will log/audit the stack trace
                        throw e;
                    }
                }
            }

            context.setVariable(ServerMqNativeRoutingAssertion.completionCodeString, 0);
            context.setVariable(ServerMqNativeRoutingAssertion.reasonCodeString, 0);
            return AssertionStatus.NONE;

        } catch ( MqNativeConfigException e ) {
            logAndAudit(MQ_ROUTING_CONFIGURATION_ERROR,
                    new String[]{ getMessage( e )},
                    getDebugException( e ));
            return AssertionStatus.FAILED;
        } catch ( AssertionStatusException e ) {
            throw e;
        } catch ( MqRetriesInterruptedException e ) {
            logAndAudit( EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "Interrupted when retrying connection" }, getDebugException( e ) );
            if (cfg!=null) mqNativeResourceManager.invalidate(cfg);
            return AssertionStatus.FAILED;
        } catch (MQException e ) {
            context.setVariable(ServerMqNativeRoutingAssertion.completionCodeString, e.completionCode);
            context.setVariable(ServerMqNativeRoutingAssertion.reasonCodeString, e.reasonCode);
            logAndAudit(EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{"Caught unexpected Throwable in outbound MQ request processing: " + getMessage( e )},
                    getDebugExceptionForExpectedReasonCode( e ) );
            if (cfg!=null) mqNativeResourceManager.invalidate(cfg);
            return AssertionStatus.SERVER_ERROR;
        } catch ( Throwable t ) {
            logAndAudit(EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{"Caught unexpected Throwable in outbound MQ request processing: " + getMessage( t )}, t);
            if (cfg!=null) mqNativeResourceManager.invalidate(cfg);
            return AssertionStatus.SERVER_ERROR;
        } finally {
            if (context.getRoutingEndTime() == 0) context.routingFinished();
            if(connectionAttempts.get()>0&&iSetAConnectionAttempt)
               connectionAttempts.decrementAndGet();
        }
    }

    private void sleep( final long retryDelay ) throws MqRetriesInterruptedException {
        if ( retryDelay > 0 ) {
            try {
                Thread.sleep(retryDelay);
            } catch ( InterruptedException e ) {
                throw new MqRetriesInterruptedException();
            }
        }
    }

    private static final class MqRetriesInterruptedException extends Exception {}

    private final class MqRoutingCallback implements MqTaskCallback {
        private final MqNativeEndpointConfig cfg;
        private final PolicyEnforcementContext context;
        private final Map<String,?> variables;
        private final com.l7tech.message.Message requestMessage;
        private Exception exception;
        private boolean messageSentOrReceived = false;

        private MqRoutingCallback( final PolicyEnforcementContext context,
                                   final MqNativeEndpointConfig cfg) {
            this.context = context;
            this.cfg = cfg;
            this.variables = unmodifiableMap( context.getVariableMap( assertion.getVariablesUsed(), getAudit() ) );
            try {
                this.requestMessage = context.getTargetMessage(assertion.getRequestTarget());
            } catch (NoSuchVariableException e) {
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
            }
        }

        private boolean isMessageSentOrReceived() {
            return messageSentOrReceived;
        }

        private void doException() throws IOException, SAXException, MQException, MqNativeConfigException {
            if ( exception != null) {
                if ( exception instanceof MQException ) {
                    context.setVariable(ServerMqNativeRoutingAssertion.completionCodeString, ((MQException) exception).completionCode);
                    context.setVariable(ServerMqNativeRoutingAssertion.reasonCodeString, ((MQException) exception).reasonCode);
                    throw (MQException) exception;
                } else if ( exception instanceof MqNativeConfigException ) {
                    throw (MqNativeConfigException) exception;
                } else if ( exception instanceof SAXException ) {
                    throw (SAXException) exception;
                } else if ( exception instanceof IOException ) {
                    throw (IOException) exception;
                } else {
                    throw ExceptionUtils.wrap( exception );
                }
            }
        }

        /**
         * Don't throw MQException since that would close the queue manager before our retry count.
         */
        @Override
        public void doWork( final MQQueueManager queueManager ) {
            MQQueue targetQueue = null;
            MQQueue replyQueue = null;
            try {
                MQMessage mqResponse;
                final int readTimeout = getTimeout();

                // route via write to queue
                if (assertion.isPutToQueue()) {
                    // create the outbound queue
                    targetQueue = queueManager.accessQueue( cfg.getQueueName(), QUEUE_OPEN_OPTIONS_OUTBOUND_PUT );

                    // create replyTo or temporary queue
                    if (context.isReplyExpected()) {
                        replyQueue = createReplyQueue(queueManager, cfg);
                    }

                    // write to queue
                    final MQPutMessageOptions pmo = new MQPutMessageOptions();
                    pmo.options = MQPMO_NO_SYNCPOINT; // make message available immediately
                    if ( cfg.isCopyCorrelationId() && cfg.getReplyType() == REPLY_SPECIFIED_QUEUE ) {
                        if ( logger.isLoggable( Level.FINE ))
                            logger.fine("New correlationId will be generated");
                        pmo.options |= MQPMO_NEW_CORREL_ID;
                    }
                    mqResponse = writeMessageToQueue(targetQueue, replyQueue, pmo, readTimeout);

                    // no write response and no write reply required
                    final boolean isReplyRequired = context.isReplyExpected() && replyQueue != null;
                    if ( !isReplyRequired ) {
                        context.routingFinished();
                        logAndAudit(MQ_ROUTING_NO_RESPONSE_EXPECTED);
                        context.setRoutingStatus( RoutingStatus.ROUTED );
                        return;
                    }

                    // else route via read from queue
                } else {
                    final MQGetMessageOptions gmo = new MQGetMessageOptions();
                    gmo.options = MQGMO_WAIT | MQGMO_NO_SYNCPOINT;
                    gmo.waitInterval = readTimeout;

                    targetQueue = queueManager.accessQueue(cfg.getQueueName(), QUEUE_OPEN_OPTIONS_OUTBOUND_GET);
                    mqResponse = readMessageFromQueue(targetQueue, gmo);
                    if ( mqResponse == null ) {
                        logAndAudit(MQ_ROUTING_NO_RESPONSE, String.valueOf(readTimeout));
                        throw new AssertionStatusException(AssertionStatus.FAILED);
                    }
                }

                // Create the response
                enforceResponseSizeLimit(mqResponse);
                final Pair<byte[], byte[]> parsedResponse = MqNativeUtils.parseHeader(mqResponse);
                final StashManager stashManager = stashManagerFactory.createStashManager();

                final com.l7tech.message.Message responseMessage;
                try {
                    responseMessage = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
                } catch (NoSuchVariableException e) {
                    throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
                }
                responseMessage.initialize(stashManager, ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(parsedResponse.right));
                logAndAudit(MQ_ROUTING_GOT_RESPONSE);

                final byte[] headerOnly = getResponseHeader(parsedResponse.left);
                MqNativeKnob mqNativeKnob = buildMqNativeKnob( headerOnly );
                responseMessage.attachKnob( mqNativeKnob, MqNativeKnob.class );

                context.setRoutingStatus( RoutingStatus.ROUTED );

                // todo: move to abstract routing assertion
                requestMessage.notifyMessage(responseMessage, MessageRole.RESPONSE);
                responseMessage.notifyMessage(requestMessage, MessageRole.REQUEST);
            } catch ( MQException e ) {
                exception = e;
            } catch ( MqNativeRuntimeException e ) {
                exception = e;
            } catch ( MqNativeConfigException e ) {
                exception = e;
            } catch ( IOException e ) {
                exception = e;
            } finally {
                closeQuietly( targetQueue );
                closeQuietly( replyQueue );
            }
        }

        private MQMessage readMessageFromQueue(MQQueue readQueue, MQGetMessageOptions gmoOptions)
                throws IOException, MqNativeRuntimeException, MQException {
            logAndAudit(MQ_ROUTING_REQUEST_ROUTED);

            // perform the route and get from destination queue
            context.routingStarted();
            if ( logger.isLoggable( Level.FINE ) ) {
                logger.fine("Receiving MQ outbound message");
            }
            MQMessage mqResponse = new MQMessage();
            try {
                readQueue.get(mqResponse, gmoOptions);
            } catch (MQException readEx) {
                if (readEx.reasonCode != MQRC_NO_MSG_AVAILABLE) {
                    throw readEx;
                }
            }

            context.routingFinished();
            return mqResponse;
        }

        private MQMessage writeMessageToQueue(MQQueue writeQueue, @Nullable MQQueue replyQueue,
                                              MQPutMessageOptions pmoOptions, int timeout)
                throws IOException, MqNativeRuntimeException, MqNativeConfigException, MQException {

            // create the MQMessage to be routed
            final MQMessage outboundRequest = makeRequest(
                    context,
                    cfg,
                    replyQueue==null ? null : replyQueue.getName());

            logAndAudit(MQ_ROUTING_REQUEST_ROUTED);

            // perform the route to the destination queue
            context.routingStarted();

            if ( logger.isLoggable( Level.FINE ))
                logger.fine("Sending MQ outbound message");

            writeQueue.put(outboundRequest, pmoOptions);
            messageSentOrReceived = true; // no retries once sent or received

            if ( logger.isLoggable( Level.FINE ))
                logger.fine("MQ outbound message sent");


            // no response nor reply required
            boolean isReplyRequired = context.isReplyExpected() && replyQueue != null;
            if ( !isReplyRequired ) {
                return null;
            }

            final byte[] selector = getSelector( outboundRequest, cfg );

            final MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.options = MQGMO_WAIT | MQGMO_NO_SYNCPOINT;
            gmo.waitInterval = timeout;
            gmo.matchOptions = MQMO_MATCH_MSG_ID | MQMO_MATCH_CORREL_ID;
            MQMessage mqResponse = new MQMessage();
            mqResponse.correlationId = selector;

            // wait for and read the reply
            try {
                replyQueue.get(mqResponse, gmo);
            } catch (MQException readEx) {
                if (readEx.getReason() == MQRC_NO_MSG_AVAILABLE) {
                    mqResponse = null;
                } else
                    throw readEx;
            }

            context.routingFinished();
            if ( mqResponse == null ) {
                logAndAudit(MQ_ROUTING_NO_RESPONSE, String.valueOf(timeout));
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }

            return mqResponse;
        }

        void enforceResponseSizeLimit( final MQMessage message ) {
            final long limit = getResponseSizeLimit();
            if ( limit > 0 && message.getTotalMessageLength() > limit ) {
                logAndAudit( MQ_ROUTING_RESPONSE_TOO_LARGE);
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
        }

        /**
         * Get the size limit for the response message.
         *
         * @return The size limit in bytes.
         */
        long getResponseSizeLimit() {
            long sizeLimit = getPropertyWithDefault(
                    assertion.getResponseSize(),
                    0L,
                    "size limit",
                    config.getLongProperty(PARAM_IO_MQ_MESSAGE_MAX_BYTES, DEFAULT_MESSAGE_MAX_BYTES),
                    new UnaryThrows<Long,String,NumberFormatException>(){
                        @Override
                        public Long call( final String text ) throws NumberFormatException {
                            return Long.parseLong( text );
                        }
                    }, MQ_ROUTING_RESPONSE_SIZE_LIMIT_ERROR );

            if ( sizeLimit < 0L ) {
                sizeLimit = com.l7tech.message.Message.getMaxBytes();
            }

            logAndAudit( MQ_ROUTING_RESPONSE_SIZE_LIMIT, String.valueOf( sizeLimit ) );

            return sizeLimit;
        }

        /**
         * Gets the timeout interval for waiting on the reply queue.
         *
         * @return the replyTo queue timeout in milliseconds.
         */
        int getTimeout() {
            int timeout = getPropertyWithDefault(
                    assertion.getResponseTimeout(),
                    1,
                    "timeout",
                    config.getIntProperty(MQ_RESPONSE_TIMEOUT_PROPERTY, 10000),
                    new UnaryThrows<Integer,String,NumberFormatException>(){
                        @Override
                        public Integer call( final String text ) throws NumberFormatException {
                            return Integer.parseInt( text );
                        }
                    }, MQ_ROUTING_RESPONSE_TIMEOUT_ERROR );

            logAndAudit( MQ_ROUTING_RESPONSE_TIMEOUT, String.valueOf( timeout ) );

            return timeout;
        }

        private <T extends Number> T getPropertyWithDefault( String stringValue,
                                                             final T minimum,
                                                             final String description,
                                                             final T defaultValue,
                                                             final UnaryThrows<T,String,NumberFormatException> parser,
                                                             final AuditDetailMessage parseErrorMessage ) {
            Option<T> value = Option.none();

            if ( stringValue != null && !stringValue.isEmpty() ) {
                stringValue = ExpandVariables.process( stringValue, variables, getAudit() );
                try {
                    value = some(parser.call(stringValue));
                    if ( value.some().longValue() < minimum.longValue() ){
                        logAndAudit( parseErrorMessage, "Negative "+description+" ("+value.some()+")" );
                        value = Option.none();
                    }
                } catch ( NumberFormatException e ) {
                    logAndAudit( parseErrorMessage, "Unable to parse "+description+" ("+stringValue+")" );
                }
            }

            return value.orSome( defaultValue );
        }
    }

    private Option<MqNativeDynamicProperties> expandMqDynamicPropertiesVariables( final PolicyEnforcementContext pec,
                                                                                  final Option<MqNativeDynamicProperties> unprocessedProperties ) {
        return unprocessedProperties.map( new Unary<MqNativeDynamicProperties,MqNativeDynamicProperties>(){
            @Override
            public MqNativeDynamicProperties call( final MqNativeDynamicProperties unprocessedProperties ) {
                try {
                    final Map<String,Object> variables = pec.getVariableMap( assertion.getVariablesUsed(), getAudit() );
                    final MqNativeDynamicProperties mqDynamicProperties = new MqNativeDynamicProperties();
                    mqDynamicProperties.setQueueName( expandVariables( unprocessedProperties.getQueueName(), variables ) );
                    mqDynamicProperties.setReplyToQueue( expandVariables( unprocessedProperties.getReplyToQueue(), variables ) );
                    return mqDynamicProperties;
                } catch ( IllegalArgumentException iae ) {
                    logAndAudit( MQ_ROUTING_TEMPLATE_ERROR, "variable processing error; " + iae.getMessage() );
                    throw new AssertionStatusException( AssertionStatus.FAILED );
                }
            }
        } );
    }
    private String expandVariables( final String value, final Map<String,Object> variableMap ) {
        String expandedValue = value;

        if ( expandedValue != null && expandedValue.contains( Syntax.SYNTAX_PREFIX ) ) {
            expandedValue = ExpandVariables.process(value, variableMap, getAudit(), true);
        }
        return expandedValue;
    }

    private boolean markedForUpdate() {
        return needsUpdate.get();
    }

    private boolean markUpdate(boolean value) {
        return needsUpdate.compareAndSet( !value, value );
    }

    private void resetEndpointInfo() {
        synchronized ( endpointConfigSync ) {
            if (markedForUpdate()) {
                endpointConfig = null;
            }
        }
    }

    private boolean isValidRequest( final com.l7tech.message.Message message, final TargetMessageType targetMessageType ) throws IOException {
        boolean valid = true;
        long maxSize = config.getLongProperty( MQ_MESSAGE_MAX_BYTES_PROPERTY, getMaxBytes() );
        final MimeKnob mk = message.getKnob(MimeKnob.class);

        if (mk == null) {
            // Uninitialized request
            logAndAudit( EXCEPTION_WARNING_WITH_MORE_INFO, targetMessageType.toString() + " is not initialized; nothing to route" );
            return false;
        }

        if ( maxSize > 0 && mk.getContentLength() > maxSize ) {
            valid = false;
        }

        return valid;
    }

    /**
     * Builds a {@link MQMessage} to be routed to a Websphere MQ endpoint.
     * @param context contains the request to be converted into a MQ Message
     * @param endpointCfg the MQ endpoint
     * @param replyQueueName the name of the temporary queue used in the replyToQueueName field
     * @return the MQMessage instance to be routed
     * @throws IOException error reading request message
     * @throws MqNativeRuntimeException error creating the routing request
     * @throws MqNativeConfigException if the outbound queue is mis-configured
     */
    private MQMessage makeRequest( final PolicyEnforcementContext context,
                                   final MqNativeEndpointConfig endpointCfg,
                                   @Nullable final String replyQueueName )
        throws IOException, MqNativeRuntimeException, MqNativeConfigException
    {
        final PoolByteArrayOutputStream outputStream = new PoolByteArrayOutputStream();
        final byte[] outboundRequestBytes;
        Message requestMessage;
        try {
            requestMessage = context.getTargetMessage(assertion.getRequestTarget());
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }
        final MimeKnob mk = requestMessage.getMimeKnob();
        try {
            IOUtils.copyStream( mk.getEntireMessageBodyAsInputStream(), outputStream );
            outboundRequestBytes = outputStream.toByteArray();
        } catch (NoSuchPartException e) {
            throw new CausedIOException("Couldn't read from MQ request"); // can't happen
        } finally {
            outputStream.close();
        }

        // instantiate the request MQMessage
        final MQMessage newRequest = new MQMessage();
        final Map<String,String> properties = assertion.getRequestMessageAdvancedProperties();
        if ( properties != null ) {
            MqNativeUtils.applyPropertiesToMessage( newRequest, properties );
        }

        // determine whether to copy over any header from the request
        writeRequestHeader(newRequest, requestMessage);

        // write the payload/body of the message
        newRequest.write(outboundRequestBytes);

        // check whether a response is expected
        final MqNativeReplyType replyType = endpointCfg.getReplyType();
        switch ( replyType ) {
            case REPLY_AUTOMATIC:
                logAndAudit( MQ_ROUTING_REQUEST_WITH_AUTOMATIC, endpointCfg.getQueueManagerName() + "/" + endpointCfg.getQueueName() );
                if ( StringUtils.isEmpty(replyQueueName) )
                    throw new IllegalStateException( "AUTOMATIC reply was selected, but no temporary queue name was specified" );
                newRequest.replyToQueueName = replyQueueName;
                return newRequest;

            case REPLY_NONE:
                logAndAudit( MQ_ROUTING_REQUEST_WITH_NO_REPLY, endpointCfg.getQueueManagerName() + "/" + endpointCfg.getQueueName() );
                return newRequest;

            case REPLY_SPECIFIED_QUEUE:
                logAndAudit( MQ_ROUTING_REQUEST_WITH_REPLY_TO_OTHER, endpointCfg.getQueueManagerName() + "/" + endpointCfg.getQueueName(), replyQueueName );
                if ( StringUtils.isEmpty(replyQueueName) )
                    throw new IllegalStateException( "REPLY_TO_OTHER was selected, but no reply-to queue name was specified" );
                newRequest.replyToQueueName = replyQueueName;
                return newRequest;

            default:
                throw new IllegalStateException( "Unknown MQ ReplyType " + replyType );
        }
    }

    /**
     * Create the replyTo queue object based on the outbound queue configuration.  Only used for AUTOMATIC where a
     * temporary queue will be created; or REPLY_TO_OTHER where a specific reply queue is stated.
     *
     * @param queueManager the MQ QueueManager
     * @param endpointConfig the outbound MQ queue definition
     * @return the MQQueue instance that is opened for reply, null if a reply is not required
     * @throws MQException error access queue
     * @throws MqNativeConfigException configuration error
     */
    private MQQueue createReplyQueue( final MQQueueManager queueManager,
                                      final MqNativeEndpointConfig endpointConfig ) throws MQException, MqNativeConfigException {

        MQQueue replyQueue = null;

        final String modelQName = endpointConfig.getReplyToModelQueueName();
        final String replyQName = endpointConfig.getReplyToQueueName();
        if (endpointConfig.getReplyType() == REPLY_AUTOMATIC && modelQName != null && modelQName.length() > 0) {
            // access the model queue
            replyQueue = queueManager.accessQueue(modelQName, QUEUE_OPEN_OPTIONS_OUTBOUND_REPLY_MODEL_QUEUE, null, modelQName + ".*", null);
            logger.log( Level.FINE, "Temp queue opened Name({2}) MQQDT({0}) MQQT({1})", new Object[]{ replyQueue.getDefinitionType(), replyQueue.getQueueType(), replyQueue.getName() } );
            if ( MQQDT_PREDEFINED == replyQueue.getDefinitionType() ) {
                closeQuietly( replyQueue );
                throw new MqNativeConfigException( "Reply queue (" + modelQName + ") is not a model, cannot create temporary queue." );
            }
        } else if (endpointConfig.getReplyType() == REPLY_SPECIFIED_QUEUE && replyQName != null && replyQName.length() > 0) {
            // access the specified replyTo queue
            replyQueue = queueManager.accessQueue(replyQName, QUEUE_OPEN_OPTIONS_OUTBOUND_REPLY_SPECIFIED_QUEUE);
        }

        return replyQueue;
    }

    private void writeRequestHeader(final MQMessage mqRequest,
                                    final Message requestMessage)
        throws MqNativeRuntimeException
    {
        final boolean passThroughHeaders = assertion.getRequestMqNativeMessagePropertyRuleSet().isPassThroughHeaders();
        try {
            if ( passThroughHeaders ) {
                MqNativeKnob mqmd = requestMessage.getKnob(MqNativeKnob.class);
                if (mqmd != null && mqmd.getMessageHeaderLength() > 0) {
                    mqRequest.write(mqmd.getMessageHeaderBytes());
                }

            }
        } catch (IOException ioex) {
            logger.log(Level.WARNING, "Unable to write MQHeader due to: {0}", new Object[] { getMessage( ioex )});
            throw new MqNativeRuntimeException(ioex);
        }
    }

    private byte[] getResponseHeader( final byte[] headerFromResponse ) {
        final boolean passThroughHeaders = assertion.getResponseMqNativeMessagePropertyRuleSet().isPassThroughHeaders();
        return passThroughHeaders ? headerFromResponse : new byte[0];
    }

    private byte[] getSelector( final MQMessage outboundRequest,
                                final MqNativeEndpointConfig mqEndpointConfig ) throws MQException {
        final byte[] selector;

        if (mqEndpointConfig.getReplyType() == REPLY_AUTOMATIC) {
            if ( logger.isLoggable( Level.FINE ))
                logger.fine("Response expected on temporary queue; not using selector");
            selector = null;

        } else {
            if ( mqEndpointConfig.isCopyCorrelationId() ) {
                selector = outboundRequest.correlationId;
                logger.fine( "Filtering on correlation id " + HexUtils.encodeBase64( selector ) );
            } else {
                selector = outboundRequest.messageId;
                logger.fine( "Filtering on message id " + HexUtils.encodeBase64( selector ) );
            }
        }
        return selector;
    }

    private MqNativeEndpointConfig getEndpointConfig( final Option<MqNativeDynamicProperties> dynamicProperties ) throws MqNativeConfigException {
        MqNativeEndpointConfig config;
        synchronized( endpointConfigSync ) {
            config = endpointConfig;
        }

        if ( config != null && !config.isDynamic() ) {
            return config;
        }

        final SsgActiveConnector ssgActiveConnector;
        try {
            ssgActiveConnector = ssgActiveConnectorManager.findByPrimaryKey( assertion.getSsgActiveConnectorId() );
        } catch ( FindException e ) {
            throw new MqNativeConfigException(
                    "Error accessing MQ endpoint #" + assertion.getSsgActiveConnectorId(), e );
        } catch ( NullPointerException e ) {
            // Unboxing of 'assertion.getSsgActiveConnectorId()' may produce 'java.lang.NullPointerException'.
            throw new MqNativeConfigException(
                    "Error accessing MQ endpoint #" + assertion.getSsgActiveConnectorId(), e );
        }

        if ( ssgActiveConnector == null ||
                !ACTIVE_CONNECTOR_TYPE_MQ_NATIVE.equals(ssgActiveConnector.getType()) ||
                !ssgActiveConnector.isEnabled() ||
                ssgActiveConnector.getBooleanProperty( PROPERTIES_KEY_IS_INBOUND ) ) {
            final String name = optional( ssgActiveConnector ).map( name() )
                    .orElse( optional( assertion.getSsgActiveConnectorName() ) )
                    .orSome( "<Unknown>" );
            final StringBuilder message = new StringBuilder(128);
            message.append( "MQ endpoint '" ).append(name).append( "' #" ).append( assertion.getSsgActiveConnectorId() );
            if ( ssgActiveConnector!=null && ssgActiveConnector.getBooleanProperty( PROPERTIES_KEY_IS_INBOUND ) ) {
                message.append( " is an inbound queue" );
            } else if ( ssgActiveConnector!=null && !ssgActiveConnector.isEnabled() ) {
                message.append( " is disabled" );
            } else {
                message.append( " could not be located! It may have been deleted" );
            }
            throw new MqNativeConfigException( message.toString() );
        }

        config = new MqNativeEndpointConfig(
                ssgActiveConnector,
                getQueuePassword( ssgActiveConnector, securePasswordManager ),
                dynamicProperties );
        config.validate();

        if ( !config.isDynamic() ) {
            synchronized( endpointConfigSync ) {
                this.endpointConfig = config;
            }
        }

        return config;
    }

    @Override
    public void close() {
        super.close();
        applicationEventProxy.removeApplicationListener( invalidator );
    }

    /**
     * Invalidation listener for MQ endpoint (SsgActiveConnector) updates.
     */
    private static final class MqNativeSsgActiveConnectorInvalidator implements ApplicationListener {
        private static final Logger logger = Logger.getLogger( MqNativeSsgActiveConnectorInvalidator.class.getName() );
        private final ServerMqNativeRoutingAssertion serverMqRoutingAssertion;

        private MqNativeSsgActiveConnectorInvalidator( @NotNull final ServerMqNativeRoutingAssertion serverMqRoutingAssertion ) {
            this.serverMqRoutingAssertion = serverMqRoutingAssertion;
        }

        @Override
        public void onApplicationEvent( final ApplicationEvent applicationEvent ) {
            if (applicationEvent instanceof EntityInvalidationEvent ) {
                EntityInvalidationEvent eie = (EntityInvalidationEvent) applicationEvent;
                if (SsgActiveConnector.class.isAssignableFrom(eie.getEntityClass())) {

                    MqNativeEndpointConfig mqEndpointConfig;
                    synchronized ( serverMqRoutingAssertion.endpointConfigSync) {
                        mqEndpointConfig = serverMqRoutingAssertion.endpointConfig;
                    }

                    if ( mqEndpointConfig != null && contains( eie.getEntityIds(), mqEndpointConfig.getMqEndpointKey().getId() ) ) {
                        if ( serverMqRoutingAssertion.markUpdate(true) ) {
                            if ( logger.isLoggable(Level.CONFIG) ) {
                                logger.log(Level.CONFIG, "Flagging MQ endpoint information for update ''{0}'' (#{1})",
                                    new Object[]{mqEndpointConfig.getName(), mqEndpointConfig.getMqEndpointKey().getId()});
                            }
                        }
                    }
                }
            }
        }
    }
}
