package com.l7tech.server.transport.jms.prov;

import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.jms.ConnectionFactory;
import javax.naming.Context;

import com.l7tech.server.transport.jms.ConnectionFactoryCustomizer;
import com.l7tech.server.transport.jms.JmsConfigException;

/**
 * ConnectionFactoryCustomizer implementation for Tibco.
 *
 * <p>This customizer is required to configure SSL parameters for the ConnectionFactory.</p>
 */
public class TibcoConnectionFactoryCustomizer implements ConnectionFactoryCustomizer {

    //- PUBLIC

    /**
     * Customize the given connection factory.
     *
     * @param connectionFactory The factory to customize.
     * @param context The configuration context
     * @throws JmsConfigException if an error occurs
     */
    public void configureConnectionFactory(ConnectionFactory connectionFactory, Context context) throws JmsConfigException {
        try {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Configuring connection factory.");

            Map environment = context.getEnvironment();
        }
        catch(Exception exception) {
            throw new JmsConfigException("Error configuring connection factory.", exception);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(TibcoConnectionFactoryCustomizer.class.getName());
}
