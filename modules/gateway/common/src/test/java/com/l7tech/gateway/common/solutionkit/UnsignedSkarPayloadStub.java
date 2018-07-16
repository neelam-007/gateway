package com.l7tech.gateway.common.solutionkit;

import com.l7tech.gateway.api.Bundle;
import com.l7tech.util.Pair;
import com.l7tech.util.PoolByteArrayOutputStream;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.mockito.internal.util.MockUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unsigned {@code SkarPayload} stub.
 */
public class UnsignedSkarPayloadStub extends SkarPayload {
    @NotNull
    private final InputStream unsignedSkar;

    public UnsignedSkarPayloadStub(@NotNull final SolutionKitsConfig solutionKitsConfig, @NotNull final InputStream unsignedSkar) {
        super(Mockito.mock(PoolByteArrayOutputStream.class), new byte[0], Mockito.mock(PoolByteArrayOutputStream.class), solutionKitsConfig);
        this.unsignedSkar = unsignedSkar;
    }

    /**
     * Create using the specified {@code solutionKitsConfig} and the {@code solutionKits} array.
     *
     * @param solutionKitsConfig    the config to use.  Required and cannot be {@code null}.
     * @param solutionKits          array of solution kits to simulate load. For collections of skars the first one is the parent.
     *                              Required and cannot be {@code null}.
     */
    public UnsignedSkarPayloadStub(@NotNull final SolutionKitsConfig solutionKitsConfig, @NotNull final SolutionKit ... solutionKits) {
        this(solutionKitsConfig, solutionKitBundlePairs(solutionKits));
    }

    /**
     * Create using the specified {@code solutionKitsConfig} and the {@code solutionKitBundleList}.
     *
     * @param solutionKitsConfig       the config to use.  Required and cannot be {@code null}.
     * @param solutionKitBundleList    list of {@code SolutionKit} and {@code Bundle} {@code Pair} to simulate load.
     *                                 For collections of skars the first one is the parent.
     *                                 Required and cannot be {@code null}.
     */
    public UnsignedSkarPayloadStub(@NotNull final SolutionKitsConfig solutionKitsConfig, @NotNull final List<Pair<SolutionKit, Bundle>> solutionKitBundleList) {
        super(Mockito.mock(PoolByteArrayOutputStream.class), new byte[0], Mockito.mock(PoolByteArrayOutputStream.class), solutionKitsConfig);
        this.unsignedSkar = Mockito.mock(InputStream.class);

        // simulate load of the specified solutionKitBundleList
        final MockUtil mockUtil = new MockUtil();
        if (mockUtil.isMock(solutionKitsConfig)) {
            final Map<SolutionKit, Bundle> loadedSolutionKits = new HashMap<>();
            if (solutionKitBundleList.size() > 1) {
                Mockito.doReturn(solutionKitBundleList.get(0).left).when(solutionKitsConfig).getParentSolutionKitLoaded();
                for (int i = 1; i < solutionKitBundleList.size(); ++i) {
                    loadedSolutionKits.put(solutionKitBundleList.get(i).left, solutionKitBundleList.get(i).right);
                }
            } else {
                loadedSolutionKits.put(solutionKitBundleList.get(0).left, solutionKitBundleList.get(0).right);
            }
            Mockito.doReturn(loadedSolutionKits).when(solutionKitsConfig).getLoadedSolutionKits();
        } else {
            solutionKitsConfig.getLoadedSolutionKits().clear();
            if (solutionKitBundleList.size() > 1) {
                solutionKitsConfig.setParentSolutionKitLoaded(solutionKitBundleList.get(0).left);
                for (int i = 1; i < solutionKitBundleList.size(); ++i) {
                    solutionKitsConfig.getLoadedSolutionKits().put(solutionKitBundleList.get(i).left, solutionKitBundleList.get(i).right);
                }
            } else {
                solutionKitsConfig.getLoadedSolutionKits().put(solutionKitBundleList.get(0).left, solutionKitBundleList.get(0).right);
            }
        }
    }

    @NotNull
    private static List<Pair<SolutionKit, Bundle>> solutionKitBundlePairs(@NotNull final SolutionKit[] solutionKits) {
        final List<Pair<SolutionKit, Bundle>> solutionKitBundleList = new ArrayList<>();
        for (final SolutionKit solutionKit : solutionKits) {
            solutionKitBundleList.add(Pair.pair(solutionKit, (Bundle)null));
        }
        return solutionKitBundleList;
    }

    @NotNull
    @Override
    public InputStream getDataStream() {
        return unsignedSkar;
    }
}