package com.l7tech.gateway.common.solutionkit;

import com.l7tech.util.PoolByteArrayOutputStream;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

import java.io.InputStream;

/**
 */
public class UnsignedSkarPayloadStub extends SkarPayload {
    @NotNull
    private InputStream unsignedSkar;

    public UnsignedSkarPayloadStub(@NotNull final SolutionKitsConfig solutionKitsConfig, @NotNull final InputStream unsignedSkar) {
        super(Mockito.mock(PoolByteArrayOutputStream.class), new byte[0], Mockito.mock(PoolByteArrayOutputStream.class), solutionKitsConfig);
        this.unsignedSkar = unsignedSkar;
    }

    @NotNull
    @Override
    public InputStream getDataStream() {
        return unsignedSkar;
    }
}