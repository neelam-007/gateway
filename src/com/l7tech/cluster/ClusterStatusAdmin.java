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
import com.l7tech.service.MetricsBin;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
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
     * Gets the time interval (in milliseconds) of the service metrics fine resolution bin.
     *
     * @return the fine bin interval in milliseconds
     * @throws RemoteException on remote communication error
     */
    int getMetricsFineInterval() throws RemoteException;

    /**
     * Finds {@link com.l7tech.service.MetricsBin} instances from the database using the specified
     * query parameters.  All parameters are optional--pass null in all parameters to find all
     * MetricsBin instances in the database.
     *
     * @param nodeId the MAC address of the cluster node to query for; null = any
     * @param minPeriodStart the minimum {@link com.l7tech.service.MetricsBin#getPeriodStart} value, inclusive; null = no minimum
     * @param maxPeriodStart the maximum {@link com.l7tech.service.MetricsBin#getPeriodStart} value, inclusive; null = no maximum
     * @param resolution the {@link com.l7tech.service.MetricsBin#getResolution()} value; null = any. Must be one of {@link com.l7tech.service.MetricsBin#RES_FINE}, {@link com.l7tech.service.MetricsBin#RES_HOURLY} or {@link com.l7tech.service.MetricsBin#RES_DAILY}
     * @param serviceOid the OID of the {@link com.l7tech.service.PublishedService} to find metrics for; null = any.
     * @return a List of {@link com.l7tech.service.MetricsBin} instances.
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.METRICS_BIN, stereotype=MethodStereotype.FIND_ENTITIES)
    List<MetricsBin> findMetricsBins(String nodeId, Long minPeriodStart, Long maxPeriodStart,
                         Integer resolution, Long serviceOid) throws RemoteException, FindException;

    /**
     * Finds the latest metrics bins in the database using the given criteria.
     *
     * @param nodeId        the MAC address of the cluster node to query for; <code>null</code> means all
     * @param duration      time duration (in milliseconds) into the past; based on gateway clock; <code>null</code> means all
     * @param resolution    the metric bin resolution ({@link com.l7tech.service.MetricsBin#RES_FINE},
     *                      {@link com.l7tech.service.MetricsBin#RES_HOURLY} or
     *                      {@link com.l7tech.service.MetricsBin#RES_DAILY}) value;
     *                      <code>null</code> means all
     * @param serviceOid    the OID of the {@link com.l7tech.service.PublishedService};
     *                      <code>null</code> means all
     * @return a {@link List} of {@link com.l7tech.service.MetricsBin} found
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.METRICS_BIN, stereotype=MethodStereotype.FIND_ENTITIES)
    List<MetricsBin> findLatestMetricsBins(String nodeId, Long duration, Integer resolution, Long serviceOid)
            throws RemoteException, FindException;

    /**
     * Summarizes the latest metrics bins in the database for the given criteria.
     * <em>NOTE:</em> This is tagged {@link MethodStereotype#FIND_ENTITIES} so that the interceptor will require READ
     * against all ServiceMetrics records.
     * @param clusterNodeId the MAC address of the cluster node to search for
     * @param serviceOid    the OID of the {@link com.l7tech.service.PublishedService}
     *                      to search for
     * @param resolution    the bin resolution to search for
     * @param duration      time duration (from latest nominal period boundary
     *                      time on gateway) to search for bins whose nominal
     *                      periods fall within
     * @return a {@link MetricsBin} summarizing the bins that fit the given
     *         criteria
     */
    @Transactional(readOnly=true)
    @Secured(types=EntityType.METRICS_BIN, stereotype=MethodStereotype.FIND_ENTITIES)
    MetricsBin getLastestMetricsSummary(final String clusterNodeId,
                                        final Long serviceOid,
                                        final int resolution,
                                        final int duration)
            throws RemoteException;

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
