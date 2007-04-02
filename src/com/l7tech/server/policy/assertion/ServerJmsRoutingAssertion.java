/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.message.JmsKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsReplyType;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.transport.jms.*;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Server side implementation of JMS routing assertion.
 */
public class ServerJmsRoutingAssertion extends ServerRoutingAssertion {

    public ServerJmsRoutingAssertion(JmsRoutingAssertion data, ApplicationContext ctx) {
        super(data, ctx, logger);
        this.data = data;
        auditor = new Auditor(this, ctx, logger);
        stashManagerFactory = (StashManagerFactory) applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
        jmsPropertyMapper = (JmsPropertyMapper) applicationContext.getBean("jmsPropertyMapper", JmsPropertyMapper.class);
        jmsInvalidator = new JmsInvalidator(this);
        final ApplicationEventProxy aep = (ApplicationEventProxy) applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class);
        aep.addApplicationListener(jmsInvalidator);
        SignerInfo signerInfo = null;
        try {
            KeystoreUtils ku = (KeystoreUtils)applicationContext.getBean("keystore", KeystoreUtils.class);
            signerInfo = ku.getSslSignerInfo();
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Error getting SAML signer information.", e);
        }
        senderVouchesSignerInfo = signerInfo;
        needsUpdate = false;
    }

    // TODO synchronized?
    public synchronized AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException {

        context.setRoutingStatus(RoutingStatus.ATTEMPTED);
        Destination jmsInboundDest = null;
        MessageProducer jmsProducer = null;
        MessageConsumer jmsConsumer = null;

        try {
            Session jmsSession = null;
            Message jmsOutboundRequest = null;
            int oopses = 0;

            // DELETE CURRENT SECURITY HEADER IF NECESSARY
            handleProcessedSecurityHeader(context,
                                          data.getCurrentSecurityHeaderHandling(),
                                          data.getXmlSecurityActorToPromote());

            if (data.isAttachSamlSenderVouches()) {
                if (senderVouchesSignerInfo == null) {
                    auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NO_SAML_SIGNER);
                    return AssertionStatus.FAILED;
                }
                doAttachSamlSenderVouches(context, senderVouchesSignerInfo);
            }

            while (true) {
                try {
                    if (markedForUpdate()) {
                        try {
                            logger.info("JMS information needs update, closing session (if open).");                            
                            closeBag();                            
                        } finally {
                            markUpdate(false);
                        }
                    }

                    JmsBag bag = getJmsBag(auditor);
                    jmsSession = bag.getSession();
                    jmsOutboundRequest = makeRequest(context, auditor);
                    break; // if successful, no need for further retries
                } catch (Throwable t) {
                    if (++oopses < MAX_OOPSES) {
                        String msg = "Failed to establish JMS connection on try #" +
                                oopses +  ".  Will retry after " + RETRY_DELAY + "ms.";
                        auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {msg}, t);
                        if ( jmsSession != null ) try { jmsSession.close(); } catch ( Exception e ) { }
                        closeBag();

                        jmsSession = null;
                        jmsOutboundRequest = null;

                        try {
                            Thread.sleep(RETRY_DELAY);
                        } catch ( InterruptedException e ) {
                            auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] {"Interrupted during retry delay"});
                        }
                    } else {
                        String msg = "Tried " + MAX_OOPSES + " times to establish JMS connection and failed.";
                        auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[] {msg}, t);
                        // Catcher will log the stack trace
                        throw t;
                    }
                }
            }

            if ( jmsSession == null || jmsOutboundRequest == null ) {
                String msg = "Null session or request escaped from retry loop!";
                throw new PolicyAssertionException(data, msg);
            }

            Destination jmsOutboundDest = getRoutedRequestDestination(auditor);
            jmsInboundDest = jmsOutboundRequest.getJMSReplyTo();

            String corrId = jmsOutboundRequest.getJMSCorrelationID();
            String selector = null;
            if ( corrId != null && !( jmsInboundDest instanceof TemporaryQueue ) ) {
                // TODO Heuristic use selector if temp queue, assuming otherwise it could be shared
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_INBOUD_REQUEST_QUEUE_NOT_EMPTY);
                selector = "JMSCorrelationID = '" + escape( corrId ) + "'";
            }

            boolean inbound = context.isReplyExpected()
                              && jmsInboundDest != null;

            if ( jmsSession instanceof QueueSession ) {
                if ( !(jmsOutboundDest instanceof Queue ) ) throw new PolicyAssertionException(data, "Destination/Session type mismatch" );
                jmsProducer = ((QueueSession)jmsSession).createSender( (Queue)jmsOutboundDest );
                if ( inbound )
                    jmsConsumer = ((QueueSession)jmsSession).createReceiver( (Queue)jmsInboundDest, selector );
            } else if ( jmsSession instanceof TopicSession ) {
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_TOPIC_NOT_SUPPORTED);
                return AssertionStatus.NOT_YET_IMPLEMENTED;
            } else {
                jmsProducer = jmsSession.createProducer( jmsOutboundDest );
                if ( inbound ) jmsConsumer = jmsSession.createConsumer( jmsInboundDest, selector );
            }

            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_REQUEST_ROUTED);
            context.routingStarted();
            jmsProducer.send( jmsOutboundRequest );

            if ( inbound ) {
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_GETTING_RESPONSE);
                int timeout = data.getResponseTimeout();
                final Message jmsResponse = jmsConsumer.receive( timeout );
                context.routingFinished();
                if ( jmsResponse == null ) {
                    auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NO_RESPONSE, new String[]{String.valueOf(timeout)});

                    return AssertionStatus.FAILED;
                } else {
                    // TODO throw PolicyAssertionException unless response isXml

                    if ( jmsResponse instanceof TextMessage ) {
                        context.getResponse().initialize(XmlUtil.stringToDocument( ((TextMessage)jmsResponse).getText() ));
                    } else if ( jmsResponse instanceof BytesMessage ) {
                        BytesMessage bmsg = (BytesMessage)jmsResponse;
                        final StashManager stashManager = stashManagerFactory.createStashManager();
                        context.getResponse().initialize(stashManager, ContentTypeHeader.XML_DEFAULT, new BytesMessageInputStream(bmsg));
                    } else {
                        auditor.logAndAudit(AssertionMessages.JMS_ROUTING_UNSUPPORTED_RESPONSE_MSG_TYPE, new String[]{jmsResponse.getClass().getName()});
                        return AssertionStatus.FAILED;
                    }
                    auditor.logAndAudit(AssertionMessages.JMS_ROUTING_GOT_RESPONSE);
                    context.getResponse().attachJmsKnob(new JmsKnob() {
                        public boolean isBytesMessage() {
                            return (jmsResponse instanceof BytesMessage);
                        }
                    });
                    context.setRoutingStatus( RoutingStatus.ROUTED );
                }
            } else {
                context.routingFinished();
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NO_RESPONSE_EXPECTED);
                context.setRoutingStatus( RoutingStatus.ROUTED );
            }

            return AssertionStatus.NONE;
        } catch ( NamingException e ) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Error in outbound JMS request processing"}, e );
            return AssertionStatus.FAILED;
        } catch ( JMSException e ) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Error outbound JMS request processing"}, e );

            closeBag();
            return AssertionStatus.FAILED;
        } catch ( FindException e ) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE, null, e);
            String msg = "Caught FindException";
            throw new PolicyAssertionException(data, msg, e);
        } catch ( JmsConfigException e ) {
            String msg = "Invalid JMS configuration";
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{msg}, e);
            throw new PolicyAssertionException(data, msg, e);
        } catch ( Throwable t ) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{"Caught unexpected Throwable in outbound JMS request processing"}, t );

            closeBag();
            return AssertionStatus.SERVER_ERROR;
        } finally {
            if (context.getRoutingEndTime() == 0) context.routingFinished(); 
            try {
                if ( jmsInboundDest instanceof TemporaryQueue ) {
                    if ( jmsConsumer != null ) jmsConsumer.close();
                    auditor.logAndAudit(AssertionMessages.JMS_ROUTING_DELETE_TEMPORARY_QUEUE);
                    ((TemporaryQueue)jmsInboundDest).delete();
                }
            } catch ( JMSException e ) {
                closeBag();
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                        new String[]{"Caught JMSException while attempting to delete temporary queue"}, e);
            }
        }
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

    private synchronized void closeBag() {
        if (bag != null)
            bag.close();
        bag = null;
    }

    private Pattern quotePattern = Pattern.compile("'");

    private String escape( String corrId ) {
        return quotePattern.matcher(corrId).replaceAll( "''" );
    }

    private Queue getTemporaryResponseQueue(Auditor auditor) throws JMSException, NamingException,
                                                     JmsConfigException, FindException {
        return getJmsBag(auditor).getSession().createTemporaryQueue(); // TODO make this thread-local or something
//        if ( tempResponseQueue == null ) {
//            JmsBag bag = getJmsBag();
//            tempResponseQueue = bag.getSession().createTemporaryQueue();
//        }
//        return tempResponseQueue;
    }

    private Destination getResponseDestination( JmsEndpoint endpoint, Message request, Auditor auditor )
            throws JMSException, NamingException, JmsConfigException, FindException {
        JmsReplyType replyType = endpoint.getReplyType();

        if (replyType == JmsReplyType.NO_REPLY) {
            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_RETURN_NO_REPLY, new String[] {toString()});
            return null;
        } else {
            if (replyType == JmsReplyType.AUTOMATIC) {

                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_RETURN_AUTOMATIC, new String[] {toString()});
                return getTemporaryResponseQueue(auditor);

            } else if (replyType == JmsReplyType.REPLY_TO_OTHER) {

                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_RETURN_REPLY_TO_OTHER,
                        new String[] {endpoint.getDestinationName(), toString()});
                return getEndpointResponseDestination(auditor);

            } else {

                String rt = (replyType == null ? "<null>" : replyType.toString());
                String msg = "Unknown JmsReplyType " + rt;
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_UNKNOW_JMS_REPLY_TYPE, new String[] {rt});
                throw new java.lang.IllegalStateException(msg);

            }
        }
    }

    private Destination getEndpointResponseDestination(Auditor auditor) throws JMSException, NamingException, JmsConfigException, FindException {
        if ( endpointResponseDestination == null ) {
            JmsEndpoint requestEndpoint = getRoutedRequestEndpoint();
            JmsEndpoint replyEndpoint = requestEndpoint.getReplyEndpoint();

            if ( requestEndpoint.getConnectionOid() != replyEndpoint.getConnectionOid() ) {
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_ENDPOINTS_ON_SAME_CONNECTION);
                String msg = "Request and reply endpoints must belong to the same connection";
                throw new JmsConfigException( msg );
            }

            endpointResponseDestination = (Destination)getJmsBag(auditor).getJndiContext().lookup( replyEndpoint.getDestinationName() );
        }
        return endpointResponseDestination;
    }

    /**
     * Builds a {@link Message} to be routed to a JMS endpoint.
     * @param context contains the request to be converted into a JMS Message
     * @param auditor for adding associated logs to audit record.
     * @return the JMS Message
     * @throws IOException
     * @throws JMSException
     */
    private javax.jms.Message makeRequest( PolicyEnforcementContext context, Auditor auditor )
        throws IOException, JMSException, NamingException, JmsConfigException, FindException
    {
        JmsEndpoint endpoint = getRoutedRequestEndpoint();

        javax.jms.Message outboundRequestMsg = null;
        BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream();
        final byte[] outboundRequestBytes;
        try {
            HexUtils.copyStream(context.getRequest().getMimeKnob().getEntireMessageBodyAsInputStream(), baos);
            outboundRequestBytes = baos.toByteArray();
        } catch (NoSuchPartException e) {
            throw new CausedIOException("Couldn't read from JMS request"); // can't happen
        } finally {
            baos.close();
        }

        if (context.getRequest().getKnob(JmsKnob.class) != null) {
            // Outgoing request should be the same type (i.e. TextMessage or BytesMessage) as the original request
            if (!context.getRequest().getJmsKnob().isBytesMessage()) {
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CREATE_REQUEST_AS_TEXT_MESSAGE);
                outboundRequestMsg = bag.getSession().createTextMessage(new String(outboundRequestBytes, JmsUtil.DEFAULT_ENCODING));
            }
        }

        if (outboundRequestMsg == null) {
            // Default to BytesMessage
            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_CREATE_REQUEST_AS_BYTES_MESSAGE);
            BytesMessage bmsg = bag.getSession().createBytesMessage();
            bmsg.writeBytes(outboundRequestBytes);
            outboundRequestMsg = bmsg;
        }

        JmsReplyType replyType = endpoint.getReplyType();
        if ( replyType == JmsReplyType.NO_REPLY ) {
            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_ROUTE_REQUEST_WITH_NO_REPLY);
        } else {
            auditor.logAndAudit(AssertionMessages.JMS_ROUTING_SET_REPLYTO_CORRELCTIONID);
            // Set replyTo & correlationId
            outboundRequestMsg.setJMSReplyTo( getResponseDestination( endpoint, outboundRequestMsg, auditor ) );
            outboundRequestMsg.setJMSCorrelationID( context.getRequestId().toString() );
        }

        return outboundRequestMsg;
    }

    private Destination getRoutedRequestDestination(Auditor auditor) throws FindException, JMSException, NamingException, JmsConfigException {
        if ( routedRequestDestination == null ) {
            Context jndiContext = getJmsBag(auditor).getJndiContext();
            routedRequestDestination = (Destination)jndiContext.lookup(getRoutedRequestEndpoint().getDestinationName());
        }

        return routedRequestDestination;
    }

    private JmsEndpoint getRoutedRequestEndpoint() throws FindException {
        if ( routedRequestEndpoint == null ) {
            JmsEndpointManager mgr = (JmsEndpointManager)applicationContext.getBean("jmsEndpointManager");
            JmsEndpoint jmsEndpoint = mgr.findByPrimaryKey(data.getEndpointOid().longValue());
            synchronized(jmsInfoSync) {
                routedRequestEndpoint = jmsEndpoint;
            }
        }
        return routedRequestEndpoint;
    }

    private JmsConnection getRoutedRequestConnection(Auditor auditor) throws FindException {
        if ( routedRequestConnection == null ) {
            JmsConnectionManager mgr = (JmsConnectionManager)applicationContext.getBean("jmsConnectionManager");
            JmsEndpoint endpoint = getRoutedRequestEndpoint();
            if ( endpoint == null ) {
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NON_EXISTENT_ENDPOINT,
                        new String[] {String.valueOf(data.getEndpointOid()), data.getEndpointName()});
            } else {
                JmsConnection jmsConn = mgr.findByPrimaryKey( endpoint.getConnectionOid() );
                synchronized(jmsInfoSync) {
                    routedRequestConnection = jmsConn;
                }
            }
        }
        return routedRequestConnection;
    }

    private synchronized JmsBag getJmsBag(Auditor auditor) throws FindException, JMSException, NamingException, JmsConfigException {
        if ( bag == null ) {
            JmsConnection conn = getRoutedRequestConnection(auditor);
            if ( conn == null ) throw new FindException( "JmsConnection could not be located! It may have been deleted" );
            JmsEndpoint endpoint = getRoutedRequestEndpoint();
            if ( endpoint == null ) throw new FindException( "JmsEndpoint could not be located! It may have been deleted" );
            PasswordAuthentication pwauth = endpoint.getPasswordAuthentication();

            bag = JmsUtil.connect( conn, pwauth, jmsPropertyMapper );
            bag.getConnection().start();
        }
        return bag;
    }

    /**
     * Invalidation listener for JMS endpoint / connection updates.
     */
    public static final class JmsInvalidator implements ApplicationListener {
        private final ServerJmsRoutingAssertion serverJmsRoutingAssertion;

        public JmsInvalidator(ServerJmsRoutingAssertion serverJmsRoutingAssertion) {
            this.serverJmsRoutingAssertion = serverJmsRoutingAssertion;
        }

        public void onApplicationEvent(ApplicationEvent applicationEvent) {
            if (applicationEvent instanceof EntityInvalidationEvent) {
                EntityInvalidationEvent eie = (EntityInvalidationEvent) applicationEvent;

                if (serverJmsRoutingAssertion != null) {
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
    
    private final JmsRoutingAssertion data;
    private final Auditor auditor;
    private final StashManagerFactory stashManagerFactory;
    private final JmsPropertyMapper jmsPropertyMapper;
    private final JmsInvalidator jmsInvalidator;
    private final SignerInfo senderVouchesSignerInfo;

    private Object needsUpdateSync = new Object();
    private boolean needsUpdate;
    private Object jmsInfoSync = new Object();
    private JmsConnection routedRequestConnection;
    private JmsEndpoint routedRequestEndpoint;

    private JmsBag bag;
    private Destination routedRequestDestination;
    private Destination endpointResponseDestination;

    private static final Logger logger = Logger.getLogger(ServerJmsRoutingAssertion.class.getName());
    private static final int MAX_OOPSES = 5;
    private static final long RETRY_DELAY = 1000;
}
