package com.l7tech.server.transport.jms2;

/**
 * User: vchan
 */
public interface JmsEndpointListenerFactory {


    public JmsEndpointListener createListener(JmsEndpointConfig endpointConfig);


}
