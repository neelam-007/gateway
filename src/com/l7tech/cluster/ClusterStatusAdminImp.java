package com.l7tech.cluster;

import com.l7tech.objectmodel.FindException;
import com.l7tech.remote.jini.export.RemoteService;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.util.Collection;

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

    public ClusterNodeInfo[] getClusterStatus() throws FindException {
        Collection res = ciman.retrieveClusterStatus();
        Object[] resarray = res.toArray();
        ClusterNodeInfo[] output = new ClusterNodeInfo[res.size()];
        for (int i = 0; i < resarray.length; i++) {
            output[i] = (ClusterNodeInfo)resarray[i];
        }
        return output;
    }

    public ServiceUsage[] getServiceUsage() throws FindException {
        Collection res = suman.getAll();
        Object[] resarray = res.toArray();
        ServiceUsage[] output = new ServiceUsage[res.size()];
        for (int i = 0; i < resarray.length; i++) {
            output[i] = (ServiceUsage)resarray[i];
        }
        return output;
    }

    private final ClusterInfoManager ciman = ClusterInfoManager.getInstance();
    private final ServiceUsageManager suman = new ServiceUsageManager();
    //private final Logger logger = LogManager.getInstance().getSystemLogger();
}
