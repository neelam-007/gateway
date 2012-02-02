package com.l7tech.policy.assertion;

import org.jetbrains.annotations.Nullable;

/**
 * Configuration interface for a SAML Issuer attribute (SAML 1.1) or Element (SAML 2.0).
 */
public interface SamlIssuerConfig extends SamlVersionConfig {

    /**
     * Useful to know as SAML Protocol 1.1 does not support an Issuer in Request or Response messages.
     *
     * @return true if configuration is for a SAML protocol message.
     */
    boolean samlProtocolUsage();

    boolean includeIssuer();

    /**
     * Issuer is optional in SAML 2.0 protocol messages. Allow this element to be configured on or off.
     *
     * @param includeIssuer true if the Issuer should be added, false otherwise
     */
    void includeIssuer(boolean includeIssuer);

    /**
     * Get the custom issuer value. If this value is null and an issuer is required, then this signals that the
     * default value of Issuer should be determined and used.
     *
     * @return Custom issuer value.
     */
    @Nullable
    String getCustomIssuerValue();

    /**
     * Set the 'Custom' issuer value. It's called 'custom' as by default the Gateway will provide a value for Issuer,
     * when needed, based on the DN from the public key that corresponds to the configured private key.
     * @param customIssuerValue custom value to use.
     */
    void setCustomIssuerValue(@Nullable String customIssuerValue);

    String getCustomIssuerFormat();

    /**
     * Set Issuer Format. Should be a URI
     * @param customIssuerFormat format URI
     */
    void setCustomIssuerFormat(@Nullable String customIssuerFormat);

    String getCustomIssuerNameQualifier();

    /**
     * Set the Issuer NameQualifier attribute value.
     *
     * @param customIssuerNameQualifier if not null it's value will be used as the NameQualifier attribute.
     */
    void setCustomIssuerNameQualifier(@Nullable String customIssuerNameQualifier);
}
