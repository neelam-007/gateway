package com.l7tech.gateway.common.security.signer;

import com.l7tech.objectmodel.FindException;

import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * Signature verifier, responsible for validating payload signature, as well as verifying that issuer is trusted.
 */
public interface TrustedSignerCertsManager {
    /**
     * Get a read-only list of root trusted certificates from Gateway's configured trust store file.
     *
     * @return a collection containing every X.509 certificate from a trusted certificate entry within the trust store, never {@code null}.
     * @throws FindException if an error happens while loading trusted certs from the Gateway's configured trust store file.
     */
    public Collection<X509Certificate> lookUpTrustedSigningCerts() throws FindException;
}
