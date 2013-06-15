package com.l7tech.common.http.prov.apache.components;

import com.l7tech.common.http.GenericHttpException;

import java.net.URL;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 4/5/13
 *
 */
public class HttpComponentsUtils {
    /**
     * this is a utility class, constructor is private
     */
    private HttpComponentsUtils() {}

    /**
     * check url validity
     * @param targetUrl
     * @throws GenericHttpException
     */
    public static void checkUrl( final URL targetUrl ) throws GenericHttpException {
        final String protocol = targetUrl.getProtocol() == null ? null : targetUrl.getProtocol().toLowerCase();
        if ( protocol == null ||
                (!protocol.equals( "http" ) && !protocol.equals( "https" ) ) ) {
            throw new GenericHttpException( "Unsupported protocol: " + protocol );
        }

        final int port = targetUrl.getPort();
        if ( port > 65535 ) {
            throw new GenericHttpException( "Invalid port: " + port );
        }
    }
}
