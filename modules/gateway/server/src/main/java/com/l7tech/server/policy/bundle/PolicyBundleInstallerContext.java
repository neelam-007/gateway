package com.l7tech.server.policy.bundle;

import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PolicyBundleInstallerContext {

    /**
     * @param bundleInfo    bundle to install
     * @param folderOid     oid of the parent folder
     * @param installFolder folder to install into. Required. May already exist.
     * @param bundleMapping Nullable Map of mappings for the bundle to install
     */
    public PolicyBundleInstallerContext(@NotNull BundleInfo bundleInfo,
                                        long folderOid,
                                        @Nullable String installFolder,
                                        @NotNull Map<String, Object> contextMap,
                                        @Nullable BundleMapping bundleMapping) {
        this.bundleInfo = bundleInfo;
        this.folderOid = folderOid;
        this.installFolder = installFolder;
        this.contextMap = contextMap;
        this.bundleMapping = bundleMapping;
    }

    @NotNull
    public BundleInfo getBundleInfo() {
        return bundleInfo;
    }

    public long getFolderOid() {
        return folderOid;
    }

    @Nullable
    public String getInstallFolder() {
        return installFolder;
    }

    @NotNull
    public Map<String, Object> getContextMap() {
        return contextMap;
    }

    @Nullable
    public BundleMapping getBundleMapping() {
        return bundleMapping;
    }

    // - PRIVATE

    @NotNull
    private final BundleInfo bundleInfo;
    private final long folderOid;
    @Nullable
    private final String installFolder;
    @NotNull
    private final Map<String, Object> contextMap;
    @Nullable
    private final BundleMapping bundleMapping;

}
