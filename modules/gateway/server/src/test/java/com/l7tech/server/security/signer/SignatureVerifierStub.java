package com.l7tech.server.security.signer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.security.SignatureException;

/**
 * <p>Stub for {@code com.l7tech.server.security.signer.SignatureVerifier}.<br/>
 * Basically acts as a proxy class, allowing you to set the real {@code SignatureVerifier},
 * using the {@link #setProxyVerifier(SignatureVerifier)}.</p>
 *
 * Throws {@code IllegalStateException} if the proxy object {@link #proxyVerifier} is not set.
 */
public class SignatureVerifierStub implements SignatureVerifier {

    private SignatureVerifier proxyVerifier;

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

    public void setProxyVerifier(@NotNull final SignatureVerifier proxyVerifier) {
        this.proxyVerifier = proxyVerifier;
    }
}
