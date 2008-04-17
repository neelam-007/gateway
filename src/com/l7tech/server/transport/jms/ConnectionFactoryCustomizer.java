package com.l7tech.server.transport.jms;

import com.l7tech.common.transport.jms.JmsConnection;

import javax.jms.ConnectionFactory;
import javax.naming.Context;

/**
 * A ConnectionFactoryCustomizer can be used to perform additional setup on a
 * ConnectionFactory.
 *
 * @author Steve Jones
 */
public interface ConnectionFactoryCustomizer {

    /**
     * Configure the given connection factory.
     *
     * @param jmsConnection the JmsConnection bean with the configuration for the desired connection
     * @param connectionFactory The factory to configure
     * @param jndiContext Contextual configuration items
     * @throws JmsConfigException if an error occurs
     */
    void configureConnectionFactory(JmsConnection jmsConnection,
                                    ConnectionFactory connectionFactory,
                                    Context jndiContext
    )
            throws JmsConfigException;
}
