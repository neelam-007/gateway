package com.l7tech.cluster;

import com.l7tech.common.InvalidLicenseException;
import com.l7tech.common.License;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.MethodStereotype;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.service.MetricsSummaryBin;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;

/**
 * Remote interface for getting the status of nodes in a gateway cluster.
 */
@Secured
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public interface ClusterStatusAdmin {
    /**
     * Get status for all nodes part of the cluster.
     * @return An array of ClusterNodeInfo (one for each node)
     * @throws FindException if there was a server-side problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CLUSTER_INFO, stereotype=MethodStereotype.FIND_ENTITIES)
    ClusterNodeInfo[] getClusterStatus() throws RemoteException, FindException;

    /**
     * Get some usage statistics on a 'per-published-service' basis.
     * @return an array of ServiceUsage (one per published service). never null
     * @throws RemoteException on remote communication error
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.SERVICE_USAGE, stereotype=MethodStereotype.FIND_ENTITIES)
    ServiceUsage[] getServiceUsage() throws RemoteException, FindException;

    /**
     * Allows the administrator to change the human readable name of a cluster node. The original
     * names are automatically set by the server when they join the cluster.
     *
     * @param nodeid the mac of the node for which we want to change the name
     * @param newName the new name of the node (must not be null)
     * @throws RemoteException on remote communication error
     * @throws UpdateException if there was a server-side problem updating the requested node
     */
    @Secured(types=EntityType.CLUSTER_INFO, stereotype=MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE)
    void changeNodeName(String nodeid, String newName) throws RemoteException, UpdateException;

    /**
     * Allows the administrator to remove a node that is no longer part of the cluster. When a
     * node is physically removed from the cluster, it will be considered as no longer
     * responding until it is removed from the watch list through this call. After this is called,
     * the getClusterStatus calls will no longer refer to this node. This operation will not be permitted
     * unless this node's status has not been updated for a while. Tables potentially affected by this
     * call are cluster_info, service_usage and ssg_logs
     *
     * @param nodeid the mac of the stale node to remove as defined in the ClusterNodeInfo object
     * @throws RemoteException on remote communication error
     * @throws DeleteException if there was a server-side problem deleting the requested node
     */
    @Secured(types=EntityType.CLUSTER_INFO, stereotype=MethodStereotype.DELETE_BY_UNIQUE_ATTRIBUTE)
    void removeStaleNode(String nodeid) throws RemoteException, DeleteException;

    /**
     * Get the current system time on the gateway.
     *
     * @return java.util.Date  The current system time
     * @throws RemoteException on remote communication error
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    java.util.Date getCurrentClusterSystemTime() throws RemoteException;

    /**
     * Get the current system time zone ID on the gateway.
     *
     * @return the current system time zone on the gateway
     * @throws RemoteException on remote communication error
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    String getCurrentClusterTimeZone() throws RemoteException;

    /**
     * Get the name of node that handles the admin request.
     *
     * @return String  The node name
     * @throws RemoteException on remote communication error
     */
     String getSelfNodeName() throws RemoteException;

    /**
     * get cluster wide properties
     * @return a list containing ClusterProperty objects (never null)
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.FIND_ENTITIES)
    Collection<ClusterProperty> getAllProperties() throws RemoteException, FindException;

    /**
     * Get a map of all known cluster wide properties.
     *
     * <p>Keys are names, values are an String[2] array with description/default either of which may be null</p>
     *
     * @return a Map of names / descriptions
     */
    @Transactional(readOnly=true)
    Map getKnownProperties() throws RemoteException;

    /**
     * get cluster wide property
     * @return may return null if the property is not set. will return the property value otherwise
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.FIND_ENTITY_BY_ATTRIBUTE)
    ClusterProperty findPropertyByName(String name) throws RemoteException, FindException;

    /**
     * set new value for the cluster-wide property. value set to null will delete the property from the table
     * @param clusterProperty
     */
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.SAVE_OR_UPDATE)
    long saveProperty(ClusterProperty clusterProperty) throws RemoteException, SaveException, UpdateException, DeleteException;

    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.DELETE_ENTITY)
    void deleteProperty(ClusterProperty clusterProperty) throws DeleteException, RemoteException;

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
     * @throws RemoteException on remote communication error
     * @throws InvalidLicenseException a license is in the database but was not installed because it was invalid
     */
    License getCurrentLicense() throws RemoteException, InvalidLicenseException;

    /**
     * Check the specified license for validity with this product and, if it is valid, install it to the cluster
     * property table and also immediately activate it.
     * <p>
     * If this method returns normally, the new license was installed successfully.
     *
     * @param newLicenseXml   the license XML to install.
     * @throws RemoteException on remote communication error
     * @throws InvalidLicenseException if the specified license XML was not valid.  The exception message explains the problem.
     * @throws UpdateException if the license was valid, but was not installed because of a database problem.
     */
    @Secured(types=EntityType.CLUSTER_PROPERTY, stereotype=MethodStereotype.SAVE_OR_UPDATE)
    void installNewLicense(String newLicenseXml) throws RemoteException, UpdateException, InvalidLicenseException;

    /**
     * @return whether collection of service metrics is currently enabled
     * @throws RemoteException on remote communication error
     */
    boolean isMetricsEnabled() throws RemoteException;

    /**
     * Gets the time interval (in milliseconds) of the service metrics fine resolution bin.
     *
     * @return the fine bin interval in milliseconds
     * @throws RemoteException on remote communication error
     */
    int getMetricsFineInterval() throws RemoteException;

    /**
     * Searches for metrics bins with the given criteria and summarizes by
     * combining bins with the same period start.
     *
     * <em>NOTE:</em> Because summary bins are aggregated from multiple published
     * services, RBAC is enforced inside the implementation instead of using attributes.
     *
     * @param nodeId            cluster node ID; null = all
     * @param serviceOids       published service OIDs; null = all services permitted for this user
     * @param resolution        bin resolution; null = all
     * @param minPeriodStart    minimum bin period start time; null = as far back as available
     * @param maxPeriodStart    maximum bin period statt time; null = up to the latest available
     * @param includeEmpty      whether to include empty uptime bins
     *
     * @return collection of summary bins; can be empty but never <code>null</code>
     * @throws FindException if there was a server-side problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    @Transactional(readOnly=true)
    Collection<MetricsSummaryBin> summarizeByPeriod(final String nodeId,
                                                    final long[] serviceOids,
                                                    final Integer resolution,
                                                    final Long minPeriodStart,
                                                    final Long maxPeriodStart,
                                                    final boolean includeEmpty)
            throws RemoteException, FindException;

    /**
     * Searches for metrics bins with the given criteria and summarizes by
     * combining bins with the same period start.
     *
     * <em>NOTE:</em> Because summary bins are aggregated from multiple published
     * services, RBAC is enforced inside the implementation instead of using attributes.
     *
     * @param nodeId            cluster node ID; null = all
     * @param serviceOids       published service OIDs; null = all services permitted for this user
     * @param resolution        bin resolution; null = all
     * @param duration          time duration (from current clock time on
     *                          gateway) to search backward for bins whose
     *                          nominal periods fall within
     * @param includeEmpty      whether to include empty uptime bins (same as include service OID -1)
     *
     * @return collection of summary bins; can be empty but never <code>null</code>
     * @throws FindException if there was a server-side problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    @Transactional(readOnly=true)
    Collection<MetricsSummaryBin> summarizeLatestByPeriod(final String nodeId,
                                                          final long[] serviceOids,
                                                          final Integer resolution,
                                                          final long duration,
                                                          final boolean includeEmpty)
            throws RemoteException, FindException;

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
     * @param duration      time duration (from latest nominal period boundary
     *                      time on gateway) to search backward for bins whose
     *                      nominal periods fall within
     * @param includeEmpty  whether to include empty uptime bins (same as include service OID -1)
     *
     * @return a summary bin; <code>null</code> if no metrics bins are found
     * @throws FindException if there was a server-side problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    @Transactional(readOnly=true)
    MetricsSummaryBin summarizeLatest(final String clusterNodeId,
                                      final long[] serviceOids,
                                      final int resolution,
                                      final int duration,
                                      final boolean includeEmpty)
            throws RemoteException, FindException;

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
     * @throws RemoteException on remote communication error
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    Collection<ModuleInfo> getAssertionModuleInfo() throws RemoteException;

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
    String getHardwareCapability(String capability);

    public static final String CAPABILITY_HWXPATH = "hardwareXpath";
    public static final String CAPABILITY_HWXPATH_TARARI = "tarari";
}
