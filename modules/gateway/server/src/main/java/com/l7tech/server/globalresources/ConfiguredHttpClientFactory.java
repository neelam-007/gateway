package com.l7tech.server.globalresources;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpClientFactory;

/**
 * Support class for GenericHttpClientFactory's that create HTTP configuration aware GenericHttpClients.
 */
abstract class ConfiguredHttpClientFactory implements GenericHttpClientFactory {

    //- PUBLIC

    @Override
    public GenericHttpClient createHttpClient() {
        return configure(newGenericHttpClient( -1, -1 ));
    }

    /**
     * Create a new HTTP Client.
     *
     * @param hostConnections IGNORED
     * @param totalConnections IGNORED
     * @param connectTimeout Ignored by some factories.
     * @param timeout Ignored by some factories.
     * @param identity IGNORED this factory does not support identity binding
     */
    @Override
    public GenericHttpClient createHttpClient( final int hostConnections,
                                               final int totalConnections,
                                               final int connectTimeout,
                                               final int timeout,
                                               final Object identity ) {
        return configure(newGenericHttpClient( connectTimeout, timeout ));
    }

    //- PACKAGE

    ConfiguredHttpClientFactory( final HttpConfigurationCache httpConfigurationCache,
                                 final boolean useSslKeyForDefault ) {
        this.httpConfigurationCache = httpConfigurationCache;
        this.useSslKeyForDefault = useSslKeyForDefault;
    }

    /**
     * Create a new GenericHttpClient (without configuration support)
     *
     * @param connectTimeout The connection timeout or -1 to use the default.
     * @param readTimeout The read timeout or -1 to use the default.
     * @return The new client
     */
    abstract GenericHttpClient newGenericHttpClient( final int connectTimeout,
                                                     final int readTimeout );

    /**
     * Wrap the given client to make it HTTP configuration aware.
     *
     * @param client The client to wrap.
     * @return The HTTP configuration aware client.
     */
    GenericHttpClient configure( final GenericHttpClient client ) {
        return new ConfiguredGenericHttpClient( client, httpConfigurationCache, useSslKeyForDefault);
    }

    //- PRIVATE

    private final HttpConfigurationCache httpConfigurationCache;
    private final boolean useSslKeyForDefault;
}
