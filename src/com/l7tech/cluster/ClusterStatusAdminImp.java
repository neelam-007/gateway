package com.l7tech.cluster;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.remote.jini.export.RemoteService;
import com.l7tech.logging.LogManager;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;
import java.sql.SQLException;

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
public class ClusterStatusAdminImp extends RemoteService implements ClusterStatusAdmin {

    public ClusterStatusAdminImp(String[] configOptions, LifeCycle lc) throws ConfigurationException, IOException {
        super(configOptions, lc);
    }

    /**
     * get status for all nodes recorded as part of the cluster.
     */
    public ClusterNodeInfo[] getClusterStatus() throws FindException {
        try {
            Collection res = ciman.retrieveClusterStatus();
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
            Collection res = suman.getAll();
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
        // todo, ciman.renameNode(nodeid, newName); and all transaction trimmings
        throw new UnsupportedOperationException();
    }

    /**
     * lets the administrator remove a node that is no longer part of the cluster. after this is called,
     * the getClusterStatus calls will no longer refer to this node. this operation will not be permitted
     * unless this node's status has not been updated for a while.
     *
     * @param nodeid the mac of the stale node to remove
     */
    public void removeStaleNode(String nodeid) throws RemoteException, UpdateException {
        // todo
        throw new UnsupportedOperationException();
    }

    private void closeContext() {
        try {
            PersistenceContext.getCurrent().close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "error closing context", e);
        }
    }

    private final ClusterInfoManager ciman = ClusterInfoManager.getInstance();
    private final ServiceUsageManager suman = new ServiceUsageManager();
    private final Logger logger = LogManager.getInstance().getSystemLogger();
}
