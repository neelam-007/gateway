/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsReplyType;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.XmlRequest;
import com.l7tech.message.XmlResponse;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.transport.jms.*;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.NamingException;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Server side implementation of JMS routing assertion.
 */
public class ServerJmsRoutingAssertion extends ServerRoutingAssertion {
    private JmsRoutingAssertion data;

    public ServerJmsRoutingAssertion(JmsRoutingAssertion data) {
        this.data = data;
        connectionManager = (JmsConnectionManager)Locator.getDefault().lookup( JmsConnectionManager.class );
        endpointManager = (JmsEndpointManager)Locator.getDefault().lookup( JmsEndpointManager.class );
    }

    // TODO synchronized?
    public synchronized AssertionStatus checkRequest( Request request, Response response )
            throws IOException, PolicyAssertionException {
        request.setRoutingStatus( RoutingStatus.ATTEMPTED );

        try {
            JmsBag bag = getJmsBag();
            Session jmsSession = bag.getSession();

            MessageProducer jmsProducer = null;
            MessageConsumer jmsConsumer = null;

            // Make outbound request
            Message jmsOutboundRequest = makeRequest( request );

            Destination jmsOutboundDest = getRoutedRequestDestination();
            Destination jmsInboundDest = jmsOutboundRequest.getJMSReplyTo();

            String corrId = jmsOutboundRequest.getJMSCorrelationID();
            String selector = corrId == null
                              ? null
                              : "JMSCorrelationID = '" + escape( corrId ) + "'";

            if ( jmsSession instanceof QueueSession ) {
                if ( !(jmsOutboundDest instanceof Queue ) ) throw new PolicyAssertionException( "Destination/Session type mismatch" );
                jmsProducer = ((QueueSession)jmsSession).createSender( (Queue)jmsOutboundDest );
                jmsConsumer = ((QueueSession)jmsSession).createReceiver( (Queue)jmsInboundDest, selector );
            } else if ( jmsSession instanceof TopicSession ) {
                logger.log( Level.SEVERE, "Topics not supported!" );
                return AssertionStatus.NOT_YET_IMPLEMENTED;
            } else {
                jmsProducer = jmsSession.createProducer( jmsOutboundDest );
                jmsConsumer = jmsSession.createConsumer( jmsInboundDest, selector );
            }

            jmsProducer.send( jmsOutboundRequest );

            if ( request.isReplyExpected() ) {
                Message jmsResponse = jmsConsumer.receive( RESPONSE_TIMEOUT );
                if ( jmsResponse == null ) {
                    logger.fine( "Did not receive a routing reply within timeout of " +
                                 RESPONSE_TIMEOUT + "ms. Will return empty response");
                    return AssertionStatus.FAILED;
                } else {
                    logger.finer( "Received routing reply" );

                    if ( !(response instanceof XmlResponse ) ) throw new PolicyAssertionException( "Only XML responses are supported" );
                    XmlResponse xresp = (XmlResponse)response;

                    if ( jmsResponse instanceof TextMessage ) {
                        xresp.setResponseXml( ((TextMessage)jmsResponse).getText() );
                    } else if ( jmsResponse instanceof BytesMessage ) {
                        BytesMessage bmsg = (BytesMessage)jmsResponse;
                        BytesMessageInputStream bmis = new BytesMessageInputStream(bmsg);
                        InputStreamReader isr = new InputStreamReader( bmis, JmsUtil.DEFAULT_ENCODING );
                        char[] buff = new char[BUFFER_SIZE]; // chars here rather than bytes but oh well
                        int got = 0;
                        StringBuffer sb = new StringBuffer();
                        while (-1 != (got = isr.read(buff))) {
                            sb.append( buff, 0, got );
                        }
                        xresp.setResponseXml( sb.toString() );
                    } else {
                        logger.warning( "Received JMS reply with unsupported message type " +
                                        jmsResponse.getClass().getName() );
                        return AssertionStatus.FAILED;
                    }

                    request.setRoutingStatus( RoutingStatus.ROUTED );
                }
            } else {
                request.setRoutingStatus( RoutingStatus.ROUTED );
            }

            return AssertionStatus.NONE;
        } catch ( NamingException e ) {
            logger.log( Level.WARNING, "Caught NamingException in outbound JMS request processing", e );
            return AssertionStatus.FAILED;
        } catch ( JMSException e ) {
            logger.log( Level.WARNING, "Caught JMSException in outbound JMS request processing", e );
            return AssertionStatus.FAILED;
        } catch ( FindException e ) {
            String msg = "Caught FindException";
            logger.log( Level.SEVERE, msg, e );
            throw new PolicyAssertionException(msg, e);
        } catch ( JmsConfigException e ) {
            String msg = "Invalid JMS configuration";
            logger.log( Level.SEVERE, msg, e );
            throw new PolicyAssertionException(msg, e);
        }
    }

