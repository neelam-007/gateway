package com.l7tech.gateway.common.module;

import com.l7tech.gateway.common.security.signer.InnerPayloadFactory;
import com.l7tech.util.PoolByteArrayOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * {@link com.l7tech.gateway.common.module.ServerModuleFilePayload} factory.
 * <p/>
 * Used by the {@link com.l7tech.gateway.common.security.signer.SignerUtils.SignedZip} framework,
 * after verifying both signature and issuer, to construct the inner {@code ServerModuleFilePayload}.
 */
public class ServerModuleFilePayloadFactory implements InnerPayloadFactory<ServerModuleFilePayload> {
    @NotNull
    private final CustomAssertionsScannerHelper customAssertionsScannerHelper;
    @NotNull
    private final ModularAssertionsScannerHelper modularAssertionsScannerHelper;
    @Nullable
    private final String fileName;

    public ServerModuleFilePayloadFactory(
            @NotNull final CustomAssertionsScannerHelper customAssertionsScannerHelper,
            @NotNull final ModularAssertionsScannerHelper modularAssertionsScannerHelper,
            @Nullable final String fileName
    ) {
        this.customAssertionsScannerHelper = customAssertionsScannerHelper;
        this.modularAssertionsScannerHelper = modularAssertionsScannerHelper;
        this.fileName = fileName;
    }

    @NotNull
    @Override
    public ServerModuleFilePayload create(
            @NotNull final PoolByteArrayOutputStream dataStream,
            @NotNull final byte[] dataDigest,
            @NotNull final PoolByteArrayOutputStream signaturePropsStream
    ) throws IOException {
        return new ServerModuleFilePayload(
                dataStream,
                dataDigest,
                signaturePropsStream,
                customAssertionsScannerHelper,
                modularAssertionsScannerHelper,
                fileName
        );
    }
}
