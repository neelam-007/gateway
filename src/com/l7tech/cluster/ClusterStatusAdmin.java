package com.l7tech.cluster;

import com.l7tech.objectmodel.FindException;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Admin interface for the cluster status panel of the SSM
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Dec 22, 2003<br/>
 * $Id$<br/>
 *
 */
public interface ClusterStatusAdmin extends Remote {
    ClusterNodeInfo[] getClusterStatus() throws RemoteException, FindException;
    ServiceUsage[] getServiceUsage() throws RemoteException, FindException;
}
