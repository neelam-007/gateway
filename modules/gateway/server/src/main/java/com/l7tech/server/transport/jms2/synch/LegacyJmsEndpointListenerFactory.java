package com.l7tech.server.transport.jms2.synch;

import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsEndpointListener;
import com.l7tech.server.transport.jms2.JmsEndpointListenerFactory;

/**
 * This factory impl class will create single-threaded endpoint listeners (i.e. "legacy" operation).
 * Using a re-factored implementation of the previous JmsReceiver class which does not support any
 * parallelism in processing Jms messages.
 *
 * @author: vchan
 */
public class LegacyJmsEndpointListenerFactory implements JmsEndpointListenerFactory {

    /**
     * Creates a JmsEndpointListeners implemented by the LegacyJmEndpointListenerImpl type.
     *
     * @param endpointConfig the configuration properties for one Jms endpoint
     * @return a JmsEndpointListener instance
     */
    @Override
    public JmsEndpointListener createListener(final JmsEndpointConfig endpointConfig) {

        return new LegacyJmsEndpointListenerImpl(endpointConfig);
    }
}
