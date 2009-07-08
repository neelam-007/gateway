package com.l7tech.security.xml;

import com.l7tech.util.Pair;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/**
 * Class <code>SignerInfo</code> is the simple holder for a public
 * private key and the certificate.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SignerInfo implements Serializable {
    private static final long serialVersionUID = 2335356651146783429L;

    protected transient PrivateKey privateKey;
    protected X509Certificate[] certificateChain;

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
     * Constructs a signer info from the given private key and certificate.
     * The public key is retrieved from the certificate.
     *
     * @param certWithPrivateKey a Pair holding the subject cert and private key.  Required.
     */
    public SignerInfo(Pair<X509Certificate, PrivateKey> certWithPrivateKey) {
        if (certWithPrivateKey.left == null) throw new IllegalArgumentException("A certificate is required.");
        this.privateKey = certWithPrivateKey.right;
        this.certificateChain = new X509Certificate[] { certWithPrivateKey.left };
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
     * @return a reference to the private key, or null if the private key is not available here.
     */
    public PrivateKey getPrivate() {
        return privateKey;
    }

    /**
     * @return the certificate chain for this private key.  Always contains at least one certificate.
     *         The zeroth entry is the target certificate, containing the public key corresponding to this entry's
     *         private key.  Entry #1, if it exists, contains the public key that was used to sign Entry #0, and so on.
     */
    public X509Certificate[] getCertificateChain() {
        return certificateChain;
    }

    /**
     * Convenience metho that returns the first cert in the cert chain.
     * Equivalent to getCertificateChain[0].
     *
     * @return the certificate for this private key.  Never null.
     */
    public X509Certificate getCertificate() {
        return getCertificateChain()[0];
    }

    /**
     * Convenience method that returns the Subject DN of the first cert in the cert chain.
     * Equivalent to getCertificate().getSubjectDN().toString().
     *
     * @return the Subject DN of the first cert in the cert chain.
     */
    public String getSubjectDN() {
        return getCertificateChain()[0].getSubjectDN().toString();
    }

    public SignerInfo getSignerInfo() {
        return this;
    }
}
