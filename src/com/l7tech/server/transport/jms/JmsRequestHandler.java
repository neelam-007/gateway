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

import javax.jms.Message;
import javax.jms.TextMessage;
import javax.jms.JMSException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
class JmsRequestHandler implements Runnable {
    JmsRequestHandler(JmsMessageListener jmsMessageListener, JmsTransportMetadata jmd) {
        _jmsMessageListener = jmsMessageListener;
        _jmsMetadata = jmd;
    }

    public JmsTransportMetadata getTransportMetadata() {
        return _jmsMetadata;
    }

    public void run() {
        AssertionStatus status = AssertionStatus.UNDEFINED;

        Message jmsRequest = _jmsMetadata.getRequest();
        JmsSoapRequest soapRequest = new JmsSoapRequest( _jmsMetadata );
        JmsSoapResponse soapResponse = new JmsSoapResponse( _jmsMetadata );

        try {
            jmsRequest.acknowledge(); // TODO parameterize acknowledge semantics?

            status = MessageProcessor.getInstance().processMessage( soapRequest, soapResponse );

            // TODO build response

            Message jmsResponse = _jmsMetadata.getResponse();
            if ( jmsResponse instanceof TextMessage ) {
                TextMessage tresp = (TextMessage)jmsResponse;
                String responseXml = soapResponse.getResponseXml();
                tresp.setText( responseXml );
            }

            _jmsMessageListener.sendResponse( soapRequest, soapResponse, status );
        } catch (IOException e) {
            _logger.log( Level.WARNING, e.toString(), e );
        } catch (PolicyAssertionException e) {
            _logger.log( Level.WARNING, e.toString(), e );
        } catch (JMSException e) {
            _logger.log( Level.WARNING, "Couldn't acknowledge message!", e );
        }
    }

    private Logger _logger = LogManager.getInstance().getSystemLogger();
    private JmsTransportMetadata _jmsMetadata;
    private JmsMessageListener _jmsMessageListener;
}
