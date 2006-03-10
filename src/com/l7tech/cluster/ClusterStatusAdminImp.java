package com.l7tech.cluster;

import com.l7tech.admin.AccessManager;
import com.l7tech.common.*;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.GatewayLicenseManager;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.service.MetricsBin;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Server side implementation of the ClusterStatusAdmin interface.
 * Accessed through RMI in the SSM.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 2, 2004<br/>
 */
public class ClusterStatusAdminImp implements ClusterStatusAdmin {
    /**
     * Constructs the new cluster status admin implementation.
     * On constructir change update the spring bean definition
     *
     * @param clusterInfoManager
     * @param serviceUsageManager
     */
    public ClusterStatusAdminImp(ClusterInfoManager clusterInfoManager,
                                 ServiceUsageManager serviceUsageManager,
                                 ClusterPropertyManager clusterPropertyManager,
                                 AccessManager accessManager,
                                 LicenseManager licenseManager,
                                 ServiceMetricsManager metricsManager)
    {
        this.clusterInfoManager = clusterInfoManager;
        this.serviceUsageManager = serviceUsageManager;
        this.accessManager = accessManager;
        this.clusterPropertyManager = clusterPropertyManager;
        this.licenseManager = (GatewayLicenseManager)licenseManager; // XXX this is... Not Very Pretty
        this.serviceMetricsManager = metricsManager;
        if (clusterInfoManager == null) {
            throw new IllegalArgumentException("Cluster Info manager is required");
        }
        if (serviceUsageManager == null) {
            throw new IllegalArgumentException("Service Usage manager is required");
        }
        if (accessManager == null) {
            throw new IllegalArgumentException("Access manager is required");
        }
        if (licenseManager == null)
            throw new IllegalArgumentException("License manager is required");
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature(Feature.ADMIN);
        } catch (LicenseException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    /**
     * get status for all nodes recorded as part of the cluster.
     */
    public ClusterNodeInfo[] getClusterStatus() throws FindException, RemoteException {
        Collection res = clusterInfoManager.retrieveClusterStatus();
        Object[] resarray = res.toArray();
        ClusterNodeInfo[] output = new ClusterNodeInfo[res.size()];
        for (int i = 0; i < resarray.length; i++) {
            output[i] = (ClusterNodeInfo)resarray[i];
        }
        return output;
    }

    /**
     * get service usage as currently recorded in database.
     */
    public ServiceUsage[] getServiceUsage() throws FindException, RemoteException {
        Collection res = serviceUsageManager.getAll();
        Object[] resarray = res.toArray();
        ServiceUsage[] output = new ServiceUsage[res.size()];
        for (int i = 0; i < resarray.length; i++) {
            output[i] = (ServiceUsage)resarray[i];
        }
        return output;
    }

    /**
     * lets the administrator change the human readable name of a node part of the cluster. these
     * names are originally automatically set by the server when they join the cluster.
     *
     * @param nodeid  the mac of the node for which we want to change the name
     * @param newName the new name of the node (must not be null)
     */
    public void changeNodeName(String nodeid, String newName) throws RemoteException, UpdateException {
        accessManager.enforceAdminRole();
        checkLicense();
        clusterInfoManager.renameNode(nodeid, newName);
    }

    /**
     * lets the administrator remove a node that is no longer part of the cluster. after this is called,
     * the getClusterStatus calls will no longer refer to this node. this operation will not be permitted
     * unless this node's status has not been updated for a while.
     * <p/>
     * tables potentially affected by this call are cluster_info, service_usage and ssg_logs
     *
     * @param nodeid the mac of the stale node to remove
     */
    public void removeStaleNode(String nodeid) throws RemoteException, DeleteException {
        accessManager.enforceAdminRole();
        checkLicense();
        logger.info("removing stale node: " + nodeid);
        clusterInfoManager.deleteNode(nodeid);
        serviceUsageManager.clear(nodeid);

        // Bugzilla #842 - remote exception (outofmemory) is thrown by the server side in the
        // case when SSG is trying to clean all log records of the stale node and the table contains
        // a huge volumn of rows. For this reason, we don't clean up the log records here and rely on
        // the housekeeping script to remove the old records periodically.
        //ServerLogHandler.cleanAllRecordsForNode((HibernatePersistenceContext)context, nodeid);
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

    public Collection getAllProperties() throws RemoteException, FindException {
        return clusterPropertyManager.findAll();
    }

    public String getProperty(String key) throws RemoteException, FindException {
        return clusterPropertyManager.getProperty(key);
    }

    public void setProperty(String key, String value) throws RemoteException, SaveException, UpdateException, DeleteException {
        accessManager.enforceAdminRole();
        if (!("license".equals(key)))
            checkLicense();
        clusterPropertyManager.setProperty(key, value);
    }

    public License getCurrentLicense() throws RemoteException, InvalidLicenseException {
        return licenseManager.getCurrentLicense();
    }

    public void installNewLicense(String newLicenseXml) throws RemoteException, InvalidLicenseException, UpdateException {
        accessManager.enforceAdminRole();
        licenseManager.installNewLicense(newLicenseXml);
    }

    public List findMetricsBins(String nodeId, Long minPeriodStart, Long maxPeriodStart, Integer resolution, Long serviceOid) throws RemoteException, FindException {
        checkLicense();
        return serviceMetricsManager.findBins(nodeId, minPeriodStart, maxPeriodStart, resolution, serviceOid);
    }

    public MetricsBin getMetricsSummary(int resolution, long startTime, int duration, String nodeId, Long serviceOid) throws RemoteException, FindException {
        checkLicense();
        return serviceMetricsManager.getMetricsSummary(resolution, startTime, duration, nodeId, serviceOid);
    }

    public String getHardwareCapability(String capability) {
        if (!ClusterStatusAdmin.CAPABILITY_HWXPATH.equals(capability)) return null;
        return TarariLoader.getGlobalContext() != null ? ClusterStatusAdmin.CAPABILITY_HWXPATH_TARARI : null;
    }

    private final ClusterInfoManager clusterInfoManager;
    private final ServiceUsageManager serviceUsageManager;
    private final ClusterPropertyManager clusterPropertyManager;
    private final AccessManager accessManager;
    private final GatewayLicenseManager licenseManager;
    private final ServiceMetricsManager serviceMetricsManager;

    private final Logger logger = Logger.getLogger(getClass().getName());

}
