package com.l7tech.cluster;

import com.l7tech.admin.RoleUtils;
import com.l7tech.objectmodel.*;
import org.springframework.context.support.ApplicationObjectSupport;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the ClusterStatusAdmin interface.
 * Accessed through RMI in the SSM.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 2, 2004<br/>
 * $Id$<br/>
 *
 */
public class ClusterStatusAdminImp extends ApplicationObjectSupport implements ClusterStatusAdmin {
    /**
     * Constructs the new cluster status admin implementation.
     * On constructir change update the spring bean definition
     *
     * @param clusterInfoManager
     * @param serviceUsageManager
     */
    public ClusterStatusAdminImp(ClusterInfoManager clusterInfoManager, ServiceUsageManager serviceUsageManager) {
        this.clusterInfoManager = clusterInfoManager;
        this.serviceUsageManager = serviceUsageManager;
        if (clusterInfoManager == null) {
            throw new IllegalArgumentException("Cluster Info manager is required");
        }
        if (serviceUsageManager == null) {
             throw new IllegalArgumentException("Service Usage manager is required");
         }
     }

    /**
     * get status for all nodes recorded as part of the cluster.
     */
    public ClusterNodeInfo[] getClusterStatus() throws FindException {
        try {
            Collection res = clusterInfoManager.retrieveClusterStatus();
            Object[] resarray = res.toArray();
            ClusterNodeInfo[] output = new ClusterNodeInfo[res.size()];
            for (int i = 0; i < resarray.length; i++) {
                output[i] = (ClusterNodeInfo)resarray[i];
            }
            return output;
        } finally {
            closeContext();
        }
    }

    /**
     * get service usage as currently recorded in database.
     */
    public ServiceUsage[] getServiceUsage() throws FindException {
        try {
            Collection res = serviceUsageManager.getAll();
            Object[] resarray = res.toArray();
            ServiceUsage[] output = new ServiceUsage[res.size()];
            for (int i = 0; i < resarray.length; i++) {
                output[i] = (ServiceUsage)resarray[i];
            }
            return output;
        } finally {
            closeContext();
        }
    }

    /**
     * lets the administrator change the human readable name of a node part of the cluster. these
     * names are originally automatically set by the server when they join the cluster.
     *
     * @param nodeid the mac of the node for which we want to change the name
     * @param newName the new name of the node (must not be null)
     */
    public void changeNodeName(String nodeid, String newName) throws RemoteException, UpdateException {
        PersistenceContext context = null;
        try {
            context = PersistenceContext.getCurrent();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "could not get context", e);
            throw new UpdateException("internal error", e);
        }
        try {
            RoleUtils.enforceAdminRole(getApplicationContext());
            try {
                context.beginTransaction();
                clusterInfoManager.renameNode(nodeid, newName);
                context.commitTransaction();
            } catch (TransactionException e) {
                logger.log(Level.WARNING, "transaction exception", e);
            }
        } finally {
            closeContext();
        }
    }

    /**
     * lets the administrator remove a node that is no longer part of the cluster. after this is called,
     * the getClusterStatus calls will no longer refer to this node. this operation will not be permitted
     * unless this node's status has not been updated for a while.
     *
     * tables potentially affected by this call are cluster_info, service_usage and ssg_logs
     *
     * @param nodeid the mac of the stale node to remove
     */
    public void removeStaleNode(String nodeid) throws RemoteException, DeleteException {
        PersistenceContext context = null;
        try {
            context = PersistenceContext.getCurrent();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "could not get context", e);
            throw new DeleteException("internal error", e);
        }
        try {
            RoleUtils.enforceAdminRole(getApplicationContext());
            try {
                logger.info("removing stale node: " + nodeid);
                context.beginTransaction();
                clusterInfoManager.deleteNode(nodeid);
                serviceUsageManager.clear(nodeid);

                // Bugzilla #842 - remote exception (outofmemory) is thrown by the server side in the
                // case when SSG is trying to clean all log records of the stale node and the table contains
                // a huge volumn of rows. For this reason, we don't clean up the log records here and rely on
                // the housekeeping script to remove the old records periodically.
                //ServerLogHandler.cleanAllRecordsForNode((HibernatePersistenceContext)context, nodeid);

                context.commitTransaction();
            } catch (TransactionException e) {
                logger.log(Level.WARNING, "transaction exception", e);
            }
        } finally {
            closeContext();
        }
    }

    private void closeContext() {
        try {
            PersistenceContext.getCurrent().close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error closing context", e);
        }
    }

    /**
     * gets the current system time
     *
     * @return long  The current system time in milli seconds
     */
    public java.util.Date getCurrentClusterSystemTime() throws RemoteException {
        return Calendar.getInstance().getTime();
    }

    /**
     * gets the name of node that handles the admin request.
     *
     * @return String  The node name
     */
    public String getSelfNodeName() throws RemoteException {
        return clusterInfoManager.getSelfNodeInf().getName();
    }

    private final ClusterInfoManager clusterInfoManager;
    private final ServiceUsageManager serviceUsageManager;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
