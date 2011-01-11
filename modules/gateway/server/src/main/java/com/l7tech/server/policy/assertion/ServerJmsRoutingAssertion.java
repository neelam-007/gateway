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
import com.l7tech.policy.JmsDynamicProperties;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.transport.jms.*;
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

import static com.l7tech.server.ServerConfig.PARAM_JMS_MESSAGE_MAX_BYTES;

/**
 * Server side implementation of JMS routing assertion.
 */
public class ServerJmsRoutingAssertion extends ServerRoutingAssertion<JmsRoutingAssertion> {
    private static final Logger logger = Logger.getLogger(ServerJmsRoutingAssertion.class.getName());
    private static final int MAX_OOPSES = 5;
    private static final long RETRY_DELAY = 1000;

    private final ApplicationContext spring;
    private final Auditor auditor;
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

    public ServerJmsRoutingAssertion(JmsRoutingAssertion data, ApplicationContext spring) {
        super(data, spring, logger);
        this.spring = spring;
        this.auditor = new Auditor(this, spring, logger);
        this.serverConfig = spring.getBean("serverConfig", ServerConfig.class);
        this.jmsEndpointManager = (JmsEndpointManager)spring.getBean("jmsEndpointManager");
        this.jmsConnectionManager = (JmsConnectionManager)spring.getBean("jmsConnectionManager");
        this.stashManagerFactory = spring.getBean("stashManagerFactory", StashManagerFactory.class);
        this.jmsPropertyMapper = spring.getBean("jmsPropertyMapper", JmsPropertyMapper.class);
        this.jmsResourceManager = spring.getBean("jmsResourceManager", JmsResourceManager.class);
        ApplicationEventProxy aep = spring.getBean("applicationEventProxy", ApplicationEventProxy.class);
        aep.addApplicationListener(new JmsInvalidator(this));
        SignerInfo signerInfo = null;
        try {
            DefaultKey ku = spring.getBean("defaultKey", DefaultKey.class);
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
                    auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NO_SAML_SIGNER);
                    return AssertionStatus.FAILED;
                }
                doAttachSamlSenderVouches(requestMessage, context.getDefaultAuthenticationContext().getLastCredentials(), senderVouchesSignerInfo);
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

            int oopses = 0;

            // Get the current JmsEndpointConfig
            cfg = getEndpointConfig(dynamicPropsWithValues);

            // Destinations are cached for retries
            final Destination[] outboundDestinationHolder = new Destination[1];
            final Destination[] inboundDestinationHolder = new Destination[1];

