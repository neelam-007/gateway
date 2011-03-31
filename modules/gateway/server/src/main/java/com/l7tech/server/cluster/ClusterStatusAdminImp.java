package com.l7tech.server.cluster;

import com.l7tech.gateway.common.*;
import com.l7tech.gateway.common.admin.LicenseRuntimeException;
import com.l7tech.gateway.common.cluster.*;
import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.*;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.audit.AuditSinkPropertiesChecker;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.EntityChangeSet;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.PersistenceEvent;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.policy.AssertionModule;
import com.l7tech.server.policy.ServerAssertionRegistry;
import com.l7tech.server.security.keystore.luna.GatewayLunaPinFinder;
import com.l7tech.server.security.keystore.luna.LunaProber;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.server.service.ServiceMetricsServices;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.*;
import com.l7tech.xml.TarariLoader;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;
import java.security.KeyStoreException;

/**
 * Server side implementation of the ClusterStatusAdmin interface.
 * Accessed through RMI in the SSM.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 2, 2004<br/>
 */
public class ClusterStatusAdminImp implements ClusterStatusAdmin, ApplicationContextAware {
    /**
     * Constructs the new cluster status admin implementation.
     * On constructor change update the spring bean definition
     */
    public ClusterStatusAdminImp(ClusterInfoManager clusterInfoManager,
                                 ServiceUsageManager serviceUsageManager,
                                 ClusterPropertyManager clusterPropertyManager,
                                 LicenseManager licenseManager,
                                 ServiceMetricsManager metricsManager,
                                 ServiceMetricsServices serviceMetricsServices,
                                 ServerConfig serverConfig,
                                 AssertionRegistry assertionRegistry,
                                 TrustedEsmManager trustedEsmManager,
                                 TrustedEsmUserManager trustedEsmUserManager,
                                 RbacServices rbacServices,
                                 AuditSinkPropertiesChecker auditSinkPropertiesChecker,
                                 PlatformTransactionManager transactionManager,
                                 Timer timer)
    {
        this.clusterInfoManager = clusterInfoManager;
        this.serviceUsageManager = serviceUsageManager;
        this.clusterPropertyManager = clusterPropertyManager;
        this.serviceMetricsServices = serviceMetricsServices;
        this.licenseManager = (GatewayLicenseManager)licenseManager;
        this.serviceMetricsManager = metricsManager;
        this.serverConfig = serverConfig;
        this.assertionRegistry = (ServerAssertionRegistry)assertionRegistry;
        this.trustedEsmManager = trustedEsmManager;
        this.trustedEsmUserManager = trustedEsmUserManager;
        this.rbacServices = rbacServices;
        this.auditSinkPropertiesChecker = auditSinkPropertiesChecker;
        this.transactionManager = transactionManager;
        this.timer = timer;

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

    @Override
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
    @Override
    public ClusterNodeInfo[] getClusterStatus() throws FindException {
        Collection res = clusterInfoManager.retrieveClusterStatus();
        Object[] resarray = res.toArray();
        ClusterNodeInfo[] output = new ClusterNodeInfo[res.size()];
        for (int i = 0; i < resarray.length; i++) {
            output[i] = (ClusterNodeInfo)resarray[i];
        }
        return output;
    }

    @Override
    public CollectionUpdate<ClusterNodeInfo> getClusterNodesUpdate(int oldVersionID) throws FindException {
        return clusterNodesUpdateProducer.createUpdate(oldVersionID);
    }

    /**
     * get service usage as currently recorded in database.
     */
    @Override
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
    @Override
    public void changeNodeName(String nodeid, String newName) throws UpdateException {
        String oldName = clusterInfoManager.renameNode(nodeid, newName);
        EntityChangeSet changes = new EntityChangeSet(new String[]{"name"}, new Object[]{oldName}, new Object[]{newName});
        publishEvent(new Updated<ClusterNodeInfo>( clusterNodeInfo( nodeid, newName ), changes ));
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
    @Override
    public void removeStaleNode(String nodeid) throws DeleteException {
        logger.info("removing stale node: " + nodeid);
        String name = clusterInfoManager.deleteNode(nodeid);
        serviceUsageManager.clear(nodeid);

        // Bugzilla #842 - remote exception (outofmemory) is thrown by the server side in the
        // case when SSG is trying to clean all log records of the stale node and the table contains
        // a huge volumn of rows. For this reason, we don't clean up the log records here and rely on
        // the housekeeping script to remove the old records periodically.
        //ServerLogHandler.cleanAllRecordsForNode((HibernatePersistenceContext)context, nodeid);

        publishEvent(new Deleted<ClusterNodeInfo>( clusterNodeInfo( nodeid, name ) ));
    }

    /**
     * gets the current system time
     *
     * @return long  The current system time in milli seconds
     */
    @Override
    public java.util.Date getCurrentClusterSystemTime() {
        return Calendar.getInstance().getTime();
    }

    @Override
    public String getCurrentClusterTimeZone() {
        return TimeZone.getDefault().getID();
    }

    /**
     * gets the name of node that handles the admin request.
     *
     * @return String  The node name
     */
    @Override
    public String getSelfNodeName() {
        return clusterInfoManager.getSelfNodeInf().getName();
    }

    @Override
    public Collection<ClusterProperty> getAllProperties() throws FindException {
        return clusterPropertyManager.findAll();
    }

    @Override
    public Map<String, String[]> getKnownProperties() {
        Map<String,String> namesToDesc =  serverConfig.getClusterPropertyNames();
        Map<String,String> namesToDefs =  serverConfig.getClusterPropertyDefaults();

        Map<String, String[]> known = new LinkedHashMap<String, String[]>();
        for (String name : namesToDesc.keySet()) {
            known.put(name, new String[]{namesToDesc.get(name), namesToDefs.get(name)});
        }

        return Collections.unmodifiableMap(known);
    }

    @Override
    public String getClusterProperty(String name){
        return serverConfig.getProperty(name);    
    }

    @Override
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

    @Override
    public ClusterProperty findPropertyByName(String key) throws FindException {
        return clusterPropertyManager.findByUniqueName(key);
    }

    @Override
    public long saveProperty(ClusterProperty clusterProperty) throws SaveException, UpdateException, DeleteException {
        if (!("license".equals(clusterProperty.getName())))
            checkLicense();
        long oid = clusterProperty.getOid();
        if (oid == ClusterProperty.DEFAULT_OID) {
            oid = clusterPropertyManager.save(clusterProperty);
        } else {
            clusterPropertyManager.update(clusterProperty);
        }
        evaluateAuditSinkPropsStatus(clusterProperty, false);
        return oid;
    }

    @Override
    public void deleteProperty(ClusterProperty clusterProperty) throws DeleteException {
        clusterPropertyManager.delete(clusterProperty);
        evaluateAuditSinkPropsStatus(clusterProperty, true);
    }

    @Override
    public License getCurrentLicense() throws InvalidLicenseException {
        return licenseManager.getCurrentLicense();
    }

    @Override
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

    @Override
    public void installNewLicense(String newLicenseXml) throws InvalidLicenseException, UpdateException {
        licenseManager.installNewLicense(newLicenseXml);

        // Make sure we don't return until any module updating has been dealt with
        assertionRegistry.runNeededScan();
    }

    @Override
    public void validateLicense(String newLicenseXml) throws InvalidLicenseException {
        licenseManager.validateLicense(newLicenseXml);
    }

    @Override
    public boolean isMetricsEnabled() {
        return serviceMetricsServices.isEnabled();
    }

    @Override
    public int getMetricsFineInterval() {
        return serviceMetricsServices.getFineInterval();
    }

    @Override
    public Collection<MetricsSummaryBin> summarizeByPeriod(final String nodeId, final long[] serviceOids, final Integer resolution, final Long minPeriodStart, final Long maxPeriodStart, final boolean includeEmpty) throws FindException {
        return serviceMetricsManager.summarizeByPeriod(nodeId, serviceOids, resolution, minPeriodStart, maxPeriodStart, includeEmpty);
    }

    @Override
    public Collection<MetricsSummaryBin> summarizeLatestByPeriod(final String nodeId, final long[] serviceOids, final Integer resolution, final long duration, final boolean includeEmpty) throws FindException {
        final long minPeriodStart = System.currentTimeMillis() - duration;
        return serviceMetricsManager.summarizeByPeriod(nodeId, serviceOids, resolution, minPeriodStart, null, includeEmpty);
    }

    @Override
    public MetricsSummaryBin summarizeLatest(final String nodeId, final long[] serviceOids, final int resolution, final int duration, final boolean includeEmpty) throws FindException {
        return serviceMetricsManager.summarizeLatest(nodeId, serviceOids, resolution, duration, includeEmpty);
    }

    @Override
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

    // Hardware capability support.
    // TODO refactor these methods into a separate HardwareCapabilityManager bean that delegates to HardwareCapability instances

    private static boolean isKnownCapability(String capability) {
        return ClusterStatusAdmin.CAPABILITY_LUNACLIENT.equals(capability) || ClusterStatusAdmin.CAPABILITY_HWXPATH.equals(capability);
    }

    @Override
    public String getHardwareCapability(String capability) {
        if (!isKnownCapability(capability))
            return null;
        if (ClusterStatusAdmin.CAPABILITY_LUNACLIENT.equals(capability)) {
            return LunaProber.isLunaClientLibraryAvailable() ? "true" : null;
        } else if (ClusterStatusAdmin.CAPABILITY_HWXPATH.equals(capability)) {
            return TarariLoader.getGlobalContext() != null ? ClusterStatusAdmin.CAPABILITY_VALUE_HWXPATH_TARARI : null;
        } else {
            return null;
        }
    }

    @Override
    public Serializable getHardwareCapabilityProperty(String capability, String property) throws NoSuchCapabilityException, NoSuchPropertyException {
        // Currently the only supported capability property, the Luna client PIN, is write-only
        if (!isKnownCapability(capability))
            throw new NoSuchCapabilityException();
        throw new NoSuchPropertyException();
    }

    @Override
    public void putHardwareCapabilityProperty(String capability, String property, Serializable value) throws NoSuchCapabilityException, NoSuchPropertyException, ClassCastException, IllegalArgumentException {
        if (!isKnownCapability(capability))
            throw new NoSuchCapabilityException();

        if (!ClusterStatusAdmin.CAPABILITY_LUNACLIENT.equals(capability) || !ClusterStatusAdmin.CAPABILITY_PROPERTY_LUNAPIN.equals(property)) {
            throw new NoSuchPropertyException();
        }

        if (value instanceof char[]) {
            char[] chars = (char[]) value;
            try {
                setLunaClientPin(chars);
            } finally {
                ArrayUtils.zero(chars);
            }
        } else if (value instanceof CharSequence) {
            setLunaClientPin(value.toString().toCharArray());
        } else {
            throw new IllegalArgumentException("Value for hardware capability propery " + ClusterStatusAdmin.CAPABILITY_PROPERTY_LUNAPIN + " must be a char array or CharSequence");
        }
    }

    @Override
    public void testHardwareTokenAvailability(String capability, int slotNum, char[] tokenPin) throws NoSuchCapabilityException, KeyStoreException {
        try {
            if (!ClusterStatusAdmin.CAPABILITY_LUNACLIENT.equals(capability))
                throw new NoSuchCapabilityException();
            LunaProber.testHardwareTokenAvailability(slotNum, tokenPin);
        } finally {
            ArrayUtils.zero(tokenPin);
        }
    }

    // TODO move this method somewhere more reasonable
    private void setLunaClientPin(char[] clientPin) {
        try {
            if (!rbacServices.isPermittedForAnyEntityOfType(JaasUtils.getCurrentUser(), OperationType.DELETE, EntityType.SSG_KEY_ENTRY))
                throw new PermissionDeniedException(OperationType.DELETE, EntityType.SSG_KEY_ENTRY, "Unable to change the Luna client PIN on this cluster");
            clusterPropertyManager.putProperty("keyStore.luna.encryptedLunaPin", GatewayLunaPinFinder.encryptLunaPin(clientPin));
        } catch (ObjectModelException e) {
            throw new RuntimeException("Unable to change Luna client PIN on this cluster: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    @Override
    public Collection<TrustedEsm> getTrustedEsmInstances() throws FindException {
        return trustedEsmManager.findAll();
    }

    @Override
    public void deleteTrustedEsmInstance(long trustedEsmOid) throws DeleteException, FindException {
        trustedEsmManager.delete(trustedEsmOid);
    }

    @Override
    public void deleteTrustedEsmUserMapping(long trustedEsmUserOid) throws DeleteException, FindException {
        trustedEsmUserManager.delete(trustedEsmUserOid);
    }

    @Override
    public Collection<TrustedEsmUser> getTrustedEsmUserMappings(long trustedEsmId) throws FindException {
        return trustedEsmUserManager.findByEsmId(trustedEsmId);
    }

    private CollectionUpdateProducer<ClusterNodeInfo, FindException> clusterNodesUpdateProducer =
            new CollectionUpdateProducer<ClusterNodeInfo, FindException>(5 * 60 * 1000, 100, null) {
                @Override
                protected Collection<ClusterNodeInfo> getCollection() throws FindException {
                    return clusterInfoManager.retrieveClusterStatus();
                }
            };

    private ClusterNodeInfo clusterNodeInfo( final String nodeid, final String name ) {
        final ClusterNodeInfo node = new ClusterNodeInfo();
        node.setNodeIdentifier( nodeid );
        node.setName( name );
        return node;
    }

    private void publishEvent( final PersistenceEvent pe ) {
        final ApplicationContext context = this.context;
        if ( context != null ) {
            context.publishEvent( pe );
        }
    }

    @Override
    public void setApplicationContext( final ApplicationContext context ) throws BeansException {
        this.context = context;
    }

    private void evaluateAuditSinkPropsStatus(final ClusterProperty clusterProperty, final boolean toBeDeleted) {
        final boolean[] prevPropStatus = new boolean[2];

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void beforeCommit(boolean readOnly) {
                if (ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK.equals(clusterProperty.getName())) {
                    prevPropStatus[0] = auditSinkPropertiesChecker.isInternalAuditSystemEnabled(true);
                } else if (ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID.equals(clusterProperty.getName())) {
                    prevPropStatus[0] = auditSinkPropertiesChecker.isAuditSinkPolicyEnabled();

                    // Also preserve the status of Internal Audit System
                    if (toBeDeleted) {
                        prevPropStatus[1] = auditSinkPropertiesChecker.isInternalAuditSystemEnabled(true);
                    }
                } else if (ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL.equals(clusterProperty.getName())) {
                    prevPropStatus[0] = auditSinkPropertiesChecker.isFallbackToDatabaseIfSinkPolicyFails();
                }
            }

            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    final AdminInfo info = AdminInfo.find(!AuditContextUtils.isSystem());
                    if (info == null) return;

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // If it is not one of three audit sink cluster properties, ignore the task below.
                            if (! ServerConfig.PARAM_AUDIT_SINK_ALWAYS_FALLBACK.equals(clusterProperty.getName()) &&
                                ! ServerConfig.PARAM_AUDIT_SINK_POLICY_GUID.equals(clusterProperty.getName()) &&
                                ! ServerConfig.PARAM_AUDIT_SINK_FALLBACK_ON_FAIL.equals(clusterProperty.getName())) {
                                return;
                            }

                            TransactionTemplate template = new TransactionTemplate(transactionManager);
                            template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
                            template.execute(new TransactionCallbackWithoutResult() {
                                @Override
                                protected void doInTransactionWithoutResult(TransactionStatus status) {
                                    auditSinkPropertiesChecker.checkAndAuditPropsStatus(clusterProperty, prevPropStatus, toBeDeleted, info);
                                }
                            });
                        }
                    }, 10L);
                }
            }
        });
    }

    private ApplicationContext context;
    private final ClusterInfoManager clusterInfoManager;
    private final ServiceUsageManager serviceUsageManager;
    private final ClusterPropertyManager clusterPropertyManager;
    private final GatewayLicenseManager licenseManager;
    private final ServiceMetricsManager serviceMetricsManager;
    private final ServiceMetricsServices serviceMetricsServices;
    private final ServerConfig serverConfig;
    private final ServerAssertionRegistry assertionRegistry;
    private final TrustedEsmManager trustedEsmManager;
    private final TrustedEsmUserManager trustedEsmUserManager;
    private final RbacServices rbacServices;
    private final AuditSinkPropertiesChecker auditSinkPropertiesChecker;
    private final PlatformTransactionManager transactionManager;
    private final Timer timer;

    private final Logger logger = Logger.getLogger(getClass().getName());
}