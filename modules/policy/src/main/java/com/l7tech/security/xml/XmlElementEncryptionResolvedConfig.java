package com.l7tech.security.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.cert.X509Certificate;

/**
 * Holds values to be used directly as is by the {@link XmlElementEncryptor} class
 */
public class XmlElementEncryptionResolvedConfig {

    public XmlElementEncryptionResolvedConfig(@NotNull X509Certificate recipientCert, @NotNull String xencAlgorithm, boolean encryptContentsOnly) {
        this.recipientCert = recipientCert;
        this.xencAlgorithm = xencAlgorithm;
        this.encryptContentsOnly = encryptContentsOnly;
    }

    public X509Certificate getRecipientCert() {
        return recipientCert;
    }

    public String getXencAlgorithm() {
        return xencAlgorithm;
    }

    public String getEncryptedDataTypeAttribute() {
        return encryptedDataTypeAttribute;
    }

    public void setEncryptedDataTypeAttribute(@Nullable String encryptedDataTypeAttribute) {
        this.encryptedDataTypeAttribute = encryptedDataTypeAttribute;
    }

    public String getEncryptedKeyRecipientAttribute() {
        return encryptedKeyRecipientAttribute;
    }

    public void setEncryptedKeyRecipientAttribute(@Nullable String encryptedKeyRecipientAttribute) {
        this.encryptedKeyRecipientAttribute = encryptedKeyRecipientAttribute;
    }

    /**
     * @return true if only the element content will be encrypted and replaced by EncryptedData, leaving its open and close tags (and any attributes) in plaintext.
     *         false if the entire element will be encrypted and replaced with EncryptedData, including open and close tags.
     */
    public boolean isEncryptContentsOnly() {
        return encryptContentsOnly;
    }

    // PRIVATE
    private final X509Certificate recipientCert;
    private final String xencAlgorithm;
    private final boolean encryptContentsOnly;

    private String encryptedDataTypeAttribute;
    private String encryptedKeyRecipientAttribute;
}
