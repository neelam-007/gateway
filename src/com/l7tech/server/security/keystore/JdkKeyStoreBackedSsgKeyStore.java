package com.l7tech.server.security.keystore;

import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.security.BouncyCastleCertUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;

import javax.naming.ldap.LdapName;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.math.BigInteger;

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
            Key key = keystore.getKey(alias, getEntryPassword());
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

    public synchronized X509Certificate generateKeyPair(final String alias, final LdapName dn, final int keybits, final int expiryDays) throws GeneralSecurityException {
        return mutateKeystore(new Functions.Nullary<X509Certificate>() {
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
                }
            }
        });
    }

    public synchronized void replaceCertificate(final String alias, final X509Certificate certificate) throws InvalidKeyException, KeyStoreException {
        mutateKeystore(new Functions.Nullary<Object>() {
            public Object call() {
                try {
                    KeyStore keystore = keyStore();
                    if (!keystore.isKeyEntry(alias))
                        throw new RuntimeException("Keystore does not contain a key with alias " + alias);
                    Key key = keystore.getKey(alias, null);
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
                    if (!(key instanceof RSAPrivateKey))
                        throw new RuntimeException("Keystore contains a key with alias " + alias + " but it is not an RSA private key");

                    PublicKey newPublicKey = certificate.getPublicKey();
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

                    keystore.setKeyEntry(alias, key, getEntryPassword(), new Certificate[] { certificate });

                    return null;
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

    public synchronized CertificateRequest makeCertificateSigningRequest(String alias, LdapName dn) throws InvalidKeyException, SignatureException, KeyStoreException {
        try {
            KeyStore keystore = keyStore();
            Key key = keystore.getKey(alias, null);
            if (!(key instanceof RSAPrivateKey))
                throw new InvalidKeyException("The key with alias " + alias + " is not an RSA private key");
            RSAPrivateKey rsaPrivate = (RSAPrivateKey)key;
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
            return BouncyCastleCertUtils.makeCertificateRequest(dn, keyPair);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidKeyException("Keystore contains no key with alias " + alias, e);
        } catch (UnrecoverableKeyException e) {
            throw new KeyStoreException(e);
        } catch (NoSuchProviderException e) {
            throw new KeyStoreException(e);
        }
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
