/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.transport.jms;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.logging.LogManager;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ComponentConfig;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerComponentLifecycle;
import com.l7tech.server.ServerConfig;

import javax.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsMessageListener implements MessageListener, ServerComponentLifecycle {
    JmsMessageListener(QueueSession session, JmsReceiver receiver) {
        _session = session;
        _receiver = receiver;

        String jmsThreadpoolSize = ServerConfig.getInstance().getProperty(ServerConfig.PARAM_JMS_THREAD_POOL_SIZE);
        int poolsize = ServerConfig.DEFAULT_JMS_THREAD_POOL_SIZE;
        if (jmsThreadpoolSize != null && jmsThreadpoolSize.length() > 0) {
            try {
                poolsize = Integer.parseInt(jmsThreadpoolSize);
            } catch (NumberFormatException nfe) {
                _logger.info("Invalid JMS thread pool size parameter '" + jmsThreadpoolSize + "'... Using default of " + poolsize);
            }
        }
        _threadPool = new PooledExecutor(poolsize);
        _threadPool.waitWhenBlocked();
    }

    public void onMessage(Message jmsRequest) {
        TextMessage jmsResponse = null;
        try {
            jmsResponse = _session.createTextMessage();
            JmsTransportMetadata jtm = new JmsTransportMetadata(jmsRequest, jmsResponse);
            JmsRequestHandler handler = new JmsRequestHandler(this, jtm);
            _threadPool.execute(handler);
        } catch (JMSException e) {
            _logger.log(Level.WARNING, "Couldn't create response message!", e);
        } catch (InterruptedException e) {
            _logger.log(Level.WARNING, e.toString(), e);
        }
    }

    void setSession(QueueSession session) {
        _session = session;
    }

    public void init(ComponentConfig config) throws LifecycleException {
    }

    public void start() throws LifecycleException {
        if (_session == null) throw new LifecycleException("Can't start without a session!");
    }

    public void stop() throws LifecycleException {
        _session = null;
    }

    public void close() throws LifecycleException {
        _session = null;
    }

    public void sendResponse(JmsSoapRequest soapRequest, JmsSoapResponse soapResponse,
                             AssertionStatus status) {
        JmsEndpoint out = _receiver.getOutboundResponseEndpoint();
        JmsEndpoint fail = _receiver.getFailureEndpoint();
        if (status == AssertionStatus.NONE) {
            if (out != null) {
                String qname = out.getDestinationName();
            }
        }

    }

    private static PooledExecutor _threadPool;
    private QueueSession _session;
    private final Logger _logger = LogManager.getInstance().getSystemLogger();
    private JmsReceiver _receiver;
}
