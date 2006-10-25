package com.l7tech.common.http.prov.apache;

import org.apache.commons.httpclient.params.HttpParamsFactory;
import org.apache.commons.httpclient.params.HttpParams;

/**
 * HttpParamsFactory that caches the result of a delegate factory.
 *
 * <p>This is only necessary since the default implementation contains pointless
 * synchronization.</p>
 *
 * @author Steve Jones
 */
public class CachingHttpParamsFactory implements HttpParamsFactory {

    //- PUBLIC

    public CachingHttpParamsFactory(final HttpParamsFactory httpParamsFactory) {
        this.cachedHttpParams = httpParamsFactory.getDefaultParams();
    }

    public HttpParams getDefaultParams() {
        return cachedHttpParams;
    }

    //- PRIVATE

    private final HttpParams cachedHttpParams;
}
