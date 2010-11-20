package com.l7tech.server.transport.jms2;

/**
 * User: vchan
 */
public interface JmsEndpointListenerFactory {

    /**
     * Create an endpoint listener for the given configuration.
     *
     * @param endpointConfig The configuration for the endpoint.
     * @return The endpoint to use
     */
    JmsEndpointListener createListener(JmsEndpointConfig endpointConfig);

}
