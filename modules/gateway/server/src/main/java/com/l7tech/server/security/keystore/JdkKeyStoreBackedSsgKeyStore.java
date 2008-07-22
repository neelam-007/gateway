package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.BouncyCastleCertUtils;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.util.ExceptionUtils;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for SsgKeyStore implementations that are based on a JDK KeyStore instance.
 */
public abstract class JdkKeyStoreBackedSsgKeyStore implements SsgKeyStore {

    protected BlockingQueue<Runnable> mutationQueue = new LinkedBlockingQueue<Runnable>();
    protected ExecutorService mutationExecutor = new ThreadPoolExecutor(1, 1, 5 * 60, TimeUnit.SECONDS, mutationQueue);

    /**
     * Get the KeyStore instance that we will be working with.
     * @return a KeyStore instance ready to use.   Never null.
     * @throws java.security.KeyStoreException if there is a problem obtaining a KeyStore instance.
     */
    protected abstract KeyStore keyStore() throws KeyStoreException;

    /** @return the format string for this keystore, to store in the format column in the DB, ie "hsm" or "sdb". */
    protected abstract String getFormat();

    public String getId() {
        return String.valueOf(getOid());
    }

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
        PrivateKey rsaPrivateKey = null;
        try {
            Key key = keystore.getKey(alias, getEntryPassword());
            if (key instanceof PrivateKey && "RSA".equals(key.getAlgorithm()))
                rsaPrivateKey = (PrivateKey)key;
        } catch (NoSuchAlgorithmException e) {
            getLogger().log(Level.WARNING, "Unsupported key type in cert entry in " + "Keystore " + getName() + " with alias " + alias + ": " + ExceptionUtils.getMessage(e), e);
            // Fallthrough and do without
        } catch (UnrecoverableKeyException e) {
            getLogger().log(Level.WARNING, "Unrecoverable key in cert entry in " + "Keystore " + getName() + " with alias " + alias + ": " + ExceptionUtils.getMessage(e), e);
            // Fallthrough and do without
        }

