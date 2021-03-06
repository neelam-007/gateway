package com.l7tech.server.globalresources;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.HttpProxyConfig;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.server.transport.http.HttpConnectionManagerListener;
import com.l7tech.util.ConfigFactory;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.jetbrains.annotations.Nullable;

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
                                            final int readTimeout,
                                            @Nullable final HttpProxyConfig proxyConfig) {
        return new HttpComponentsClient( connectionManager, connectTimeout, readTimeout, proxyConfig );
    }


    //- PRIVATE

    private static final String PROP_MAX_CONN_PER_HOST = ConfiguredCommonsHttpClientFactory.class.getName() + ".maxConnectionsPerHost";
    private static final String PROP_MAX_TOTAL_CONN = ConfiguredCommonsHttpClientFactory.class.getName() + ".maxTotalConnections";

    private static final int MAX_CONNECTIONS_PER_HOST = ConfigFactory.getIntProperty( PROP_MAX_CONN_PER_HOST, 100 );
    private static final int MAX_CONNECTIONS = ConfigFactory.getIntProperty( PROP_MAX_TOTAL_CONN, 1000 );

    private static final AtomicBoolean notifiedConnectionManager = new AtomicBoolean(false);
    private static final PoolingClientConnectionManager connectionManager = new PoolingClientConnectionManager();
    static {
        connectionManager.setMaxTotal(MAX_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_HOST);
    }

}
