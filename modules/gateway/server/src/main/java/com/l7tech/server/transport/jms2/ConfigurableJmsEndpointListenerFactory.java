package com.l7tech.server.transport.jms2;

import com.l7tech.util.Config;

import java.util.Map;
import java.util.logging.Logger;

/**
 * JmsEndpointListenerFactory that delegates to a configured underlying factory.
 */
public class ConfigurableJmsEndpointListenerFactory implements JmsEndpointListenerFactory {

    //- PUBLIC

    public ConfigurableJmsEndpointListenerFactory( final Config config,
                                                   final String defaultFactory,
                                                   final Map<String,JmsEndpointListenerFactory> factories ) {
        factory = getFactory( config, defaultFactory, factories );
    }

    @Override
    public JmsEndpointListener createListener( final JmsEndpointConfig endpointConfig ) {
        return factory.createListener( endpointConfig );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ConfigurableJmsEndpointListenerFactory.class.getName() );

    private final JmsEndpointListenerFactory factory;


    private static JmsEndpointListenerFactory getFactory( final Config config,
                                                          final String defaultFactory,
                                                          final Map<String,JmsEndpointListenerFactory> factories ) {
        JmsEndpointListenerFactory factory = null;
        String name = null;

        final String configuredFactory = config != null ? config.getProperty( "ioJmsEndpointListenerFactory", null ) : null;
        if ( configuredFactory != null ) {
            factory = factories.get( configuredFactory );
            if ( factory == null ) {
                logger.warning( "Configured JMS endpoint listener factory not found '"+configuredFactory+"', using default." );
            } else {
                name = configuredFactory;
            }
        }

        if ( factory == null ) {
            factory = factories.get( defaultFactory );
            name = defaultFactory;
        }

        if ( name != null ) {
            logger.config( "Using JMS endpoint listener factory '"+name+"'." );
        }

        return factory;
    }
}
