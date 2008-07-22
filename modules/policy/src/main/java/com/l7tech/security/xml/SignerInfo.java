package com.l7tech.security.xml;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

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
