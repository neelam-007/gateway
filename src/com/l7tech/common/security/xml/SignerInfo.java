package com.l7tech.common.security.xml;

import java.security.*;
import java.security.cert.X509Certificate;
import java.util.NoSuchElementException;

/**
 * Class <code>SignerInfo</code> is the simple holder for a public
 * private key and the certificate.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SignerInfo {

    private PrivateKey privateKey;
    private X509Certificate[] certificateChain;

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
     * @throws NoSuchElementException thrown if unable to find the key or certificate for the alias
     */
    public static SignerInfo newInstance(KeyStore keyStore, String keyAlias, String password)
      throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if (keyStore == null || keyAlias == null) {
            throw new IllegalArgumentException();
        }

        X509Certificate[] certs = (X509Certificate[])keyStore.getCertificateChain(keyAlias);
        if (certs == null || certs.length == 0) {
            throw new NoSuchElementException("No certificate for alias '"+keyAlias+"' in keystore");
        }
        char[] pwd = password !=null ? password.toCharArray() : new char[] {};
        Key key = keyStore.getKey(keyAlias, pwd);
        if (key == null) {
            throw new NoSuchElementException("No private key for alias '"+keyAlias+"' in keystore");
        }

        PrivateKey pk = null;
        if (key instanceof PrivateKey) {
            pk = (PrivateKey)key;
        } else {
            throw new IllegalArgumentException("Expected private key, received :"+key.getClass()+"\n"+key);
        }
        return new SignerInfo(pk, certs);
    }

    /**
     * Constructs a signer info from the given private key and certificate.
     * The public key is retrieved from the certificate.
     * 
     * @param privateKey the private key.
     * @param certificateChain the certificate chain; the first element contains the relevant public key.
     */
    public SignerInfo(PrivateKey privateKey, X509Certificate[] certificateChain) {
        this.privateKey = privateKey;
        this.certificateChain = certificateChain;
    }

    /**
     * Returns a reference to the public key
     * 
     * @return a reference to the public key.
     */
    public PublicKey getPublic() {
        return certificateChain[0].getPublicKey();
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
    public X509Certificate[] getCertificateChain() {
        return certificateChain;
    }
}
