package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.transport.jms2.AbstractJmsEndpointListener;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;

import java.util.logging.Logger;

/**
 * @author vchan
 */
class PooledJmsEndpointListenerImpl extends AbstractJmsEndpointListener {

    private static final Logger _logger = Logger.getLogger(PooledJmsEndpointListenerImpl.class.getName());

    /**
     * Constructor.
     *
     * @param endpointConfig attributes for the Jms endpoint to listen to
     */
    PooledJmsEndpointListenerImpl(final JmsEndpointConfig endpointConfig) {
        super(endpointConfig, _logger);
    }

    @Override
    public void stop() {
        super.stop();
    }

}
