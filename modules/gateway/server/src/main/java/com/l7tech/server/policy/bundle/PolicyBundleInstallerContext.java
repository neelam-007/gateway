package com.l7tech.server.policy.bundle;

import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Installation context used by clients of the Policy Bundle Installer to pass all required information
 */
public class PolicyBundleInstallerContext {

    /**
     * Create an installation context which has a default folder id of the root node.
     *
     * @param bundleInfo    bundle to install
     * @param bundleMapping Nullable Map of mappings for the bundle to install
     * @param installationPrefix If not null, the value will be used to prefix the installation.
     * @param bundleResolver used to resolve items from bundleInfo
     * @param checkingAssertionExistenceRequired a flag to indicate if checking assertions have been installed on gateway
     */
    public PolicyBundleInstallerContext(@NotNull BundleInfo bundleInfo,
                                        @Nullable BundleMapping bundleMapping,
                                        @Nullable String installationPrefix,
                                        @NotNull BundleResolver bundleResolver,
                                        boolean checkingAssertionExistenceRequired) {
        this(bundleInfo, ROOT_FOLDER_OID, bundleMapping, installationPrefix, bundleResolver, checkingAssertionExistenceRequired);
    }



    /**
     * @param bundleInfo    bundle to install
     * @param folderOid     oid of the parent folder
     * @param bundleMapping Nullable Map of mappings for the bundle to install
     * @param installationPrefix If not null, the value will be used to prefix the installation.
     * @param bundleResolver used to resolve items from bundleInfo
     * @param checkingAssertionExistenceRequired a flag to indicate if checking assertions have been installed on gateway
     */
    public PolicyBundleInstallerContext(@NotNull BundleInfo bundleInfo,
                                        long folderOid,
                                        @Nullable BundleMapping bundleMapping,
                                        @Nullable String installationPrefix,
                                        @NotNull BundleResolver bundleResolver,
                                        boolean checkingAssertionExistenceRequired) {
        this.bundleInfo = bundleInfo;
        this.folderOid = folderOid;
        this.bundleMapping = bundleMapping;
        this.bundleResolver = bundleResolver;
        this.checkingAssertionExistenceRequired = checkingAssertionExistenceRequired;

        this.installationPrefix = (installationPrefix == null || installationPrefix.trim().isEmpty())?
                null:
                installationPrefix.trim();
    }

    @NotNull
    public BundleInfo getBundleInfo() {
        return bundleInfo;
    }

    public long getFolderOid() {
        return folderOid;
    }

    @Nullable
    public BundleMapping getBundleMapping() {
        return bundleMapping;
    }

    /**
     * If not null, this returned value should be used to prefix each installed policy and each routing URI of
     * each published service.
     * @return installation prefix. If not null then the value is never an empty string.
     */
    @Nullable
    public String getInstallationPrefix() {
        return installationPrefix;
    }

    @NotNull
    public BundleResolver getBundleResolver() {
        return bundleResolver;
    }

    public boolean isCheckingAssertionExistenceRequired() {
        return checkingAssertionExistenceRequired;
    }

    public void setCheckingAssertionExistenceRequired(boolean checkingAssertionExistenceRequired) {
        this.checkingAssertionExistenceRequired = checkingAssertionExistenceRequired;
    }

    // - PRIVATE

    private static final long ROOT_FOLDER_OID = -5002L;

    @NotNull
    private final BundleInfo bundleInfo;
    private final long folderOid;
    @Nullable
    private final BundleMapping bundleMapping;
    @Nullable
    private final String installationPrefix;
    @NotNull
    final BundleResolver bundleResolver;

    private boolean checkingAssertionExistenceRequired;
}