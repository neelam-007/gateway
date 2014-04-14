package com.l7tech.common.http.prov.apache;

import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.params.HttpParams;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 3/14/14
 */
public class Ntlm2SchemeFactory implements AuthSchemeFactory {
    /**
     * Creates an instance of {@link org.apache.http.auth.AuthScheme} using given HTTP parameters.
     *
     * @param params HTTP parameters.
     * @return auth scheme.
     */
    @Override
    public AuthScheme newInstance(HttpParams params) {
        return new Ntlm2AuthScheme();
    }
}
