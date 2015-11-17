package com.l7tech.gateway.common.security.signer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SignatureException;

/**
 * Signature verifier
 */
public interface SignatureVerifier {

    /**
     * Validates signature and also verifies that signer cert is trusted.
     *
     * @param digest                Calculated SHA-256 digest of the content to check signature.  Required and cannot be {@code null}.
     *                              Note: this MUST NOT just be the value claimed by the sender -- it must be a
     *                              freshly computed value from hashing the information covered by the signature.
     * @param signatureProperties   A {@code String} with the signature properties, as produced by the signer tool,
     *                              holding ASN.1 encoded X.509 certificate as Base64 string and ASN.1 encoded signature
     *                              value as Base64 string.
     *                              Optional and can be {@code null} if module is not signed.
     * @throws SignatureException if signature cannot be validated or signer cert is not trusted.
     */
    public void verify(@NotNull byte[] digest, @Nullable String signatureProperties) throws SignatureException;

    /**
     * Validates signature and also verifies that signer cert is trusted.
     *
     * @param digest              Calculated SHA-256 digest of the content to check signature.  Required and cannot be {@code null}.
     *                            Note: this MUST NOT just be the value claimed by the sender -- it must be a
     *                            freshly computed value from hashing the information covered by the signature.
     * @param signatureProperties Signature properties bytes, as produced by the signer tool, holding ASN.1 encoded
     *                            X.509 certificate as Base64 string and ASN.1 encoded signature value as Base64 string.
     *                            Optional and can be {@code null} if module is not signed.
     * @throws SignatureException if signature cannot be validated or signer cert is not trusted.
     */
    public void verify(@NotNull byte[] digest, @Nullable byte[] signatureProperties) throws SignatureException;
}
