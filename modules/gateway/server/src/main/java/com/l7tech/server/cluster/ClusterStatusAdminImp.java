package com.l7tech.server.cluster;

import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.License;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.admin.LicenseRuntimeException;
import com.l7tech.gateway.common.cluster.*;
import com.l7tech.gateway.common.emstrust.TrustedEms;
import com.l7tech.gateway.common.emstrust.TrustedEmsUser;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.*;
import com.l7tech.server.policy.AssertionModule;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.util.CollectionUpdate;
import com.l7tech.util.CollectionUpdateProducer;
import com.l7tech.util.TimeUnit;
import com.l7tech.xml.TarariLoader;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
                                 ServerConfig serverConfig,
                                 AssertionRegistry assertionRegistry,
                                 TrustedEmsManager trustedEmsManager,
                                 TrustedEmsUserManager trustedEmsUserManager)
    {
        this.clusterInfoManager = clusterInfoManager;
        this.serviceUsageManager = serviceUsageManager;
        this.clusterPropertyManager = clusterPropertyManager;
        this.licenseManager = (GatewayLicenseManager)licenseManager;
        this.serviceMetricsManager = metricsManager;
        this.serverConfig = serverConfig;
        this.assertionRegistry = (ServerAssertionRegistry)assertionRegistry;
        this.trustedEmsManager = trustedEmsManager;
        this.trustedEmsUserManager = trustedEmsUserManager;

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

    private void checkLicense() {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch ( LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new LicenseRuntimeException(e);
        }
    }

    public boolean isCluster() {
        try {
            return getClusterStatus().length > 1;
        } catch(FindException e) {
            return false;
        }
    }
    
    /**
     * get status for all nodes recorded as part of the cluster.
     */
    public ClusterNodeInfo[] getClusterStatus() throws FindException {
        Collection res = clusterInfoManager.retrieveClusterStatus();
        Object[] resarray = res.toArray();
        ClusterNodeInfo[] output = new ClusterNodeInfo[res.size()];
        for (int i = 0; i < resarray.length; i++) {
            output[i] = (ClusterNodeInfo)resarray[i];
        }
        return output;
    }

    public CollectionUpdate<ClusterNodeInfo> getClusterNodesUpdate(int oldVersionID) throws FindException {
        return clusterNodesUpdateProducer.createUpdate(oldVersionID);
    }

    /**
     * get service usage as currently recorded in database.
     */
    public ServiceUsage[] getServiceUsage() throws FindException {
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
    public void changeNodeName(String nodeid, String newName) throws UpdateException {
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
    public void removeStaleNode(String nodeid) throws DeleteException {
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
    public java.util.Date getCurrentClusterSystemTime() {
        return Calendar.getInstance().getTime();
    }

    public String getCurrentClusterTimeZone() {
        return TimeZone.getDefault().getID();
    }

    /**
     * gets the name of node that handles the admin request.
     *
     * @return String  The node name
     */
    public String getSelfNodeName() {
        return clusterInfoManager.getSelfNodeInf().getName();
    }

    public Collection<ClusterProperty> getAllProperties() throws FindException {
        return clusterPropertyManager.findAll();
    }

    public Map<String, String[]> getKnownProperties() {
        Map<String,String> namesToDesc =  serverConfig.getClusterPropertyNames();
        Map<String,String> namesToDefs =  serverConfig.getClusterPropertyDefaults();

        Map<String, String[]> known = new LinkedHashMap<String, String[]>();
        for (String name : namesToDesc.keySet()) {
            known.put(name, new String[]{namesToDesc.get(name), namesToDefs.get(name)});
        }

        return Collections.unmodifiableMap(known);
    }

    public Collection<ClusterPropertyDescriptor> getAllPropertyDescriptors() {
        Map<String,String> namesToDesc =  serverConfig.getClusterPropertyNames();
        Map<String,String> namesToDefs =  serverConfig.getClusterPropertyDefaults();
        Map<String,String> namesToVisi =  serverConfig.getClusterPropertyVisibilities();

        Collection<ClusterPropertyDescriptor> properties = new ArrayList<ClusterPropertyDescriptor>();
        for (String name : namesToDesc.keySet()) {
            String visible = namesToVisi.get(name);
            if ( visible == null ) {
                visible = "true";
            }

            properties.add( new ClusterPropertyDescriptor(
                    name,
                    namesToDesc.get(name),
                    namesToDefs.get(name),
                    Boolean.valueOf(visible)) );
        }

        return Collections.unmodifiableCollection(properties);
    }

    public ClusterProperty findPropertyByName(String key) throws FindException {
        return clusterPropertyManager.findByUniqueName(key);
    }

    public long saveProperty(ClusterProperty clusterProperty) throws SaveException, UpdateException, DeleteException {
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

    public void deleteProperty(ClusterProperty clusterProperty) throws DeleteException {
        clusterPropertyManager.delete(clusterProperty);
    }

    public License getCurrentLicense() throws InvalidLicenseException {
        return licenseManager.getCurrentLicense();
    }

    public long getLicenseExpiryWarningPeriod() {
        long expiryWarnPeriod = 0;
        String propertyName = "license.expiryWarnAge";
        String propStr = serverConfig.getPropertyCached(propertyName);
        if (propStr != null) {
            try {
                expiryWarnPeriod = TimeUnit.parse(propStr, TimeUnit.DAYS);    
            } catch (NumberFormatException nfe) {
                logger.warning("Unable to parse property '" + propertyName + "' with value '"+propStr+"'.");
            }
        }
        return expiryWarnPeriod;
    }

    public void installNewLicense(String newLicenseXml) throws InvalidLicenseException, UpdateException {
        licenseManager.installNewLicense(newLicenseXml);

        // Make sure we don't return until any module updating has been dealt with
        assertionRegistry.runNeededScan();
    }

    public boolean isMetricsEnabled() {
        return serviceMetricsManager.isEnabled();
    }

    public int getMetricsFineInterval() {
        return serviceMetricsManager.getFineInterval();
    }

    public Collection<MetricsSummaryBin> summarizeByPeriod(final String nodeId, final long[] serviceOids, final Integer resolution, final Long minPeriodStart, final Long maxPeriodStart, final boolean includeEmpty) throws FindException {
        return serviceMetricsManager.summarizeByPeriod(nodeId, serviceOids, resolution, minPeriodStart, maxPeriodStart, includeEmpty);
    }

    public Collection<MetricsSummaryBin> summarizeLatestByPeriod(final String nodeId, final long[] serviceOids, final Integer resolution, final long duration, final boolean includeEmpty) throws FindException {
        final long minPeriodStart = System.currentTimeMillis() - duration;
        return serviceMetricsManager.summarizeByPeriod(nodeId, serviceOids, resolution, minPeriodStart, null, includeEmpty);
    }

    public MetricsSummaryBin summarizeLatest(final String nodeId, final long[] serviceOids, final int resolution, final int duration, final boolean includeEmpty) throws FindException {
        return serviceMetricsManager.summarizeLatest(nodeId, serviceOids, resolution, duration, includeEmpty);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Collection<ModuleInfo> getAssertionModuleInfo() {
        Collection<ModuleInfo> ret = new ArrayList<ModuleInfo>();
        Set<AssertionModule> modules = assertionRegistry.getLoadedModules();
        for (AssertionModule module : modules) {
            Collection<String> assertions = new ArrayList<String>();
            for (Assertion assertion : module.getAssertionPrototypes())
                assertions.add(assertion.getClass().getName());
            ret.add(new ModuleInfo(module.getName(), module.getSha1(), assertions));
        }
        return ret;
    }

    public String getHardwareCapability(String capability) {
        if (!ClusterStatusAdmin.CAPABILITY_HWXPATH.equals(capability)) return null;
        return TarariLoader.getGlobalContext() != null ? ClusterStatusAdmin.CAPABILITY_HWXPATH_TARARI : null;
    }

    public Collection<TrustedEms> getTrustedEmsInstances() throws FindException {
        return trustedEmsManager.findAll();
    }

    public void deleteTrustedEmsInstance(long trustedEmsOid) throws DeleteException, FindException {
        trustedEmsManager.delete(trustedEmsOid);
    }

    public void deleteTrustedEmsUserMapping(long trustedEmsUserOid) throws DeleteException, FindException {
        trustedEmsUserManager.delete(trustedEmsUserOid);
    }

    public Collection<TrustedEmsUser> getTrustedEmsUserMappings(long trustedEmsId) throws FindException {
        return trustedEmsUserManager.findByEmsId(trustedEmsId);
    }

    private CollectionUpdateProducer<ClusterNodeInfo, FindException> clusterNodesUpdateProducer =
            new CollectionUpdateProducer<ClusterNodeInfo, FindException>(5 * 60 * 1000, 100, null) {
                protected Collection<ClusterNodeInfo> getCollection() throws FindException {
                    return clusterInfoManager.retrieveClusterStatus();
                }
            };

    private final ClusterInfoManager clusterInfoManager;
    private final ServiceUsageManager serviceUsageManager;
    private final ClusterPropertyManager clusterPropertyManager;
    private final GatewayLicenseManager licenseManager;
    private final ServiceMetricsManager serviceMetricsManager;
    private final ServerConfig serverConfig;
    private final ServerAssertionRegistry assertionRegistry;
    private final TrustedEmsManager trustedEmsManager;
    private final TrustedEmsUserManager trustedEmsUserManager;

    private final Logger logger = Logger.getLogger(getClass().getName());

}
