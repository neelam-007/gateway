package com.l7tech.external.assertions.oauthinstaller;

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
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Admin interface for installing the OAuth Toolkit.
 */
@Administrative
@Secured(types = EntityType.FOLDER)
public interface OAuthInstallerAdmin extends AsyncAdminMethods{

    /**
     * Get the overall version of the OAuth Toolkit this installer will install.
     *
     * Each time a new AAR is created due to a change the version should be increased.
     *
     * @return String representing the version.
     */
    @NotNull
    String getOAuthToolkitVersion() throws OAuthToolkitInstallationException;

    /**
     * Get the list of OTK components (name + description) available for installation.
     * @return list of triples of bundle id, bundle name and bundle description. Never null and no null contents.
     * @throws OAuthToolkitInstallationException
     */
    @NotNull
    List<BundleInfo> getAllOtkComponents() throws OAuthToolkitInstallationException;

    /**
     * Get the MySQL database schema for the OTK database
     * @return
     */
    @NotNull
    String getOAuthDatabaseSchema();

    /**
     * Create the OTK database on a MySQL database server.
     *
     * @param mysqlHost mysql server host
     * @param mysqlPort mysql server port
     * @param adminUsername mysql server user with create permissions
     * @param adminPassword mysql server user password
     * @param otkDbName new otk database name
     * @param otkDbUsername new otk database user. Ok if user already exists.
     * @param otkUserPasswordOid new otk database password Oid
     * @param newJdbcConnName name of the 'JDBC Connection' entity to create.
     * @param grantHostNames The list of host to allow the otk user access from
     * @param createUser True if the user should be created if it doesn't already exist
     * @param failIfUserExists True if this should fail if the user already exists.
     * @return
     */
    @Secured(types = EntityType.JDBC_CONNECTION, stereotype = MethodStereotype.SAVE)
    @Transactional
    JobId<String> createOtkDatabase(String mysqlHost,
                                    String mysqlPort,
                                    String adminUsername,
                                    String adminPassword,
                                    String otkDbName,
                                    String otkDbUsername,
                                    long otkUserPasswordOid,
                                    String newJdbcConnName,
                                    List<String> grantHostNames,
                                    boolean createUser,
                                    boolean failIfUserExists);


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
     * @param integrateApiPortal true if the API portal should be integrated during installation. Only relevant when the
     *                           Secure Zone Storage bundle is being installed.
     * @return Map of component id to a map which is keyed on service, policy, jdbc and assertion, whose values are
     *         the list of items which have conflicts.
     */
    @NotNull
    JobId<PolicyBundleDryRunResult> dryRunOtkInstall(@NotNull Collection<String> otkComponentId,
                                                     @NotNull Map<String, BundleMapping> bundleMappings,
                                                     @Nullable String installationPrefix,
                                                     boolean integrateApiPortal);
    /**
     * Install the bundle identified by the supplied name
     *
     * @param otkComponentId     collection of all bundle ids to install. Bundles may depend on each others items, but there is no
     *                           install dependency order.
     * @param folderOid          oid of the folder to install into.
     * @param bundleMappings     Mapping of bundleId to mappings for that bundle. Required.
     * @param installationPrefix installation prefix. If not null and not empty this value will be prepended to the names
     *                           of all installed policies and the routing URIs of all installed services.
     * @param integrateApiPortal true if the API portal should be integrated during installation. Only relevant when the
     *                           Secure Zone Storage bundle is being installed.
     * @return the name of each bundle installed. If successful this will be each bundle requested.
     */
    @Secured(stereotype = MethodStereotype.SAVE_OR_UPDATE, relevantArg = 1)
    @NotNull
    AsyncAdminMethods.JobId<ArrayList> installOAuthToolkit(@NotNull Collection<String> otkComponentId,
                                                           long folderOid,
                                                           @NotNull Map<String, BundleMapping> bundleMappings,
                                                           @Nullable String installationPrefix,
                                                           boolean integrateApiPortal) throws OAuthToolkitInstallationException;

    public static class OAuthToolkitInstallationException extends Exception{
        public OAuthToolkitInstallationException(String message) {
            super(message);
        }

        public OAuthToolkitInstallationException(String message, Throwable cause) {
            super(message, cause);
        }

        public OAuthToolkitInstallationException(Throwable cause) {
            super(cause);
        }
    }
}
