package com.l7tech.server.security.keystore;

import com.l7tech.common.security.JceProvider;
import com.l7tech.server.security.keystore.sca.ScaSsgKeyStore;
import com.l7tech.server.security.keystore.software.TomcatSsgKeyFinder;
import com.l7tech.server.security.keystore.software.DatabasePkcs12SsgKeyStore;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.cluster.ClusterPropertyManager;

import java.util.*;
import java.security.KeyStoreException;

/**
 *
 */
public class SsgKeyStoreManagerImpl implements SsgKeyStoreManager {
    private final List<SsgKeyFinder> keystores;
    private char[] softwareKeystorePasssword = "asdfhwkje".toCharArray(); // TODO use secure cluster shared key

    public SsgKeyStoreManagerImpl(ClusterPropertyManager cpm, KeystoreUtils keystoreUtils) throws KeyStoreException {
        // TODO maybe offer software keystores even if HSM is available?
        if (isHsmAvailable()) {
            List<SsgKeyFinder> list = new ArrayList<SsgKeyFinder>();
            list.add(new ScaSsgKeyStore(1, "HSM"));
            keystores = Collections.unmodifiableList(list);
        } else {
            List<SsgKeyFinder> list = new ArrayList<SsgKeyFinder>();
            list.add(new TomcatSsgKeyFinder(0, "Software Static", keystoreUtils));
            list.add(new DatabasePkcs12SsgKeyStore(2, "Software DB", cpm, softwareKeystorePasssword));
            keystores = Collections.unmodifiableList(list);
        }
    }

    public boolean isHsmAvailable() {
        return JceProvider.PKCS11_ENGINE.equals(JceProvider.getEngineClass());
    }

    public List<SsgKeyFinder> findAll() {
        return keystores;
    }
}
