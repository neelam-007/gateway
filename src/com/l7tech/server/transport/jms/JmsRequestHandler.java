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
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.service.PublishedService;
import com.l7tech.message.Request;

import javax.jms.*;
import javax.xml.soap.*;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

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
    public void onMessage( JmsReceiver receiver, JmsBag bag, Message jmsRequest ) throws JmsRuntimeException {
        Message jmsResponse = null;
        try {
            if ( jmsRequest instanceof TextMessage ) {
                jmsResponse = bag.getSession().createTextMessage();
            } else if ( jmsRequest instanceof BytesMessage ) {
                jmsResponse = bag.getSession().createBytesMessage();
            } else {
                String msg = "Received message of unsupported type " + jmsRequest.getClass().getName() +
                             " on " + receiver.getInboundRequestEndpoint().getDestinationName() +
                             ".  Only TextMessage and BytesMessage are supported";
                _logger.warning( msg );
                throw new JmsRuntimeException( msg );
            }
            JmsTransportMetadata jtm = new JmsTransportMetadata(jmsRequest, jmsResponse);
            processMessage( jtm, receiver, bag );
        } catch (JMSException e) {
            _logger.log(Level.WARNING, "Couldn't create response message!", e);
        }
    }

    private void processMessage( JmsTransportMetadata jmsMetadata, JmsReceiver receiver, JmsBag bag ) throws JmsRuntimeException {
        AssertionStatus status = AssertionStatus.UNDEFINED;

        Message jmsRequest = jmsMetadata.getRequest();
        JmsSoapRequest soapRequest = new JmsSoapRequest( jmsMetadata );
        JmsSoapResponse soapResponse = new JmsSoapResponse( jmsMetadata );

        try {
            jmsRequest.acknowledge(); // TODO parameterize acknowledge semantics?

            status = MessageProcessor.getInstance().processMessage( soapRequest, soapResponse );

            // TODO build response
            Message jmsResponse = jmsMetadata.getResponse();

            String responseXml = soapResponse.getResponseXml();
            if ( responseXml == null || responseXml.length() == 0 ) {
                SOAPMessage msg = SoapUtil.makeMessage();
                SOAPPart spart = msg.getSOAPPart();
                SOAPEnvelope senv = spart.getEnvelope();
                SOAPFault fault = SoapUtil.addFaultTo( msg, SoapUtil.FC_SERVER,
                                                       status.getMessage() );
                responseXml = SoapUtil.soapMessageToString( msg, "UTF-8" ); // TODO ENCODING @)$(*)!!
            }

            if ( jmsResponse instanceof TextMessage ) {
                TextMessage tresp = (TextMessage)jmsResponse;
                tresp.setText( responseXml );
            } else if ( jmsResponse instanceof BytesMessage ) {
                BytesMessage bresp = (BytesMessage)jmsResponse;
                bresp.writeBytes( responseXml.getBytes( "UTF-8" ) ); // TODO ENCODING
            } else {
                throw new JmsRuntimeException( "Can't send a " + jmsResponse.getClass().getName() +
                                               ". Only BytesMessage and TextMessage are supported" );
            }

            sendResponse( soapRequest, receiver, bag, status );
        } catch (IOException e) {
            _logger.log( Level.WARNING, e.toString(), e );
        } catch (PolicyAssertionException e) {
            _logger.log( Level.WARNING, e.toString(), e );
        } catch (JMSException e) {
            _logger.log( Level.WARNING, "Couldn't acknowledge message!", e );
        } catch ( SOAPException e ) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private SOAPEnvelope makeFault( String faultCode, String faultString ) throws SOAPException {
        SOAPMessage msg = SoapUtil.makeMessage();
        SOAPPart spart = msg.getSOAPPart();
        SOAPEnvelope senv = spart.getEnvelope();
        SOAPFault fault = SoapUtil.addFaultTo(msg, faultCode, faultString);
        msg.saveChanges();
        return senv;
    }

    private void sendResponse( JmsSoapRequest soapRequest,
                              JmsReceiver receiver, JmsBag bag,
                              AssertionStatus status ) {

        JmsTransportMetadata jtm = (JmsTransportMetadata)soapRequest.getTransportMetadata();
        Message jmsRequestMsg = jtm.getRequest();
        Message jmsResponseMsg = jtm.getResponse();

        try {
            Destination jmsReplyDest = receiver.getOutboundResponseDestination( jmsRequestMsg, jmsResponseMsg );
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
                MessageProducer producer = null;
                try {
                    Session session = bag.getSession();
                    if ( session instanceof QueueSession ) {
                        producer = ((QueueSession)session).createSender( (Queue)jmsReplyDest );
                    } else if ( session instanceof TopicSession ) {
                        producer = ((TopicSession)session).createPublisher( (Topic)jmsReplyDest );
                    } else {
                        producer = session.createProducer( jmsReplyDest );
                    }
                    jmsResponseMsg.setJMSCorrelationID( jmsRequestMsg.getJMSCorrelationID() );
                    producer.send( jmsResponseMsg );
                    _logger.fine( "Sent response to " + jmsReplyDest );
                } finally {
                    if ( producer != null ) producer.close();
                }
            }
        } catch ( JMSException e ) {
            _logger.log( Level.WARNING, "Caught JMS exception while sending response", e );
        }
    }

    private static Logger _logger = LogManager.getInstance().getSystemLogger();
}
