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
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.transport.jms.*;
import org.springframework.context.ApplicationContext;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.logging.Level;
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
                    JmsBag bag = getJmsBag();
                    jmsSession = bag.getSession();
                    jmsOutboundRequest = makeRequest( context );
                    break; // if successful, no need for further retries
                } catch ( Throwable t ) {
                    if ( ++oopses < MAX_OOPSES ) {
                        logger.log( Level.WARNING, "Failed to establish JMS connection on try #" + oopses +
                                                   ".  Will retry after " + RETRY_DELAY + "ms.", t );
                        if ( jmsSession != null ) try { jmsSession.close(); } catch ( Exception e ) { }
                        closeBag();

                        jmsSession = null;
                        jmsOutboundRequest = null;

                        try {
                            Thread.sleep(RETRY_DELAY);
                        } catch ( InterruptedException e ) {
                            logger.fine("Interrupted during retry delay");
                        }
                    } else {
                        logger.severe( "Tried " + MAX_OOPSES + " times to establish JMS connection and failed." );
                        // Catcher will log the stack trace
                        throw t;
                    }
                }
            }

            if ( jmsSession == null || jmsOutboundRequest == null ) {
                String msg = "Null session or request escaped from retry loop!";
                throw new PolicyAssertionException(msg);
            }

            Destination jmsOutboundDest = getRoutedRequestDestination();
            jmsInboundDest = jmsOutboundRequest.getJMSReplyTo();

            String corrId = jmsOutboundRequest.getJMSCorrelationID();
            String selector = null;
            if ( corrId != null && !( jmsInboundDest instanceof TemporaryQueue ) ) {
                // TODO Heuristic use selector if temp queue, assuming otherwise it could be shared
                logger.fine( "Inbound request queue is not temporary; using selector to filter responses to our message" );
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
                logger.log( Level.SEVERE, "Topics not supported!" );
                return AssertionStatus.NOT_YET_IMPLEMENTED;
            } else {
                jmsProducer = jmsSession.createProducer( jmsOutboundDest );
                if ( inbound ) jmsConsumer = jmsSession.createConsumer( jmsInboundDest, selector );
            }

            logger.finer( "Routing request to protected service" );
            jmsProducer.send( jmsOutboundRequest );

            if ( inbound ) {
                logger.finer( "Getting response from protected service" );
                int timeout = data.getResponseTimeout();
                Message jmsResponse = jmsConsumer.receive( timeout );
                if ( jmsResponse == null ) {
                    logger.warning( "Did not receive a routing reply within timeout of " +
                                 timeout + "ms. Will return empty response");
                    return AssertionStatus.FAILED;
                } else {
                    logger.finer( "Received routing reply" );

                    // TODO throw PolicyAssertionException unless response isXml

                    if ( jmsResponse instanceof TextMessage ) {
                        context.getResponse().initialize(XmlUtil.stringToDocument( ((TextMessage)jmsResponse).getText() ));
                    } else if ( jmsResponse instanceof BytesMessage ) {
                        BytesMessage bmsg = (BytesMessage)jmsResponse;
                        context.getResponse().initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new BytesMessageInputStream(bmsg));
                    } else {
                        logger.warning( "Received JMS reply with unsupported message type " +
                                        jmsResponse.getClass().getName() );
                        return AssertionStatus.FAILED;
                    }

                    logger.info( "Received response from protected service" );
                    context.setRoutingStatus( RoutingStatus.ROUTED );
                }
            } else {
                logger.info( "No response expected from protected service" );
                context.setRoutingStatus( RoutingStatus.ROUTED );
            }

            return AssertionStatus.NONE;
        } catch ( NamingException e ) {
            logger.log( Level.WARNING, "Caught NamingException in outbound JMS request processing", e );
            return AssertionStatus.FAILED;
        } catch ( JMSException e ) {
            logger.log( Level.WARNING, "Caught JMSException in outbound JMS request processing", e );
            closeBag();
            return AssertionStatus.FAILED;
        } catch ( FindException e ) {
            String msg = "Caught FindException";
            logger.log( Level.SEVERE, msg, e );
            throw new PolicyAssertionException(msg, e);
        } catch ( JmsConfigException e ) {
            String msg = "Invalid JMS configuration";
            logger.log( Level.SEVERE, msg, e );
            throw new PolicyAssertionException(msg, e);
        } catch ( Throwable t ) {
            logger.log( Level.SEVERE, "Caught unexpected Throwable in outbound JMS request processing", t );
            closeBag();
            return AssertionStatus.SERVER_ERROR;
        } finally {
            try {
                if ( jmsInboundDest instanceof TemporaryQueue ) {
                    if ( jmsConsumer != null ) jmsConsumer.close();
                    logger.finer( "Deleting temporary queue" );
                    ((TemporaryQueue)jmsInboundDest).delete();
                }
            } catch ( JMSException e ) {
                closeBag();
                logger.log( Level.WARNING, "Caught JMSException while attempting to delete temporary queue", e );
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

    private Queue getTemporaryResponseQueue() throws JMSException, NamingException,
                                                     JmsConfigException, FindException {
        return getJmsBag().getSession().createTemporaryQueue(); // TODO make this thread-local or something
//        if ( tempResponseQueue == null ) {
//            JmsBag bag = getJmsBag();
//            tempResponseQueue = bag.getSession().createTemporaryQueue();
//        }
//        return tempResponseQueue;
    }

    private Destination getResponseDestination( JmsEndpoint endpoint, Message request )
            throws JMSException, NamingException, JmsConfigException, FindException {
        JmsReplyType replyType = endpoint.getReplyType();

        if (replyType == JmsReplyType.NO_REPLY) {
            logger.finer("Returning NO_REPLY (null) for '" + toString() + "'");
            return null;
        } else {
            if (replyType == JmsReplyType.AUTOMATIC) {

                logger.finer("Returning AUTOMATIC '" + request.getJMSReplyTo() + "' for '" + toString() + "'");
                return getTemporaryResponseQueue();

            } else if (replyType == JmsReplyType.REPLY_TO_OTHER) {

                logger.finer("Returning REPLY_TO_OTHER '" + endpoint.getDestinationName() + "' for '" + toString() + "'");
                return getEndpointResponseDestination();

            } else {

                String msg = "Unknown JmsReplyType " + (replyType == null ? "<null>" : replyType.toString());
                logger.severe(msg);
                throw new java.lang.IllegalStateException(msg);

            }
        }
    }

    private Destination getEndpointResponseDestination() throws JMSException, NamingException, JmsConfigException, FindException {
        if ( endpointResponseDestination == null ) {
            JmsEndpoint requestEndpoint = getRoutedRequestEndpoint();
            JmsEndpoint replyEndpoint = requestEndpoint.getReplyEndpoint();

            if ( requestEndpoint.getConnectionOid() != replyEndpoint.getConnectionOid() ) {
                String msg = "Request and reply endpoints must belong to the same connection";
                logger.severe( msg );
                throw new JmsConfigException( msg );
            }

            endpointResponseDestination = (Destination)getJmsBag().getJndiContext().lookup( replyEndpoint.getDestinationName() );
        }
        return endpointResponseDestination;
    }

    /**
     * Builds a {@link Message} to be routed to a JMS endpoint.
     * @param context contains the request to be converted into a JMS Message
     * @return the JMS Message
     * @throws IOException
     * @throws JMSException
     */
    private javax.jms.Message makeRequest( PolicyEnforcementContext context )
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
                logger.finer( "Creating request as TextMessage" );
                outboundRequestMsg = bag.getSession().createTextMessage(new String(outboundRequestBytes, JmsUtil.DEFAULT_ENCODING));
            }
        }

        if (outboundRequestMsg == null) {
            // Default to BytesMessage
            logger.finer( "Creating request as BytesMessage" );
            BytesMessage bmsg = bag.getSession().createBytesMessage();
            bmsg.writeBytes(outboundRequestBytes);
        }

        JmsReplyType replyType = endpoint.getReplyType();
        if ( replyType == JmsReplyType.NO_REPLY ) {
            logger.fine( "Routed request endpoint specified NO_REPLY, won't set JMSReplyTo and JMSCorrelationID" );
        } else {
            logger.fine( "Setting JMSReplyTo and JMSCorrelationID" );
            // Set replyTo & correlationId
            outboundRequestMsg.setJMSReplyTo( getResponseDestination( endpoint, outboundRequestMsg ) );
            outboundRequestMsg.setJMSCorrelationID( context.getRequestId().toString() );
        }

        return outboundRequestMsg;
    }

    private Destination getRoutedRequestDestination() throws FindException, JMSException, NamingException, JmsConfigException {
        if ( routedRequestDestination == null ) {
            Context jndiContext = getJmsBag().getJndiContext();
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

    private JmsConnection getRoutedRequestConnection() throws FindException {
        if ( routedRequestConnection == null ) {
            JmsConnectionManager mgr = (JmsConnectionManager)applicationContext.getBean("jmsConnectionManager");
            JmsEndpoint endpoint = getRoutedRequestEndpoint();
            if ( endpoint == null ) {
                String msg = "JmsRoutingAssertion contains a reference to nonexistent JmsEndpoint #"
                             + data.getEndpointOid()
                             + " (" + data.getEndpointName() + ")";
                logger.severe( msg );
            } else {
                routedRequestConnection = mgr.findConnectionByPrimaryKey( endpoint.getConnectionOid() );
            }
        }
        return routedRequestConnection;
    }

    private synchronized JmsBag getJmsBag() throws FindException, JMSException, NamingException, JmsConfigException {
        if ( bag == null ) {
            JmsConnection conn = getRoutedRequestConnection();
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
