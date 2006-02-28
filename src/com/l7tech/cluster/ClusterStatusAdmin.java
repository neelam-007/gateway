package com.l7tech.cluster;

import com.l7tech.common.InvalidLicenseException;
import com.l7tech.common.License;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

/**
 * Remote interface for getting the status of nodes in a gateway cluster.
 */
public interface ClusterStatusAdmin {
    /**
     * Get status for all nodes part of the cluster.
     * @return An array of ClusterNodeInfo (one for each node)
     * @throws FindException if there was a server-side problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    ClusterNodeInfo[] getClusterStatus() throws RemoteException, FindException;

    /**
     * Get some usage statistics on a 'per-published-service' basis.
     * @return an array of ServiceUsage (one per published service). never null
     * @throws RemoteException on remote communication error
     * @throws FindException if there was a server-side problem accessing the requested information
     */
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
    void removeStaleNode(String nodeid) throws RemoteException, DeleteException;

    /**
     * Get the current system time on the gateway.
     *
     * @return java.util.Date  The current system time
     * @throws RemoteException on remote communication error
     */
    java.util.Date getCurrentClusterSystemTime() throws RemoteException;

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
    Collection getAllProperties() throws RemoteException, FindException;

    /**
     * get cluster wide property
     * @return may return null if the property is not set. will return the property value otherwise
     */
    String getProperty(String key) throws RemoteException, FindException;

    /**
     * set new value for the cluster-wide property. value set to null will delete the property from the table
     */
    void setProperty(String key, String value) throws RemoteException, SaveException, UpdateException, DeleteException;

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
    void installNewLicense(String newLicenseXml) throws RemoteException, UpdateException, InvalidLicenseException;

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
    List findMetricsBins(String nodeId, Long minPeriodStart, Long maxPeriodStart, Integer resolution, Long serviceOid) throws RemoteException, FindException;

    /**
     * Check hardware capabilities of the node that receives this admin request.
     * TODO this information should be moved to ClusterNodeInfo instead, since it is really per-node.
     *
     * @param capability the capability to interrogate.  Must not be null.
     * @return a string describing the support for the requested capability, or null if the capability is not supported.
     */
    String getHardwareCapability(String capability);

    public static final String CAPABILITY_HWXPATH = "hardwareXpath";
    public static final String CAPABILITY_HWXPATH_TARARI = "tarari";
}
