package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.DefaultKey;
import com.l7tech.util.Pair;

import javax.net.ssl.KeyManager;
import java.io.IOException;
import java.security.KeyStoreException;

/**
 * User: rseminoff
 * Date: 24/05/12
 */
public class MockDefaultKey implements DefaultKey {
    @Override
    public SsgKeyEntry getSslInfo() throws IOException {
        System.out.println("*** MockDefaultKey :: getSslInfo()");
        return null;
    }

    @Override
    public SsgKeyEntry getCaInfo() {
        System.out.println("*** MockDefaultKey :: getCaInfo()");
        return null;
    }

    @Override
    public SsgKeyEntry getAuditSigningInfo() {
        System.out.println("*** MockDefaultKey :: getAuditSigningInfo()");
        return null;
    }

    @Override
    public SsgKeyEntry getAuditViewerInfo() {
        System.out.println("*** MockDefaultKey :: getAuditViewerInfo()");
        return null;
    }

    @Override
    public Pair<Goid, String> getAuditViewerAlias() {
        System.out.println("*** MockDefaultKey :: getAuditViewerAliasInfo()");
        return null;
    }

    @Override
    public KeyManager[] getSslKeyManagers() {
        System.out.println("*** MockDefaultKey :: getSslKeyManagers()");
        return new KeyManager[0];
    }

    @Override
    public SsgKeyEntry lookupKeyByKeyAlias(String keyAlias, Goid preferredKeystoreId) throws FindException, KeyStoreException, IOException {
        System.out.println("*** MockDefaultKey :: lookupKeyByKeyAlias()");
        return null;
    }
}
