package com.l7tech.gateway.common.solutionkit;

import com.l7tech.gateway.common.security.signer.InnerPayloadFactory;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * SKAR payload.
 * Creates a new {@link SkarProcessor}, with {@link #solutionKitsConfig}, and loads the inner payloads.
 * todo: consider merging this class with {@link com.l7tech.gateway.common.solutionkit.SkarProcessor}.
 */
public final class SkarPayload extends SignerUtils.SignedZip.InnerPayload {
    /**
     * Override {@link com.l7tech.gateway.common.security.signer.SignerUtils.SignedZip.InnerPayload#FACTORY} to avoid accidental usage.
     */
    @SuppressWarnings("UnusedDeclaration")
    @Nullable
    private static final InnerPayloadFactory<SkarPayload> FACTORY = null;

    @NotNull
    private final SolutionKitsConfig solutionKitsConfig;

    /**
     * Package access constructor, used by {@link com.l7tech.gateway.common.solutionkit.SkarPayloadFactory}.
     */
    SkarPayload(
            @NotNull final PoolByteArrayOutputStream dataStream,
            @NotNull final byte[] dataDigest,
            @NotNull final PoolByteArrayOutputStream signaturePropsStream,
            @NotNull final SolutionKitsConfig solutionKitsConfig
    ) {
        super(dataStream, dataDigest, signaturePropsStream);
        this.solutionKitsConfig = solutionKitsConfig;
    }

    // todo: add more constructors for unsigned zips/content

    /**
     * Creates a new {@link SkarProcessor} and loads the payload within this class.
     *
     * @throws SolutionKitException if an error happens while processing the specified SKAR file.
     * @see SkarProcessor
     * @see SkarProcessor#load(java.io.InputStream)
     */
    @NotNull
    public SkarProcessor load() throws SolutionKitException {
        final SkarProcessor skarProcessor = new SkarProcessor(solutionKitsConfig);
        skarProcessor.load(getDataStream());
        return skarProcessor;
    }
}
