package com.l7tech.policy.assertion.ext;

import java.security.cert.X509Certificate;

/**
 * Layer 7 API that provides access to certificates known to the gateway.  These are the certificates that appear in the
 * Manager Trusted Certificates table in the SecureSpan Manager.
 */

public interface CertificateFinder {

    /** Outbound SSL Connection. */
    public static final String TRUSTED_FOR_OUTBOUND_SSL = "outboundSsl";

    /** Signing Certificates for Outbound SSL Connections.*/
    public static final String TRUSTED_FOR_OUTBOUND_SSL_CA = "caOutboundSsl";
    
    /** Signing Client Certificate. */
    public static final String TRUSTED_FOR_CLIENT_CERT_CA = "clientCertCa";

    /** Signing SAML Token. */
    public static final String TRUSTED_FOR_SAML_ISSUER = "samlIssuer";

    /** SAML Attesting Entity. */
    public static final String TRUSTED_FOR_SAML_ATTESTING_ENTITY = "samlAttestingEntity";

    /**
     * Look up certificate based on usage option. The usage options correspond to the check boxes in the Options tab in the
     * Manage Certificates dialog in the SecureSpan Manager UI.
     * <p>
     * Note that this method does not filter out expired certificates.  It is up to the Custom Assertion developer to check
     * the validity for the certificate. 
     *
     * @param usageOption a String naming a usage option ie. {@link #TRUSTED_FOR_CLIENT_CERT_CA}
     * @return an array of matching certificates, may be empty, never null
     * @throws IllegalArgumentException if usageOption is not recognized
     * @throws ServiceException if there is a problem querying for certificates
     */
    public X509Certificate[] findByUsageOption(String usageOption) throws ServiceException, IllegalArgumentException;
}
