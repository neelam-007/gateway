package com.l7tech.cluster;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for getting the status of nodes in a gateway cluster.
 * This is used by the cluster status panel of the SSM.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Dec 22, 2003<br/>
 * $Id$<br/>
 *
 */
public interface ClusterStatusAdmin extends Remote {
    /**
     * Get status for all nodes recorded as part of the cluster.
     * @return An array of ClusterNodeInfo (one for each node)
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    ClusterNodeInfo[] getClusterStatus() throws RemoteException, FindException;

    /**
     * Get some usage statistics on a 'per-published-service' basis.
     * @return an array of ServiceUsage (one per published service). never null
     * @throws FindException if there was a server-side problem accessing the requested information
     */
    ServiceUsage[] getServiceUsage() throws RemoteException, FindException;

    /**
     * Allows the administrator change the human readable name of a node part of the cluster. these
     * names are originally automatically set by the server when they join the cluster.
     *
     * @param nodeid the mac of the node for which we want to change the name
     * @param newName the new name of the node (must not be null)
     * @throws UpdateException if there was a server-side problem updating the requested node
     */
    void changeNodeName(String nodeid, String newName) throws RemoteException, UpdateException;

    /**
     * Allows the administrator remove a node that is no longer part of the cluster. When a
     * node is removed from the cluster, the other nodes might think that the node is no longer
     * responding. After this is called,
     * the getClusterStatus calls will no longer refer to this node. this operation will not be permitted
     * unless this node's status has not been updated for a while. Tables potentially affected by this
     * call are cluster_info, service_usage and ssg_logs
     *
     * @param nodeid the mac of the stale node to remove as defined in the ClusterNodeInfo object
     * @throws DeleteException if there was a server-side problem deleting the requested node
     */
    void removeStaleNode(String nodeid) throws RemoteException, DeleteException;

    /**
     * Return the current system time on the gateway.
     *
     * @return java.util.Date  The current system time
     */
    java.util.Date getCurrentClusterSystemTime() throws RemoteException;

    /**
     * Gets the name of node that handles the admin request.
     *
     * @return String  The node name
     */
     String getSelfNodeName() throws RemoteException;
}
