package com.l7tech.gateway.common.cluster;

import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.License;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.emstrust.TrustedEms;
import com.l7tech.gateway.common.emstrust.TrustedEmsUser;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.objectmodel.*;
import com.l7tech.util.CollectionUpdate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Remote interface for getting the status of nodes in a gateway cluster.
 */
@Secured
@Administrative
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public interface ClusterStatusAdmin {
    /**
     * Returns true if this node is part of a cluster.
     * @return true if this node is part of a cluster
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.SSG_CONNECTOR, stereotype=MethodStereotype.FIND_ENTITIES)
    @Administrative(licensed=false)
    boolean isCluster();

    /**
     * Get status for all nodes part of the cluster.
     * @return An array of ClusterNodeInfo (one for each node)
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CLUSTER_INFO, stereotype=MethodStereotype.FIND_ENTITIES)
    @Administrative(licensed=false)
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
    @Administrative(licensed=false)
    CollectionUpdate<ClusterNodeInfo> getClusterNodesUpdate(int oldVersionID) throws FindException;

    /**
     * Get some usage statistics on a 'per-published-service' basis.
     * @return an array of ServiceUsage (one per published service). never null
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.SERVICE_USAGE, stereotype=MethodStereotype.FIND_ENTITIES)
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
    @Transactional(propagation=Propagation.SUPPORTS)
    @Administrative(licensed=false)
    java.util.Date getCurrentClusterSystemTime();

    /**
     * Get the current system time zone ID on the gateway.
     *
     * @return the current system time zone on the gateway
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Administrative(licensed=false)
    String getCurrentClusterTimeZone();

    /**
     * Get the name of node that handles the admin request.
     *
     * @return String  The node name
     */
    @Administrative(licensed=false)
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
    Map getKnownProperties();

    /**
     * Get a collection of metadata for all cluster wide properties.
     *
     * @return The collection of property descriptors
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    Collection<ClusterPropertyDescriptor> getAllPropertyDescriptors();

    /**
     * get cluster wide property
     * @return may return null if the property is not set. will return the property value otherwise
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.FIND_ENTITY_BY_ATTRIBUTE)
    @Administrative(licensed=false)
    ClusterProperty findPropertyByName(String name) throws FindException;

    /**
     * set new value for the cluster-wide property. value set to null will delete the property from the table
     *
     * @param clusterProperty The property to save
     */
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.SAVE_OR_UPDATE)
    @Administrative(licensed=false)            
    long saveProperty(ClusterProperty clusterProperty) throws SaveException, UpdateException, DeleteException;

    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.DELETE_ENTITY)
    @Administrative(licensed=false)
    void deleteProperty(ClusterProperty clusterProperty) throws DeleteException;

    /**
     * Get the currently-installed License from the LicenseManager, if it possesses a valid license.
     * If there is license XML in the database but not live (perhaps because it is invalid)
     * this method will throw a LicenseException explaining what the problem was with that license XML.
     * <p>
     * Summary:
     * <pre>
     *     If:                                then this method will:
     *     ================================   =======================
     *     No license is installed or in DB   return null
     *     A valid license is installed       return the license
     *     Invalid license is in the DB       throws LicenseException
     * </pre>
     *
     * @return the License currently live inside the LicenseManager, if a valid signed license is currently
     *         live, or null if no license at all is live and no license XML is present in the cluster property
     *         table.
     * @throws InvalidLicenseException a license is in the database but was not installed because it was invalid
     */
    @Administrative(licensed=false)
    License getCurrentLicense() throws InvalidLicenseException;

    /**
     * Get the warning period for license expiry.
     *
     * <p>If the license expires within this period a warning should be shown
     * to allow action to be taken.</p>
     *
     * @return The warning period
     */
    @Administrative(licensed=false)
    long getLicenseExpiryWarningPeriod();

    /**
     * Check the specified license for validity with this product and, if it is valid, install it to the cluster
     * property table and also immediately activate it.
     * <p>
     * If this method returns normally, the new license was installed successfully.
     *
     * @param newLicenseXml   the license XML to install.
     * @throws InvalidLicenseException if the specified license XML was not valid.  The exception message explains the problem.
     * @throws UpdateException if the license was valid, but was not installed because of a database problem.
     */
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.SAVE_OR_UPDATE)
    @Administrative(licensed=false)
    void installNewLicense(String newLicenseXml) throws UpdateException, InvalidLicenseException;

    /**
     * @return whether collection of service metrics is currently enabled
     */
    boolean isMetricsEnabled();

    /**
     * Gets the time interval (in milliseconds) of the service metrics fine resolution bin.
     *
     * @return the fine bin interval in milliseconds
     */
    int getMetricsFineInterval();

    /**
     * Searches for metrics bins with the given criteria and summarizes by
     * combining bins with the same period start.
     *
     * <em>NOTE:</em> Because summary bins are aggregated from multiple published
     * services, RBAC is enforced inside the implementation instead of using attributes.
     *
     * @param nodeId            cluster node ID; null = all
     * @param serviceOids       published service OIDs; null = all services permitted for this user
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
    Collection<MetricsSummaryBin> summarizeByPeriod(final String nodeId,
                                                    final long[] serviceOids,
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
     * @param serviceOids       published service OIDs; null = all services permitted for this user
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
    Collection<MetricsSummaryBin> summarizeLatestByPeriod(final String nodeId,
                                                          final long[] serviceOids,
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
     * @param serviceOids   published service OIDs; null = all services permitted for this user
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
    MetricsSummaryBin summarizeLatest(final String clusterNodeId,
                                      final long[] serviceOids,
                                      final int resolution,
                                      final int duration,
                                      final boolean includeEmpty)
            throws FindException;

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
    Collection<ModuleInfo> getAssertionModuleInfo();

    /** Exception thrown when a named assertion module is not currently loaded. */
    public static final class ModuleNotFoundException extends Exception {
        private static final long serialVersionUID = 982374277812732928L;
    }

    /**
     * Check hardware capabilities of the node that receives this admin request.
     * TODO this information should be moved to ClusterNodeInfo instead, since it is really per-node.
     *
     * @param capability the capability to interrogate.  Must not be null.
     * @return a string describing the support for the requested capability, or null if the capability is not supported.
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Administrative(licensed=false)
    String getHardwareCapability(String capability);

    /**
     * Get all TrustedEms instances currently registered to administer this Gateway cluster.
     *
     * @return a List of TrustedEms descriptors.  May be empty but never null.
     * @throws FindException if there is a problem finding the requested information.
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    @Secured(types=EntityType.TRUSTED_EMS, stereotype=MethodStereotype.FIND_ENTITIES)
    Collection<TrustedEms> getTrustedEmsInstances() throws FindException;

    /**
     * Get all TrustedEmsUser instances belonging to the registered TrustedEms instance with the specified
     * OID on this Gateway.
     *
     * @param trustedEmsId object ID of a TrustedEms instance.
     * @return a List of the user mappings for this TrustedEms.  May be empty but never null.
     * @throws FindException if there is a problem finding the requested information.
     */
    @Secured(types=EntityType.TRUSTED_EMS_USER, stereotype=MethodStereotype.FIND_ENTITIES)
    Collection<TrustedEmsUser> getTrustedEmsUserMappings(long trustedEmsId) throws FindException;

    public static final String CAPABILITY_HWXPATH = "hardwareXpath";
    public static final String CAPABILITY_HWXPATH_TARARI = "tarari";
}
