package com.l7tech.server.transport.jms;

import org.springframework.context.ApplicationContext;

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
     * @param spring
     * @throws JmsConfigException if an error occurs
     */
    void configureConnectionFactory(ConnectionFactory connectionFactory, Context context, ApplicationContext spring) 
            throws JmsConfigException;
}
