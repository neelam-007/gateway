package com.l7tech.security.xml;

import com.l7tech.policy.assertion.UsesVariables;
import com.l7tech.policy.variable.Syntax;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Holds settings for encrypting a non-SOAP element.
 *
 * These settings may reference context variables.
 *
 * Note: Any new properties need to be delegated to from Non-SOAP Encrypt assertion to ensure getVariablesUsed() works.
 *
 * Warning: Any properties added here will not affect encryption until supported by {@link XmlElementEncryptionResolvedConfig}
 */
public final class XmlElementEncryptionConfig implements Serializable, UsesVariables {

    public static final String TYPE_ATTRIBUTE_DEFAULT = "http://www.w3.org/2001/04/xmlenc#Element";

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

    @NotNull private String encryptedDataTypeAttribute = TYPE_ATTRIBUTE_DEFAULT;

    private boolean includeEncryptedDataTypeAttribute;

    private String encryptedKeyRecipientAttribute;

    public String getRecipientCertificateBase64() {
        return recipientCertificateBase64;
    }

    public void setRecipientCertificateBase64(@Nullable String recipientCertificateBase64) {
        this.recipientCertificateBase64 = recipientCertificateBase64;
    }

    public boolean isEncryptContentsOnly() {
        return encryptContentsOnly;
    }

    /**
     * @param encryptContentsOnly if true, only the element content will be encrypted and replaced by EncryptedData, leaving its open and close tags (and any attributes) in plaintext.
     *                            if false, the entire element will be encrypted and replaced with EncryptedData, including open and close tags.
     */
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

    @NotNull
    public String getEncryptedDataTypeAttribute() {
        return encryptedDataTypeAttribute;
    }

    public void setEncryptedDataTypeAttribute(@Nullable String encryptedDataTypeAttribute) {
        this.encryptedDataTypeAttribute = encryptedDataTypeAttribute == null? TYPE_ATTRIBUTE_DEFAULT : encryptedDataTypeAttribute;
    }

    @Nullable
    public String getEncryptedKeyRecipientAttribute() {
        return encryptedKeyRecipientAttribute;
    }

    public void setEncryptedKeyRecipientAttribute(@Nullable String encryptedKeyRecipientAttribute) {
        this.encryptedKeyRecipientAttribute = encryptedKeyRecipientAttribute;
    }

    public boolean isIncludeEncryptedDataTypeAttribute() {
        return includeEncryptedDataTypeAttribute;
    }

    public void setIncludeEncryptedDataTypeAttribute(boolean includeEncryptedDataTypeAttribute) {
        this.includeEncryptedDataTypeAttribute = includeEncryptedDataTypeAttribute;
    }

    @Override
    @NotNull
    public String[] getVariablesUsed() {

        final StringBuilder builder = new StringBuilder();
        if (recipientCertContextVariableName != null) {
            builder.append(Syntax.getVariableExpression(recipientCertContextVariableName));
            builder.append(" ");
        }

        builder.append(encryptedDataTypeAttribute);
        if (encryptedKeyRecipientAttribute != null) {
            builder.append(" ");
            builder.append(encryptedKeyRecipientAttribute);
        }

        return Syntax.getReferencedNames(builder.toString());
    }
}
