package com.l7tech.server.security.signer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.security.SignatureException;

/**
 * <p>Stub for {@code SignatureVerifier}.<br/>
 * Basically acts as a proxy class, allowing you to set the real {@code SignatureVerifier},
 * using the {@link #setProxyVerifier(SignatureVerifierServer)}.</p>
 *
 * Throws {@code IllegalStateException} if the proxy object {@link #proxyVerifier} is not set.
 */
public class SignatureVerifierServerStub implements SignatureVerifierServer {

    private SignatureVerifierServer proxyVerifier;

    @Override
    public void verify(@NotNull final InputStream zipToVerify) throws SignatureException {
        if (proxyVerifier == null) {
            throw new IllegalStateException("proxyVerifier not set");
        }
        proxyVerifier.verify(zipToVerify);
    }

    @Override
    public void verify(@NotNull final byte[] digest, @Nullable final String signatureProperties) throws SignatureException {
        if (proxyVerifier == null) {
            throw new IllegalStateException("proxyVerifier not set");
        }
        proxyVerifier.verify(digest, signatureProperties);
    }

    @Override
    public void verify(@NotNull final InputStream content, @Nullable final String signatureProperties) throws SignatureException {
        if (proxyVerifier == null) {
            throw new IllegalStateException("proxyVerifier not set");
        }
        proxyVerifier.verify(content, signatureProperties);
    }

    @Override
    public void verify(@NotNull final byte[] digest, @Nullable final byte[] signatureProperties) throws SignatureException {
        if (proxyVerifier == null) {
            throw new IllegalStateException("proxyVerifier not set");
        }
        proxyVerifier.verify(digest, signatureProperties);
    }

    public void setProxyVerifier(@NotNull final SignatureVerifierServer proxyVerifier) {
        this.proxyVerifier = proxyVerifier;
    }
}
