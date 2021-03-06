package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsOutboundMessageType;
import com.l7tech.gateway.common.transport.jms.JmsReplyType;
import com.l7tech.message.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.JmsDynamicProperties;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.transport.jms.*;
import com.l7tech.server.transport.jms2.JmsConnectionMaxWaitException;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsResourceManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.xml.sax.SAXException;

import javax.jms.*;
import javax.jms.Message;
import javax.naming.CommunicationException;
import javax.naming.NamingException;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.message.JmsKnob.HEADER_TYPE_JMS_HEADER;
import static com.l7tech.message.JmsKnob.HEADER_TYPE_JMS_PROPERTY;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;

/**
 * Server side implementation of JMS routing assertion.
 */
public class ServerJmsRoutingAssertion extends ServerRoutingAssertion<JmsRoutingAssertion> {
    private static final Logger logger = Logger.getLogger(ServerJmsRoutingAssertion.class.getName());

    private static final String PROP_RETRY_DELAY = "com.l7tech.server.policy.assertion.jmsRoutingRetryDelay";
    private static final String PROP_MAX_OOPS = "com.l7tech.server.policy.assertion.jmsRoutingMaxRetries";
    private static final int MAX_OOPSES = 5;
    private static final long RETRY_DELAY = 1000L;
    private static final long DEFAULT_MESSAGE_MAX_BYTES = 2621440L;
    private static final String BEAN_NAME_APP_EVENT_PROXY = "applicationEventProxy";
    private static final String BEAN_NAME_DEFAULT_KEY = "defaultKey";
    private static final String BEAN_NAME_SERVER_CONFIG = "serverConfig";
    private static final String PROPERTY_NAME_IO_JMS_MESSAGE_MAX_BYTES = "ioJmsMessageMaxBytes";
    private static final String BEAN_NAME_JMS_ENDPOINT_MANAGER = "jmsEndpointManager";
    private static final String BEAN_NAME_JMS_CONNECTION_MANAGER = "jmsConnectionManager";
    private static final String BEAN_NAME_STASH_MANAGER_FACTORY = "stashManagerFactory";
    private static final String BEAN_NAME_JMS_PROPERTY_MAPPER = "jmsPropertyMapper";
    private static final String BEAN_NAME_JMS_RESOURCE_MANAGER = "jmsResourceManager";


    private final ApplicationContext spring;
    private final ApplicationEventProxy applicationEventProxy;
    private final ServerConfig serverConfig;
    private final JmsEndpointManager jmsEndpointManager;
    private final JmsConnectionManager jmsConnectionManager;
    private final StashManagerFactory stashManagerFactory;
    private final JmsPropertyMapper jmsPropertyMapper;
    private final JmsResourceManager jmsResourceManager;
    private final SignerInfo senderVouchesSignerInfo;

    private final AtomicBoolean needsUpdate = new AtomicBoolean(false);

    private final Object jmsInfoSync = new Object(); // sync for routedRequestConnection, routedRequestEndpoint, endpointConfig
    private JmsConnection routedRequestConnection;
    private JmsEndpoint routedRequestEndpoint;
    private JmsEndpointConfig endpointConfig;
    private final JmsInvalidator invalidator;

    public ServerJmsRoutingAssertion(JmsRoutingAssertion data, ApplicationContext spring) {
        super(data, spring);
        this.spring = spring;
        this.applicationEventProxy = spring.getBean(BEAN_NAME_APP_EVENT_PROXY, ApplicationEventProxy.class);
        this.serverConfig = spring.getBean(BEAN_NAME_SERVER_CONFIG, ServerConfig.class);
        this.jmsEndpointManager = (JmsEndpointManager)spring.getBean(BEAN_NAME_JMS_ENDPOINT_MANAGER);
        this.jmsConnectionManager = (JmsConnectionManager)spring.getBean(BEAN_NAME_JMS_CONNECTION_MANAGER);
        this.stashManagerFactory = spring.getBean(BEAN_NAME_STASH_MANAGER_FACTORY, StashManagerFactory.class);
        this.jmsPropertyMapper = spring.getBean(BEAN_NAME_JMS_PROPERTY_MAPPER, JmsPropertyMapper.class);
        this.jmsResourceManager = spring.getBean(BEAN_NAME_JMS_RESOURCE_MANAGER, JmsResourceManager.class);
        this.invalidator = new JmsInvalidator(this);
        applicationEventProxy.addApplicationListener(invalidator);
        SignerInfo signerInfo = null;
        try {
            DefaultKey ku = spring.getBean(BEAN_NAME_DEFAULT_KEY, DefaultKey.class);
            signerInfo = ku.getSslInfo();
        } catch(Exception e) {
            logger.log(Level.WARNING, "Error getting SAML signer information.", e);
        }
        this.senderVouchesSignerInfo = signerInfo;
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException {

        context.setRoutingStatus(RoutingStatus.ATTEMPTED);

        final com.l7tech.message.Message requestMessage;
        try {
            requestMessage = context.getTargetMessage(assertion.getRequestTarget());
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.MESSAGE_TARGET_ERROR, e.getVariable(), getMessage( e ));
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);

        }
        if ( !isValidRequest(requestMessage) ) {
            return AssertionStatus.BAD_REQUEST;
        }

        JmsEndpointConfig cfg = null;

