package com.l7tech.policy.assertion;

/**
 * Interface for extension for any SAML configuration which need to know what version of SAML it's configuring.
 */
public interface SamlVersionConfig {
    /**
     * Get the SAML version for this assertion
     *
     * <p>The value 0 means any version, null means unspecified (in which case 1 should
     * be used for backwards compatibility).</p>
     *
     * @return The saml version (0/1/2) or null.
     */
    Integer getVersion();

    /**
     * Set the SAML version for this assertion.
     *
     * @param version (may be null)
     */
    void setVersion(Integer version);
}
