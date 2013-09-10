package com.l7tech.external.assertions.salesforceinstaller;

import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Admin interface for installing Salesforce.
 */

@Secured
@Administrative
public interface SalesforceInstallerAdmin extends AsyncAdminMethods {

    /**
     * Get the overall version this installer will install.
     *
     * Each time a new AAR is created due to a change the version should be increased.
     *
     * @return String representing the version.
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    String getVersion() throws SalesforceInstallationException;

    /**
     * Get the list of components (name + description) available for installation.
     * @return list of triples of bundle id, bundle name and bundle description. Never null and no null contents.
     * @throws SalesforceInstallationException
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
    List<BundleInfo> getAllComponents() throws SalesforceInstallationException;

    /**
     * Dry run the OTK installation.
     * Checks to see if:
     * <ul>
     * <li>any services routing URI will collide with any existing services.</li>
     * <li>any policy names will collide with any existing policies (names are unique across a gateway)</li>
     * <li>any referenced jdbc connection does not exist</li>
     * </ul>
     *
     * @param otkComponentId     collection of all bundle ids to dry run
     * @param bundleMappings     Mapping of bundleId to mappings for that bundle. Required.
     * @param installationPrefix installation prefix. If not null and not empty this value will be prepended to the names
     *                           of all installed policies and the routing URIs of all installed services before checking
     *                           for conflicts of those values.
     * @return Map of component id to a map which is keyed on service, policy, jdbc and assertion, whose values are
     *         the list of items which have conflicts.
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.TEST_CONFIGURATION, types=EntityType.FOLDER)
    JobId<PolicyBundleDryRunResult> dryRunInstall(@NotNull Collection<String> otkComponentId,
                                                  @NotNull Map<String, BundleMapping> bundleMappings,
                                                  @Nullable String installationPrefix);
    /**
     * Install the bundle identified by the supplied name
     *
     * @param otkComponentId     collection of all bundle ids to install. Bundles may depend on each others items, but there is no
     *                           install dependency order.
     * @param bundleMappings     Mapping of bundleId to mappings for that bundle. Required.
     * @param installationPrefix installation prefix. If not null and not empty this value will be prepended to the names
     *                           of all installed policies and the routing URIs of all installed services.
     * @return the name of each bundle installed. If successful this will be each bundle requested.
     */
    @Secured(stereotype = MethodStereotype.SAVE_OR_UPDATE, relevantArg = 1)
    @NotNull
    AsyncAdminMethods.JobId<ArrayList> install(@NotNull Collection<String> otkComponentId,
                                               @NotNull Map<String, BundleMapping> bundleMappings,
                                               @Nullable String installationPrefix) throws SalesforceInstallationException;

    public static class SalesforceInstallationException extends Exception{
        public SalesforceInstallationException(String message) {
            super(message);
        }

        public SalesforceInstallationException(String message, Throwable cause) {
            super(message, cause);
        }

        public SalesforceInstallationException(Throwable cause) {
            super(cause);
        }
    }
}