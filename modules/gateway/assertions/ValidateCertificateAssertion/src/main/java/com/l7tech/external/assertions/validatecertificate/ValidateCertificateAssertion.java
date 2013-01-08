package com.l7tech.external.assertions.validatecertificate;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.security.types.CertificateValidationType;
import org.jetbrains.annotations.NotNull;

/**
 * Assertion which can validate an X509Certificate.
 */
public class ValidateCertificateAssertion extends Assertion {
    private String sourceVariable;
    private CertificateValidationType validationType = CertificateValidationType.CERTIFICATE_ONLY;
    private boolean logOnly = false;

    public String getSourceVariable() {
        return sourceVariable;
    }

    /**
     * @param sourceVariable the name of an X509Certificate context variable to validate.
     */
    public void setSourceVariable(@NotNull final String sourceVariable) {
        this.sourceVariable = sourceVariable;
    }

    public CertificateValidationType getValidationType() {
        return validationType;
    }

    /**
     * @param validationType the CertificateValidationType. Default is CertificateValidationType.CERTIFICATE_ONLY.
     */
    public void setValidationType(@NotNull final CertificateValidationType validationType) {
        this.validationType = validationType;
    }

    public boolean isLogOnly() {
        return logOnly;
    }

    /**
     * @param logOnly True if we should only log an error if the certificate is found to be invalid.
     *                False if we should fail if the certificate is found to be invalid. Default is false.
     */
    public void setLogOnly(final boolean logOnly) {
        this.logOnly = logOnly;
    }

    private static final String META_INITIALIZED = ValidateCertificateAssertion.class.getName() + ".metadataInitialized";

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;
        meta.put(AssertionMetadata.SHORT_NAME, "Validate Certificate");
        meta.put(AssertionMetadata.LONG_NAME, "Validate Certificate");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/check16.gif");
        meta.put(AssertionMetadata.POLICY_NODE_ICON, "com/l7tech/console/resources/check16.gif");
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

}
