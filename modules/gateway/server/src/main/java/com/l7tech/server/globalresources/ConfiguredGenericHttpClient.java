package com.l7tech.server.globalresources;

import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.http.GenericHttpException;
import com.l7tech.common.http.GenericHttpRequest;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpMethod;

import java.net.URL;

/**
 * A GenericHttpClient that is HTTP configuration aware.
 */
class ConfiguredGenericHttpClient implements GenericHttpClient {

    //- PUBLIC

    @Override
    public GenericHttpRequest createRequest( final HttpMethod method,
                                             final GenericHttpRequestParams params ) throws GenericHttpException {
        return httpClient.createRequest( method, resolvingParams( params ) );
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

    private GenericHttpRequestParams resolvingParams( final GenericHttpRequestParams params ) {
        final ResolvingGenericHttpRequestParams resolved
                = new ResolvingGenericHttpRequestParams( httpConfigurationCache, useSslKeyForDefault, params );
        return resolved.resolve( params.getTargetUrl() );
    }

    private static final class ResolvingGenericHttpRequestParams extends GenericHttpRequestParams {
        private final HttpConfigurationCache httpConfigurationCache;
        private final boolean useSslKeyForDefault;
        private final GenericHttpRequestParams originalParameters;

        private ResolvingGenericHttpRequestParams( final HttpConfigurationCache httpConfigurationCache,
                                                   final boolean useSslKeyForDefault,
                                                   final GenericHttpRequestParams originalParameters) {
            super( originalParameters );
            this.httpConfigurationCache = httpConfigurationCache;
            this.useSslKeyForDefault = useSslKeyForDefault;
            this.originalParameters = originalParameters;
        }

        @Override
        public GenericHttpRequestParams resolve( final URL url ) {
            final ResolvingGenericHttpRequestParams resolved = new ResolvingGenericHttpRequestParams(
                httpConfigurationCache,
                useSslKeyForDefault,
                originalParameters    
            );

            resolved.setTargetUrl( url );

            httpConfigurationCache.configure( resolved, useSslKeyForDefault );

            return resolved;
        }
    }
}
