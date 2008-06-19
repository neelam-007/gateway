package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsEndpointListener;
import com.l7tech.server.transport.jms2.JmsEndpointListenerFactory;

/**
 *
 * @author: vchan
 */
public class PooledJmsEndpointListenerFactory implements JmsEndpointListenerFactory {

    /**
     * Creates JmsEndpointListener implemented by the PooledJmsEndpointListener type.
     *
     * @param endpointConfig the configuration properties for one Jms endpoint
     * @return a JmsEndpointListener instance
     */
    @Override
    public JmsEndpointListener createListener(final JmsEndpointConfig endpointConfig) {

        return new PooledJmsEndpointListenerImpl(endpointConfig);
    }
}