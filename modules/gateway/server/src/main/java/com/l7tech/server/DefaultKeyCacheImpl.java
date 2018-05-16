package com.l7tech.server;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Pair;

import javax.net.ssl.KeyManager;
import java.io.IOException;
import java.security.KeyStoreException;

/**
 * Knows about the location and passwords for the SSL and CA keystores, server certificates, etc.
 */
public class DefaultKeyCacheImpl implements DefaultKeyCache {

    public DefaultKeyCacheImpl( final DefaultKey defaultKey ) {
        this.defaultKey = defaultKey;
    }

    @Override
    public SsgKeyEntry getSslInfo() throws IOException {
        return defaultKey.getSslInfo();
    }

    @Override
    public SsgKeyEntry getCaInfo() {
        return defaultKey.getCaInfo();
    }

    @Override
    public SsgKeyEntry getAuditSigningInfo() {
        return defaultKey.getAuditSigningInfo();
    }

    @Override
    public SsgKeyEntry getAuditViewerInfo() {
        return defaultKey.getAuditViewerInfo();
    }

    @Override
    public Pair<Goid, String> getAuditViewerAlias() {
        return defaultKey.getAuditViewerAlias();
    }

    public KeyManager[] getSslKeyManagers() {
        return defaultKey.getSslKeyManagers();
    }

    public SsgKeyEntry lookupKeyByKeyAlias(Pair<String,Goid> key) throws FindException, KeyStoreException, IOException {
        return defaultKey.lookupKeyByKeyAlias(key.getKey(), key.getValue());
    }

    private final DefaultKey defaultKey;
}
