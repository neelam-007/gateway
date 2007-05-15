package com.l7tech.server.security.keystore.sca;

import com.l7tech.common.security.JceProvider;
import com.l7tech.server.security.keystore.JdkKeyStoreBackedSsgKeyStore;
import static com.l7tech.server.security.keystore.SsgKeyFinder.SsgKeyStoreType.PKCS11_HARDWARE;
import com.l7tech.server.security.keystore.SsgKeyStore;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.logging.Logger;

/**
 * SsgKeyStore view of this node's local SCA 6000 board.
 */
public class ScaSsgKeyStore extends JdkKeyStoreBackedSsgKeyStore implements SsgKeyStore {
    protected static final Logger logger = Logger.getLogger(ScaSsgKeyStore.class.getName());
    private static final String DB_FORMAT = "hsm.sca.targz";

    private final long id;
    private final String name;
    private final KeyStore keystore;

    public ScaSsgKeyStore(long id, String name) throws KeyStoreException {
        if (!(JceProvider.PKCS11_ENGINE.equals(JceProvider.getEngineClass())))
            throw new KeyStoreException("Can only create ScaSsgKeyStore if current JceProvider is " + JceProvider.PKCS11_ENGINE);
        this.id = id;
        this.name = name;
        keystore = KeyStore.getInstance("PKCS11");
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SsgKeyStoreType getType() {
        return PKCS11_HARDWARE;
    }

    protected KeyStore keyStore() throws KeyStoreException {
        return keystore;
    }

    protected String getFormat() {
        return DB_FORMAT;
    }

    protected Logger getLogger() {
        return logger;
    }
}
