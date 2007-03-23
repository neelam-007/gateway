package com.l7tech.server.transport.jms;

import javax.naming.Context;
import javax.jms.ConnectionFactory;

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
     * @param connectionFactory The factory to configure
     * @param context Contextual configuration items
     * @throws JmsConfigException if an error occurs
     */
    void configureConnectionFactory(ConnectionFactory connectionFactory, Context context) throws JmsConfigException;
}
