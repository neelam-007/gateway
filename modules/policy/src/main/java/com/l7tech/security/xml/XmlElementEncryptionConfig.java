package com.l7tech.security.xml;

import java.io.Serializable;

/**
 * Holds settings for encrypting a non-SOAP element.
 */
public final class XmlElementEncryptionConfig implements Serializable {
    /**
     * The recipient's certificate, if generating an EncryptedKey.
     */
    private String recipientCertificateBase64 = null;

    /**
     * If true, replace all contents of the element with a single EncryptedData element, but leave the open and close tags unencrypted.
     * If false, replace the entire element with an EncryptedData element.
     */
    private boolean encryptContentsOnly = false;

    /** Symmetric encryption algorithm. */
    private String xencAlgorithm = XencUtil.AES_128_CBC;

    public String getRecipientCertificateBase64() {
        return recipientCertificateBase64;
    }

    public void setRecipientCertificateBase64(String recipientCertificateBase64) {
        this.recipientCertificateBase64 = recipientCertificateBase64;
    }

    public boolean isEncryptContentsOnly() {
        return encryptContentsOnly;
    }

    public void setEncryptContentsOnly(boolean encryptContentsOnly) {
        this.encryptContentsOnly = encryptContentsOnly;
    }

    public String getXencAlgorithm() {
        return xencAlgorithm;
    }

    public void setXencAlgorithm(String xencAlgorithm) {
        this.xencAlgorithm = xencAlgorithm;
    }
}
