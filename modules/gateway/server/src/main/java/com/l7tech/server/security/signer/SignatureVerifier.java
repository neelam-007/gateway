package com.l7tech.server.security.signer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.security.SignatureException;

/**
 * {@code ServerModuleFile}'s and SKAR's Signature Verifier.
 */
public interface SignatureVerifier {

    /**
     * Validates signature and also verifies that signer cert is trusted.<br/>
     * The .ZIP file must be created using our signer tool, as the content of the zip must be in specified order.
     *
     * @param zipToVerify    a {@code InputStream} containing .ZIP file as produced by the signer tool.  Required and cannot be {@code null}.
     * @throws SignatureException if signature cannot be validated or signer cert is not trusted.
     */
    public void verify(@NotNull InputStream zipToVerify) throws SignatureException;

    /**
     * Validates signature and also verifies that signer cert is trusted.
     *
     * @param digest                Calculated digest of the file content to check signature.  Required and cannot be {@code null}.
     * @param signatureProperties   A {@code String} with the signature properties, as produced by the signer tool.
     *                              Optional and can be {@code null} if module is not signed.
     * @throws SignatureException if signature cannot be validated or signer cert is not trusted.
     */
    public void verify(@NotNull byte[] digest, @Nullable String signatureProperties) throws SignatureException;

    /**
     * Validates signature and also verifies that signer cert is trusted.
     *
     * @param content               {@code InputStream} of the file content to check signature.  Required and cannot be {@code null}.
     * @param signatureProperties   A {@code String} with the signature properties, as produced by the signer tool.
     *                              Optional and can be {@code null} if module is not signed.
     * @throws SignatureException if signature cannot be validated or signer cert is not trusted.
     */
    public void verify(@NotNull InputStream content, @Nullable String signatureProperties) throws SignatureException;
}
