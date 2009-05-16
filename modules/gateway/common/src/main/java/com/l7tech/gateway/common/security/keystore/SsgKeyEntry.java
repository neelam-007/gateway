package com.l7tech.gateway.common.security.keystore;

import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.security.xml.SignerInfo;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

/**
 * Represents a private key entry in a Gateway key store, including information about a cert chain and
 * RSA private key.
 */
public class SsgKeyEntry extends SignerInfo implements NamedEntity, Serializable {
    private static final long serialVersionUID = 23272983482973429L;

    private long keystoreId;
    private String alias;

    /**
     * Create an SsgKeyEntry.
     *
     * @param keystoreId        ID of the keystore to which this entry belongs
     * @param alias  the alias for this SsgKeyEntry, or null if it doesn't (yet?) have one
     * @param certificateChain  the certificate chain for this entry.  Must contain at least one certificate.
     * @param privateKey        the private key for this entry, or null if the private key is not available
     *                          (perhaps because it is stored in an HSM and cannot be exported/serialized, and this
     *                          code is currently running on the client).
     */
    public SsgKeyEntry(long keystoreId,
                       String alias,
                       X509Certificate[] certificateChain,
                       PrivateKey privateKey)
    {
        super(privateKey, certificateChain);
        if (keystoreId != Long.MIN_VALUE && alias != null && (certificateChain == null || certificateChain.length < 1 || certificateChain[0] == null))
            throw new IllegalArgumentException("certificateChain must contain at least one certificate");
        this.keystoreId = keystoreId;
        this.alias = alias;
    }

    /**
     * Create an SsgKeyEntry with just a keystore ID and alias, for auditing purposes.
     *
     * @param keystoreId        ID of the keystore to which this entry belongs
     * @param alias  the alias for this SsgKeyEntry.  Required.
     */
    private SsgKeyEntry(long keystoreId, String alias) {
        super(null, null);
        if (alias == null)
            throw new IllegalArgumentException("alias is required for auditing purposes");
        this.keystoreId = keystoreId;
        this.alias = alias;
    }

    /**
     * Create a key entry to serve as a dummy entity for admin auditing purposes.
     *
     * @param keystoreId        ID of the keystore to which this entry belongs
     * @param alias  the alias for this SsgKeyEntry.  Required.
     * @return a new KeyEntry.  never null.
     */
    public static SsgKeyEntry createDummyEntityForAuditing(long keystoreId, String alias) {
        return new SsgKeyEntry(keystoreId, alias);
    }

    /**
     * @return the ID of this entry, or null if it's not yet assigned.
     *         This ID is the keystoreId converted to a string, followed by a colon, followed by the alias.
     */
    public String getId() {
        return alias == null ? null : (keystoreId + ":" + alias);
    }

    public String getName() {
        return getAlias();
    }

    /** @return the alias of this entry, or null if it's not yet assigned.  This is also used as the ID. */
    public String getAlias() {
        return alias;
    }

    /**
     * @return true if getPrivateKey() would return non-null without throwing.
     */
    public boolean isPrivateKeyAvailable() {
        return privateKey != null;
    }

    /**
     * @return the private key for this certificate entry.  Corresponds to the public key in the zeroth certificate
     *         in the certificate chain.  Never null.
     * @throws UnrecoverableKeyException  if the private key is not available.  Note that the private key
     *                                    is never sent outside the Gateway (and in any case may just be a handle
     *                                    to a secure PKCS#11 object).
     */
    public PrivateKey getPrivateKey() throws UnrecoverableKeyException {
        if (privateKey == null)
            throw new UnrecoverableKeyException("The private key is not available to this code");
        return privateKey;
    }

    /** @return the keystore id from which this entry came. */
    public long getKeystoreId() {
        return keystoreId;
    }

    /** @param keystoreId the new keystore id. */
    public void setKeystoreId(long keystoreId) {
        this.keystoreId = keystoreId;
    }

    /** @param alias the new alias. */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /** @param certificateChain the new certificate chain.  Must contain at least one certificate. */
    public void setCertificateChain(X509Certificate[] certificateChain) {
        this.certificateChain = certificateChain;
    }

    /** @param privateKey the new RSA private key, or null to clear it. */
    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }
}
