/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import com.l7tech.server.ServerComponentLifecycle;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.MessageProcessor;
import com.l7tech.logging.LogManager;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;

import javax.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsMessageListener implements MessageListener, ServerComponentLifecycle {
    JmsMessageListener(QueueSession session, JmsReceiver receiver) {
        _session = session;
        _receiver = receiver;
    }

    public void onMessage(Message jmsRequest) {
        TextMessage jmsResponse = null;
        try {
            jmsResponse = _session.createTextMessage();
            JmsTransportMetadata jtm = new JmsTransportMetadata( jmsRequest, jmsResponse );

            JmsSoapRequest soapRequest = new JmsSoapRequest( jtm );
            JmsSoapResponse soapResponse = new JmsSoapResponse( jtm );

            AssertionStatus status = _messageProcessor.processMessage( soapRequest, soapResponse );

            // TODO build response

            jmsRequest.acknowledge(); // TODO ack semantics

        } catch (JMSException e) {
            _logger.log( Level.WARNING, "Couldn't create response message!", e );
        } catch (PolicyAssertionException e) {
            _logger.log( Level.SEVERE, e.toString(), e );
        } catch (IOException e) {
            _logger.log( Level.WARNING, e.toString(), e );
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    void setSession( QueueSession session ) {
        _session = session;
    }

    public void init(ServerConfig config) throws LifecycleException {
        _messageProcessor = MessageProcessor.getInstance();
    }

    public void start() throws LifecycleException {
        if ( _session == null ) throw new LifecycleException( "Can't start without a session!" );
    }

    public void stop() throws LifecycleException {
        _session = null;
    }

    public void close() throws LifecycleException {
        _session = null;
    }

    private QueueSession _session;
    private JmsReceiver _receiver;
    private MessageProcessor _messageProcessor;
    private final Logger _logger = LogManager.getInstance().getSystemLogger();
}
