package com.l7tech.common.security.xml;

import java.security.*;
import java.security.cert.X509Certificate;

/**
 * Class <code>SignerInfo</code> is the simple holder for a public
 * private key and the certificate.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SignerInfo {

    private PrivateKey privateKey;
    private X509Certificate certificate;

    /**
     * Create the <code>SignerInfo</code> from the private key and certificate retrieved
     * from the keystore.
     *
     * @param keyStore the keystore
     * @param keyAlias the key alias in the key store
     * @param password the key password
     * @return the <code>SignerInfo</code> instance for the obtaied key and certificate
     * @throws KeyStoreException thrown on error when working with the keystore
     * @throws NoSuchAlgorithmException thrown if the key alghoritm cannot be found
     * @throws UnrecoverableKeyException thrown if unable to retrieve the key (password wrong)
     */
    public static SignerInfo newInstance(KeyStore keyStore, String keyAlias, String password)
      throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if (keyStore == null || keyAlias == null) {
            throw new IllegalArgumentException();
        }

        X509Certificate cert = (X509Certificate)keyStore.getCertificate(keyAlias);
        if (cert == null) {

        }
        char[] pwd = password !=null ? password.toCharArray() : new char[] {};
        Key key = keyStore.getKey(keyAlias, pwd);
        if (key == null) {
            throw new IllegalArgumentException();
        }

        PrivateKey pk = null;
        if (key instanceof PrivateKey) {
            pk = (PrivateKey)key;
        } else {
            throw new IllegalArgumentException("Expected private key, received :"+key.getClass()+"\n"+key);
        }
        return new SignerInfo(pk, cert);
    }

    /**
     * Constructs a signer info from the given private key and certificate.
     * The public key is retrieved from the certificate.
     * 
     * @param privateKey the private key.
     * @param certificate  the public key.
     */
    public SignerInfo(PrivateKey privateKey, X509Certificate certificate) {
        this.privateKey = privateKey;
        this.certificate = certificate;
    }

    /**
     * Returns a reference to the public key
     * 
     * @return a reference to the public key.
     */
    public PublicKey getPublic() {
        return certificate.getPublicKey();
    }

    /**
     * Returns a reference to the private key
     * 
     * @return a reference to the private key.
     */
    public PrivateKey getPrivate() {
        return privateKey;
    }

      /**
     * Returns a reference to the certificate
     *
     * @return a reference to the certificate.
     */
    public X509Certificate getCertificate() {
        return certificate;
    }
}
