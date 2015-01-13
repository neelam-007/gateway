package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Admin interface for implementing Policy Bundle install.
 */

@Secured
@Administrative
public interface PolicyBundleInstallerAdmin extends AsyncAdminMethods {

    /**
     * Policy Bundle Installer Exception
     */
    public class PolicyBundleInstallerException extends Exception {
        public PolicyBundleInstallerException(String message) {
            super(message);
        }

        public PolicyBundleInstallerException(String message, Throwable cause) {
            super(message, cause);
        }

        public PolicyBundleInstallerException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Get the overall version the installer will install.
     *
     * Each time a new AAR is created due to a change the version should be increased.
     *
     * @return String representing the version.
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    String getVersion();

    /**
     * Get the list of components (name + description) available for installation.
     * @return list of triples of bundle id, bundle name and bundle description. Never null and no null contents.
     * @throws PolicyBundleInstallerAdmin.PolicyBundleInstallerException
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    List<BundleInfo> getAllComponents() throws PolicyBundleInstallerException;

    /**
     * Optionally pass in the authenticated user from a service using authentication assertions (e.g. from the PolicyEnforcementContext).
     * @param authenticatedUser authenticated user from the PolicyEnforcementContext
     */
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    void setAuthenticatedUser(@Nullable UserBean authenticatedUser);

    /**
     * Dry run the installation.
     * Checks to see if:
     * <ul>
     * <li>any services routing URI will collide with any existing services.</li>
     * <li>any policy names will collide with any existing policies (names are unique across a gateway)</li>
     * <li>any referenced JDBC connection does not exist</li>
     * </ul>
     *
     * @param componentIds       Collection of all bundle ids to dry run.
     * @param bundleMappings     Mapping of bundleId to mappings for that bundle. Required.
     * @param installationPrefix installation prefix. If not null and not empty this value will be prepended to the names
     *                           of all installed policies and the routing URIs of all installed services before checking
     *                           for conflicts of those values.
     * @return Map of component id to a map which is keyed on service, policy, JDBC and assertion, whose values are
     *         the list of items which have conflicts.
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.TEST_CONFIGURATION)
    JobId<PolicyBundleDryRunResult> dryRunInstall(@NotNull Collection<String> componentIds,
                                                  @NotNull Map<String, BundleMapping> bundleMappings,
                                                  @Nullable String installationPrefix);

    /**
     * This method is the same as the above method, except a target folder with a goid folderGoid" used during dry run.
     * @param folderGoid: the goid of the target folder
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.TEST_CONFIGURATION)
    JobId<PolicyBundleDryRunResult> dryRunInstall(@NotNull Collection<String> componentIds,
                                                  @NotNull Map<String, BundleMapping> bundleMappings,
                                                  @NotNull Goid folderGoid,
                                                  @Nullable String installationPrefix);
    /**
     * Install the bundle identified by the supplied name
     *
     * @param componentIds       collection of all bundle ids to install. Bundles may depend on each others items, but there is no
     *                           install dependency order.
     * @param folderGoid         Folder to install component into.
     * @param bundleMappings     Mapping of bundleId to mappings for that bundle. Required.
     * @return the name of each bundle installed. If successful this will be each bundle requested.
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.SAVE_OR_UPDATE, relevantArg = 1)
    JobId<ArrayList> install(@NotNull Collection<String> componentIds,
                             @NotNull Goid folderGoid,
                             @NotNull Map<String, BundleMapping> bundleMappings) throws PolicyBundleInstallerException;

    /**
     * Install the bundle identified by the supplied name
     *
     * @param componentIds collection of all bundle ids to install. Bundles may depend on each others items, but there is no
     *                     install dependency order.
     * @param folderGoid Folder to install component into.
     * @param bundleMappings Mapping of bundleId to mappings for that bundle. Required.
     * @param installationPrefix installation prefix. If not null and not empty this value will be prepended to the names
     *                           of all installed policies and the routing URIs of all installed services.
     * @param migrationBundleOverrides override value to resolve migration conflicts
     * @return the name of each bundle installed. If successful this will be each bundle requested.
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.SAVE_OR_UPDATE, relevantArg = 1)
    JobId<ArrayList> install(@NotNull Collection<String> componentIds,
                             @NotNull Goid folderGoid,
                             @NotNull Map<String, BundleMapping> bundleMappings,
                             @Nullable String installationPrefix,
                             @Nullable Map<String,Pair<String,Properties>> migrationBundleOverrides) throws PolicyBundleInstallerException;
}