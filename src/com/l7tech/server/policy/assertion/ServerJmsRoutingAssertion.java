/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.message.JmsKnob;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsReplyType;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.transport.jms.*;
import com.l7tech.common.audit.AssertionMessages;
import org.springframework.context.ApplicationContext;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Server side implementation of JMS routing assertion.
 */
public class ServerJmsRoutingAssertion extends ServerRoutingAssertion {
    private JmsRoutingAssertion data;

    public ServerJmsRoutingAssertion(JmsRoutingAssertion data, ApplicationContext ctx) {
        super(ctx);
        this.data = data;
    }

    // TODO synchronized?
    public synchronized AssertionStatus checkRequest(PolicyEnforcementContext context)
            throws IOException, PolicyAssertionException {

        Auditor auditor = new Auditor(context.getAuditContext(), logger);
        context.setRoutingStatus( RoutingStatus.ATTEMPTED );
        Destination jmsInboundDest = null;
        MessageProducer jmsProducer = null;
        MessageConsumer jmsConsumer = null;

        try {
            Session jmsSession = null;
            Message jmsOutboundRequest = null;
            int oopses = 0;

            while ( true ) {
                try {
                    JmsBag bag = getJmsBag(auditor);
                    jmsSession = bag.getSession();
                    jmsOutboundRequest = makeRequest( context, auditor );
                    break; // if successful, no need for further retries
                } catch ( Throwable t ) {
                    if ( ++oopses < MAX_OOPSES ) {
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
                throw new PolicyAssertionException(msg);
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
                if ( !(jmsOutboundDest instanceof Queue ) ) throw new PolicyAssertionException( "Destination/Session type mismatch" );
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
            jmsProducer.send( jmsOutboundRequest );

            if ( inbound ) {
                auditor.logAndAudit(AssertionMessages.JMS_ROUTING_GETTING_RESPONSE);
                int timeout = data.getResponseTimeout();
                final Message jmsResponse = jmsConsumer.receive( timeout );
                if ( jmsResponse == null ) {
                    auditor.logAndAudit(AssertionMessages.JMS_ROUTING_NO_RESPONSE, new String[]{String.valueOf(timeout)});

                    return AssertionStatus.FAILED;
                } else {
                    // TODO throw PolicyAssertionException unless response isXml

                    if ( jmsResponse instanceof TextMessage ) {
                        context.getResponse().initialize(XmlUtil.stringToDocument( ((TextMessage)jmsResponse).getText() ));
                    } else if ( jmsResponse instanceof BytesMessage ) {
                        BytesMessage bmsg = (BytesMessage)jmsResponse;
                        context.getResponse().initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new BytesMessageInputStream(bmsg));
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
            throw new PolicyAssertionException(msg, e);
        } catch ( JmsConfigException e ) {
            String msg = "Invalid JMS configuration";
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{msg}, e);
            throw new PolicyAssertionException(msg, e);
        } catch ( Throwable t ) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, new String[]{"Caught unexpected Throwable in outbound JMS request processing"}, t );

            closeBag();
            return AssertionStatus.SERVER_ERROR;
        } finally {
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

    private synchronized void closeBag() {
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            HexUtils.copyStream(context.getRequest().getMimeKnob().getEntireMessageBodyAsInputStream(), baos);
        } catch (NoSuchPartException e) {
            throw new CausedIOException("Couldn't read from JMS request"); // can't happen
        }
        byte[] outboundRequestBytes = baos.toByteArray();

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
            routedRequestEndpoint = mgr.findByPrimaryKey(data.getEndpointOid().longValue());
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
                routedRequestConnection = mgr.findConnectionByPrimaryKey( endpoint.getConnectionOid() );
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

            bag = JmsUtil.connect( conn, pwauth );
            bag.getConnection().start();
        }
        return bag;
    }

    private JmsConnection routedRequestConnection;
    private JmsEndpoint routedRequestEndpoint;

    private JmsBag bag;
    private Destination routedRequestDestination;
    private Destination endpointResponseDestination;

    private final Logger logger = Logger.getLogger(getClass().getName());
    public static final int BUFFER_SIZE = 8192;
    private static final int MAX_OOPSES = 5;
    private static final long RETRY_DELAY = 1000;
}