            // Message send retry loop.
            // Once the message is sent, there are no more retries
            while ( true ) {
                final JmsRoutingCallback jrc = new JmsRoutingCallback(context, cfg, outboundDestinationHolder, inboundDestinationHolder);
                try {
                    jmsResourceManager.doWithJmsResources( cfg, jrc );
                    jrc.doException();
                    break; // no error
                } catch (JmsRuntimeException jre) {
                    if ( jrc.isMessageSent() ) {
                        throw jre.getCause() != null ? jre.getCause() : jre;
                    }

                    if (++oopses < MAX_OOPSES) {
                        auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(RETRY_DELAY)}, ExceptionUtils.getDebugException(jre));
                        jmsResourceManager.invalidate(cfg);

                        try {
                            Thread.sleep(RETRY_DELAY);
                        } catch ( InterruptedException e ) {
                            throw new JMSException("Interrupted during retry delay");
                        }
                    } else {
                        auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_NOMORETRIES, String.valueOf(MAX_OOPSES));
                        // Catcher will log/audit the stack trace
                        throw jre.getCause() != null ? jre.getCause() : jre;
                    }
                } catch (JMSException e) {
                    if ( jrc.isMessageSent() ) throw e;

                    if (++oopses < MAX_OOPSES) {
                        if (ExceptionUtils.causedBy(e, InvalidDestinationException.class)) {
                            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(RETRY_DELAY)}, ExceptionUtils.getDebugException(e));
                        } else {
                            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(RETRY_DELAY)}, e);
                        }
                        jmsResourceManager.invalidate(cfg);
                        outboundDestinationHolder[0] = null;
                        inboundDestinationHolder[0] = null;

                        try {
                            Thread.sleep(RETRY_DELAY);
                        } catch ( InterruptedException ie ) {
                            throw new JMSException("Interrupted during send retry");
                        }
                    } else {
                        auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_NOMORETRIES, String.valueOf(MAX_OOPSES));
                        // Catcher will log/audit the stack trace
                        throw e;
                    }
                } catch (NamingException nex) {
                    if ( jrc.isMessageSent() ) {
                        throw nex; // this is an error, there should be no possibility of a NamingException after message send
                    }

                    // there is a chance that the LDAP connection will timeout when connecting to a
                    // MQSeries provider with LDAP -
                    if (++oopses < MAX_OOPSES && nex instanceof CommunicationException) {
                        jmsResourceManager.invalidate(cfg);
                        outboundDestinationHolder[0] = null;
                        inboundDestinationHolder[0] = null;
                    } else {
                        if (oopses >= MAX_OOPSES)
                            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_NOMORETRIES, String.valueOf(MAX_OOPSES));
                        final NamingException auditException = JmsUtil.isCausedByExpectedNamingException( nex ) ? null : nex;
                        auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Error in outbound JMS request processing: " + JmsUtil.getJNDIErrorMessage( nex )}, auditException );
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
            String msg = "Caught FindException";
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{msg}, e);
            return AssertionStatus.FAILED;
        } catch ( JmsConfigException e ) {
            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CONFIGURATION_ERROR,
                    new String[]{ExceptionUtils.getMessage(e)}, 
                    ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        } catch ( AssertionStatusException e ) {
            throw e;
        } catch ( Throwable t ) {
            auditException( "Caught unexpected Throwable in outbound JMS request processing", t );
            if (cfg!=null) jmsResourceManager.invalidate(cfg);
            return AssertionStatus.SERVER_ERROR;
        } finally {
            if (context.getRoutingEndTime() == 0) context.routingFinished();
        }
    }

    private void auditException( final String description, final Throwable throwable ) {
        final Throwable auditException = JmsUtil.isCausedByExpectedJMSException( throwable ) ? null : throwable;
        final JMSException jmsException = ExceptionUtils.getCauseIfCausedBy( throwable, JMSException.class );

        String exceptionMessage = jmsException==null ?
            ExceptionUtils.getMessage( throwable ) :
            JmsUtil.getJMSErrorMessage( jmsException );

        if ( throwable != jmsException ) {
            exceptionMessage = ExceptionUtils.getMessage( throwable ) + "; " + exceptionMessage;    
        }

        auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ description + ": " + exceptionMessage }, auditException );
    }

    private final class JmsRoutingCallback implements JmsResourceManager.JmsResourceCallback {
        private final JmsEndpointConfig cfg;
        private final PolicyEnforcementContext context;
        private final com.l7tech.message.Message requestMessage;
        private final Destination[] jmsOutboundDestinationHolder;
        private final Destination[] jmsInboundDestinationHolder;
        private Exception exception;
        private boolean messageSent = false;

        private JmsRoutingCallback ( final PolicyEnforcementContext context,
                                     final JmsEndpointConfig cfg,
                                     final Destination[] jmsOutboundDestinationHolder,
                                     final Destination[] jmsInboundDestinationHolder ) {
            this.context = context;
            this.cfg = cfg;
            this.jmsOutboundDestinationHolder = jmsOutboundDestinationHolder;
            this.jmsInboundDestinationHolder = jmsInboundDestinationHolder;
            try {
                this.requestMessage = context.getTargetMessage(assertion.getRequestTarget());
            } catch (NoSuchVariableException e) {
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
            }
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
        public void doWork( final Connection connection,
                            final Session jmsSession,
                            final JmsResourceManager.JndiContextProvider jndiContextProvider ) {
            try {
                final Message jmsOutboundRequest = makeRequest(context, jmsSession, jndiContextProvider, cfg);

                if (jmsOutboundDestinationHolder[0] == null)
                    jmsOutboundDestinationHolder[0] = getRoutedRequestDestination(jndiContextProvider, cfg);
                if (jmsInboundDestinationHolder[0] == null)
                    jmsInboundDestinationHolder[0] = jmsOutboundRequest.getJMSReplyTo();

                final Destination jmsOutboundDestination = jmsOutboundDestinationHolder[0];
                final Destination jmsInboundDestination = jmsInboundDestinationHolder[0];

                // Enforces rules on propagation of request JMS message properties.
                final JmsKnob jmsInboundKnob = requestMessage.getKnob(JmsKnob.class);
                Map<String, Object> inboundRequestProps;
                if ( jmsInboundKnob != null ) {
                    inboundRequestProps = jmsInboundKnob.getJmsMsgPropMap();
                } else {
                    inboundRequestProps = new HashMap<String, Object>();
                }
                final Map<String, Object> outboundRequestProps = new HashMap<String, Object>();
                enforceJmsMessagePropertyRuleSet(context, assertion.getRequestJmsMessagePropertyRuleSet(), inboundRequestProps, outboundRequestProps);
                for ( String name : outboundRequestProps.keySet() ) {
                    jmsOutboundRequest.setObjectProperty(name, outboundRequestProps.get(name));
                }

                boolean processReply = context.isReplyExpected() && jmsInboundDestination != null;

                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_ROUTED);

                MessageProducer jmsProducer = null;
                try {
                    jmsProducer = JmsUtil.createMessageProducer( jmsSession, jmsOutboundDestination );

                    context.routingStarted();

                    if ( logger.isLoggable( Level.FINE ))
                        logger.fine("Sending JMS outbound message");

                    jmsProducer.send( jmsOutboundRequest );
                    messageSent = true; // no retries once sent

                    if ( logger.isLoggable( Level.FINE ))
                        logger.fine("JMS outbound message sent");
                } finally {
                   if (jmsProducer != null) jmsProducer.close();
                }

                if ( !processReply ) {
                    context.routingFinished();
                    auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NO_RESPONSE_EXPECTED);
                    context.setRoutingStatus( RoutingStatus.ROUTED );
                } else {
                    final String selector = getSelector( jmsOutboundRequest, cfg.getEndpoint() );
                    int emergencyTimeoutDefault = 10000;
                    String timeoutStr = assertion.getResponseTimeout();
                    int timeout;
                    if (timeoutStr == null) {
                        timeout = serverConfig.getIntProperty(ServerConfig.PARAM_JMS_RESPONSE_TIMEOUT, emergencyTimeoutDefault);
                    }
                    else  {
                        try {
                            timeout = Integer.parseInt(timeoutStr);
                            if (timeout <= 0){
                                timeout = serverConfig.getIntProperty(ServerConfig.PARAM_JMS_RESPONSE_TIMEOUT,emergencyTimeoutDefault);
                                logger.info("Using server default value (" + timeout + ") for JMS response timeout.");
                            }
                        } catch (NumberFormatException e) {
                            // do nothing 
                        }

                        // try resolving context var
                        timeoutStr = ExpandVariables.process(timeoutStr,context.getVariableMap( assertion.getVariablesUsed(), auditor),auditor);
                        try {
                            timeout = Integer.parseInt(timeoutStr);
                            if (timeout <= 0){
                                timeout = serverConfig.getIntProperty(ServerConfig.PARAM_JMS_RESPONSE_TIMEOUT,emergencyTimeoutDefault);
                                logger.info("Using server default value (" + timeout + ") for JMS response timeout.");
                            }
                        } catch (NumberFormatException e) {

                            timeout = emergencyTimeoutDefault;
                            logger.warning("Using default value (" + emergencyTimeoutDefault + ") for undefined cluster property: " + serverConfig.getClusterPropertyName(ServerConfig.PARAM_JMS_RESPONSE_TIMEOUT));
                        }
                    }
                    MessageConsumer jmsConsumer = null;
                    final Message jmsResponse;
                    try {
                        jmsConsumer = JmsUtil.createMessageConsumer( jmsSession, jmsInboundDestination, selector );

                        auditor.logAndAudit(AssertionMessages.JMS_ROUTING_GETTING_RESPONSE);
                        jmsResponse = jmsConsumer.receive( timeout );

                        if ( jmsResponse != null && !(jmsInboundDestination instanceof TemporaryQueue))
                            jmsResponse.acknowledge();

                    } finally {
                        if ( jmsConsumer != null ) jmsConsumer.close();
                    }

                    context.routingFinished();
                    if ( jmsResponse == null ) {
                        auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NO_RESPONSE, String.valueOf(timeout));
                        throw new AssertionStatusException(AssertionStatus.FAILED);
                    }

                    final com.l7tech.message.Message responseMessage;
                    try {
                        responseMessage = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
                    } catch (NoSuchVariableException e) {
                        throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);

                    }

                    long maxBytes = 0;
                    if (assertion.getResponseSize() <0){
                        maxBytes = serverConfig.getLongProperty(PARAM_JMS_MESSAGE_MAX_BYTES, 2621440);
                    }
                    else{
                        maxBytes = assertion.getResponseSize();
                    }
                    if ( jmsResponse instanceof TextMessage ) {
                        responseMessage.initialize(XmlUtil.stringToDocument( ((TextMessage)jmsResponse).getText() ),maxBytes);
                    } else if ( jmsResponse instanceof BytesMessage ) {
                        BytesMessage bytesMessage = (BytesMessage)jmsResponse;
                        final StashManager stashManager = stashManagerFactory.createStashManager();
                        responseMessage.initialize(stashManager, ContentTypeHeader.XML_DEFAULT, new BytesMessageInputStream(bytesMessage),maxBytes);
                    } else {
                        auditor.logAndAudit(AssertionMessages.JMS_ROUTING_UNSUPPORTED_RESPONSE_MSG_TYPE, jmsResponse.getClass().getName());
                        throw new AssertionStatusException(AssertionStatus.FAILED);
                    }
                    auditor.logAndAudit(AssertionMessages.JMS_ROUTING_GOT_RESPONSE);

                    // Copies the response JMS message properties into the response JmsKnob.
                    // Do this before enforcing the propagation rules so that they will
                    // be available as context variables.
                    final Map<String, Object> inResJmsMsgProps = new HashMap<String, Object>();
                    for (Enumeration e = jmsResponse.getPropertyNames(); e.hasMoreElements() ;) {
                        final String name = (String)e.nextElement();
                        final Object value = jmsResponse.getObjectProperty(name);
                        inResJmsMsgProps.put(name, value);
                    }

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
                        public long getServiceOid() {
                            return 0;
                        }
                    });

                    final Map<String, Object> outResJmsMsgProps = new HashMap<String, Object>();
                    enforceJmsMessagePropertyRuleSet(context, assertion.getResponseJmsMessagePropertyRuleSet(), inResJmsMsgProps, outResJmsMsgProps);
                    // After enforcing propagation rules, replace the JMS message properties
                    // in the response JmsKnob with enforced/expanded values.
                    responseMessage.getJmsKnob().getJmsMsgPropMap().clear();
                    responseMessage.getJmsKnob().getJmsMsgPropMap().putAll(outResJmsMsgProps);

                    context.setRoutingStatus( RoutingStatus.ROUTED );

                    // todo: move to abstract routing assertion
                    requestMessage.notifyMessage(responseMessage, MessageRole.RESPONSE);
                    responseMessage.notifyMessage(requestMessage, MessageRole.REQUEST);
                }
            } catch ( JMSException e ) {
                exception = e;
            } catch ( SAXException e ) {
                exception = e;
            } catch ( IOException e ) {
                exception = e;
            } catch ( NamingException e ) {
                exception = e;
            } finally {
                if ( closeDestinationIfTemporaryQueue( jmsInboundDestinationHolder[0] ) ) {
                    jmsInboundDestinationHolder[0] = null;
                }
            }
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

    private boolean closeDestinationIfTemporaryQueue( final Destination destination ) {
        boolean closed = false;

        if ( destination instanceof TemporaryQueue ) {
            closed = true;
            auditor.logAndAudit( AssertionMessages.JMS_ROUTING_DELETE_TEMPORARY_QUEUE );
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

        long maxSize = serverConfig.getLongPropertyCached( "ioJmsMessageMaxBytes", 5242880, 30000L );
        final MimeKnob mk = message.getKnob(MimeKnob.class);

        if (mk == null) {
            // Uninitialized request
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Request is not initialized; nothing to route");
            return false;
        }

        if ( maxSize > 0 && mk.getContentLength() > maxSize ) {
            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_TOO_LARGE);
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
        BufferPoolByteArrayOutputStream outputStream = new BufferPoolByteArrayOutputStream();
        final byte[] outboundRequestBytes;
        com.l7tech.message.Message requestMessage;
        try {
            requestMessage = context.getTargetMessage(assertion.getRequestTarget());
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }
        final MimeKnob mk = requestMessage.getMimeKnob();
        try {
            IOUtils.copyStream(mk.getEntireMessageBodyAsInputStream(), outputStream);
            outboundRequestBytes = outputStream.toByteArray();
        } catch (NoSuchPartException e) {
            throw new CausedIOException("Couldn't read from JMS request"); // can't happen
        } finally {
            outputStream.close();
        }

        final JmsOutboundMessageType outboundType = outboundRequestEndpoint.getOutboundMessageType();
        boolean useTextMode = outboundType.isDefaultsToText();
        if (outboundType.isCopyRequestType() && requestMessage.getKnob(JmsKnob.class) != null) {
            // Outgoing request should be the same type (i.e. TextMessage or BytesMessage) as the original request
            useTextMode = !requestMessage.getJmsKnob().isBytesMessage();
        }

        if (useTextMode) {
            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CREATE_REQUEST_AS_TEXT_MESSAGE);
            // TODO get encoding from mk?
            outboundRequestMsg = jmsSession.createTextMessage(new String(outboundRequestBytes, JmsUtil.DEFAULT_ENCODING));
        } else {
            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CREATE_REQUEST_AS_BYTES_MESSAGE);
            BytesMessage bytesMessage = jmsSession.createBytesMessage();
            bytesMessage.writeBytes(outboundRequestBytes);
            outboundRequestMsg = bytesMessage;
        }

        final JmsReplyType replyType = outboundRequestEndpoint.getReplyType();
        switch (replyType) {
            case NO_REPLY:
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_WITH_NO_REPLY, outboundRequestEndpoint.getDestinationName());
                return outboundRequestMsg;
            case AUTOMATIC:
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_WITH_AUTOMATIC, outboundRequestEndpoint.getDestinationName());
                outboundRequestMsg.setJMSReplyTo(jmsSession.createTemporaryQueue());
                return outboundRequestMsg;
            case REPLY_TO_OTHER:
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_WITH_REPLY_TO_OTHER, outboundRequestEndpoint.getDestinationName(), outboundRequestEndpoint.getReplyToQueueName());

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

    private Destination getRoutedRequestDestination( final JmsResourceManager.JndiContextProvider jndiContextProvider,
                                                     final JmsEndpointConfig cfg) throws JMSException, NamingException {
        return JmsUtil.cast( jndiContextProvider.lookup(cfg.getEndpoint().getDestinationName()), Destination.class );
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
                    auditor.logAndAudit(AssertionMessages.JMS_ROUTING_MISSING_MESSAGE_ID);
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
            jmsEndpoint = jmsEndpointManager.findByPrimaryKey(assertion.getEndpointOid());
            synchronized(jmsInfoSync) {
                routedRequestEndpoint = jmsEndpoint;
            }
        }
        return jmsEndpoint;
    }

    private JmsConnection getRoutedRequestConnection( final JmsEndpoint endpoint,
                                                      final Auditor auditor) throws FindException {
        JmsConnection jmsConn;
        synchronized(jmsInfoSync) {
            jmsConn = routedRequestConnection;
        }

        if ( jmsConn == null ) {
            if ( endpoint == null ) {
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NON_EXISTENT_ENDPOINT,
                        String.valueOf(assertion.getEndpointOid()) + "/" + assertion.getEndpointName());
            } else {
                jmsConn = jmsConnectionManager.findByPrimaryKey( endpoint.getConnectionOid() );
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

        final JmsConnection conn = getRoutedRequestConnection(endpoint, auditor);
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
            auditor.logAndAudit( AssertionMessages.JMS_ROUTING_TEMPLATE_ERROR, "invalid configuration; " + e.getMessage() );
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

        final Map<String,Object> variables = pec.getVariableMap( assertion.getVariablesUsed(), auditor);
        try {
            jmsDynamicProperties.setDestQName( expandVariables( unprocessedProperties.getDestQName(), variables ) );
            jmsDynamicProperties.setReplytoQName( expandVariables( unprocessedProperties.getReplytoQName(), variables ) );
            jmsDynamicProperties.setJndiUrl( expandVariables( unprocessedProperties.getJndiUrl(), variables ) );
            jmsDynamicProperties.setIcfName( expandVariables( unprocessedProperties.getIcfName(), variables ) );
            jmsDynamicProperties.setQcfName( expandVariables( unprocessedProperties.getQcfName(), variables ) );
        } catch ( IllegalArgumentException iae ) {
            auditor.logAndAudit( AssertionMessages.JMS_ROUTING_TEMPLATE_ERROR, "variable processing error; " + iae.getMessage() );
            throw new AssertionStatusException( AssertionStatus.FAILED );
        }

        return jmsDynamicProperties;
    }

    private String expandVariables( final String value, final Map<String,Object> variableMap ) {
        String expandedValue = value;

        if ( expandedValue != null && expandedValue.contains( Syntax.SYNTAX_PREFIX ) ) {
            expandedValue = ExpandVariables.process(value, variableMap, auditor, true);
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
            final Map<String, Object> vars = context.getVariableMap(variablesUsed, auditor);

            for (JmsMessagePropertyRule rule : ruleSet.getRules()) {
                final String name = rule.getName();
                if (rule.isPassThru()) {
                    if (src.containsKey(name) && !name.startsWith("JMS_") && !name.startsWith("JMSX")) {
                        dst.put(name, src.get(name));
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.finest("Propagating a JMS message property with pass through. (name=" + name + ", value=" + src.get(name) + ")");
                        }
                    }
                } else {
                    final String pattern = rule.getCustomPattern();
                    final String value = ExpandVariables.process(pattern, vars, auditor);
                    dst.put(name, value);
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest("Propagating a JMS message property with custom pattern (name=" + name + ", pattern=" + pattern + ", value=" + value + ")");
                    }
                }
            }
        }
    }

    /**
     * Invalidation listener for JMS endpoint / connection updates.
     */
    public static final class JmsInvalidator implements ApplicationListener {
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
                        for(long invalidatedId : eie.getEntityIds()) {
                            if (connection.getOid() == invalidatedId)
                                updated = true;
                        }
                    }
                    if (JmsEndpoint.class.isAssignableFrom(eie.getEntityClass())) {
                        for(long invalidatedId : eie.getEntityIds()) {
                            if (endpoint.getOid() == invalidatedId)
                                updated = true;
                        }
                    }

                    if (updated) {
                        if (serverJmsRoutingAssertion.markUpdate(true) ) {
                            if (logger.isLoggable(Level.INFO)) {
                                logger.log(Level.CONFIG, "Flagging JMS information for update [conn:{0}; epnt:{1}].",
                                    new Object[]{Long.toString(connection.getOid()), Long.toString(endpoint.getOid())});
                            }
                        }
                    }
                }
            }
        }
    }
    
}
