package com.l7tech.cluster;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

import java.rmi.Remote;
import java.rmi.RemoteException;

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
}
