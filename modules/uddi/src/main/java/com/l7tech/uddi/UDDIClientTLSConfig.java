package com.l7tech.uddi;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.HostnameVerifier;
import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;

/**
 * TLS Configuration settings for UDDIClients.
 */
public class UDDIClientTLSConfig {

    //- PUBLIC

    public UDDIClientTLSConfig( final KeyManager keyManager,
                                final TrustManager trustManager,
                                final HostnameVerifier hostnameVerifier ) {
        this.keyManager = keyManager;
        this.trustManager = trustManager;
        this.hostnameVerifier = hostnameVerifier;
    }

    public KeyManager[] getKeyManagers() {
        if ( keyManager == null ) {
            return null;            
        } else {
            return new KeyManager[]{ keyManager };
        }
    }

    public TrustManager[] getTrustManagers() {
        if ( trustManager == null ) {
            return null;
        } else {
            return new TrustManager[]{ trustManager };
        }
    }

    public static Collection<TLSConfigAdapter> getDefaultAdapters() {
        Collection<TLSConfigAdapter> adapters;
        synchronized (defaultAdapters) {
            adapters = new ArrayList<TLSConfigAdapter>(defaultAdapters);
        }
        return adapters;
    }

    public static void resetDefaultAdapters() {
        defaultAdapters.clear();
    }

    public static void addDefaultAdapter( final TLSConfigAdapter adapter ) {
        defaultAdapters.add( adapter );
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public static interface TLSConfigAdapter {
        /**
         * Configuration adapter for client/framework SSL settings.
         *
         * @param target The target to configure.
         * @param config The configuration to use.
         * @return True if the target type was supported.
         */
        public boolean configure( Object target, UDDIClientTLSConfig config );
    }

    //- PRIVATE

    private static final Collection<TLSConfigAdapter> defaultAdapters = Collections.synchronizedCollection( new ArrayList<TLSConfigAdapter>() );

    private final KeyManager keyManager;
    private final TrustManager trustManager;
    private final HostnameVerifier hostnameVerifier;
}
