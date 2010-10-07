package com.l7tech.server.globalresources;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpException;
import com.l7tech.common.http.GenericHttpRequest;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpMethod;

/**
 * A GenericHttpClient that is HTTP configuration aware.
 */
class ConfiguredGenericHttpClient implements GenericHttpClient {

    //- PUBLIC

    @Override
    public GenericHttpRequest createRequest( final HttpMethod method,
                                             final GenericHttpRequestParams params ) throws GenericHttpException {
        updateParams( params );
        return httpClient.createRequest( method, params );
    }

    //- PACKAGE

    ConfiguredGenericHttpClient( final GenericHttpClient httpClient,
                                 final HttpConfigurationCache httpConfigurationCache,
                                 final boolean useSslKeyForDefault ) {
        this.httpClient = httpClient;
        this.httpConfigurationCache = httpConfigurationCache;
        this.useSslKeyForDefault = useSslKeyForDefault;
    }

    //- PRIVATE

    private final GenericHttpClient httpClient;
    private final HttpConfigurationCache httpConfigurationCache;
    private final boolean useSslKeyForDefault;

    private void updateParams( final GenericHttpRequestParams params ) {
        httpConfigurationCache.configure( params, useSslKeyForDefault );
    }
}
