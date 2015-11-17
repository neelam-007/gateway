package com.l7tech.server.security.signer;

import com.l7tech.gateway.common.security.signer.SignatureVerifierAdmin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.security.SignatureException;

/**
 * Signature Verifier Admin Implementation.<br/>
 * Proxies all methods to {@link SignatureVerifierServer}.
 */
public class SignatureVerifierAdminImpl implements SignatureVerifierAdmin {

    /**
     * Our proxy {@link com.l7tech.server.security.signer.SignatureVerifierServer}.
     */
    private SignatureVerifierServer signatureVerifier;

    @Override
    public void verify(@NotNull final byte[] digest, @Nullable final String signatureProperties) throws SignatureException {
        getSignatureVerifier().verify(digest, signatureProperties);
    }

    @Override
    public void verify(@NotNull final byte[] digest, @Nullable final byte[] signatureProperties) throws SignatureException {
        getSignatureVerifier().verify(digest, signatureProperties);
    }

    /**
     * Throws {@code IllegalStateException} if {@link #signatureVerifier} is {@code null}.
     */
    @NotNull
    public SignatureVerifierServer getSignatureVerifier() {
        if (signatureVerifier == null) {
            throw new IllegalStateException("signatureVerifier cannot be null");
        }
        return signatureVerifier;
    }

    @Inject
    @Named("signatureVerifier")
    public void setSignatureVerifier(final SignatureVerifierServer signatureVerifier) {
        this.signatureVerifier = signatureVerifier;
    }
}
