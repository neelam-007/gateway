/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.XmlRequest;
import com.l7tech.message.XmlResponse;
import com.l7tech.server.transport.jms.*;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.logging.LogManager;

import javax.jms.*;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public synchronized /* TODO */ AssertionStatus checkRequest( Request request, Response response ) throws IOException,
                                                                                     PolicyAssertionException {
        request.setRoutingStatus( RoutingStatus.ATTEMPTED );

        try {
            Session jmsSession = getJmsBag().getSession();
            Destination jmsDest = getRoutedRequestDestination();
            MessageProducer jmsProducer = null;
            if ( jmsSession instanceof QueueSession ) {
                if ( !(jmsDest instanceof Queue ) ) throw new PolicyAssertionException( "Destination/Session type mismatch" );
                jmsProducer = ((QueueSession)jmsSession).createSender( (Queue)jmsDest );
            } else if ( jmsSession instanceof TopicSession ) {
                logger.log( Level.SEVERE, "Topics not supported!" );
                return AssertionStatus.NOT_YET_IMPLEMENTED;
            } else {
                jmsProducer = jmsSession.createProducer( jmsDest );
            }

            // Make outbound request
            Message jmsOutboundRequest = makeRequest(jmsSession, request);
            jmsProducer.send( jmsOutboundRequest );

            if ( request.isReplyExpected() ) {
                // TODO Get response!!

                SOAPMessage msg = SoapUtil.makeFaultMessage( SoapUtil.FC_SERVER, "JMS routing replies are not yet implemented" );
                String fault = SoapUtil.soapMessageToString( msg, "UTF-8" );
                if ( response instanceof XmlResponse )
                    ((XmlResponse)response).setResponseXml( fault );
                else
                    throw new PolicyAssertionException( "Only XML responses are supported" );

                request.setRoutingStatus( RoutingStatus.ROUTED );

                return AssertionStatus.NOT_YET_IMPLEMENTED;
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
        } catch ( SOAPException e ) {
            logger.log( Level.WARNING, "Caught SOAPException while constructing fault", e );
            return AssertionStatus.FAILED;
        }
    }

    private Message makeRequest( Session session, Request request ) throws PolicyAssertionException,
                                                                           JMSException, IOException {
        if ( !(request instanceof XmlRequest ) ) throw new PolicyAssertionException( "Only XML messages are supported" );
        XmlRequest xreq = (XmlRequest)request;
        if ( xreq instanceof JmsSoapRequest ) {
            // Outgoing request should be the same type (i.e. TextMessage or BytesMessage) as the original request
            JmsSoapRequest jreq = (JmsSoapRequest)xreq;
            JmsTransportMetadata jtm = (JmsTransportMetadata)jreq.getTransportMetadata();
            Message jmsOriginalRequest = jtm.getRequest();
            if ( jmsOriginalRequest instanceof TextMessage ) {
                return session.createTextMessage( xreq.getRequestXml() );
            }
        }

        // Default to BytesMessage
        BytesMessage msg = session.createBytesMessage();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        baos.write( xreq.getRequestXml().getBytes("UTF-8")); // TODO ENCODING #@)($*&!)!
        msg.writeBytes( baos.toByteArray() );
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
}
