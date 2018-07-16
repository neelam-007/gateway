package com.l7tech.gateway.common.security.signer;

import com.l7tech.util.PoolByteArrayOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Signed zip inner payload factory, used to create inner payload after verifying both signature and issuer.
 * <p/>
 * Currently there are two signed entities {@code ServerModuleFile} and {@code SolutionKit} archive (i.e. {@code SKAR}) file.<br/>
 * Use this factory to declare future payloads.
 */
public interface InnerPayloadFactory<T extends SignerUtils.SignedZip.InnerPayload> {
    /**
     * Create the payload using the specified parameters.<br/>
     * Called by the {@link com.l7tech.gateway.common.security.signer.SignerUtils.SignedZip} framework,
     * after verifying both signature and issuer, in order to construct the inner payload.
     *
     * @param dataStream              inner payload raw bytes.  Required and cannot be {@code null}.
     * @param dataDigest              pre calculated inner payload digest.  Required and cannot be {@code null}.
     * @param signaturePropsStream    signature properties.  Required and cannot be {@code null}.
     * @return the payload object, specified by the generic type {@code T}, never {@code null}.
     * @throws IOException if an error happens while creating the payload.
     */
    @NotNull
    T create(@NotNull PoolByteArrayOutputStream dataStream,
             @NotNull byte[] dataDigest,
             @NotNull PoolByteArrayOutputStream signaturePropsStream
    ) throws IOException;
}
