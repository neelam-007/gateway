package com.l7tech.cluster;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Admin interface for the cluster status panel of the SSM.
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
     * get status for all nodes recorded as part of the cluster.
     */
    ClusterNodeInfo[] getClusterStatus() throws RemoteException, FindException;

    /**
     * get service usage as currently recorded in database.
     */
    ServiceUsage[] getServiceUsage() throws RemoteException, FindException;

    /**
     * lets the administrator change the human readable name of a node part of the cluster. these
     * names are originally automatically set by the server when they join the cluster.
     *
     * @param nodeid the mac of the node for which we want to change the name
     * @param newName the new name of the node (must not be null)
     */
    void changeNodeName(String nodeid, String newName) throws RemoteException, UpdateException;

    /**
     * lets the administrator remove a node that is no longer part of the cluster. after this is called,
     * the getClusterStatus calls will no longer refer to this node. this operation will not be permitted
     * unless this node's status has not been updated for a while.
     *
     * tables potentially affected by this call are cluster_info, service_usage and ssg_logs
     *
     * @param nodeid the mac of the stale node to remove
     */
    void removeStaleNode(String nodeid) throws RemoteException, DeleteException;

    /**
     * gets the current system time
     *
     * @return java.util.Date  The current system time
     */
    java.util.Date getCurrentClusterSystemTime() throws RemoteException;

    /**
     * gets the name of node that handles the admin request.
     *
     * @return String  The node name
     */
     String getSelfNodeName() throws RemoteException;
}
