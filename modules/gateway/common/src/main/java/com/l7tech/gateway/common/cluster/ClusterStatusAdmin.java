package com.l7tech.gateway.common.cluster;

import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.gateway.common.licensing.*;
import com.l7tech.gateway.common.module.ServerModuleConfig;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.module.ServerModuleFileState;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.objectmodel.*;
import com.l7tech.util.CollectionUpdate;
import com.l7tech.util.Either;
import com.l7tech.util.Option;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.security.KeyStoreException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.SSG_KEY_ENTRY;
import static com.l7tech.objectmodel.EntityType.TRUSTED_ESM;

/**
 * Remote interface for getting the status of nodes in a gateway cluster.
 */
@Secured
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
@Administrative
public interface ClusterStatusAdmin extends AsyncAdminMethods {
    /**
     * Returns true if this node is part of a cluster.
     * @return true if this node is part of a cluster
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CLUSTER_INFO, stereotype=MethodStereotype.FIND_ENTITIES)
    @Administrative(licensed=false)
    boolean isCluster();

    /**
     * Get status for all nodes part of the cluster.
     * @return An array of ClusterNodeInfo (one for each node). Never null, may be of 0 length.
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CLUSTER_INFO, stereotype=MethodStereotype.FIND_ENTITIES)
    @Administrative(licensed=false, background = true)
    ClusterNodeInfo[] getClusterStatus() throws FindException;

    /**
     * Retrieves changes in list of cluster nodes.
     *
     * @param oldVersionID  version ID from previous retrieval
     * @return collection changes; never null
     * @throws FindException if there was a problem accessing the requested information
     * @see CollectionUpdate
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CLUSTER_INFO, stereotype=MethodStereotype.FIND_ENTITIES)
    @Administrative(licensed=false, background = true)
    CollectionUpdate<ClusterNodeInfo> getClusterNodesUpdate(int oldVersionID) throws FindException;

    /**
     * Get some usage statistics on a 'per-published-service' basis.
     * @return an array of ServiceUsage (one per published service). never null
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.SERVICE_USAGE, stereotype=MethodStereotype.FIND_ENTITIES)
    @Administrative(background = true)
    ServiceUsage[] getServiceUsage() throws FindException;

    /**
     * Allows the administrator to change the human readable name of a cluster node. The original
     * names are automatically set by the server when they join the cluster.
     *
     * @param nodeid the mac of the node for which we want to change the name
     * @param newName the new name of the node (must not be null)
     * @throws UpdateException if there was a server-side problem updating the requested node
     */
    @Secured(types=EntityType.CLUSTER_INFO, stereotype=MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE)
    void changeNodeName(String nodeid, String newName) throws UpdateException;

    /**
     * Allows the administrator to remove a node that is no longer part of the cluster. When a
     * node is physically removed from the cluster, it will be considered as no longer
     * responding until it is removed from the watch list through this call. After this is called,
     * the getClusterStatus calls will no longer refer to this node. This operation will not be permitted
     * unless this node's status has not been updated for a while. Tables potentially affected by this
     * call are cluster_info, service_usage and ssg_logs
     *
     * @param nodeid the mac of the stale node to remove as defined in the ClusterNodeInfo object
     * @throws DeleteException if there was a server-side problem deleting the requested node
     */
    @Secured(types=EntityType.CLUSTER_INFO, stereotype=MethodStereotype.DELETE_BY_UNIQUE_ATTRIBUTE)
    void removeStaleNode(String nodeid) throws DeleteException;

    /**
     * Get the current system time on the gateway.
     *
     * @return java.util.Date  The current system time
     */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    @Administrative(licensed=false, background = true)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    java.util.Date getCurrentClusterSystemTime();

    /**
     * Get the current system time zone ID on the gateway.
     *
     * @return the current system time zone on the gateway
     */
    @Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
    @Administrative(licensed=false)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    String getCurrentClusterTimeZone();

    /**
     * Get the name of node that handles the admin request.
     *
     * @return String  The node name
     */
    @Administrative(licensed=false)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    String getSelfNodeName();

