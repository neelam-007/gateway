package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.gateway.common.admin.PolicyBundleInstallerAdmin;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.bundle.BundleMapping;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Admin interface for installing the OAuth Toolkit.
 */
public interface OAuthInstallerAdmin extends PolicyBundleInstallerAdmin {

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
     * @param integrateApiPortal true if the API portal should be integrated during installation. Only relevant when the
     *                           Secure Zone Storage bundle is being installed.
     * @return Map of component id to a map which is keyed on service, policy, JDBC and assertion, whose values are
     *         the list of items which have conflicts.
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.TEST_CONFIGURATION)
    JobId<PolicyBundleDryRunResult> dryRunInstall(@NotNull Collection<String> componentIds,
                                                  @NotNull Goid folderGoid,
                                                  @NotNull Map<String, BundleMapping> bundleMappings,
                                                  @Nullable String installationPrefix,
                                                  boolean integrateApiPortal);
    /**
     * Install the bundle identified by the supplied name
     *
     * @param componentIds       collection of all bundle ids to install. Bundles may depend on each others items, but there is no
     *                           install dependency order.
     * @param folderGoid         goid of the folder to install into.
     * @param bundleMappings     Mapping of bundleId to mappings for that bundle. Required.
     * @param installationPrefix installation prefix. If not null and not empty this value will be prepended to the names
     *                           of all installed policies and the routing URIs of all installed services.
     * @param integrateApiPortal true if the API portal should be integrated during installation. Only relevant when the
     *                           Secure Zone Storage bundle is being installed.
     * @return the name of each bundle installed. If successful this will be each bundle requested.
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.SAVE_OR_UPDATE, relevantArg = 1)
    JobId<ArrayList> install(@NotNull Collection<String> componentIds,
                             @NotNull Goid folderGoid,
                             @NotNull Map<String, BundleMapping> bundleMappings,
                             @Nullable String installationPrefix,
                             boolean integrateApiPortal) throws PolicyBundleInstallerException;

    /**
     * @return Get the MySQL database schema for the OTK database
     */
    @NotNull
    @Secured(stereotype = MethodStereotype.UNCHECKED_WIDE_OPEN)
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
     * @param otkUserPasswordGoid new otk database password Goid
     * @param newJdbcConnName name of the 'JDBC Connection' entity to create.
     * @param grantHostNames The list of host to allow the otk user access from
     * @param createUser True if the user should be created if it doesn't already exist
     * @param failIfUserExists True if this should fail if the user already exists.
     * @return Server job ID.
     */
    @Secured(types = EntityType.JDBC_CONNECTION, stereotype = MethodStereotype.SAVE)
    @Transactional
    JobId<String> createOtkDatabase(String mysqlHost,
                                    String mysqlPort,
                                    String adminUsername,
                                    String adminPassword,
                                    String otkDbName,
                                    String otkDbUsername,
                                    Goid otkUserPasswordGoid,
                                    String newJdbcConnName,
                                    List<String> grantHostNames,
                                    boolean createUser,
                                    boolean failIfUserExists);
}
