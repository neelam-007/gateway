package com.l7tech.gateway.common.security.keystore;

import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.ZoneableEntity;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;

/**
 * Represents a private key entry in a Gateway key store, including information about a cert chain and
 * RSA private key.
 */
public class SsgKeyEntry extends SignerInfo implements NamedEntity, Serializable, ZoneableEntity {
    private static final long serialVersionUID = 23272983482973430L;

    private static Functions.Nullary<Boolean> restrictedKeyAccessChecker;

    private long keystoreId;
    private String alias;
    private boolean restrictedAccess;
    private SsgKeyMetadata keyMetadata;

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
     * Configure code which will check whether the current thread context has permission to use a restricted-access private key.
     * <p/>
     * This method may only be called once for the lifetime of our class loader.
     *
     * @param accessChecker an access checker to set.  If null, this method takes no action.
     */
    public static void setRestrictedKeyAccessChecker(Functions.Nullary<Boolean> accessChecker) {
        if (accessChecker != null) {
            if (restrictedKeyAccessChecker != null)
                throw new IllegalStateException("Restricted key access checker has already been set.");
            restrictedKeyAccessChecker = accessChecker;
        }
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
    @Override
    public String getId() {
        return alias == null ? null : (keystoreId + ":" + alias);
    }

    @Override
    public String getName() {
        return getAlias();
    }

    /** @return the alias of this entry, or null if it's not yet assigned.  This is also used as the ID. */
    public String getAlias() {
        return alias;
    }

    /**
     * @return true if getPrivateKey() would return non-null without throwing (aside from restricted key access issues).
     */
    public boolean isPrivateKeyAvailable() {
        try {
            return super.getPrivate() != null;
        } catch (UnrecoverableKeyException e) {
            // Can't happen here
            return false;
        }
    }

    /**
     * @return true if getPrivateKey() would return non-null without throwing if called now, by the current thread in the current context.
     */
    public boolean isPrivateKeyAvailableAndAccessible() {
        return isPrivateKeyAvailable() && isAccessAllowed();
    }

    /**
     * @return true if this key entry has restricted private key access.
     */
    @Override
    public boolean isRestrictedAccess() {
        return restrictedAccess;
    }

    @Override
    public PrivateKey getPrivate() throws UnrecoverableKeyException {
        return getPrivateKey();
    }

    /**
     * @return the private key for this certificate entry.  Corresponds to the public key in the zeroth certificate
     *         in the certificate chain.  Never null.
     * @throws UnrecoverableKeyException  if the private key is not available.  Note that the private key
     *                                    is never sent outside the Gateway (and in any case may just be a handle
     *                                    to a secure PKCS#11 object).
     */
    public final PrivateKey getPrivateKey() throws UnrecoverableKeyException {
        PrivateKey privateKey = super.getPrivate();
        if (privateKey == null)
            throw new UnrecoverableKeyException("The private key is not available to this code");
        checkRestrictedAccess();
        return privateKey;
    }

    private boolean isAccessAllowed() {
        return !restrictedAccess || restrictedKeyAccessChecker == null || restrictedKeyAccessChecker.call();
    }

    private void checkRestrictedAccess() throws UnrecoverableKeyException {
        if (!isAccessAllowed())
            throw new UnrecoverableKeyException("Use of this private key is restricted and its use is not permitted in this context.");
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

    /**
     * Set restricted access to the private key field of this key entry.
     * <p/>
     * This will prevent access to the private key unless the restrictedKeyAccessChecker approves access
     * (or there is no restrictedKeyAccessChecker, in which case access is always approved).
     * <p/>
     * Restriction cannot be turned off, once enabled, without creating a new key entry.
     */
    public void setRestrictedAccess() {
        this.restrictedAccess = true;
    }

    @Override
    @NotNull
    @Size(min=1)
    public X509Certificate[] getCertificateChain() {
        return super.getCertificateChain();
    }

    /** @param certificateChain the new certificate chain.  Must contain at least one certificate. */
    @Override
    public void setCertificateChain(X509Certificate[] certificateChain) {
        super.setCertificateChain(certificateChain);
    }

    /** @param privateKey the new RSA private key, or null to clear it. */
    public void setPrivateKey(PrivateKey privateKey) {
        super.setPrivate(privateKey);
    }

    /**
     * Attach metadata to this key entry.
     * <p/>
     * Metadata is any information about the key that doesn't come from the keystore.  The private key and cert chain
     * come from the key store, but any other metadata needs to be provided via an SsgKeyMetadata entity.
     * <p/>
     * Metadata may not be changed once it is attached.
     *
     * @param keyMetadata metadata for this entry.  Required.
     */
    public void attachMetadata(@NotNull SsgKeyMetadata keyMetadata) {
        if (this.keyMetadata != null && this.keyMetadata != keyMetadata)
            throw new IllegalArgumentException("key metadata already attached");
        this.keyMetadata = keyMetadata;
    }

    @Nullable
    public SsgKeyMetadata getKeyMetadata() {
        return keyMetadata;
    }

    @Override
    public SecurityZone getSecurityZone() {
        return keyMetadata == null ? null : keyMetadata.getSecurityZone();
    }

    /**
     * Sets the SecurityZone on the SsgKeyMetadata.
     *
     * If there is no SsgKeyMetadata currently attached, one with the SsgKeyEntry's keystoreId and alias and the given securityZone will be attached.
     *
     * @param securityZone a new security zone to assign to this entity, or null to take it out of all security zones.
     */
    @Override
    public void setSecurityZone(SecurityZone securityZone) {
        if (keyMetadata == null) {
            keyMetadata = new SsgKeyMetadata(keystoreId, alias, securityZone);
        } else {
            keyMetadata.setSecurityZone(securityZone);
        }
    }
}
