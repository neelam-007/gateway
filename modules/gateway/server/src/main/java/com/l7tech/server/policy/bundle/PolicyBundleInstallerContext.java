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
     * @param installationPrefix If not null, the value will be used to prefix the installation.
     * @param bundleMapping Nullable Map of mappings for the bundle to install
     */
    public PolicyBundleInstallerContext(@NotNull BundleInfo bundleInfo,
                                        long folderOid,
                                        @NotNull Map<String, Object> contextMap,
                                        @Nullable BundleMapping bundleMapping,
                                        @Nullable String installationPrefix) {
        this.bundleInfo = bundleInfo;
        this.folderOid = folderOid;
        this.contextMap = contextMap;
        this.bundleMapping = bundleMapping;
        this.installationPrefix = installationPrefix;
    }

    @NotNull
    public BundleInfo getBundleInfo() {
        return bundleInfo;
    }

    public long getFolderOid() {
        return folderOid;
    }

    @NotNull
    public Map<String, Object> getContextMap() {
        return contextMap;
    }

    @Nullable
    public BundleMapping getBundleMapping() {
        return bundleMapping;
    }

    /**
     * If not null, this returned value should be used to prefix each installed policy and each routing URI of
     * each published service.
     * @return installation prefix.
     */
    @Nullable
    public String getInstallationPrefix() {
        return installationPrefix;
    }

    // - PRIVATE

    @NotNull
    private final BundleInfo bundleInfo;
    private final long folderOid;
    @NotNull
    private final Map<String, Object> contextMap;
    @Nullable
    private final BundleMapping bundleMapping;
    @Nullable
    private final String installationPrefix;

}
