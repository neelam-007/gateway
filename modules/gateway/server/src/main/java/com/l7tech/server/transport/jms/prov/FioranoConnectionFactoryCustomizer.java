package com.l7tech.server.transport.jms.prov;

import com.l7tech.server.transport.jms.ConnectionFactoryCustomizer;
import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.gateway.common.transport.jms.JmsConnection;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import java.util.logging.Level;
import java.util.logging.Logger;

import fiorano.jms.runtime.ptp.FioranoQueueConnectionFactory;
import fiorano.jms.md.MetaDataConstants;
import fiorano.jms.md.ConnectionFactoryMetaData;
import org.apache.ws.commons.schema.constants.Constants;

/**
 * ConnectionFactoryCustomizer implementation for FioranoMQ.
 *
 * User: dlee
 * Date: May 28, 2008
 */
public class FioranoConnectionFactoryCustomizer implements ConnectionFactoryCustomizer {

    private static final Logger logger = Logger.getLogger(FioranoConnectionFactoryCustomizer.class.getName());

    public void configureConnectionFactory(JmsConnection jmsConnection, ConnectionFactory connectionFactory, Context jndiContext) throws JmsConfigException {
        try {
            logger.log(Level.FINE, "Configuring Fiorano connection factory.");

            //Map environment = jndiContext.getEnvironment(); // not dealt with here; will/should be passed to the security manager by fiorano lib

            if (connectionFactory instanceof FioranoQueueConnectionFactory) {
                final FioranoQueueConnectionFactory f = (FioranoQueueConnectionFactory) connectionFactory;
                // connection factory (ssl) configuration is inherited from the jndi config
                // fiorano client lib seems to ignore the new params set here
                ConnectionFactoryMetaData md = f.getConnectionFactoryMetaData();
            }
        }
        catch(Exception exception) {
            throw new JmsConfigException("Error configuring Fiorano connection factory.", exception);
        }
    }
}
