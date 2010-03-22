/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */

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

import javax.jms.*;
import javax.jms.Message;
import javax.jms.Queue;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final SignerInfo senderVouchesSignerInfo;

    private final Object needsUpdateSync = new Object();
    private boolean needsUpdate;
    private final Object jmsInfoSync = new Object();
    private JmsConnection routedRequestConnection;
    private JmsEndpoint routedRequestEndpoint;
    private JmsEndpointConfig endpointConfig;

    public ServerJmsRoutingAssertion(JmsRoutingAssertion data, ApplicationContext spring) {
        super(data, spring, logger);
        this.spring = spring;
        this.auditor = new Auditor(this, spring, logger);
        this.serverConfig = (ServerConfig)spring.getBean("serverConfig", ServerConfig.class);
        this.jmsEndpointManager = (JmsEndpointManager)spring.getBean("jmsEndpointManager");
        this.jmsConnectionManager = (JmsConnectionManager)spring.getBean("jmsConnectionManager");
        this.stashManagerFactory = (StashManagerFactory)spring.getBean("stashManagerFactory", StashManagerFactory.class);
        this.jmsPropertyMapper = (JmsPropertyMapper)spring.getBean("jmsPropertyMapper", JmsPropertyMapper.class);
        ApplicationEventProxy aep = (ApplicationEventProxy) spring.getBean("applicationEventProxy", ApplicationEventProxy.class);
        aep.addApplicationListener(new JmsInvalidator(this));
        SignerInfo signerInfo = null;
        try {
            DefaultKey ku = (DefaultKey)spring.getBean("defaultKey", DefaultKey.class);
            signerInfo = ku.getSslInfo();
        } catch(Exception e) {
            logger.log(Level.WARNING, "Error getting SAML signer information.", e);
        }
        this.senderVouchesSignerInfo = signerInfo;
        this.needsUpdate = false;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException {

        context.setRoutingStatus(RoutingStatus.ATTEMPTED);
        if ( !isValidRequest(context) ) {
            return AssertionStatus.BAD_REQUEST;
        }

        final com.l7tech.message.Message requestMessage = context.getRequest();
        JmsBag jmsBag = null;
        JmsEndpointConfig cfg = null;

        try {
            Session jmsSession;
            int oopses = 0;

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
            JmsDynamicProperties dynamicProps = getDynamicRoutingProps();
            JmsDynamicProperties dynamicPropsWithValues = null;
            if (dynamicProps != null) {
                dynamicPropsWithValues = getContextVariablesValues(context, dynamicProps);
            }

            // Get JMS Session
            while (true) {
                try {
                    if (markedForUpdate()) {
                        try {
                            logger.info("JMS information needs update, closing session (if open).");

                            resetEndpointInfo();
                            dynamicProps = getDynamicRoutingProps();
                            dynamicPropsWithValues = null;
                            if (dynamicProps != null) {
                                dynamicPropsWithValues = getContextVariablesValues(context, dynamicProps);
                            }
                        } finally {
                            markUpdate(false);
                        }
                    }

                    // Get the current JmsEndpointConfig
                    cfg = getEndpointConfig(dynamicPropsWithValues);
                    jmsBag = getJmsBag(cfg);
                    jmsSession = jmsBag.getSession();
                    break; // if successful, no need for further retries
                } catch (FindException e) {
                    JMSException jmsException = new JMSException("Failed to lookup the JMS endpoint or connector");
                    jmsException.setLinkedException(e);
                    throw jmsException;
                } catch (Throwable t) {
                    if (++oopses < MAX_OOPSES) {
                        auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(RETRY_DELAY)}, ExceptionUtils.getDebugException(t));
                        closeBagDueToError(cfg, jmsBag);

                        try {
                            Thread.sleep(RETRY_DELAY);
                        } catch ( InterruptedException e ) {
                            throw new JMSException("Interrupted during retry delay");
                        }
                    } else {
                        auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_NOMORETRIES, String.valueOf(MAX_OOPSES));
                        // Catcher will log/audit the stack trace
                        throw t;
                    }
                }
            }

            if ( jmsSession == null ) {
                String msg = "Null session escaped from retry loop!";
                throw new PolicyAssertionException(assertion, msg);
            }

            // Message send retry loop. Reuses oops count from before
            // once the message is sent, there are no more retries
            boolean messageSent = false;
            Destination jmsOutboundDest = null;
            Destination jmsInboundDest = null;
            while ( true ) {
                try {
                    Message jmsOutboundRequest = makeRequest(context, jmsBag, cfg);

                    if (jmsOutboundDest == null)
                        jmsOutboundDest = getRoutedRequestDestination(jmsBag, cfg);
                    if (jmsInboundDest == null)
                        jmsInboundDest = jmsOutboundRequest.getJMSReplyTo();

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

                    boolean inbound = context.isReplyExpected() && jmsInboundDest != null;

                    auditor.logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_ROUTED);

                    final JmsEndpoint routedRequestEndpoint1 = cfg.getEndpoint();
                    MessageProducer jmsProducer = null;
                    try {
                        if ( jmsSession instanceof QueueSession ) {
                            if ( !(jmsOutboundDest instanceof Queue ) ) throw new PolicyAssertionException(assertion, "Destination/Session type mismatch" );
                            // the reason for this distinction is that IBM throws java.lang.AbstractMethodError: com.ibm.mq.jms.MQQueueSession.createProducer(Ljavax/jms/Destination;)Ljavax/jms/MessageProducer;
                            jmsProducer = ((QueueSession)jmsSession).createSender( (Queue)jmsOutboundDest );
                        } else if ( jmsSession instanceof TopicSession && routedRequestEndpoint1.getReplyType() != JmsReplyType.NO_REPLY) {
                            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NO_TOPIC_WITH_REPLY);
                            return AssertionStatus.NOT_APPLICABLE;
                        } else {
                            jmsProducer = jmsSession.createProducer( jmsOutboundDest );
                        }

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

                    final String selector;
                    if (routedRequestEndpoint1.getReplyType() == JmsReplyType.AUTOMATIC) {
                        if ( logger.isLoggable( Level.FINE ))
                            logger.fine("Response expected on temporary queue; not using selector");
                        selector = null;
                    } else {
                        final StringBuilder sb = new StringBuilder("JMSCorrelationID = '");
                        if (routedRequestEndpoint.isUseMessageIdForCorrelation()) {
                            final String id = jmsOutboundRequest.getJMSMessageID();
                            if (id == null) throw new PolicyAssertionException(assertion, "Sent message had no message ID");
                            sb.append(id);
                        } else {
                            sb.append(jmsOutboundRequest.getJMSCorrelationID());
                        }
                        sb.append("'");
                        selector = sb.toString();
                    }

                    if (inbound) {
                        if ( logger.isLoggable( Level.FINE )) {
                            if (selector != null) {
                                logger.fine("Filtering on correlation id " + selector);
                            } else {
                                logger.fine("Not using a selector");
                            }
                        }

                        int timeout = assertion.getResponseTimeout();
                        MessageConsumer jmsConsumer = null;
                        final Message jmsResponse;
                        try {
                            if (jmsSession instanceof QueueSession) {
                                jmsConsumer = ((QueueSession)jmsSession).createReceiver((Queue)jmsInboundDest, selector);
                            } else {
                                jmsConsumer = jmsSession.createConsumer(jmsInboundDest, selector);
                            }

                            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_GETTING_RESPONSE);
                            jmsResponse = jmsConsumer.receive( timeout );

                            if ( jmsResponse != null && !(jmsInboundDest instanceof TemporaryQueue))
                                jmsResponse.acknowledge();

                        } finally {
                            if ( jmsConsumer != null ) jmsConsumer.close();
                        }

                        context.routingFinished();
                        if ( jmsResponse == null ) {
                            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NO_RESPONSE, String.valueOf(timeout));
                            return AssertionStatus.FAILED;
                        }

                        com.l7tech.message.Message responseMessage = context.getResponse();
                        if ( jmsResponse instanceof TextMessage ) {
                            responseMessage.initialize(XmlUtil.stringToDocument( ((TextMessage)jmsResponse).getText() ));
                        } else if ( jmsResponse instanceof BytesMessage ) {
                            BytesMessage bmsg = (BytesMessage)jmsResponse;
                            final StashManager stashManager = stashManagerFactory.createStashManager();
                            responseMessage.initialize(stashManager, ContentTypeHeader.XML_DEFAULT, new BytesMessageInputStream(bmsg));
                        } else {
                            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_UNSUPPORTED_RESPONSE_MSG_TYPE, jmsResponse.getClass().getName());
                            return AssertionStatus.FAILED;
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

                        return AssertionStatus.NONE;
                    } else {
                        context.routingFinished();
                        auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NO_RESPONSE_EXPECTED);
                        context.setRoutingStatus( RoutingStatus.ROUTED );
                        return AssertionStatus.NONE;
                    }
                } catch (JMSException jmse) {
                    if ( messageSent ) throw jmse;

                    if (++oopses < MAX_OOPSES) {
                        if (ExceptionUtils.causedBy(jmse, InvalidDestinationException.class)) {
                            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(RETRY_DELAY)}, ExceptionUtils.getDebugException(jmse));
                        } else {
                            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(RETRY_DELAY)}, jmse);
                        }
                        closeDestinationIfTemporaryQueue( jmsInboundDest );
                        jmsInboundDest = null;
                        closeBagDueToError(cfg, jmsBag);

                        try {
                            Thread.sleep(RETRY_DELAY);
                        } catch ( InterruptedException e ) {
                            throw new JMSException("Interrupted during send retry");
                        }

                        jmsBag = getJmsBag(cfg);
                        jmsSession = jmsBag.getSession();
                    } else {
                        auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_NOMORETRIES, String.valueOf(MAX_OOPSES));
                        // Catcher will log/audit the stack trace
                        throw jmse;
                    }
                } catch (NamingException nex) {

                    // there is a chance that the LDAP connection will timeout when connecting to a
                    // MQSeries provider with LDAP -
                    if (++oopses < MAX_OOPSES && nex instanceof CommunicationException) {

//                        auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_RETRYING, new String[] {String.valueOf(oopses), String.valueOf(RETRY_DELAY)}, nex);
                        closeDestinationIfTemporaryQueue( jmsInboundDest );
                        jmsInboundDest = null;
                        closeBagDueToError(cfg, jmsBag);

                        jmsBag = getJmsBag(cfg);
                        jmsSession = jmsBag.getSession();

                    } else {
                        if (oopses >= MAX_OOPSES)
                            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CANT_CONNECT_NOMORETRIES, String.valueOf(MAX_OOPSES));
                        throw nex;
                    }
                } finally {
                    closeDestinationIfTemporaryQueue( jmsInboundDest );
                }
            }
        } catch ( NamingException e ) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Error in outbound JMS request processing"}, e );
            return AssertionStatus.FAILED;
        } catch ( JMSException e ) {
            if (ExceptionUtils.causedBy(e, InvalidDestinationException.class)) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Error outbound JMS request processing"}, ExceptionUtils.getDebugException(e) );
            } else {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Error outbound JMS request processing"}, e );
            }

            closeBagDueToError(cfg, jmsBag);
            return AssertionStatus.FAILED;
        } catch ( FindException e ) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
            String msg = "Caught FindException";
            throw new PolicyAssertionException(assertion, msg, e);
        } catch ( JmsConfigException e ) {
            String msg = "Invalid JMS configuration";
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{msg}, e);
            throw new PolicyAssertionException(assertion, msg, e);
        } catch ( Throwable t ) {
            if (ExceptionUtils.causedBy(t, InvalidDestinationException.class)) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, "Caught unexpected Throwable in outbound JMS request processing: " + t.getMessage());
            } else {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{"Caught unexpected Throwable in outbound JMS request processing"}, t );
            }

            closeBagDueToError(cfg, jmsBag);
            return AssertionStatus.SERVER_ERROR;
        } finally {
            if (context.getRoutingEndTime() == 0) context.routingFinished();

            // we need to release the Jms Session
            closeBag(cfg, jmsBag);
        }
    }


    /**
     * Returns whether the configured JMS endpoint is setup for dynamic routing (i.e. uses context variables).
     *
     * This method will potentially access the Jms tables in the database, so it should only be called once per
     * checkRequest call to find the value at time of request.
     *
     * @return the Dynamically configured JMS endpoint properties, if configured, Null otherwise
     */
    private JmsDynamicProperties getDynamicRoutingProps()  {
        return assertion.getDynamicJmsRoutingProperties();


//        final String SPACE = " ";
//        StringBuffer sb = new StringBuffer();
//        JmsEndpoint endpt = getRoutedRequestEndpoint();
//        JmsConnection conn = getRoutedRequestConnection(endpt, auditor);
//
//        // get all 5 possible context variable values
//        sb.append(conn.getInitialContextFactoryClassname()).append(SPACE);
//        sb.append(conn.getJndiUrl()).append(SPACE);
//        sb.append(conn.getQueueFactoryUrl()).append(SPACE);
//        sb.append(endpt.getDestinationName()).append(SPACE);
//        sb.append(endpt.getReplyToQueueName());
//
//        String[] vars = Syntax.getReferencedNames(sb.toString());
//        if (vars.length > 0) {
//            return new Pair<Boolean, String[]>(true, vars);
//        }
//        return new Pair<Boolean, String[]>(false, new String[0]);
    }


    private boolean markedForUpdate() {
        boolean updatedRequired;

        synchronized(needsUpdateSync) {
            updatedRequired = needsUpdate;
        }

        return updatedRequired;        
    }

    private void markUpdate(boolean needsUpdate) {
        synchronized(needsUpdateSync) {
            this.needsUpdate = needsUpdate;
        }
    }

    private void closeBag(JmsEndpointConfig cfg, JmsBag bag) {
        if (bag != null) {
            JmsResourceManager.getInstance().release(cfg, bag, false);
        }
    }

    private final Object jmsConnSync = new Object();
    private int errorCloseCount;
    private void closeBagDueToError(JmsEndpointConfig cfg, JmsBag bag) {

        synchronized(jmsConnSync) {
            if (bag != null) {

                if (++errorCloseCount >= MAX_OOPSES-1) {
                    JmsResourceManager.getInstance().release(cfg, bag, true);
                    errorCloseCount = 0;
                }
                else {
                    closeBag(cfg, bag);
                }
            }
        }
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

    private void closeDestinationIfTemporaryQueue( final Destination destination ) {
        if ( destination instanceof TemporaryQueue ) {
            auditor.logAndAudit( AssertionMessages.JMS_ROUTING_DELETE_TEMPORARY_QUEUE );
            try {
                ((TemporaryQueue)destination).delete();
            } catch (JMSException jmse) {
                logger.log( Level.WARNING, "Error closing temporary queue", jmse );
            }
        }
    }

    private boolean isValidRequest(PolicyEnforcementContext context) throws IOException {
        boolean valid = true;

        long maxSize = serverConfig.getLongPropertyCached( "ioJmsMessageMaxBytes", 5242880, 30000L );
        final MimeKnob mk = context.getRequest().getMimeKnob();
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
    private javax.jms.Message makeRequest(PolicyEnforcementContext context, JmsBag jmsBag, JmsEndpointConfig endpointCfg)
        throws IOException, JMSException, NamingException, JmsConfigException, FindException
    {
        final JmsEndpoint outboundRequestEndpoint = endpointCfg.getEndpoint();

        javax.jms.Message outboundRequestMsg;
        BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream();
        final byte[] outboundRequestBytes;
        final MimeKnob mk = context.getRequest().getMimeKnob();
        try {
            IOUtils.copyStream(mk.getEntireMessageBodyAsInputStream(), baos);
            outboundRequestBytes = baos.toByteArray();
        } catch (NoSuchPartException e) {
            throw new CausedIOException("Couldn't read from JMS request"); // can't happen
        } finally {
            baos.close();
        }

        final JmsOutboundMessageType outboundType = outboundRequestEndpoint.getOutboundMessageType();
        boolean useTextMode = outboundType.isDefaultsToText();
        if (outboundType.isCopyRequestType() && context.getRequest().getKnob(JmsKnob.class) != null) {
            // Outgoing request should be the same type (i.e. TextMessage or BytesMessage) as the original request
            useTextMode = !context.getRequest().getJmsKnob().isBytesMessage();
        }

        if (useTextMode) {
            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CREATE_REQUEST_AS_TEXT_MESSAGE);
            // TODO get encoding from mk?
            outboundRequestMsg = jmsBag.getSession().createTextMessage(new String(outboundRequestBytes, JmsUtil.DEFAULT_ENCODING));
        } else {
            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CREATE_REQUEST_AS_BYTES_MESSAGE);
            BytesMessage bmsg = jmsBag.getSession().createBytesMessage();
            bmsg.writeBytes(outboundRequestBytes);
            outboundRequestMsg = bmsg;
        }

        final JmsReplyType replyType = outboundRequestEndpoint.getReplyType();
        switch (replyType) {
            case NO_REPLY:
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_WITH_NO_REPLY, outboundRequestEndpoint.getDestinationName());
                return outboundRequestMsg;
            case AUTOMATIC:
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_WITH_AUTOMATIC, outboundRequestEndpoint.getDestinationName());
                outboundRequestMsg.setJMSReplyTo(jmsBag.getSession().createTemporaryQueue());
                return outboundRequestMsg;
            case REPLY_TO_OTHER:
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_WITH_REPLY_TO_OTHER, outboundRequestEndpoint.getDestinationName(), outboundRequestEndpoint.getReplyToQueueName());

                // set the replyTo queue
                String replyToQueueName = outboundRequestEndpoint.getReplyToQueueName();
                if (replyToQueueName == null || replyToQueueName.length() == 0)
                    throw new IllegalStateException("REPLY_TO_OTHER was selected, but no reply-to queue name was specified");
                outboundRequestMsg.setJMSReplyTo((Destination)jmsBag.getJndiContext().lookup(replyToQueueName));

                if (!routedRequestEndpoint.isUseMessageIdForCorrelation()) {
                    final String id = "L7_REQ_ID:" + context.getRequestId().toString();
                    outboundRequestMsg.setJMSCorrelationID(id);
                }
                return outboundRequestMsg;
            default:
                throw new java.lang.IllegalStateException("Unknown JmsReplyType " + replyType);
        }
    }

    private Destination getRoutedRequestDestination(JmsBag jmsBag, JmsEndpointConfig cfg) throws FindException, JMSException, NamingException, JmsConfigException {
        Context jndiContext = jmsBag.getJndiContext();
        return (Destination)jndiContext.lookup(cfg.getEndpoint().getDestinationName());
    }

    private JmsEndpoint getRoutedRequestEndpoint() throws FindException {
        if ( routedRequestEndpoint == null ) {
            JmsEndpoint jmsEndpoint = jmsEndpointManager.findByPrimaryKey(assertion.getEndpointOid());
            synchronized(jmsInfoSync) {
                routedRequestEndpoint = jmsEndpoint;
            }
        }
        return routedRequestEndpoint;
    }

    private JmsConnection getRoutedRequestConnection(JmsEndpoint endpoint, Auditor auditor) throws FindException {
        if ( routedRequestConnection == null ) {
            if ( endpoint == null ) {
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NON_EXISTENT_ENDPOINT,
                        String.valueOf(assertion.getEndpointOid()) + "/" + assertion.getEndpointName());
            } else {
                JmsConnection jmsConn = jmsConnectionManager.findByPrimaryKey( endpoint.getConnectionOid() );
                synchronized(jmsInfoSync) {
                    routedRequestConnection = jmsConn;
                }
            }
        }
        return routedRequestConnection;
    }

    private JmsEndpointConfig getEndpointConfig(JmsDynamicProperties ctxVariablesValues)
        throws FindException
    {
        if (ctxVariablesValues == null && endpointConfig != null) {
            return endpointConfig;
        }

        JmsEndpoint endpoint = getRoutedRequestEndpoint();
        if ( endpoint == null ) throw new FindException( "JmsEndpoint could not be located! It may have been deleted" );

        JmsConnection conn = getRoutedRequestConnection(endpoint, auditor);
        if ( conn == null ) throw new FindException( "JmsConnection could not be located! It may have been deleted" );

        // check for the need to use dynamic routing properties
        if (ctxVariablesValues!= null) {
            return new JmsEndpointConfig(conn, endpoint, jmsPropertyMapper, spring, ctxVariablesValues);
        } else {
            endpointConfig = new JmsEndpointConfig(conn, endpoint, jmsPropertyMapper, spring);
            return endpointConfig;
        }
    }

    private JmsBag getJmsBag(final JmsEndpointConfig endpointCfg) throws JmsRuntimeException, FindException {

        // for multi-threaded execution, use the JmsResourceManager to obtain a JmsBag
        return JmsResourceManager.getInstance().getJmsBag(endpointCfg);
    }

    /**
     * returns the string value from the pec for the context variable in var. If the variable is not found by name or is
     * not of type String, then a message is logged, missingValues has the missing variable added to it, and null is returned.
     * @param var the variable name to look for
     * @param pec the pec in which the values reside
     * @param missingValues a list containing the values that are not found (not String, or not present in pec)
     * @return the string value from the pec for the context variable in var if found, null otherwise.
     */
    private String getVariableValueFromName(String var, PolicyEnforcementContext pec, List<String> missingValues) {
        String [] referencedNames = Syntax.getReferencedNames(var);
        if (referencedNames.length > 0) {
            try {
                Object val = pec.getVariable(referencedNames[0]);
                if (val instanceof String) {
                     return (String) val;
                } else {
                    logger.log(Level.WARNING, MessageFormat.format("Dynamic JMS Routing variable is not of type String ({0}:{1})", referencedNames[0], val.getClass().getName()));
                }
            } catch (NoSuchVariableException e) {
                logger.log(Level.WARNING, MessageFormat.format("Dynamic JMS Routing variable not found ({0})", referencedNames[0]));
            }
        }
        missingValues.add(var);
        return null;
    }

    private JmsDynamicProperties getContextVariablesValues(PolicyEnforcementContext pec, JmsDynamicProperties variablesWanted) {

        JmsDynamicProperties propsWithValues = new JmsDynamicProperties();

        if (variablesWanted == null) return propsWithValues;

        List<String> missing = new ArrayList<String>();

        //dest Q Name
        if (variablesWanted.getDestQName() != null && !"".equals(variablesWanted.getDestQName().trim()))
            propsWithValues.setDestQName(getVariableValueFromName(variablesWanted.getDestQName(), pec, missing));

        //replyTo Q Name
        if (variablesWanted.getReplytoQName() != null && !"".equals(variablesWanted.getReplytoQName().trim()))
            propsWithValues.setReplytoQName(getVariableValueFromName(variablesWanted.getReplytoQName(), pec, missing));

        // JNDI Url
        if (variablesWanted.getJndiUrl() != null && !"".equals(variablesWanted.getJndiUrl().trim()))
            propsWithValues.setJndiUrl(getVariableValueFromName(variablesWanted.getJndiUrl(), pec, missing));

        //ICF Name
        if (variablesWanted.getIcfName() != null && !"".equals(variablesWanted.getIcfName().trim()))
            propsWithValues.setIcfName(getVariableValueFromName(variablesWanted.getIcfName(), pec, missing));

        //QCF Name
        if (variablesWanted.getQcfName() != null && !"".equals(variablesWanted.getQcfName().trim()))
            propsWithValues.setQcfName(getVariableValueFromName(variablesWanted.getQcfName(), pec, missing));

        // check for mismatch && log warning as the assertion will likely fail
        if (!missing.isEmpty()) {
            try {
                logger.log(Level.WARNING,"Mismatched JMS dynamic routing variables not set in policy");

                final String SEP = "; ";
                StringBuffer sb = new StringBuffer("JMS variables missing from policy: ");

                for (String s : missing) {
                    sb.append(s).append(SEP);
                }
                logger.log(Level.INFO, sb.toString());
            } catch (Throwable t) {
                // prevent logging from stopping the policy execution
                logger.log(Level.FINE, "Problem creating logs: {0}", ExceptionUtils.getMessage(t));
            }
        }
        return propsWithValues;
    }

    private void enforceJmsMessagePropertyRuleSet(PolicyEnforcementContext context, JmsMessagePropertyRuleSet ruleSet, Map<String, Object> src,  Map<String, Object> dst) {
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

        public JmsInvalidator(ServerJmsRoutingAssertion serverJmsRoutingAssertion) {
            if (serverJmsRoutingAssertion == null)
                throw new IllegalArgumentException("serverJmsRoutingAssertion must not be null");
            this.serverJmsRoutingAssertion = serverJmsRoutingAssertion;
        }

        @Override
        public void onApplicationEvent(ApplicationEvent applicationEvent) {
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
                        if (logger.isLoggable(Level.INFO)) {
                            logger.log(Level.INFO, "Flagging JMS information for update [conn:{0}; epnt:{1}].",
                                new Object[]{Long.toString(connection.getOid()), Long.toString(endpoint.getOid())});
                        }
                        serverJmsRoutingAssertion.markUpdate(true);
                    }
                }
            }
        }
    }
    
}
