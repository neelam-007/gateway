package com.l7tech.server.transport.jms2.asynch;

import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsEndpointListener;
import com.l7tech.server.transport.jms2.JmsEndpointListenerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 *
 * @author vchan
 */
public class PooledJmsEndpointListenerFactory implements JmsEndpointListenerFactory, ApplicationContextAware {


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

    // - PRIVATE
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}