        try {
            // DELETE CURRENT SECURITY HEADER IF NECESSARY
            handleProcessedSecurityHeader(requestMessage);

            if (assertion.isAttachSamlSenderVouches()) {
                if (senderVouchesSignerInfo == null) {
                    logAndAudit(AssertionMessages.JMS_ROUTING_NO_SAML_SIGNER);
                    return AssertionStatus.FAILED;
                }
                doAttachSamlSenderVouches( assertion, requestMessage, context.getDefaultAuthenticationContext().getLastCredentials(), senderVouchesSignerInfo);
            }

            /*
             * When context variables are used for JMS queue properties, this check must be
             * done to see whether a different JMS destination queue needs to be initialized
             * for routing.
             */
            final JmsDynamicProperties dynamicPropsWithValues = getExpandedDynamicRoutingProps( context );

            if (markedForUpdate()) {
                try {
                    logger.info("JMS information needs update, closing session (if open).");

                    resetEndpointInfo();
                } finally {
                    markUpdate(false);
                }
            }

            final long retryDelay = serverConfig.getTimeUnitProperty( PROP_RETRY_DELAY, RETRY_DELAY );
            final int maxOopses = serverConfig.getIntProperty( PROP_MAX_OOPS, MAX_OOPSES );
            int oopses = 0;

            // Get the current JmsEndpointConfig
            cfg = getEndpointConfig(dynamicPropsWithValues);

            // Destinations are cached for retries
            final Destination[] inboundDestinationHolder = new Destination[1];

            // Message send retry loop.
            // Once the message is sent, there are no more retries
            while ( true ) {
                final JmsRoutingCallback jrc = new JmsRoutingCallback(context, cfg, inboundDestinationHolder);
                try {
                    jmsResourceManager.doWithJmsResources(cfg, jrc);
                    jrc.doException();
                    break; // no error
                } catch(JmsConnectionMaxWaitException pe) {
                    if ( jrc.isMessageSent() ) {
                        throw pe;
                    }

                    if (++oopses < maxOopses) {
                        logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(retryDelay)}, getDebugException( pe ));
                        sleep( retryDelay );
                    } else {
                        logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_NOMORETRIES, String.valueOf(maxOopses));
                        // Catcher will log/audit the stack trace
                        return AssertionStatus.FAILED;
                    }
                } catch (JmsRuntimeException jre) {
                    logger.log(Level.FINE, "JmsRuntimeException thrown", jre);
                    if ( jrc.isMessageSent() ) {
                        throw jre.getCause() != null ? jre.getCause() : jre;
                    }

                    if (++oopses < maxOopses) {
                        logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(retryDelay)}, getDebugException( jre ));
                        jmsResourceManager.invalidate(cfg);
                        sleep( retryDelay );
                    } else {
                        logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_NOMORETRIES, String.valueOf(maxOopses));
                        // Catcher will log/audit the stack trace
                        throw jre.getCause() != null ? jre.getCause() : jre;
                    }
                } catch (JMSException e) {
                    logger.log(Level.FINE, "JMSException thrown", e);
                    if ( jrc.isMessageSent()) {
                        throw e;
                    }

                    if( e.getLinkedException() != null && e.getLinkedException() instanceof ClassCastException) {
                        return AssertionStatus.FAILED;
                    }

                    if (++oopses < maxOopses) {
                        if (ExceptionUtils.causedBy(e, InvalidDestinationException.class)) {
                            logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(retryDelay)}, getDebugException( e ));
                        } else {
                            logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(retryDelay)}, e);
                        }
                        jmsResourceManager.invalidate(cfg);
                        inboundDestinationHolder[0] = null;
                        sleep( retryDelay );
                    } else {
                        logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_NOMORETRIES, String.valueOf(maxOopses));
                        // Catcher will log/audit the stack trace
                        throw e;
                    }
                } catch (NamingException nex) {
                    logger.log(Level.FINE, "NamingException thrown", nex);
                    if ( jrc.isMessageSent() ) {
                        throw nex; // this is an error, there should be no possibility of a NamingException after message send
                    }

                    // there is a chance that the LDAP connection will timeout when connecting to a
                    // MQSeries provider with LDAP -
                    if (++oopses < maxOopses && nex instanceof CommunicationException) {
                        jmsResourceManager.invalidate(cfg);
                        inboundDestinationHolder[0] = null;
                    } else {
                        if (oopses >= maxOopses)
                            logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_NOMORETRIES, String.valueOf(maxOopses));
                        final NamingException auditException = JmsUtil.isCausedByExpectedNamingException( nex ) ? null : nex;
                        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Error in outbound JMS request processing: " + JmsUtil.getJNDIErrorMessage( nex )}, auditException );
                        return AssertionStatus.FAILED;
                    }
                }
            }

            return AssertionStatus.NONE;
        } catch ( JMSException e ) {
            auditException( "Error in outbound JMS request processing", e );
            if (cfg!=null) jmsResourceManager.invalidate(cfg);
            return AssertionStatus.FAILED;
        } catch ( FindException e ) {
            String msg = "Caught FindException: ";
            logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{msg + getMessage(e)}, getDebugException(e));
            return AssertionStatus.FAILED;
        } catch ( JmsConfigException e ) {
            logAndAudit(AssertionMessages.JMS_ROUTING_CONFIGURATION_ERROR,
                    new String[]{ getMessage( e )},
                    getDebugException( e ));
            return AssertionStatus.FAILED;
        } catch ( AssertionStatusException e ) {
            throw e;
        } catch ( Throwable t ) {
            auditException( "Caught unexpected Throwable in outbound JMS request processing", t );
            if (cfg!=null) jmsResourceManager.invalidate(cfg);
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private void sleep( final long retryDelay ) throws JMSException {
        if ( retryDelay > 0 ) {
            try {
                Thread.sleep(retryDelay);
            } catch ( InterruptedException e ) {
                throw new JMSException("Interrupted during retry delay");
            }
        }
    }

    private void auditException( final String description, final Throwable throwable ) {
        final Throwable auditException = JmsUtil.isCausedByExpectedJMSException( throwable ) ? null : throwable;
        final JMSException jmsException = ExceptionUtils.getCauseIfCausedBy( throwable, JMSException.class );

        String exceptionMessage = jmsException==null ?
            getMessage( throwable ) :
            JmsUtil.getJMSErrorMessage( jmsException );

        if ( throwable != jmsException ) {
            exceptionMessage = getMessage( throwable ) + "; " + exceptionMessage;
        }

        logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ description + ": " + exceptionMessage }, auditException );
    }

    final class JmsRoutingCallback implements JmsResourceManager.JmsResourceCallback {
        private final JmsEndpointConfig cfg;
        private final PolicyEnforcementContext context;
        private final com.l7tech.message.Message requestMessage;
        private final Destination[] jmsInboundDestinationHolder;
        private Exception exception;
        private boolean messageSent = false;

        JmsRoutingCallback ( final PolicyEnforcementContext context,
                                     final JmsEndpointConfig cfg,
                                     final Destination[] jmsInboundDestinationHolder ) {
            this.context = context;
            this.cfg = cfg;
            this.jmsInboundDestinationHolder = jmsInboundDestinationHolder;
            try {
                this.requestMessage = context.getTargetMessage(assertion.getRequestTarget());
            } catch (NoSuchVariableException e) {
                logAndAudit(AssertionMessages.MESSAGE_TARGET_ERROR, e.getVariable(), getMessage( e ));
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
            }
        }

        Exception getException(){
            return exception;
        }

        private boolean isMessageSent() {
            return messageSent;
        }

        private void doException() throws IOException, SAXException, NamingException, JMSException {
            if ( exception != null) {
                if ( exception instanceof JMSException ) {
                    throw (JMSException) exception;
                } else if ( exception instanceof SAXException ) {
                    throw (SAXException) exception;
                } else if ( exception instanceof IOException ) {
                    throw (IOException) exception;
                } else if ( exception instanceof NamingException ) {
                    throw (NamingException) exception;
                } else {
                    throw ExceptionUtils.wrap( exception );
                }
            }
        }

        /**
         * Don't throw JMSException since that would close the connection before our retry count. 
         */
        @Override
        public void doWork( final JmsBag bag,
                            final JmsResourceManager.JndiContextProvider jndiContextProvider ) {
            boolean routingStarted = false;
            boolean routingFinished = false;
            try {
                final Message jmsOutboundRequest = makeRequest(context, bag.getSession(), jndiContextProvider, cfg);

                if (jmsInboundDestinationHolder[0] == null)
                    jmsInboundDestinationHolder[0] = jmsOutboundRequest.getJMSReplyTo();

                final Destination jmsInboundDestination = jmsInboundDestinationHolder[0];

                // Enforces rules on propagation of request JMS message properties.
                final HeadersKnob jmsInboundHeadersKnob = requestMessage.getKnob(HeadersKnob.class);

                Map<String, Object> inboundRequestProps = new HashMap<>();//JMS Properties
                Map<String, Object> inboundRequestHeaders = new HashMap<>();//JMS Standard Headers

                if (jmsInboundHeadersKnob != null) {
                    Collection<Header> properties = jmsInboundHeadersKnob.getHeaders(HEADER_TYPE_JMS_PROPERTY);

                    HashMap<String, Object> propertyMap = new HashMap<>();

                    for (Header property : properties) {
                        propertyMap.put(property.getKey(), property.getValue());
                    }

                    for (String key : propertyMap.keySet()) {
                        inboundRequestProps.put(key, propertyMap.get(key));
                    }

                    Collection<Header> headers = jmsInboundHeadersKnob.getHeaders(HEADER_TYPE_JMS_HEADER);
                    for(Header header : headers) {
                        inboundRequestHeaders.put(header.getKey(), header.getValue());
                    }

                }

                final Map<String, Object> outboundRequestProps = new HashMap<>();
                enforceJmsMessagePropertyRuleSet(context, assertion.getRequestJmsMessagePropertyRuleSet(), inboundRequestProps, outboundRequestProps);

                for (String name : outboundRequestProps.keySet()) {
                    try {
                        jmsOutboundRequest.setObjectProperty(name, outboundRequestProps.get(name));
                    } catch ( MessageFormatException e ) {
                        if ( e.getErrorCode() != null && e.getErrorCode().startsWith("MQ") ) {
                            logAndAudit(AssertionMessages.JMS_ROUTING_NON_SETTABLE_JMS_PROPERTY, new String[] {name, outboundRequestProps.get(name).toString(), ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
                        } else {
                            logAndAudit(AssertionMessages.JMS_ROUTING_MESSAGE_FORMAT_ERROR, new String[] {e.getMessage()}, ExceptionUtils.getDebugException(e));
                        }
                    } catch (NumberFormatException nfe) {
                        //catching exception so that if incoming request with only JMSXGroupSeq and no JMSXGroupID, it will pass through.
                        logAndAudit(AssertionMessages.JMS_ROUTING_NOT_SETTABLE_JMS_PROPERTY,
                                new String[]{name,
                                        (outboundRequestProps.get(name) == null ? "null" : outboundRequestProps.get(name).toString()),
                                        ExceptionUtils.getMessage(nfe)},
                                ExceptionUtils.getDebugException(nfe));
                    }
                }

                for (Map.Entry<String,Object> entry : inboundRequestHeaders.entrySet()) {
                    try {
                        if(JmsUtil.isJmsHeader(entry.getKey())) {
                            //Set JMS Header defined in the context that might override the default value
                            Object value = null;
                            if((entry.getKey().equals(JmsUtil.JMS_REPLY_TO) || entry.getKey().equals(JmsUtil.JMS_DESTINATION)) && entry.getValue() instanceof String) {
                                value = JmsUtil.cast( jndiContextProvider.lookup((String)entry.getValue()), Destination.class);
                            }
                            else {
                                value = entry.getValue();
                            }
                            if(!JmsUtil.setJmsHeader(jmsOutboundRequest, new Pair<>(entry.getKey(), value))) {
                                logAndAudit(AssertionMessages.JMS_ROUTING_NOT_SETTABLE_JMS_HEADER, entry.getKey());
                            }
                        }
                        else {
                            logAndAudit(AssertionMessages.JMS_ROUTING_NOT_SUPPORTED_JMS_HEADER, entry.getKey(), (String)entry.getValue());
                        }
                    } catch (JMSException je) {
                        if(JmsUtil.getCause(je) instanceof ClassCastException) {
                            logAndAudit(AssertionMessages.JMS_ROUTING_INCOMPATIBLE_JMS_HEADER_TYPE, new String[] {entry.getKey()}, ExceptionUtils.getDebugException(je));
                        }
                        messageSent = true;
                        throw je;
                    }

                }

                boolean processReply = context.isReplyExpected() && jmsInboundDestination != null;

                logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_ROUTED);

                final Map<String,Object> variables = context.getVariableMap( assertion.getVariablesUsed(), getAudit() );
                MessageProducer jmsProducer = bag.getMessageProducer();

                final int deliveryMode = assertion.getRequestDeliveryMode() != null? assertion.getRequestDeliveryMode().getValue() : jmsOutboundRequest.getJMSDeliveryMode();
                final int priority = assertion.getRequestPriority() != null ? expandVariableAsInt( assertion.getRequestPriority(), "priority", 0, 9, variables ) : jmsOutboundRequest.getJMSPriority();
                final long timeToLive = assertion.getRequestTimeToLive() != null ? expandVariableAsLong( assertion.getRequestTimeToLive(), "time to live", 0L, Long.MAX_VALUE, variables ) :
                        jmsOutboundRequest.getJMSExpiration() > 0 ? jmsOutboundRequest.getJMSExpiration() - System.currentTimeMillis() : 0;

                if(timeToLive < 0) {
                    logAndAudit(AssertionMessages.JMS_ROUTING_ERROR_SENDING_MESSAGE, jmsProducer.getDestination().toString(), "JMS message expired");
                    messageSent = true;
                    throw new JmsMessageExpiredException();
                }

                context.routingStarted();
                routingStarted = true;

                if ( logger.isLoggable( Level.FINE ))
                    logger.fine("Sending JMS outbound message");

                jmsProducer.send(jmsOutboundRequest, deliveryMode, priority, timeToLive);

                messageSent = true; // no retries once sent

                if ( logger.isLoggable( Level.FINE ))
                    logger.fine("JMS outbound message sent");

                if ( !processReply ) {
                    context.routingFinished();
                    routingFinished = true;
                    logAndAudit(AssertionMessages.JMS_ROUTING_NO_RESPONSE_EXPECTED);
                    context.setRoutingStatus( RoutingStatus.ROUTED );
                } else {

                    final String selector = getSelector( jmsOutboundRequest, cfg.getEndpoint() );
                    int emergencyTimeoutDefault = 10000;
                    String timeoutStr = assertion.getResponseTimeout();
                    int timeout;

                    if (timeoutStr == null) {
                        timeout = serverConfig.getIntProperty( ServerConfigParams.PARAM_JMS_RESPONSE_TIMEOUT, emergencyTimeoutDefault);
                    } else {
                        // try resolving context var
                        timeoutStr = ExpandVariables.process(timeoutStr,variables,getAudit());
                        try {
                            timeout = Integer.parseInt(timeoutStr);
                            if (timeout <= 0){
                                timeout = serverConfig.getIntProperty( ServerConfigParams.PARAM_JMS_RESPONSE_TIMEOUT,emergencyTimeoutDefault);
                                logger.info("Using server default value (" + timeout + ") for JMS response timeout.");
                            }
                        } catch (NumberFormatException e) {
                            timeout = emergencyTimeoutDefault;
                            logger.warning("Using default value (" + emergencyTimeoutDefault + ") for undefined cluster property: " + serverConfig.getClusterPropertyName( ServerConfigParams.PARAM_JMS_RESPONSE_TIMEOUT));
                        }
                    }

                    MessageConsumer jmsConsumer = null;
                    final Message jmsResponse;

                    try {
                        jmsConsumer = JmsUtil.createMessageConsumer( bag.getSession(), jmsInboundDestination, selector );

                        logAndAudit(AssertionMessages.JMS_ROUTING_GETTING_RESPONSE);
                        jmsResponse = jmsConsumer.receive( (long)timeout );

                        if (jmsResponse != null) {
                            try {
                                jmsResponse.acknowledge();
                            } catch (JMSException e) {
                                logger.log( Level.WARNING, "Error acknowledge consumed message.", e );
                            }
                        }

                    } finally {
                        if ( jmsConsumer != null ) jmsConsumer.close();
                    }

                    context.routingFinished();
                    routingFinished = true;

                    if ( jmsResponse == null ) {
                        logAndAudit(AssertionMessages.JMS_ROUTING_NO_RESPONSE, String.valueOf(timeout));
                        throw new AssertionStatusException(AssertionStatus.FAILED);
                    }

                    // enforce size restriction
                    long sizeLimit;

                    if (assertion.getResponseSize()== null) {
                        long clusterPropValue = serverConfig.getLongProperty(ServerConfigParams.PARAM_JMS_MESSAGE_MAX_BYTES, DEFAULT_MESSAGE_MAX_BYTES);
                        if(clusterPropValue >= 0L ){
                            sizeLimit = clusterPropValue;
                        }else{
                            sizeLimit = com.l7tech.message.Message.getMaxBytes();
                        }
                    } else {
                        sizeLimit = expandVariableAsLong( assertion.getResponseSize(), "response message size", 0L, Long.MAX_VALUE, variables );
                    }

                    long size;

                    if ( jmsResponse instanceof TextMessage ) {
                        size = ((TextMessage)jmsResponse).getText().length() ;
                    } else if ( jmsResponse instanceof BytesMessage ) {
                        size = ((BytesMessage)jmsResponse).getBodyLength();
                    } else {
                        logAndAudit(AssertionMessages.JMS_ROUTING_UNSUPPORTED_RESPONSE_MSG_TYPE, jmsResponse.getClass().getName());
                        throw new AssertionStatusException(AssertionStatus.FAILED);
                    }

                    if ( sizeLimit > 0 && size > sizeLimit ) {
                        logAndAudit(AssertionMessages.JMS_ROUTING_RESPONSE_TOO_LARGE);
                        throw new AssertionStatusException(AssertionStatus.FAILED);
                    }

                    final com.l7tech.message.Message responseMessage;

                    try {
                        responseMessage = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
                    } catch (NoSuchVariableException e) {
                        throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);

                    }

                    // copy into response message
                    if ( jmsResponse instanceof TextMessage ) {
                        responseMessage.initialize(XmlUtil.stringToDocument( ((TextMessage)jmsResponse).getText() ),0);
                    } else if ( jmsResponse instanceof BytesMessage ) {
                        BytesMessage bytesMessage = (BytesMessage)jmsResponse;
                        final StashManager stashManager = stashManagerFactory.createStashManager();
                        responseMessage.initialize(stashManager, ContentTypeHeader.XML_DEFAULT, new BytesMessageInputStream(bytesMessage),0);
                    } else {
                        logAndAudit(AssertionMessages.JMS_ROUTING_UNSUPPORTED_RESPONSE_MSG_TYPE, jmsResponse.getClass().getName());
                        throw new AssertionStatusException(AssertionStatus.FAILED);
                    }

                    logAndAudit(AssertionMessages.JMS_ROUTING_GOT_RESPONSE);

                    // Copies the response JMS message properties into the response JmsKnob.
                    // Do this before enforcing the propagation rules so that they will
                    // be available as context variables.                       ;
                    final Map<String, Object> inResJmsMsgProps = new HashMap<>();
                    for (Enumeration e = jmsResponse.getPropertyNames(); e.hasMoreElements() ;) {
                        final String name = (String)e.nextElement();
                        final Object value = jmsResponse.getObjectProperty(name);
                        inResJmsMsgProps.put(name, value);
                    }

                    final Map<String, String> inResJmsMsgHeaders = JmsUtil.getJmsHeaders(jmsResponse);

                    responseMessage.attachJmsKnob(new JmsKnob() {
                        @Override
                        public boolean isBytesMessage() {
                            return (jmsResponse instanceof BytesMessage);
                        }
                        @Override
                        public Map<String, Object> getJmsMsgPropMap() {
                            return inResJmsMsgProps;
                        }
                        @Override
                        public String getSoapAction() {
                            return null;
                        }
                        @Override
                        public Goid getServiceGoid() {
                            return null;
                        }

                        @Override
                        public String[] getHeaderValues(final String name) {
                            final String headerValue = inResJmsMsgHeaders.get(name);

                            if (headerValue != null) {
                                return new String[]{headerValue};
                            } else {
                                return new String[0];
                            }
                        }

                        @Override
                        public String[] getHeaderNames() {
                            return inResJmsMsgHeaders.keySet().toArray(new String[inResJmsMsgHeaders.size()]);
                        }
                    });

                    final Map<String, Object> outResJmsMsgProps = new HashMap<>();
                    enforceJmsMessagePropertyRuleSet(context, assertion.getResponseJmsMessagePropertyRuleSet(), inResJmsMsgProps, outResJmsMsgProps);
                    // After enforcing propagation rules, replace the JMS message properties
                    // in the response JmsKnob with enforced/expanded values and copy them to the response HeadersKnob.
                    responseMessage.getJmsKnob().getJmsMsgPropMap().clear();
                    responseMessage.getJmsKnob().getJmsMsgPropMap().putAll(outResJmsMsgProps);

                    HeadersKnob responseMessageHeadersKnob = responseMessage.getHeadersKnob();

                    for (Map.Entry<String, Object> property : outResJmsMsgProps.entrySet()) {
                        responseMessageHeadersKnob.addHeader(property.getKey(),
                                property.getValue(), HEADER_TYPE_JMS_PROPERTY);
                    }

                    context.setRoutingStatus(RoutingStatus.ROUTED);

                    // todo: move to abstract routing assertion
                    requestMessage.notifyMessage(responseMessage, MessageRole.RESPONSE);
                    responseMessage.notifyMessage(requestMessage, MessageRole.REQUEST);
                }
            } catch (JMSException | SAXException | IOException | NamingException | NumberFormatException e) {
                exception = e;
            } finally {
                if ( closeDestinationIfTemporaryQueue( jmsInboundDestinationHolder[0], cfg.getReplyType() ) ) {
                    jmsInboundDestinationHolder[0] = null;
                }
                if ( routingStarted && !routingFinished ) {
                    context.routingFinished();
                }
            }
        }

        private Integer expandVariableAsInt( final String expressionValue,
                                             final String description,
                                             final int min,
                                             final int max,
                                             final Map<String, Object> variables ) {
            final Integer value;

            if ( expressionValue != null ) {
                final String textValue = ExpandVariables.process( expressionValue, variables, getAudit()).trim();

                if ( ValidationUtils.isValidInteger( textValue, false, min, max ) ) {
                    value = Integer.parseInt( textValue );
                } else {
                    logAndAudit(AssertionMessages.JMS_ROUTING_CONFIGURATION_ERROR, description + " invalid: " + textValue);
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }
            } else {
                value = null;
            }

            return value;
        }

        private Long expandVariableAsLong( final String expressionValue,
                                           final String description,
                                           final long min,
                                           final long max,
                                           final Map<String, Object> variables ) {
            final Long value;

            if ( expressionValue != null ) {
                final String textValue = ExpandVariables.process( expressionValue, variables, getAudit()).trim();

                if ( ValidationUtils.isValidLong( textValue, false, min, max ) ) {
                    value = Long.parseLong( textValue );
                } else {
                    logAndAudit(AssertionMessages.JMS_ROUTING_CONFIGURATION_ERROR, description + " invalid: " + textValue);
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }
            } else {
                value = null;
            }

            return value;
        }
    }

    private boolean markedForUpdate() {
        return needsUpdate.get();
    }

    private boolean markUpdate(boolean value) {
        return needsUpdate.compareAndSet( !value, value );
    }

    private synchronized void resetEndpointInfo() {
        synchronized (jmsInfoSync) {
            if (markedForUpdate()) {
                routedRequestConnection = null;
                routedRequestEndpoint = null;
                endpointConfig = null;
            }
        }
    }

    private boolean closeDestinationIfTemporaryQueue( final Destination destination, final JmsReplyType replyType ) {
        boolean closed = false;

        //SSG-5595 For Weblogic, it treats all queue type as temporary queue
        if ( destination instanceof TemporaryQueue  && replyType == JmsReplyType.AUTOMATIC) {
            closed = true;
            logAndAudit( AssertionMessages.JMS_ROUTING_DELETE_TEMPORARY_QUEUE );
            try {
                ((TemporaryQueue)destination).delete();
            } catch (JMSException e) {
                logger.log( Level.WARNING, "Error closing temporary queue", e );
            }
        }

        return closed;
    }

    private boolean isValidRequest( final com.l7tech.message.Message message ) throws IOException {
        boolean valid = true;

        long maxSize = serverConfig.getLongProperty(PROPERTY_NAME_IO_JMS_MESSAGE_MAX_BYTES, 5242880L );
        final MimeKnob mk = message.getKnob(MimeKnob.class);

        if (mk == null || !message.isInitialized()) {
            // Uninitialized request
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Request is not initialized; nothing to route");
            return false;
        }

        if ( maxSize > 0L && mk.getContentLength() > maxSize ) {
            logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_TOO_LARGE);
            valid = false;
        }

        return valid;
    }

    /**
     * Builds a {@link Message} to be routed to a JMS endpoint.
     * @param context contains the request to be converted into a JMS Message
     * @return the JMS Message
     * @throws IOException
     * @throws JMSException
     */
    private javax.jms.Message makeRequest( final PolicyEnforcementContext context,
                                           final Session jmsSession,
                                           final JmsResourceManager.JndiContextProvider jndiContextProvider,
                                           final JmsEndpointConfig endpointCfg )
        throws IOException, JMSException, NamingException
    {
        final JmsEndpoint outboundRequestEndpoint = endpointCfg.getEndpoint();

        javax.jms.Message outboundRequestMsg;
        final byte[] outboundRequestBytes;
        com.l7tech.message.Message requestMessage;

        try {
            requestMessage = context.getTargetMessage(assertion.getRequestTarget());
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.MESSAGE_TARGET_ERROR, e.getVariable(), getMessage( e ));
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }

        final MimeKnob mk = requestMessage.getMimeKnob();

        try (PoolByteArrayOutputStream outputStream = new PoolByteArrayOutputStream()) {
            IOUtils.copyStream(mk.getEntireMessageBodyAsInputStream(), outputStream);
            outboundRequestBytes = outputStream.toByteArray();
        } catch (NoSuchPartException e) {
            throw new CausedIOException("Couldn't read from JMS request"); // can't happen
        }

        final JmsOutboundMessageType outboundType = outboundRequestEndpoint.getOutboundMessageType();
        boolean useTextMode = outboundType.isDefaultsToText();
        if (outboundType.isCopyRequestType() && requestMessage.getKnob(JmsKnob.class) != null) {
            // Outgoing request should be the same type (i.e. TextMessage or BytesMessage) as the original request
            useTextMode = !requestMessage.getJmsKnob().isBytesMessage();
        }

        if (useTextMode) {
            logAndAudit(AssertionMessages.JMS_ROUTING_CREATE_REQUEST_AS_TEXT_MESSAGE);
            // TODO get encoding from mk?
            outboundRequestMsg = jmsSession.createTextMessage(new String(outboundRequestBytes, JmsUtil.DEFAULT_ENCODING));
        } else {
            logAndAudit(AssertionMessages.JMS_ROUTING_CREATE_REQUEST_AS_BYTES_MESSAGE);
            BytesMessage bytesMessage = jmsSession.createBytesMessage();
            bytesMessage.writeBytes(outboundRequestBytes);
            outboundRequestMsg = bytesMessage;
        }

        final JmsReplyType replyType = outboundRequestEndpoint.getReplyType();
        switch (replyType) {
            case NO_REPLY:
                logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_WITH_NO_REPLY, outboundRequestEndpoint.getDestinationName());
                return outboundRequestMsg;
            case AUTOMATIC:
                logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_WITH_AUTOMATIC, outboundRequestEndpoint.getDestinationName());
                outboundRequestMsg.setJMSReplyTo(jmsSession.createTemporaryQueue());
                return outboundRequestMsg;
            case REPLY_TO_OTHER:
                logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_WITH_REPLY_TO_OTHER, outboundRequestEndpoint.getDestinationName(), outboundRequestEndpoint.getReplyToQueueName());

                // set the replyTo queue
                String replyToQueueName = outboundRequestEndpoint.getReplyToQueueName();
                if (replyToQueueName == null || replyToQueueName.length() == 0)
                    throw new IllegalStateException("REPLY_TO_OTHER was selected, but no reply-to queue name was specified");
                outboundRequestMsg.setJMSReplyTo( JmsUtil.cast( jndiContextProvider.lookup(replyToQueueName), Destination.class) );

                if (!endpointCfg.getEndpoint().isUseMessageIdForCorrelation()) {
                    final String id = "L7_REQ_ID:" + context.getRequestId().toString();
                    outboundRequestMsg.setJMSCorrelationID(id);
                }
                return outboundRequestMsg;
            default:
                throw new java.lang.IllegalStateException("Unknown JmsReplyType " + replyType);
        }
    }

    private String getSelector( final Message jmsOutboundRequest,
                                final JmsEndpoint jmsEndpoint ) throws JMSException {
        final String selector;

        if (jmsEndpoint.getReplyType() == JmsReplyType.AUTOMATIC) {
            if ( logger.isLoggable( Level.FINE ))
                logger.fine("Response expected on temporary queue; not using selector");
            selector = null;
        } else {
            final StringBuilder sb = new StringBuilder("JMSCorrelationID = '");
            if (jmsEndpoint.isUseMessageIdForCorrelation()) {
                final String id = jmsOutboundRequest.getJMSMessageID();
                if (id == null) {
                    logAndAudit(AssertionMessages.JMS_ROUTING_MISSING_MESSAGE_ID);
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }
                sb.append(id);
            } else {
                sb.append(jmsOutboundRequest.getJMSCorrelationID());
            }
            sb.append("'");
            selector = sb.toString();
        }

        if ( logger.isLoggable( Level.FINE )) {
            if (selector != null) {
                logger.fine("Filtering on correlation id " + selector);
            } else {
                logger.fine("Not using a selector");
            }
        }

        return selector;
    }

    private JmsEndpoint getRoutedRequestEndpoint() throws FindException {
        JmsEndpoint jmsEndpoint;
        synchronized(jmsInfoSync) {
            jmsEndpoint = routedRequestEndpoint;
        }

        if ( jmsEndpoint == null ) {
            jmsEndpoint = getJmsEndpoint();
            synchronized(jmsInfoSync) {
                routedRequestEndpoint = jmsEndpoint;
            }
        }
        return jmsEndpoint;
    }

    private JmsEndpoint getJmsEndpoint() throws FindException {
        Goid endpointGoid = assertion.getEndpointOid() == null ? null : new Goid(assertion.getEndpointOid());
        return jmsEndpointManager.findByPrimaryKey(endpointGoid);
    }

    private JmsConnection getRoutedRequestConnection( final JmsEndpoint endpoint ) throws FindException {
        JmsConnection jmsConn;
        synchronized(jmsInfoSync) {
            jmsConn = routedRequestConnection;
        }

        if ( jmsConn == null ) {
            if ( endpoint == null ) {
                logAndAudit(AssertionMessages.JMS_ROUTING_NON_EXISTENT_ENDPOINT,
                        String.valueOf(assertion.getEndpointOid()) + "/" + assertion.getEndpointName());
            } else {
                jmsConn = jmsConnectionManager.findByPrimaryKey( endpoint.getConnectionGoid() );
                synchronized(jmsInfoSync) {
                    routedRequestConnection = jmsConn;
                }
            }
        }

        return jmsConn;
    }

    private JmsEndpointConfig getEndpointConfig( final JmsDynamicProperties jmsDynamicProperties ) throws FindException {
        JmsEndpointConfig config;
        synchronized(jmsInfoSync) {
            config = endpointConfig;
        }

        if ( config != null && !config.isDynamic() ) {
            return config;
        }

        final JmsEndpoint endpoint = getRoutedRequestEndpoint();
        if ( endpoint == null ) throw new FindException( "JmsEndpoint could not be located! It may have been deleted" );

        final JmsConnection conn = getRoutedRequestConnection(endpoint);
        if ( conn == null ) throw new FindException( "JmsConnection could not be located! It may have been deleted" );

        // check for the need to use dynamic routing properties
        if ( endpoint.isTemplate() || conn.isTemplate() ) {
            JmsEndpointConfig jmsEndpointConfig = new JmsEndpointConfig(
                    conn,
                    endpoint,
                    jmsPropertyMapper,
                    spring,
                    jmsDynamicProperties!=null ? jmsDynamicProperties : new JmsDynamicProperties() );
            validateEndpointConfig( jmsEndpointConfig );
            return jmsEndpointConfig;
        } else {
            synchronized(jmsInfoSync) {
                return endpointConfig = new JmsEndpointConfig(conn, endpoint, jmsPropertyMapper, spring);
            }
        }
    }

    private void validateEndpointConfig( final JmsEndpointConfig jmsEndpointConfig ) {
        try {
            jmsEndpointConfig.validate();
        } catch ( JmsConfigException e ) {
            logAndAudit( AssertionMessages.JMS_ROUTING_TEMPLATE_ERROR, "invalid configuration; " + e.getMessage() );
            throw new AssertionStatusException( AssertionStatus.FAILED );
        }
    }

    private JmsDynamicProperties getExpandedDynamicRoutingProps( final PolicyEnforcementContext pec )  {
        JmsDynamicProperties properties = assertion.getDynamicJmsRoutingProperties();
        if ( properties != null ) {
            properties = expandJmsDynamicPropertiesVariables( pec, properties );
        }
        return properties;
    }

    private JmsDynamicProperties expandJmsDynamicPropertiesVariables( final PolicyEnforcementContext pec,
                                                                      final JmsDynamicProperties unprocessedProperties ) {
        final JmsDynamicProperties jmsDynamicProperties = new JmsDynamicProperties();

        final Map<String,Object> variables = pec.getVariableMap( assertion.getVariablesUsed(), getAudit());
        try {
            jmsDynamicProperties.setDestQName( expandVariables( unprocessedProperties.getDestQName(), variables ) );
            jmsDynamicProperties.setDestUserName( expandVariables( unprocessedProperties.getDestUserName(), variables ) );
            jmsDynamicProperties.setDestPassword( expandVariables( unprocessedProperties.getDestPassword(), variables ) );
            jmsDynamicProperties.setReplytoQName( expandVariables( unprocessedProperties.getReplytoQName(), variables ) );
            jmsDynamicProperties.setJndiUrl( expandVariables( unprocessedProperties.getJndiUrl(), variables ) );
            jmsDynamicProperties.setJndiUserName( expandVariables( unprocessedProperties.getJndiUserName(), variables ) );
            jmsDynamicProperties.setJndiPassword( expandVariables( unprocessedProperties.getJndiPassword(), variables ) );
            jmsDynamicProperties.setIcfName( expandVariables( unprocessedProperties.getIcfName(), variables ) );
            jmsDynamicProperties.setQcfName( expandVariables( unprocessedProperties.getQcfName(), variables ) );
        } catch ( IllegalArgumentException iae ) {
            logAndAudit( AssertionMessages.JMS_ROUTING_TEMPLATE_ERROR, "variable processing error; " + iae.getMessage() );
            throw new AssertionStatusException( AssertionStatus.FAILED );
        }

        return jmsDynamicProperties;
    }

    private String expandVariables( final String value, final Map<String,Object> variableMap ) {
        String expandedValue = value;

        if ( expandedValue != null && expandedValue.contains( Syntax.SYNTAX_PREFIX ) ) {
            expandedValue = ExpandVariables.process(value, variableMap, getAudit(), true);
        }

        return expandedValue;
    }

    private void enforceJmsMessagePropertyRuleSet( final PolicyEnforcementContext context,
                                                   final JmsMessagePropertyRuleSet ruleSet,
                                                   final Map<String, Object> src,
                                                   final Map<String, Object> dst) {
        if (ruleSet.isPassThruAll()) {
            for (String name : src.keySet()) {
                if (!name.startsWith("JMS_") && !name.startsWith("JMSX")) {
                    dst.put(name, src.get(name));
                }
            }
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Propagating all JMS message properties with pass through.");
            }
        } else {
            // For efficiency, obtain all context variables used in the rule set once.
            final StringBuilder sb = new StringBuilder();
            for (JmsMessagePropertyRule rule : ruleSet.getRules()) {
                if (! rule.isPassThru()) {
                    sb.append(rule.getCustomPattern());
                }
            }
            final String[] variablesUsed = Syntax.getReferencedNames(sb.toString());
            final Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());

            for (JmsMessagePropertyRule rule : ruleSet.getRules()) {
                final String name = rule.getName();
                if (rule.isPassThru()) {
                    //Allowing customers to send any jms property using custom pass through, as long as it is available in the request.
                    if (src.containsKey(name)) {
                        dst.put(name, src.get(name));
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.finest("Propagating a JMS message property with pass through. (name=" + name + ", value=" + src.get(name) + ")");
                        }
                    }
                } else {
                    final String pattern = rule.getCustomPattern();
                    final String value = ExpandVariables.process(pattern, vars, getAudit());
                    dst.put(name, value);
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest("Propagating a JMS message property with custom pattern (name=" + name + ", pattern=" + pattern + ", value=" + value + ")");
                    }
                }
            }
        }
    }

    /**
     *
     */
    @Override
    public void close() {
        super.close();
        applicationEventProxy.removeApplicationListener(invalidator);
    }

    /**
     * Invalidation listener for JMS endpoint / connection updates.
     */
    public static final class JmsInvalidator implements ApplicationListener {
        private static final Logger logger = Logger.getLogger( JmsInvalidator.class.getName() );
        private final ServerJmsRoutingAssertion serverJmsRoutingAssertion;

        public JmsInvalidator( final ServerJmsRoutingAssertion serverJmsRoutingAssertion ) {
            if (serverJmsRoutingAssertion == null)
                throw new IllegalArgumentException("serverJmsRoutingAssertion must not be null");
            this.serverJmsRoutingAssertion = serverJmsRoutingAssertion;
        }

        @Override
        public void onApplicationEvent( final ApplicationEvent applicationEvent ) {
            if (applicationEvent instanceof EntityInvalidationEvent) {
                EntityInvalidationEvent eie = (EntityInvalidationEvent) applicationEvent;

                JmsConnection connection;
                JmsEndpoint endpoint;
                synchronized (serverJmsRoutingAssertion.jmsInfoSync) {
                    connection = serverJmsRoutingAssertion.routedRequestConnection;
                    endpoint = serverJmsRoutingAssertion.routedRequestEndpoint;
                }

                if (connection != null && endpoint != null) {
                    boolean updated = false;
                    if (JmsConnection.class.isAssignableFrom(eie.getEntityClass())) {
                        for(Goid invalidatedId : eie.getEntityIds()) {
                            if (connection.getGoid().equals(invalidatedId))
                                updated = true;
                        }
                    }
                    if (JmsEndpoint.class.isAssignableFrom(eie.getEntityClass())) {
                        for(Goid invalidatedId : eie.getEntityIds()) {
                            if (endpoint.getGoid().equals(invalidatedId))
                                updated = true;
                        }
                    }

                    if (updated) {
                        if (serverJmsRoutingAssertion.markUpdate(true) ) {
                            if (logger.isLoggable(Level.INFO)) {
                                logger.log(Level.CONFIG, "Flagging JMS information for update [conn:{0}; epnt:{1}].",
                                    new Object[]{connection.getGoid(),endpoint.getGoid()});
                            }
                        }
                    }
                }
            }
        }
    }
    
}
