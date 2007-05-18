package com.l7tech.server.security.keystore;

import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;

import javax.naming.ldap.LdapName;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for SsgKeyStore implementations that are based on a JDK KeyStore instance.
 */
public abstract class JdkKeyStoreBackedSsgKeyStore implements SsgKeyStore {
    /**
     * Get the KeyStore instance that we will be working with.
     * @return a KeyStore instance ready to use.   Never null.
     * @throws java.security.KeyStoreException if there is a problem obtaining a KeyStore instance.
     */
    protected abstract KeyStore keyStore() throws KeyStoreException;

    /** @return the format string for this keystore, to store in the format column in the DB, ie "hsm" or "sdb". */
    protected abstract String getFormat();

    public List<String> getAliases() throws KeyStoreException {
        KeyStore keystore = keyStore();

        List<String> ret = new ArrayList<String>();
        Enumeration<String> en = keystore.aliases();
        while (en.hasMoreElements()) {
            String s = en.nextElement();
            if (keystore.isKeyEntry(s)) {
                ret.add(s);
            }
        }
        return ret;
    }

    public SsgKeyEntry getCertificateChain(String alias) throws KeyStoreException {
        KeyStore keystore = keyStore();

        Certificate[] chain = keystore.getCertificateChain(alias);
        if (chain == null)
            throw new KeyStoreException("Keystore " + getName() + " does not contain any certificate chain entry with alias " + alias);
        if (chain.length < 1)
            throw new KeyStoreException("Keystore " + getName() + " contains an empty certificate chain entry for alias " + alias);

        // Make sure all are correct type
        X509Certificate[] x509chain = new X509Certificate[chain.length];
        for (int i = 0; i < chain.length; ++i) {
            if (chain[i] == null)
                throw new KeyStoreException("Keystore " + getName() + " entry with alias " + alias + " contains a null entry in its cert chain"); // shouldn't happen
            if (chain[i] instanceof X509Certificate)
                x509chain[i] = (X509Certificate)chain[i];
            else
                throw new KeyStoreException("Keystore " + getName() + " entry with alias " + alias + " contains a non-X.509 Certificate of type " + chain[i].getClass());
        }

        // Get the private key, if we can access it
        RSAPrivateKey rsaPrivateKey = null;
        try {
            Key key = keystore.getKey(alias, null);
            if (key instanceof RSAPrivateKey)
                rsaPrivateKey = (RSAPrivateKey)key;
        } catch (NoSuchAlgorithmException e) {
            getLogger().log(Level.WARNING, "Unsupported key type in cert entry in " + "Keystore " + getName() + " with alias " + alias + ": " + ExceptionUtils.getMessage(e), e);
            // Fallthrough and do without
        } catch (UnrecoverableKeyException e) {
            getLogger().log(Level.WARNING, "Unrecoverable key in cert entry in " + "Keystore " + getName() + " with alias " + alias + ": " + ExceptionUtils.getMessage(e), e);
            // Fallthrough and do without
        }

        return new SsgKeyEntry(getId(), alias, x509chain, rsaPrivateKey);
    }

    /**
     * Get a Logger that can be used to log warnings about things like cert chain entries that are being
     * omitted from a list due to some problem, or private key entries that cannot be used.
     *
     * @return a Logger instance.  Never null.
     */
    protected abstract Logger getLogger();

    private void storePrivateKeyEntryImpl(SsgKeyEntry entry) throws KeyStoreException {
        final String alias = entry.getAlias();
        if (alias == null || alias.trim().length() > 0)
            throw new KeyStoreException("Unable to store private key entry with null or empty alias");

        final X509Certificate[] chain = entry.getCertificateChain();
        if (chain == null || chain.length < 1)
            throw new KeyStoreException("Unable to store private key entry with null or empty certificate chain");

        final RSAPrivateKey key;
        try {
            key = entry.getRSAPrivateKey();
            if (key == null)
                throw new KeyStoreException("Unable to store private key entry with null private key");
        } catch (UnrecoverableKeyException e) {
            throw new KeyStoreException("Unable to store private key entry with unrecoverable private key");
        }

        KeyStore keystore = keyStore();
        keystore.setKeyEntry(alias, key, null, chain);
    }

    public CertificateRequest makeCsr(LdapName dn, KeyPair keyPair) throws InvalidKeyException, SignatureException, KeyStoreException {
        // Requires that current crypto engine already by the correct one for this keystore type
        return JceProvider.makeCsr(dn.toString(), keyPair);
    }

    public boolean isMutable() {
        return true;
    }

    public SsgKeyStore getKeyStore() {
        return this;
    }

    public synchronized void storePrivateKeyEntry(final SsgKeyEntry entry) throws KeyStoreException {
        mutateKeystore(new Functions.Nullary<Object>() {
            public Object call() {
                try {
                    storePrivateKeyEntryImpl(entry);
                    return null;
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public synchronized KeyPair generateRsaKeyPair(final int keyBits) throws InvalidAlgorithmParameterException, KeyStoreException {
        return mutateKeystore(new Functions.Nullary<KeyPair>() {
            public KeyPair call() {
                // Requires that current crypto engine already by the correct one for this keystore type
                return JceProvider.generateRsaKeyPair(keyBits);
            }
        });
    }


    /**
     * Load the keystore from the database, mutate it, and save it back, all atomically.
     *
     * @param mutator  a Runnable that will mutate the current keystore, which will be guaranteed
     *                 to be up-to-date and non-null when the runnable is invoked.
     * @throws java.security.KeyStoreException if the runnable throws a RuntimeException or if any other problem occurs during
     *                           the process
     * @return the value returned by the mutator
     */
    protected abstract <OUT> OUT mutateKeystore(Functions.Nullary<OUT> mutator) throws KeyStoreException;
}
