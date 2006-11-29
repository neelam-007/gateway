package com.l7tech.cluster;

import com.l7tech.common.InvalidLicenseException;
import com.l7tech.common.License;
import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.MethodStereotype;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.GatewayLicenseManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.service.MetricsBin;

import java.rmi.RemoteException;
import java.util.*;
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
     */
    public ClusterStatusAdminImp(ClusterInfoManager clusterInfoManager,
                                 ServiceUsageManager serviceUsageManager,
                                 ClusterPropertyManager clusterPropertyManager,
                                 LicenseManager licenseManager,
                                 ServiceMetricsManager metricsManager,
                                 ServerConfig serverConfig)
    {
        this.clusterInfoManager = clusterInfoManager;
        this.serviceUsageManager = serviceUsageManager;
        this.clusterPropertyManager = clusterPropertyManager;
        this.licenseManager = (GatewayLicenseManager)licenseManager;
        this.serviceMetricsManager = metricsManager;
        this.serverConfig = serverConfig;

        if (clusterInfoManager == null)
            throw new IllegalArgumentException("Cluster Info manager is required");
        if (serviceUsageManager == null)
            throw new IllegalArgumentException("Service Usage manager is required");
        if (clusterPropertyManager == null)
            throw new IllegalArgumentException("Cluster Property manager is required");
        if (licenseManager == null)
            throw new IllegalArgumentException("License manager is required");
        if (metricsManager == null)
            throw new IllegalArgumentException("Metrics manager is required");
        if (serverConfig == null)
            throw new IllegalArgumentException("Server Config manager is required");
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new RemoteException(ExceptionUtils.getMessage(e), new LicenseException(e.getMessage()));
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

    public String getCurrentClusterTimeZone() throws RemoteException {
        return TimeZone.getDefault().getID();
    }

    /**
     * gets the name of node that handles the admin request.
     *
     * @return String  The node name
     */
    public String getSelfNodeName() throws RemoteException {
        return clusterInfoManager.getSelfNodeInf().getName();
    }

    public Collection<ClusterProperty> getAllProperties() throws RemoteException, FindException {
        return clusterPropertyManager.findAll();
    }

    public Map<String, String[]> getKnownProperties() throws RemoteException {
        Map<String,String> namesToDesc =  serverConfig.getClusterPropertyNames();
        Map<String,String> namesToDefs =  serverConfig.getClusterPropertyDefaults();

        Map<String, String[]> known = new LinkedHashMap<String, String[]>();
        for (String name : namesToDesc.keySet()) {
            known.put(name, new String[]{namesToDesc.get(name), namesToDefs.get(name)});
        }

        return Collections.unmodifiableMap(known);
    }

    public ClusterProperty findPropertyByName(String key) throws RemoteException, FindException {
        return clusterPropertyManager.findByUniqueName(key);
    }

    public long saveProperty(ClusterProperty clusterProperty) throws RemoteException, SaveException, UpdateException, DeleteException {
        if (!("license".equals(clusterProperty.getName())))
            checkLicense();
        long oid = clusterProperty.getOid();
        if (oid == ClusterProperty.DEFAULT_OID) {
            return clusterPropertyManager.save(clusterProperty);
        } else {
            clusterPropertyManager.update(clusterProperty);
            return oid;
        }
    }

    @Secured(types = EntityType.CLUSTER_PROPERTY, stereotype = MethodStereotype.DELETE_ENTITY)
    public void deleteProperty(ClusterProperty clusterProperty) throws DeleteException, RemoteException {
        if ("license".equals(clusterProperty.getName()))
            throw new DeleteException("Can't delete license");
        clusterPropertyManager.delete(clusterProperty);
    }

    public License getCurrentLicense() throws RemoteException, InvalidLicenseException {
        return licenseManager.getCurrentLicense();
    }

    public void installNewLicense(String newLicenseXml) throws RemoteException, InvalidLicenseException, UpdateException {
        licenseManager.installNewLicense(newLicenseXml);
    }

    public List<MetricsBin> findMetricsBins(String nodeId, Long minPeriodStart, Long maxPeriodStart, Integer resolution, Long serviceOid) throws RemoteException, FindException {
        checkLicense();
        return serviceMetricsManager.findBins(nodeId, minPeriodStart, maxPeriodStart, resolution, serviceOid);
    }

    public List<MetricsBin> findLatestMetricsBins(String nodeId, Long duration, Integer resolution, Long serviceOid) throws RemoteException, FindException {
        checkLicense();
        if (duration == null) {
            return serviceMetricsManager.findBins(nodeId, null, null, resolution, serviceOid);
        } else {
            final long now = System.currentTimeMillis();
            return serviceMetricsManager.findBins(nodeId, now - duration, now, resolution, serviceOid);
        }
    }

    public MetricsBin getLastestMetricsSummary(final String clusterNodeId, final Long serviceOid, final int resolution, final int duration) throws RemoteException {
        checkLicense();
        return serviceMetricsManager.getLatestMetricsSummary(clusterNodeId, serviceOid, resolution, duration);
    }

    public String getHardwareCapability(String capability) {
        if (!ClusterStatusAdmin.CAPABILITY_HWXPATH.equals(capability)) return null;
        return TarariLoader.getGlobalContext() != null ? ClusterStatusAdmin.CAPABILITY_HWXPATH_TARARI : null;
    }

    private final ClusterInfoManager clusterInfoManager;
    private final ServiceUsageManager serviceUsageManager;
    private final ClusterPropertyManager clusterPropertyManager;
    private final GatewayLicenseManager licenseManager;
    private final ServiceMetricsManager serviceMetricsManager;
    private final ServerConfig serverConfig;

    private final Logger logger = Logger.getLogger(getClass().getName());

}
