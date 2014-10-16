package com.l7tech.external.assertions.policybundleexporter.server;

import com.l7tech.external.assertions.policybundleexporter.ComponentInfo;
import com.l7tech.objectmodel.Goid;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Export context used to pass required information.
 */
public class PolicyBundleExporterContext {
    @NotNull
    private final Goid bundleFolderGoid;

    @NotNull
    private final List<ComponentInfo> componentInfoList;

    public PolicyBundleExporterContext(
            @NotNull Goid bundleFolderGoid,
            @NotNull List<ComponentInfo> componentInfoList) {
        this.bundleFolderGoid = bundleFolderGoid;
        this.componentInfoList = componentInfoList;
    }

    @NotNull
    public Goid getBundleFolderGoid() {
        return bundleFolderGoid;
    }

    @NotNull
    public List<ComponentInfo> getComponentInfoList() {
        return componentInfoList;
    }
}