package com.l7tech.server.transport.jms.prov;

import com.l7tech.server.transport.jms.ConnectionFactoryCustomizer;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.gateway.common.transport.jms.JmsConnection;

import javax.jms.ConnectionFactory;
import javax.naming.Context;

/**
 * ConnectionFactoryCustomizer implementation for FioranoMQ.
 *
 * <p>This currently does nothing, but can be used in the future if we need to perform any
 * customization for Fiorano.</p>
 *
 * User: dlee
 * Date: May 28, 2008
 */
public class FioranoConnectionFactoryCustomizer implements ConnectionFactoryCustomizer {

    public void configureConnectionFactory( final JmsConnection jmsConnection,
                                            final ConnectionFactory connectionFactory,
                                            final Context jndiContext ) throws JmsConfigException {        
    }
}
