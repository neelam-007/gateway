package com.l7tech.server.globalresources;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.prov.apache.CommonsHttpClient;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;

/**
 * A GenericHttpClientFactory that creates HTTP configuration aware CommonsHttpClients.
 */
public class ConfiguredCommonsHttpClientFactory extends ConfiguredHttpClientFactory {

    //- PUBLIC

    public ConfiguredCommonsHttpClientFactory( final HttpConfigurationCache httpConfigurationCache,
                                               final boolean useSslKeyForDefault ) {
        super( httpConfigurationCache, useSslKeyForDefault );
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

    private static final int MAX_CONNECTIONS_PER_HOST = SyspropUtil.getInteger( PROP_MAX_CONN_PER_HOST, 100 );
    private static final int MAX_CONNECTIONS = SyspropUtil.getInteger( PROP_MAX_TOTAL_CONN, 1000 );

    private static final MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    static {
        final HttpConnectionManagerParams params = connectionManager.getParams();
        params.setMaxConnectionsPerHost( HostConfiguration.ANY_HOST_CONFIGURATION, MAX_CONNECTIONS_PER_HOST );
        params.setMaxTotalConnections( MAX_CONNECTIONS );
    }

}