    private Pattern quotePattern = Pattern.compile("'");

    private String escape( String corrId ) {
        return quotePattern.matcher(corrId).replaceAll( "''" );
    }

    private Queue getTemporaryResponseQueue() throws JMSException, NamingException,
                                                     JmsConfigException, FindException {
        if ( tempResponseQueue == null ) {
            JmsBag bag = getJmsBag();
            tempResponseQueue = bag.getSession().createTemporaryQueue();
        }
        return tempResponseQueue;
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
     * @param request
     * @return
     * @throws PolicyAssertionException
     * @throws IOException
     * @throws JMSException
     */
    private Message makeRequest( Request request )
            throws PolicyAssertionException, IOException, JMSException,
                   NamingException, JmsConfigException, FindException {
        if ( !(request instanceof XmlRequest ) ) throw new PolicyAssertionException( "Only XML messages are supported" );

        JmsEndpoint endpoint = getRoutedRequestEndpoint();

        Message msg = null;
        XmlRequest xreq = (XmlRequest)request;
        if ( xreq instanceof JmsSoapRequest ) {
            // Outgoing request should be the same type (i.e. TextMessage or BytesMessage) as the original request
            JmsSoapRequest jreq = (JmsSoapRequest)xreq;
            JmsTransportMetadata jtm = (JmsTransportMetadata)jreq.getTransportMetadata();
            Message jmsOriginalRequest = jtm.getRequest();
            if ( jmsOriginalRequest instanceof TextMessage ) {
                logger.finer( "Creating request as TextMessage" );
                msg = bag.getSession().createTextMessage( xreq.getRequestXml() );
            }
        }

        if ( msg == null ) {
            // Default to BytesMessage
            logger.finer( "Creating request as BytesMessage" );
            BytesMessage bmsg = bag.getSession().createBytesMessage();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
            baos.write( xreq.getRequestXml().getBytes(JmsUtil.DEFAULT_ENCODING) ); // TODO ENCODING #@)($*&!)!
            bmsg.writeBytes( baos.toByteArray() );
            msg = bmsg;
        }

        JmsReplyType replyType = endpoint.getReplyType();
        if ( replyType == JmsReplyType.NO_REPLY ) {
            logger.fine( "Routed request endpoint specified NO_REPLY, won't set JMSReplyTo and JMSCorrelationID" );
        } else {
            logger.fine( "Setting JMSReplyTo and JMSCorrelationID" );
            // Set replyTo & correlationId
            msg.setJMSReplyTo( getResponseDestination( endpoint, msg ) );
            msg.setJMSCorrelationID( request.getId().toString() );
        }

        return msg;
    }

    private Destination getRoutedRequestDestination() throws FindException, JMSException, NamingException, JmsConfigException {
        if ( routedRequestDestination == null ) {
            Context jndiContext = getJmsBag().getJndiContext();
            routedRequestDestination = (Destination)jndiContext.lookup(getRoutedRequestEndpoint().getDestinationName());
        }

        return routedRequestDestination;
    }

    private JmsEndpoint getRoutedRequestEndpoint() throws FindException {
        if ( routedRequestEndpoint == null )
            routedRequestEndpoint = endpointManager.findByPrimaryKey(data.getEndpointOid().longValue());

        return routedRequestEndpoint;
    }

    private JmsConnection getRoutedRequestConnection() throws FindException {
        if ( routedRequestConnection == null )
            routedRequestConnection = connectionManager.findConnectionByPrimaryKey(
                    getRoutedRequestEndpoint().getConnectionOid() );
        return routedRequestConnection;
    }

    private JmsBag getJmsBag() throws FindException, JMSException, NamingException, JmsConfigException {
        if ( bag == null )
            bag = JmsUtil.connect( getRoutedRequestConnection(),
                                   getRoutedRequestEndpoint().getPasswordAuthentication() );
        return bag;
    }

    private JmsConnection routedRequestConnection;
    private JmsEndpoint routedRequestEndpoint;
    private JmsBag bag;

    private final JmsConnectionManager connectionManager;
    private final JmsEndpointManager endpointManager;
    private Logger logger = LogManager.getInstance().getSystemLogger();
    private Destination routedRequestDestination;
    private Destination endpointResponseDestination;
    private Queue tempResponseQueue;
    public static final long RESPONSE_TIMEOUT = 10 * 1000;
    public static final int BUFFER_SIZE = 79; // TODO set to something reasonable when testing is finished
}
