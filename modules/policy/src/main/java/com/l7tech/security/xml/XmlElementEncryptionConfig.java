package com.l7tech.security.xml;

import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.Syntax;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Holds settings for encrypting a non-SOAP element.
 */
public final class XmlElementEncryptionConfig implements Serializable, UsesVariables {
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

    /**
     * If non-null, ignore any base64 cert and use the contents of the specified context variable instead.
     */
    private String recipientCertContextVariableName = null;

    public String getRecipientCertificateBase64() {
        return recipientCertificateBase64;
    }

    public void setRecipientCertificateBase64(@Nullable String recipientCertificateBase64) {
        this.recipientCertificateBase64 = recipientCertificateBase64;
    }

    public boolean isEncryptContentsOnly() {
        return encryptContentsOnly;
    }

    // Warning: not currently implemented
    public void setEncryptContentsOnly(boolean encryptContentsOnly) {
        this.encryptContentsOnly = encryptContentsOnly;
    }

    public String getXencAlgorithm() {
        return xencAlgorithm;
    }

    public void setXencAlgorithm(String xencAlgorithm) {
        this.xencAlgorithm = xencAlgorithm;
    }

    public String getRecipientCertContextVariableName() {
        return recipientCertContextVariableName;
    }

    public void setRecipientCertContextVariableName(@Nullable String recipientCertContextVariableName) {
        this.recipientCertContextVariableName = recipientCertContextVariableName;
    }

    @Override
    public String[] getVariablesUsed() {
        return recipientCertContextVariableName == null ? new String[0] : Syntax.getReferencedNames("${" + recipientCertContextVariableName + "}");
    }
}