        return new SsgKeyEntry(getOid(), alias, x509chain, rsaPrivateKey);
    }

    /**
     * Get a Logger that can be used to log warnings about things like cert chain entries that are being
     * omitted from a list due to some problem, or private key entries that cannot be used.
     *
     * @return a Logger instance.  Never null.
     */
    protected abstract Logger getLogger();

    private void storePrivateKeyEntryImpl(SsgKeyEntry entry, boolean overwriteExisting) throws KeyStoreException {
        final String alias = entry.getAlias();
        if (alias == null || alias.trim().length() < 1)
            throw new KeyStoreException("Unable to store private key entry with null or empty alias");

        final X509Certificate[] chain = entry.getCertificateChain();
        if (chain == null || chain.length < 1)
            throw new KeyStoreException("Unable to store private key entry with null or empty certificate chain");

        final PrivateKey key;
        try {
            key = entry.getPrivateKey();
            if (key == null)
                throw new KeyStoreException("Unable to store private key entry with null private key");
        } catch (UnrecoverableKeyException e) {
            throw new KeyStoreException("Unable to store private key entry with unrecoverable private key");
        }

        KeyStore keystore = keyStore();
        if (!overwriteExisting && keystore.containsAlias(alias))
            throw new KeyStoreException("Keystore already contains an entry with the alias '" + alias + '\'');

        keystore.setKeyEntry(alias, key, getEntryPassword(), chain);
    }

    /**
     * Get a password that will be used to protect each key entry set, and
     * to decrypt each key entry read.
     *
     * @return a non-null (but possibly empty) character array
     */
    protected abstract char[] getEntryPassword();

    public boolean isMutable() {
        return true;
    }

    public SsgKeyStore getKeyStore() {
        return this;
    }

    public synchronized Future<Boolean> storePrivateKeyEntry(final SsgKeyEntry entry, final boolean overwriteExisting) throws KeyStoreException {
        if (entry == null) throw new NullPointerException("entry must not be null");
        if (entry.getAlias() == null) throw new NullPointerException("entry's alias must not be null");
        if (entry.getAlias().length() < 1) throw new IllegalArgumentException("entry's alias must not be empty");
        final PrivateKey privateKey;
        try {
            privateKey = entry.getPrivateKey();
            if (privateKey == null) throw new NullPointerException("entry's private key must be present");
        } catch (UnrecoverableKeyException e) {
            throw new IllegalArgumentException("entry's private key must be present", e);
        }

        return mutateKeystore(new Callable<Boolean>() {
            public Boolean call() {
                try {
                    storePrivateKeyEntryImpl(entry, overwriteExisting);
                    return Boolean.TRUE;
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public synchronized Future<Boolean> deletePrivateKeyEntry(final String keyAlias) throws KeyStoreException {
        return mutateKeystore(new Callable<Boolean>() {
            public Boolean call() {
                try {
                    keyStore().deleteEntry(keyAlias);
                    return Boolean.TRUE;
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public synchronized Future<X509Certificate> generateKeyPair(final String alias, final X500Principal dn, final int keybits, final int expiryDays)
            throws GeneralSecurityException
    {
        return mutateKeystore(new Callable<X509Certificate>() {
            public X509Certificate call() {
                try {
                    KeyStore keystore = keyStore();
                    if (keystore.containsAlias(alias))
                        throw new RuntimeException("Keystore already contains alias " + alias);

                    // Requires that current crypto engine already by the correct one for this keystore type
                    KeyPair keyPair = JceProvider.generateRsaKeyPair(keybits);
                    X509Certificate cert = BouncyCastleCertUtils.generateSelfSignedCertificate(dn, expiryDays, keyPair);

                    keystore.setKeyEntry(alias, keyPair.getPrivate(), getEntryPassword(), new Certificate[] { cert });

                    return cert;
                } catch (CertificateEncodingException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (SignatureException e) {
                    throw new RuntimeException(e);
                } catch (InvalidKeyException e) {
                    throw new RuntimeException(e);
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchProviderException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public synchronized Future<Boolean> replaceCertificateChain(final String alias, final X509Certificate[] chain) throws InvalidKeyException, KeyStoreException {
        if (chain == null || chain.length < 1 || chain[0] == null)
            throw new IllegalArgumentException("Cert chain must contain at least one cert.");
        return mutateKeystore(new Callable<Boolean>() {
            public Boolean call() {
                try {
                    KeyStore keystore = keyStore();
                    if (!keystore.isKeyEntry(alias))
                        throw new RuntimeException("Keystore does not contain a key with alias " + alias);
                    Key key = keystore.getKey(alias, getEntryPassword());
                    Certificate[] oldChain = keystore.getCertificateChain(alias);
                    if (oldChain == null || oldChain.length < 1)
                        throw new RuntimeException("Existing certificate chain for alias " + alias + " is empty");
                    if (!(oldChain[0] instanceof X509Certificate))
                        throw new RuntimeException("Existing certificate for alias " + alias + " is not an X.509 certificate");
                    X509Certificate oldCert = (X509Certificate)oldChain[0];
                    PublicKey oldPublicKey = oldCert.getPublicKey();
                    if (!(oldPublicKey instanceof RSAPublicKey))
                        throw new RuntimeException("Existing certificate public key is not an RSA public key");
                    RSAPublicKey oldRsaPublicKey = (RSAPublicKey)oldPublicKey;
                    if (!(key instanceof PrivateKey) || !"RSA".equals(key.getAlgorithm()))
                        throw new RuntimeException("Keystore contains a key with alias " + alias + " but it is not an RSA private key: " + key);

                    PublicKey newPublicKey = chain[0].getPublicKey();
                    if (!(newPublicKey instanceof RSAPublicKey)) {
                        throw new RuntimeException("New certificate public key is not an RSA public key");
                    }
                    RSAPublicKey newRsaPublicKey = (RSAPublicKey)newPublicKey;

                    BigInteger newModulus = newRsaPublicKey.getModulus();
                    BigInteger oldModulus = oldRsaPublicKey.getModulus();
                    BigInteger newExponent = newRsaPublicKey.getPublicExponent();
                    BigInteger oldExponent = oldRsaPublicKey.getPublicExponent();

                    if (!newExponent.equals(oldExponent))
                        throw new RuntimeException("New certificate public key's RSA public exponent is not the same as the old certificate's public key");

                    if (!newModulus.equals(oldModulus))
                        throw new RuntimeException("New certificate public key's RSA modulus is not the same as the old certificate's public key");

                    keystore.setKeyEntry(alias, key, getEntryPassword(), chain);

                    return Boolean.TRUE;
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                } catch (KeyStoreException e) {
                    throw new RuntimeException(e);
                } catch (UnrecoverableKeyException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public synchronized CertificateRequest makeCertificateSigningRequest(String alias, String dn) throws InvalidKeyException, SignatureException, KeyStoreException {
        try {
            X500Principal dnObj = new X500Principal(dn);
            KeyStore keystore = keyStore();
            Key key = keystore.getKey(alias, getEntryPassword());
            if (!(key instanceof PrivateKey) || !"RSA".equals(key.getAlgorithm()))
                throw new InvalidKeyException("The key with alias " + alias + " is not an RSA private key");
            PrivateKey rsaPrivate = (PrivateKey)key;
            Certificate[] chain = keystore.getCertificateChain(alias);
            if (chain == null || chain.length < 1)
                throw new RuntimeException("Existing certificate chain for alias " + alias + " is empty");
            if (!(chain[0] instanceof X509Certificate))
                throw new RuntimeException("Existing certificate for alias " + alias + " is not an X.509 certificate");
            X509Certificate cert = (X509Certificate)chain[0];
            PublicKey publicKey = cert.getPublicKey();
            if (!(publicKey instanceof RSAPublicKey))
                throw new RuntimeException("Existing certificate public key is not an RSA public key");
            RSAPublicKey rsaPublic = (RSAPublicKey)publicKey;
            KeyPair keyPair = new KeyPair(rsaPublic, rsaPrivate);
            return BouncyCastleCertUtils.makeCertificateRequest(dnObj, keyPair);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidKeyException("Keystore contains no key with alias " + alias, e);
        } catch (UnrecoverableKeyException e) {
            throw new KeyStoreException(e);
        } catch (NoSuchProviderException e) {
            throw new KeyStoreException(e);
        }
    }

    /**
     * Load the keystore from the database, mutate it, and save it back, all atomically.  The actual transaction
     * will not necessarily occur before this call returns -- it may be scheduled for later execution.  The returned
     * Future will show Done when the mutation eventually completes.
     *
     * @param mutator  a Runnable that will mutate the current keystore, which will be guaranteed
     *                 to be up-to-date and non-null when the runnable is invoked.
     * @throws java.security.KeyStoreException if the runnable throws a RuntimeException or if any other problem occurs during
     *                           the process
     * @return the value returned by the mutator
     */
    protected abstract <OUT> Future<OUT> mutateKeystore(Callable<OUT> mutator) throws KeyStoreException;
}
