package com.l7tech.server.security.keystore;

import com.l7tech.objectmodel.Entity;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.UnrecoverableKeyException;

/**
 * Represents a private key entry in a Gateway key store, including information about a cert chain and
 * RSA private key.
 */
public class SsgKeyEntry implements Entity {
    private final String alias;
    private final X509Certificate[] certificateChain;
    private final RSAPrivateKey rsaPrivateKey;

    /**
     * Create an SsgKeyEntry.
     *
     * @param alias  the alias for this SsgKeyEntry, or null if it doesn't (yet?) have one
     * @param certificateChain  the certificate chain for this entry.  Must contain at least one certificate.
     * @param rsaPrivateKey     the private key for this entry, or null if the private key is not available
     *                          (perhaps because it is stored in an HSM and cannot be exported/serialized, and this
     *                          code is currently running on the client).
     */
    public SsgKeyEntry(String alias, X509Certificate[] certificateChain, RSAPrivateKey rsaPrivateKey) {
        if (certificateChain == null || certificateChain.length < 1 || certificateChain[0] == null)
            throw new IllegalArgumentException("certificateChain must contain at least one certificate");
        this.alias = alias;
        this.certificateChain = certificateChain;
        this.rsaPrivateKey = rsaPrivateKey;        
    }

    /** @return the ID of this entry, or null if it's not yet assigned.  This is a synonym for getAlias. */
    public String getId() {
        return alias;
    }

    /** @return the alias of this entry, or null if it's not yet assigned.  This is also used as the ID. */
    public String getAlias() {
        return alias;
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
     * @return the private key for this certificate entry.  Corresponds to the public key in the zeroth certificate
     *         in the certificate chain.  Never null.
     * @throws UnrecoverableKeyException  if the private key is not available, perhaps because it is inside the HSM
     *                                    and this code is running on the client.
     */
    public RSAPrivateKey getRSAPrivateKey() throws UnrecoverableKeyException {
        if (rsaPrivateKey == null)
            throw new UnrecoverableKeyException("The private key cannot be extracted from the hardware security module");
        return rsaPrivateKey;
    }
}