    /**
     * get cluster wide properties
     * @return a list containing ClusterProperty objects (never null)
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.FIND_ENTITIES)
    @Administrative(licensed=false)
    Collection<ClusterProperty> getAllProperties() throws FindException;

    /**
     * Get a map of all known cluster wide properties.
     *
     * <p>Keys are names, values are an String[2] array with description/default either of which may be null</p>
     *
     * @deprecated Use {@link #getAllPropertyDescriptors()} instead
     * @return a Map of names / descriptions
     */
    @Deprecated
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    Map getKnownProperties();

    /**
     * Get a collection of metadata for all cluster wide properties.
     *
     * @return The collection of property descriptors
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    Collection<ClusterPropertyDescriptor> getAllPropertyDescriptors();

    /**
     * get cluster wide property
     * @return may return null if the property is not set. will return the property value otherwise
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.FIND_ENTITY)
    @Administrative(licensed=false)
    ClusterProperty findPropertyByName(String name) throws FindException;

    /**
     * set new value for the cluster-wide property. value set to null will delete the property from the table
     *
     * @param clusterProperty The property to save
     */
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.SAVE_OR_UPDATE)
    @Administrative(licensed=false)
    Goid saveProperty(ClusterProperty clusterProperty) throws SaveException, UpdateException, DeleteException;

    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.DELETE_ENTITY)
    @Administrative(licensed=false)
    void deleteProperty(ClusterProperty clusterProperty) throws DeleteException;

    /**
     * Get the current CompositeLicense from the LicenseManager, or null if there are no LicenseDocuments in the
     * database.
     *
     * @return the current CompositeLicense
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    CompositeLicense getCompositeLicense();

    /**
     * Get the warning period for license expiry.
     *
     * <p>If the license expires within this period a warning should be shown
     * to allow action to be taken.</p>
     *
     * @return The warning period, in milliseconds
     */
    @Administrative(licensed=false)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    long getLicenseExpiryWarningPeriod();

    /**
     * Generates a FeatureLicense from the contents of the specified LicenseDocument. N.B. This method
     * does *not* save the given LicenseDocument to the database. To to so, invoke
     * {@link #installLicense(FeatureLicense)} with the FeatureLicense returned by this method.
     *
     * @param document LicenseDocument from which to create the FeatureLicense
     * @return the created FeatureLicense
     * @throws InvalidLicenseException
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    public FeatureLicense createLicense(LicenseDocument document) throws InvalidLicenseException;

    /**
     * Check the validity (i.e. applicability to current environment) of the license.
     *
     * @param license the Feature License to validate
     * @throws InvalidLicenseException Specifies that the license is invalid with reason.
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    void validateLicense(FeatureLicense license) throws InvalidLicenseException;

    /**
     * Installs the specified FeatureLicense on the Gateway. The license will be activated immediately, enabling all
     * newly licensed features and propagating automatically to all nodes in the cluster.
     * <p/>
     * The installation will occur immediately but in the background.  This method
     * will return immediately.  To see the result and pick up any exception that may have resulted,
     * poll for the result of the returned JobId.
     *
     * @param license the FeatureLicense to install.
     * @return a JobId that can be redeemed later for true (if we succeeded)
     *          or a LicenseInstallationException if there was a problem installing the license.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
    @Secured(types=EntityType.LICENSE_DOCUMENT, stereotype=MethodStereotype.SAVE)
    @Administrative(licensed=false)
    JobId<Boolean> installLicense(FeatureLicense license);

    /**
     * Uninstalls the specified LicenseDocument. Of any features that are not enabled by other installed licenses, most
     * will automatically become unavailable. Policies containing Assertions that are no longer enabled will fail.
     * Some subsystems will not be disabled until the Gateway is restarted.
     * <p/>
     * The uninstallation will occur immediately but in the background.  This method
     * will return immediately.  To see the result and pick up any exception that may have resulted,
     * poll for the result of the returned JobId.
     *
     * @param document the LicenseDocument to uninstall
     * @return a JobId that can be redeemed later for either true (if we succeeded)
     *          or a LicenseRemovalException if there was a problem uninstalling the license.
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
    @Secured(types=EntityType.LICENSE_DOCUMENT, stereotype=MethodStereotype.DELETE_MULTI)
    @Administrative(licensed=false)
    JobId<Boolean> uninstallLicense(LicenseDocument document);

    /**
     * @return the upgrade map for mapping legacy OIDs to GOIDs.  Never null.  Will be empty if this Gateway was not
     *         upgrade from pre-8.0.
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    Map<String,Long> getGoidUpgradeMap();

    /**
    * @return whether collection of service metrics is currently enabled
    */
    @Transactional(readOnly=true)
    @Administrative(background = true)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    boolean isMetricsEnabled();

    /**
     * Gets the time interval (in milliseconds) of the service metrics fine resolution bin.
     *
     * @return the fine bin interval in milliseconds
     */
    @Transactional(readOnly=true)
    @Administrative
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    int getMetricsFineInterval();

    /**
     * Searches for metrics bins with the given criteria and summarizes by
     * combining bins with the same period start.
     *
     * <em>NOTE:</em> Because summary bins are aggregated from multiple published
     * services, RBAC is enforced inside the implementation instead of using attributes.
     *
     * @param nodeId            cluster node ID; null = all
     * @param serviceGoids      published service GOIDs; null = all services permitted for this user
     * @param resolution        bin resolution
     *                          ({@link com.l7tech.gateway.common.service.MetricsBin#RES_FINE},
     *                          {@link com.l7tech.gateway.common.service.MetricsBin#RES_HOURLY} or
     *                          {@link com.l7tech.gateway.common.service.MetricsBin#RES_DAILY});
     *                          null = all
     * @param minPeriodStart    minimum bin period start time (milliseconds since epoch); null = as far back as available
     * @param maxPeriodStart    maximum bin period statt time (milliseconds since epoch); null = up to the latest available
     * @param includeEmpty      whether to include empty uptime bins
     *
     * @return collection of summary bins; can be empty but never <code>null</code>
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(readOnly=true)
    @Administrative( background = true)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN) // RBAC checking done by impl
    Collection<MetricsSummaryBin> summarizeByPeriod(final String nodeId,
                                                    final Goid[] serviceGoids,
                                                    final Integer resolution,
                                                    final Long minPeriodStart,
                                                    final Long maxPeriodStart,
                                                    final boolean includeEmpty)
            throws FindException;

    /**
     * Searches for metrics bins with the given criteria and summarizes by
     * combining bins with the same period start.
     *
     * <em>NOTE:</em> Because summary bins are aggregated from multiple published
     * services, RBAC is enforced inside the implementation instead of using attributes.
     *
     * @param nodeId            cluster node ID; null = all
     * @param serviceGoids      published service GOIDs; null = all services permitted for this user
     * @param resolution        bin resolution
     *                          ({@link com.l7tech.gateway.common.service.MetricsBin#RES_FINE},
     *                          {@link com.l7tech.gateway.common.service.MetricsBin#RES_HOURLY} or
     *                          {@link com.l7tech.gateway.common.service.MetricsBin#RES_DAILY});
     *                          null = all
     * @param duration          time duration (milliseconds from current clock time on
     *                          gateway) to search backward for bins whose
     *                          nominal periods fall within
     * @param includeEmpty      whether to include empty uptime bins (same as include service OID -1)
     *
     * @return collection of summary bins; can be empty but never <code>null</code>
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(readOnly=true)
    @Administrative(background = true)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN) // RBAC checking done by impl
    Collection<MetricsSummaryBin> summarizeLatestByPeriod(final String nodeId,
                                                          final Goid[] serviceGoids,
                                                          final Integer resolution,
                                                          final long duration,
                                                          final boolean includeEmpty)
            throws FindException;

    /**
     * Searches for the latest metrics bins for the given criteria and
     * summarizes by combining them into one summary bin.
     *
     * <em>NOTE:</em> Because summary bins are aggregated from multiple published
     * services, RBAC is enforced inside the implementation instead of using attributes.
     *
     * @param clusterNodeId cluster node ID; null = all
     * @param serviceGoids  published service GOIDs; null = all services permitted for this user
     * @param resolution    bin resolution
     *                      ({@link com.l7tech.gateway.common.service.MetricsBin#RES_FINE},
     *                      {@link com.l7tech.gateway.common.service.MetricsBin#RES_HOURLY} or
     *                      {@link com.l7tech.gateway.common.service.MetricsBin#RES_DAILY})
     * @param duration      time duration (milliseconds from latest nominal period boundary
     *                      time on gateway) to search backward for bins whose
     *                      nominal periods fall within
     * @param includeEmpty  whether to include empty uptime bins (same as include service OID -1)
     *
     * @return a summary bin; <code>null</code> if no metrics bins are found
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(readOnly=true)
    @Administrative(background = true)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN) // RBAC checking done by impl
    MetricsSummaryBin summarizeLatest(final String clusterNodeId,
                                      final Goid[] serviceGoids,
                                      final int resolution,
                                      final int duration,
                                      final boolean includeEmpty)
            throws FindException;

    /**
     * Get all configured date formats configured on the gateway.
     *
     * @return List of configured date formats. Never null. May be empty if none are configured.
     */
    @Transactional(readOnly = true)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    @NotNull List<String> getConfiguredDateFormats();

    /**
     * Get all date formats the gateway can automatically parse based on configuration.
     *
     * @return List of configured date formats. Never null. May be empty if none are configured. Each pair's left value
     * is the Simple Date Format and the right is the Regex pattern which strictly matches the format.
     */
    @Transactional(readOnly = true)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    @NotNull List<Pair<String, Pattern>> getAutoDateFormats();

    /**
     * Describes a loaded module.
     */
    public static final class ModuleInfo implements Serializable {
        private static final long serialVersionUID = 1937230947284240743L;

        /** The filename of the module, ie "RateLimitAssertion-3.7.0.jar". */
        public final String moduleFilename;

        /** The hex encoded SHA-1 hash of the module file, ie "75f53368f8ef850bfb89ba2adcb4eacd0534b173". */
        public final String moduleSha1;

        /** The assertion classnames provided by this module, ie { "com.yoyodyne.integration.layer7.SqlSelectAssertion" }. */
        public final Collection<String> assertionClasses;

        public ModuleInfo(String moduleFilename, String moduleSha1, Collection<String> assertionClasses) {
            this.moduleFilename = moduleFilename;
            this.moduleSha1 = moduleSha1;
            this.assertionClasses = assertionClasses;
        }
    }

    /**
     * Get information about the modular assertions currently loaded on this cluster node.
     *
     * @return A Collection of ModuleInfo, one for each loaded module.  May be empty but never null.
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    Collection<ModuleInfo> getAssertionModuleInfo();

    /** Exception thrown when a named assertion module is not currently loaded. */
    public static final class ModuleNotFoundException extends Exception {
        private static final long serialVersionUID = 982374277812732928L;
    }

    public static final class NoSuchCapabilityException extends Exception {
        private static final long serialVersionUID = 682345724356023459L;
    }

    public static final class NoSuchPropertyException extends Exception {
        private static final long serialVersionUID = 356702534346778195L;
    }

    /**
     * Get information about any currently-registered admin extension interfaces.
     * @return a collection of pairs of (interface classname, instance identifier) for any registered extension interface implementations.  The instance identifieres may be null.
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    Collection<Pair<String, String>> getExtensionInterfaceInstances();

    /**
     * Check whether a particular admin extension interface has an implementation registered on the current Gateway.
     *
     * @param interfaceClassname the interface to query.  Required.
     * @param instanceIdentifier an instance identifier to name a particular implementation, or null.
     * @return true if {@link #invokeExtensionMethod} might currently succeed against this interface.  False if it would not currently succeed currently regardless of arguments.
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    boolean isExtensionInterfaceAvailable(String interfaceClassname, String instanceIdentifier);

    /**
     * Invoke a method of an extension interface registered by a module.
     * <p/>
     * The extension interface may use @Transactional and @Secured annotations; any present
     * on the interface or implementation will be honored when the invocation is processed by the Gateway.
     *
     * @param interfaceClassname full classname of the extension interface to use.  Required.
     * @param targetObjectId opaque identifier naming target object.  Required if more than one implementation of the specified extension interface is registered.
     * @param methodName name of method to invoke on interface.  Required.
     * @param parameterTypes  parameter types.  Required, but may be zero-length for a nullary method.  Must be same length as arguments.
     * @param arguments method arguments.  Required, but may be zero-length for a nullary method.  Must be same length as parameterTypes.
     * @return the result of invoking the specified method on the specified extension interface.  May contain an optional return value or thrown exception.  Never null.
     * @throws ClassNotFoundException if no provider of the specified extension interface is registered.
     * @throws NoSuchMethodException if the invocation refers to a method that does not exist on the server's version of the specified extension interface.
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN) // checked by impl, before invocation is dispatched to extension method, if extension method is annotated
    Either<Throwable,Option<Object>> invokeExtensionMethod(String interfaceClassname, String targetObjectId, String methodName, Class[] parameterTypes, Object[] arguments)
            throws ClassNotFoundException, NoSuchMethodException;

    /**
     * Check hardware capabilities of the node that receives this admin request.
     * TODO this information should be moved to ClusterNodeInfo instead, since it is really per-node.
     *
     * @param capability the capability to interrogate.  Must not be null.
     * @return a string describing the support for the requested capability, or null if the capability is not supported.
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Administrative(licensed=false)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    String getHardwareCapability(String capability);

    /**
     * Read a property for a hardware capability of the node that receives this admin request.
     *
     * @param capability the capability to interrogate.  Must not be null.
     * @param property the capability-specific property to read.  Required.
     * @return the value of this property, which may be of some capability- and property-specific type, and which may be null.
     * @throws NoSuchCapabilityException if the specified hardware capability is not available.
     * @throws NoSuchPropertyException if the specified hardware capability does not have or does not allow read access to the specified property.
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    Serializable getHardwareCapabilityProperty(String capability, String property) throws NoSuchCapabilityException, NoSuchPropertyException;

    /**
     * Configure a property for a hardware capability of the node that receives this admin request.
     *
     * @param capability the capability to interrogate.  Must not be null.
     * @param property the capability-specific property to set.  Required.
     * @param value the value this property is to be set to.  May be null.
     * @throws NoSuchCapabilityException if the specified hardware capability is not available.
     * @throws NoSuchPropertyException if the specified hardware capability does not have or does not allow write access to the specified property.
     * @throws ClassCastException  if the specified property value is not the correct type for the specified property.
     * @throws IllegalArgumentException  if the specified property value is not a valid setting for the specified property.
     */
    @Secured(stereotype=DELETE_MULTI, types=SSG_KEY_ENTRY)
    void putHardwareCapabilityProperty(String capability, String property, Serializable value) throws NoSuchCapabilityException, NoSuchPropertyException, ClassCastException, IllegalArgumentException;

    /**
     * Check if this Gateway node is capable of connecting to the specific hardware token type (identified
     * by a hardware capability name) using the specified slot number (or -1 to use the default), using
     * the specified token pin.
     * <p/>
     * If this method returns without throwing, this Gateway node is confirmed as able to make use of the specified
     * token using the specified settings.
     *
     * @param capability the hardware capability that provides the hardware token.  Required.
     * @param slotNum the slot number, or -1 to use the default.
     * @param tokenPin the token pin to use when connecting, or null if not relevant.
     * @throws NoSuchCapabilityException if the specified hardware capability is not available.
     * @throws KeyStoreException if the specified token cannot be accessed using the specified settings.
     */
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    void testHardwareTokenAvailability(String capability, int slotNum, char[] tokenPin) throws NoSuchCapabilityException, KeyStoreException;

    /**
     * Get all TrustedEsm instances currently registered to administer this Gateway cluster.
     *
     * @return a List of TrustedEsm descriptors.  May be empty but never null.
     * @throws FindException if there is a problem finding the requested information.
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(types=EntityType.TRUSTED_ESM, stereotype=MethodStereotype.FIND_ENTITIES)
    Collection<TrustedEsm> getTrustedEsmInstances() throws FindException;

    /**
     * Find a TrustedEsm by its goid.
     *
     * @param trustedEsmGoid the goid of the TrustedEsm.
     * @return the TrustedEsm or null if none was found.
     * @throws FindException if an error occurs when trying to retrieve the TrustedEsm.
     */
    @Transactional(readOnly = true)
    @Secured(types = TRUSTED_ESM, stereotype = MethodStereotype.FIND_ENTITY)
    TrustedEsm findTrustedEsm(@NotNull final Goid trustedEsmGoid) throws FindException;

    /**
     * Delete an ESM registration and all its user mappings.
     *
     * @param trustedEsmGoid the object ID of the TrustedEsm instance to delete.
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(types=EntityType.TRUSTED_ESM, stereotype=MethodStereotype.DELETE_BY_ID, relevantArg=0)
    void deleteTrustedEsmInstance(Goid trustedEsmGoid) throws DeleteException, FindException;

    /**
     * Delete an ESM user mapping.
     *
     * @param trustedEsmUserGoid the object ID of the TrustedEsmUser instance to delete.
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(types=EntityType.TRUSTED_ESM_USER, stereotype=MethodStereotype.DELETE_BY_ID, relevantArg=0)
    void deleteTrustedEsmUserMapping(Goid trustedEsmUserGoid) throws DeleteException, FindException;

    /**
     * Get all TrustedEsmUser instances belonging to the registered TrustedEsm instance with the specified
     * OID on this Gateway.
     *
     * @param trustedEsmGoid object ID of a TrustedEsm instance.
     * @return a List of the user mappings for this TrustedEsm.  May be empty but never null.
     * @throws FindException if there is a problem finding the requested information.
     */
    @Secured(types=EntityType.TRUSTED_ESM_USER, stereotype=MethodStereotype.FIND_ENTITIES)
    Collection<TrustedEsmUser> getTrustedEsmUserMappings(Goid trustedEsmGoid) throws FindException;

    /**
     * Find a TrustedEsmUser by its goid.
     *
     * @param trustedEsmUserGoid the goid of the TrustedEsmUser.
     * @return the TrustedEsmUser or null if none found.
     * @throws FindException if an error occurs while trying to retrieve the TrustedEsmUser.
     */
    @Transactional(readOnly = true)
    @Secured(types = EntityType.TRUSTED_ESM_USER, stereotype = FIND_ENTITY)
    TrustedEsmUser findTrustedEsmUser(@NotNull final Goid trustedEsmUserGoid) throws FindException;

    /**
     * Retrieve all the failover strategies
     * @return an array of Failover strategy, should not be empty.
     */
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    FailoverStrategy[] getAllFailoverStrategies();

    /**
     * Retrieve all the server module files, with databytes omitted.
     * <p/>
     * To download the databytes for a module, use {@link #findServerModuleFileById} passing true for
     * the second parameter.
     *
     * @return A collection of all ServerModuleFile entities visible to the current admin user.
     *         Each returned {@link ServerModuleFile} instance will have a {@code null} data field.
     * @throws FindException if there is a problem reading the database
     */
    @NotNull
    @Transactional(readOnly = true)
    @Secured( types = EntityType.SERVER_MODULE_FILE, stereotype = FIND_ENTITIES )
    List<ServerModuleFile> findAllServerModuleFiles() throws FindException;

    /**
     * Find a server module file, optionally downloading its data bytes.
     *
     * @param moduleGoid ID of ServerModuleFile to read.  Required.
     * @param includeDataBytes true to include databytes.  false to return a ServerModuleFile databytes field nulled out
     * @return the requested ServerModuleFile, or null if it is not found.
     * @throws FindException if there is a problem reading the database
     */
    @Nullable
    @Transactional(readOnly = true)
    @Secured( types = EntityType.SERVER_MODULE_FILE, stereotype = FIND_ENTITY )
    ServerModuleFile findServerModuleFileById(@NotNull Goid moduleGoid, boolean includeDataBytes) throws FindException;

    /**
     * Save a new or updated ServerModuleFile, uploading new databytes if the databytes field is non-null.
     * <p/>
     * If this method succeeds, the new or updated ServerModuleFile has been written to the database.
     * The module will soon be installed and activated on cluster nodes that have module installation enabled,
     * provided the module file is valid, compatible with the Gateway version, enabled by the Gateway license,
     * and (for an update to an existing CUSTOM_ASSERTION module) the node has dynamic updates of custom assertions enabled
     * and the custom assertion enables this capability.
     * <p/>
     * If this is a new ServerModuleFile, a non-null databytes field must be included.
     * For an update to an existing ServerModuleFile, the databytes field may be omitted, in which case
     * the existing databytes value will be kept.
     *
     * @param moduleFile module file, optionally including its databytes.  Required.
     * @return the ID of the saved or updated module.  Never null.
     * @throws FindException if there is a problem reading from the database
     * @throws SaveException if a new entity cannot be saved
     * @throws UpdateException if an existing entity cannot be updated
     */
    @NotNull
    @Secured( types = EntityType.SERVER_MODULE_FILE, stereotype = SAVE_OR_UPDATE )
    Goid saveServerModuleFile( @NotNull ServerModuleFile moduleFile ) throws FindException, SaveException, UpdateException;

    /**
     * Delete a ServerModuleFile.
     * <p/>
     * If this method succeeds, the module file is removed from the database.
     * The module will soon be uninstalled from cluster nodes that have module installation enabled.
     * The uninstallation may not fully take effect until nodes are restarted (this depends on how the module code is
     * written and, for modules of type CUSTOM_ASSERTION, whether the node has dynamic updates of custom
     * assertions enabled and whether the custom assertion enables this property).
     *
     * @param id ID of module to delete.  Required.
     * @throws DeleteException if module can't be deleted, or DB error updating.
     */
    @Secured( types = EntityType.SERVER_MODULE_FILE, stereotype = DELETE_BY_ID )
    void deleteServerModuleFile( @NotNull Goid id ) throws DeleteException;

    /**
     * Get the {@link ServerModuleFileState state} of the current node.
     * <p/>
     * {@link ServerModuleFile} contains a list os states from all nodes in the cluster.
     * This method is a convenience method for getting the module state, if any, for the current cluster node.
     * This method will loop through the {@link ServerModuleFile#states states} list finding the state belonging to this node,
     * this means that there are not going to be calls made into the DB.
     *
     * @param moduleFile    the module file, holding the states.  Required.
     * @return the {@link ServerModuleFileState state} for the current cluster node, or {@code null} is there is no state for this node.
     */
    @Nullable
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Secured(types = EntityType.SERVER_MODULE_FILE, stereotype = GET_PROPERTY_OF_ENTITY)
    ServerModuleFileState findServerModuleFileStateForCurrentNode(@NotNull final ServerModuleFile moduleFile);

    /**
     * Get server module configurations.
     * @return Server configuration properties about custom and modular assertion setup.
     */
    @NotNull
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Secured(stereotype = UNCHECKED_WIDE_OPEN)
    public ServerModuleConfig getServerModuleConfig();

    /**
     * Query for this capability to see if hardware XPath acceleration is available.
     * The return value will be either null, or the type of Xpath acceleration, ie {@link #CAPABILITY_VALUE_HWXPATH_TARARI}.
     */
    public static final String CAPABILITY_HWXPATH = "hardwareXpath";

    /** Value returned from a query for {@link #CAPABILITY_HWXPATH} if Tarari XPath acceleration is available. */
    public static final String CAPABILITY_VALUE_HWXPATH_TARARI = "tarari";

    /**
     * Query for this capability to see if the SafeNet HSM (formerly Luna) client libraries are installed and available.
     * The return value will be either null, or "true" if Luna client libraries are detected.
     */
    public static final String CAPABILITY_LUNACLIENT = "keystore.luna";

    /**
     * Write-only property.  Set this capability property to a String set the Luna client PIN.  This will end up setting a cluster property
     * to the client PIN, encrypted in some way that the GatewayLunaPinFinder will be able to decrypt.
     */
    public static final String CAPABILITY_PROPERTY_LUNAPIN = "keystore.luna.clientPin";

    /**
     * A read-only property to indicate the ability to configure firewall rules.
     */
    public static final String CAPABILITY_FIREWALL = "appliance.firewall";

    public static final String CAPABILITY_SITEMINDER = "siteminder";
}
