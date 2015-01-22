package com.l7tech.server.policy.bundle;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Properties;

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
        this(bundleInfo, Folder.ROOT_FOLDER_ID, bundleMapping, installationPrefix, bundleResolver, checkingAssertionExistenceRequired, null, null);
    }

    /**
     * @param bundleInfo    bundle to install
     * @param folderGoid    goid of the parent folder
     * @param bundleMapping Nullable Map of mappings for the bundle to install
     * @param installationPrefix If not null, the value will be used to prefix the installation.
     * @param bundleResolver used to resolve items from bundleInfo
     * @param checkingAssertionExistenceRequired a flag to indicate if checking assertions have been installed on gateway
     * @param migrationBundleOverrides migration bundle overrides for a particular bundle component.
     * @param migrationSourceAndTargetIdsMapping a map to hold entity id mappings for one entity mapping to other entity.
     *                                           Each map entry is (sourceId, targetId), which implies that one entity with sourceId wil be mapping to a new entity with targetId.
     */
    public PolicyBundleInstallerContext(@NotNull BundleInfo bundleInfo,
                                        @NotNull Goid folderGoid,
                                        @Nullable BundleMapping bundleMapping,
                                        @Nullable String installationPrefix,
                                        @NotNull BundleResolver bundleResolver,
                                        boolean checkingAssertionExistenceRequired,
                                        @Nullable Map<String, Pair<String, Properties>> migrationBundleOverrides,
                                        @Nullable Map<String, String> migrationSourceAndTargetIdsMapping) {
        this.bundleInfo = bundleInfo;
        this.folderGoid = folderGoid;
        if (bundleMapping != null) {
            this.bundleMapping = bundleMapping;
        } else {
            this.bundleMapping = new BundleMapping();
        }
        this.bundleResolver = bundleResolver;
        this.checkingAssertionExistenceRequired = checkingAssertionExistenceRequired;
        this.migrationBundleOverrides = migrationBundleOverrides;
        this.migrationSourceAndTargetIdsMapping = migrationSourceAndTargetIdsMapping;

        this.installationPrefix = (installationPrefix == null || installationPrefix.trim().isEmpty())?
                null:
                installationPrefix.trim();
    }

    @NotNull
    public BundleInfo getBundleInfo() {
        return bundleInfo;
    }

    @NotNull
    public Goid getFolderGoid() {
        return folderGoid;
    }

    @NotNull
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

    @Nullable
    public Map<String,Pair<String,Properties>> getMigrationBundleOverrides() {
        return migrationBundleOverrides;
    }

    @NotNull
    public BundleResolver getBundleResolver() {
        return bundleResolver;
    }

    public boolean isCheckingAssertionExistenceRequired() {
        return checkingAssertionExistenceRequired;
    }

    public Map<String, String> getMigrationSourceAndTargetIdsMapping() {
        return migrationSourceAndTargetIdsMapping;
    }

    // - PRIVATE

    @NotNull
    private final BundleInfo bundleInfo;
    @NotNull
    private final Goid folderGoid;
    @NotNull
    private final BundleMapping bundleMapping;
    @Nullable
    private final String installationPrefix;
    @NotNull
    private final BundleResolver bundleResolver;
    private final boolean checkingAssertionExistenceRequired;
    private final Map<String, Pair<String,Properties>> migrationBundleOverrides;
    private final Map<String, String> migrationSourceAndTargetIdsMapping;
}