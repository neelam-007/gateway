/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.MessageProcessor;
import com.l7tech.logging.LogManager;
import com.l7tech.common.transport.jms.JmsEndpoint;

import javax.jms.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
class JmsRequestHandler {
    /**
     * Handle an incoming JMS SOAP request.  Also takes care of sending the reply if appropriate.
     * @param receiver
     * @param bag
     * @param jmsRequest
     */
    public void onMessage( JmsReceiver receiver, JmsBag bag, Message jmsRequest ) {
        TextMessage jmsResponse = null;
        try {
            jmsResponse = bag.getSession().createTextMessage();
            JmsTransportMetadata jtm = new JmsTransportMetadata(jmsRequest, jmsResponse);
            processMessage( jtm, receiver, bag );
        } catch (JMSException e) {
            _logger.log(Level.WARNING, "Couldn't create response message!", e);
        }
    }

    private void processMessage( JmsTransportMetadata jmsMetadata, JmsReceiver receiver, JmsBag bag ) {
        AssertionStatus status = AssertionStatus.UNDEFINED;

        Message jmsRequest = jmsMetadata.getRequest();
        JmsSoapRequest soapRequest = new JmsSoapRequest( jmsMetadata );
        JmsSoapResponse soapResponse = new JmsSoapResponse( jmsMetadata );

        try {
            jmsRequest.acknowledge(); // TODO parameterize acknowledge semantics?

            status = MessageProcessor.getInstance().processMessage( soapRequest, soapResponse );

            // TODO build response

            Message jmsResponse = jmsMetadata.getResponse();
            if ( jmsResponse instanceof TextMessage ) {
                TextMessage tresp = (TextMessage)jmsResponse;
                String responseXml = soapResponse.getResponseXml();
                tresp.setText( responseXml );
            }

            sendResponse( soapRequest, soapResponse, receiver, bag, status );
        } catch (IOException e) {
            _logger.log( Level.WARNING, e.toString(), e );
        } catch (PolicyAssertionException e) {
            _logger.log( Level.WARNING, e.toString(), e );
        } catch (JMSException e) {
            _logger.log( Level.WARNING, "Couldn't acknowledge message!", e );
        }
    }

    private void sendResponse( JmsSoapRequest soapRequest, JmsSoapResponse soapResponse,
                              JmsReceiver receiver, JmsBag bag,
                              AssertionStatus status ) {

        JmsTransportMetadata jtm = (JmsTransportMetadata)soapRequest.getTransportMetadata();
        Message jmsRequestMsg = jtm.getRequest();
        Message jmsResponseMsg = jtm.getResponse();

        try {
            Queue jmsReplyDest = (Queue) receiver.getOutboundResponseDestination( jmsRequestMsg, jmsResponseMsg );
            if ( status != AssertionStatus.NONE ) {
                // Send response to failure endpoint if defined
                JmsEndpoint fail = receiver.getFailureEndpoint();

                if ( fail == null ) {
                    _logger.fine( "Failure response will be sent to response destination" );
                } else {
                    jmsReplyDest = receiver.getJmsFailureQueue();
                    _logger.fine( "Failure response will be sent to " + fail.getDestinationName() );
                }
            }

            if ( jmsReplyDest == null ) {
                _logger.fine( "No response will be sent!" );
            } else {
                _logger.fine( "Sending response to " + jmsReplyDest );
                MessageProducer replySender = null;
                try {
                    replySender = bag.getSession().createProducer( jmsReplyDest );
                    jmsResponseMsg.setJMSCorrelationID( jmsRequestMsg.getJMSCorrelationID() );
                    replySender.send( jmsResponseMsg );
                    _logger.fine( "Sent response to " + jmsReplyDest );
                } finally {
                    if ( replySender != null ) replySender.close();
                }
            }
        } catch ( JMSException e ) {
            _logger.log( Level.WARNING, "Caught JMS exception while sending response", e );
        }
    }

    private static Logger _logger = LogManager.getInstance().getSystemLogger();
}
