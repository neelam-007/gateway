/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.common.message.JmsKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import org.xml.sax.SAXException;

import javax.jms.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    public void onMessage( JmsReceiver receiver, final JmsBag bag, final javax.jms.Message jmsRequest ) throws JmsRuntimeException {

        try {
            InputStream requestStream = null;
            ContentTypeHeader ctype = ContentTypeHeader.XML_DEFAULT;
            if ( jmsRequest instanceof TextMessage ) {
                requestStream = new ByteArrayInputStream(((TextMessage)jmsRequest).getText().getBytes("UTF-8"));
            } else if ( jmsRequest instanceof BytesMessage ) {
                requestStream = new BytesMessageInputStream((BytesMessage)jmsRequest);
                String requestCtype = jmsRequest.getStringProperty("Content-Type");
                if (requestCtype != null)
                    ctype = ContentTypeHeader.parseValue(requestCtype);
            } else {
                String msg = "Received message of unsupported type " + jmsRequest.getClass().getName() +
                             " on " + receiver.getInboundRequestEndpoint().getDestinationName() +
                             ".  Only TextMessage and BytesMessage are supported";
                _logger.warning( msg );
                throw new JmsRuntimeException( msg );
            }

            com.l7tech.common.message.Message request = new com.l7tech.common.message.Message();
            request.initialize(StashManagerFactory.createStashManager(), ctype, requestStream );
            request.attachJmsKnob(new JmsKnob() {
                public boolean isBytesMessage() {
                    return jmsRequest instanceof BytesMessage;
                }
            });

            final PolicyEnforcementContext context = new PolicyEnforcementContext(request,
                                                                                  new com.l7tech.common.message.Message());
            AssertionStatus status = AssertionStatus.UNDEFINED;

            Message jmsResponse = null;
            String faultMessage = null;
            String faultCode = null;

            try {
                // WebSphere MQ doesn't like this with AUTO_ACKNOWLEDGE
                // jmsRequest.acknowledge(); // TODO parameterize acknowledge semantics?

                InputStream responseStream = null;
                try {
                    status = MessageProcessor.getInstance().processMessage(context);
                    _logger.finest("Policy resulted in status " + status);
                    responseStream = context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream();
                } catch ( PolicyVersionException pve ) {
                    String msg1 = "Request referred to an outdated version of policy";
                    _logger.log( Level.INFO, msg1 );
                    faultMessage = msg1;
                    faultCode = SoapFaultUtils.FC_CLIENT;
                } catch ( Throwable t ) {
                    _logger.log( Level.WARNING, "Exception while processing JMS message", t );
                    faultMessage = t.getMessage();
                    if ( faultMessage == null ) faultMessage = t.toString();
                }

                if ( responseStream == null ) {
                    if ( faultMessage == null ) faultMessage = status.getMessage();
                    try {
                        responseStream = new ByteArrayInputStream(
                                SoapFaultUtils.generateRawSoapFault(faultCode == null ? SoapFaultUtils.FC_SERVER : faultCode,
                                                                    faultMessage, null, "").getBytes("UTF-8"));
                    } catch (SAXException e) {
                        throw new JmsRuntimeException(e);
                    }
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                HexUtils.copyStream(responseStream, baos);
                byte[] responseBytes = baos.toByteArray();
                JmsKnob requestJmsKnob = (JmsKnob)context.getRequest().getKnob(JmsKnob.class);
                if (requestJmsKnob == null)
                    throw new JmsRuntimeException("Request wasn't a JMS message");
                if (requestJmsKnob.isBytesMessage()) {
                    BytesMessage bresp = (BytesMessage)jmsResponse;
                    bresp.writeBytes(responseBytes);
                } else if ( jmsResponse instanceof TextMessage ) {
                    TextMessage tresp = (TextMessage)jmsResponse;
                    tresp.setText(new String(responseBytes, JmsUtil.DEFAULT_ENCODING));
                } else {
                    throw new JmsRuntimeException( "Can't send a " + jmsResponse.getClass().getName() +
                                                   ". Only BytesMessage and TextMessage are supported" );
                }

                sendResponse( jmsRequest, jmsResponse, bag, receiver, status );
            } catch (IOException e) {
                _logger.log( Level.WARNING, e.toString(), e );
            } catch (JMSException e) {
                _logger.log( Level.WARNING, "Couldn't acknowledge message!", e );
            } finally {
                if (context != null) {
                    try {
                        context.close();
                    } catch (Throwable t) {
                        _logger.log(Level.SEVERE, "soapRequest cleanup threw", t);
                    }
                }
            }
        } catch (JMSException e) {
            _logger.log(Level.WARNING, "Couldn't create response message!", e);
        } catch (NoSuchPartException e) {
            throw new RuntimeException(e); // can't happen
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    private void sendResponse(Message jmsRequestMsg, Message jmsResponseMsg, JmsBag bag, JmsReceiver receiver, AssertionStatus status ) {
        try {
            Destination jmsReplyDest = receiver.getOutboundResponseDestination( jmsRequestMsg, jmsResponseMsg );
            if ( status != AssertionStatus.NONE ) {
                // TODO send response to failure endpoint if defined
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

    private static final Logger _logger = Logger.getLogger(JmsRequestHandler.class.getName());
}
