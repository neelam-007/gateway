package com.l7tech.server.transport;

import com.l7tech.server.DefaultKey;
import com.l7tech.server.transport.http.DefaultHttpCiphers;

/**
 */
public class TransportAdminHelper {
    private final DefaultKey defaultKeystore;

    public TransportAdminHelper(final DefaultKey defaultKeystore) {
        this.defaultKeystore = defaultKeystore;
    }

    public String[] getDefaultCipherSuiteNames() {
        return DefaultHttpCiphers.getRecommendedCiphers().split(",");
    }

    public String[] getAllProtocolVersions(boolean defaultProviderOnly) {
        return DefaultHttpCiphers.getAllProtocolVersions(defaultProviderOnly, defaultKeystore);
    }

    public String[] getAllCipherSuiteNames() {
        return DefaultHttpCiphers.getAllSupportedCiphers(defaultKeystore);
    }

    public String[] getVisibleCipherSuiteNames() {
        return DefaultHttpCiphers.getAllVisibleCiphers().split(",");
    }
}
