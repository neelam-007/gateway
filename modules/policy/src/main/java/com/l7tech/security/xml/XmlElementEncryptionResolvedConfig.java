package com.l7tech.security.xml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.cert.X509Certificate;

/**
 * Holds values to be used directly as is by the {@link XmlElementEncryptor} class
 */
public class XmlElementEncryptionResolvedConfig {

    public XmlElementEncryptionResolvedConfig(@NotNull X509Certificate recipientCert, @NotNull String xencAlgorithm) {
        this.recipientCert = recipientCert;
        this.xencAlgorithm = xencAlgorithm;
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

    // PRIVATE
    private final X509Certificate recipientCert;
    private final String xencAlgorithm;

    private String encryptedDataTypeAttribute;
    private String encryptedKeyRecipientAttribute;

}
