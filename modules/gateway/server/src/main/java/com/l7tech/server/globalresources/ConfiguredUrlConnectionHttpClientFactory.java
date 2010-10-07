package com.l7tech.server.globalresources;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;

/**
 * A GenericHttpClientFactory that creates HTTP configuration aware UrlConnectionHttpClient.
 */
public class ConfiguredUrlConnectionHttpClientFactory extends ConfiguredHttpClientFactory {

    //- PUBLIC

    public ConfiguredUrlConnectionHttpClientFactory( final HttpConfigurationCache httpConfigurationCache,
                                                     final boolean useSslKeyForDefault ) {
        super( httpConfigurationCache, useSslKeyForDefault );
    }

    //- PACKAGE

    @Override
    GenericHttpClient newGenericHttpClient( final int connectTimeout,
                                            final int readTimeout ) {
        return new UrlConnectionHttpClient();
    }
}
