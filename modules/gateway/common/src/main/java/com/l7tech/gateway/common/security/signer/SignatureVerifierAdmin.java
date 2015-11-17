package com.l7tech.gateway.common.security.signer;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.Secured;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SignatureException;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.UNCHECKED_WIDE_OPEN;

/**
 * Signature Verifier Admin interface
 */
@Secured
@Administrative
public interface SignatureVerifierAdmin extends SignatureVerifier {
    @Override
    @Administrative(background = true)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    void verify(@NotNull byte[] digest, @Nullable String signatureProperties) throws SignatureException;

    @Override
    @Administrative(background = true)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    void verify(@NotNull byte[] digest, @Nullable byte[] signatureProperties) throws SignatureException;
}
