package com.l7tech.gateway.common.solutionkit;

import com.l7tech.gateway.common.security.signer.InnerPayloadFactory;
import com.l7tech.util.PoolByteArrayOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * {@link com.l7tech.gateway.common.solutionkit.SkarPayload} factory.
 * <p/>
 * Used by the {@link com.l7tech.gateway.common.security.signer.SignerUtils.SignedZip} framework,
 * after verifying both signature and issuer, to construct the inner {@code SkarPayload}.
 */
public class SkarPayloadFactory implements InnerPayloadFactory<SkarPayload> {
    @NotNull
    private final SolutionKitsConfig solutionKitsConfig;

    public SkarPayloadFactory(@NotNull final SolutionKitsConfig solutionKitsConfig) {
        this.solutionKitsConfig = solutionKitsConfig;
    }

    @NotNull
    @Override
    public SkarPayload create(
            @NotNull final PoolByteArrayOutputStream dataStream,
            @NotNull final byte[] dataDigest,
            @NotNull final PoolByteArrayOutputStream signaturePropsStream
    ) throws IOException {
        return new SkarPayload(dataStream, dataDigest, signaturePropsStream, solutionKitsConfig);
    }
}
