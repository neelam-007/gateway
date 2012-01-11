package com.l7tech.server.globalresources;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.server.transport.http.HttpConnectionManagerListener;
import com.l7tech.util.ConfigFactory;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A GenericHttpClientFactory that creates HTTP configuration aware CommonsHttpClients.
 */
public class ConfiguredCommonsHttpClientFactory extends ConfiguredHttpClientFactory {

    //- PUBLIC

    public ConfiguredCommonsHttpClientFactory( final HttpConfigurationCache httpConfigurationCache,
                                               final HttpConnectionManagerListener listener,
                                               final boolean useSslKeyForDefault ) {
        super( httpConfigurationCache, useSslKeyForDefault );
        if ( notifiedConnectionManager.compareAndSet( false, true ) ) {
            // This is a bit dubious since we won't always notify the given
            // listener. The manager is only created once though, so it seems
            // safest to only notify once.
            listener.notifyHttpConnectionManagerCreated( connectionManager );
        }
    }

    //- PACKAGE

    @Override
    GenericHttpClient newGenericHttpClient( final int connectTimeout,
                                            final int readTimeout ) {
        return new CommonsHttpClient( connectionManager, connectTimeout, readTimeout );
    }

    //- PRIVATE

    private static final String PROP_MAX_CONN_PER_HOST = ConfiguredCommonsHttpClientFactory.class.getName() + ".maxConnectionsPerHost";
    private static final String PROP_MAX_TOTAL_CONN = ConfiguredCommonsHttpClientFactory.class.getName() + ".maxTotalConnections";

    private static final int MAX_CONNECTIONS_PER_HOST = ConfigFactory.getIntProperty( PROP_MAX_CONN_PER_HOST, 100 );
    private static final int MAX_CONNECTIONS = ConfigFactory.getIntProperty( PROP_MAX_TOTAL_CONN, 1000 );

    private static final AtomicBoolean notifiedConnectionManager = new AtomicBoolean(false);
    private static final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    static {
        final HttpConnectionManagerParams params = connectionManager.getParams();
        params.setMaxConnectionsPerHost( HostConfiguration.ANY_HOST_CONFIGURATION, MAX_CONNECTIONS_PER_HOST );
        params.setMaxTotalConnections( MAX_CONNECTIONS );
    }

}
