package com.l7tech.gateway.common.cluster;

import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.gateway.common.licensing.*;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.objectmodel.*;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

/*
 * Test stub for ClusterStatusAdmin interface
 */
public class ClusterStatusAdminStub implements ClusterStatusAdmin {
    private static CompositeLicense license = null;

    private CollectionUpdateProducer<ClusterNodeInfo, FindException> clusterNodesUpdateProducer =
            new CollectionUpdateProducer<ClusterNodeInfo, FindException>(5000, 10, null) {
                @Override
                protected Collection<ClusterNodeInfo> getCollection() throws FindException {
                    return Arrays.asList(getClusterStatus());
                }
            };

    @Override
    public String getCurrentClusterTimeZone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCluster() {
        return false;
    }
    
    @Override
    public ClusterNodeInfo[] getClusterStatus() {

        ClusterNodeInfo[] cluster = new ClusterNodeInfo[8];
        ClusterNodeInfo c1 = new ClusterNodeInfo();
        ClusterNodeInfo c2 = new ClusterNodeInfo();
        ClusterNodeInfo c3 = new ClusterNodeInfo();
        ClusterNodeInfo c4 = new ClusterNodeInfo();
        ClusterNodeInfo c5 = new ClusterNodeInfo();
        ClusterNodeInfo c6 = new ClusterNodeInfo();
        ClusterNodeInfo c7 = new ClusterNodeInfo();
        ClusterNodeInfo c8 = new ClusterNodeInfo();

        c1.setMac("00:0c:11:f0:43:01"); c1.setName("SSG1"); c1.setAddress("192.128.1.100"); c1.setAvgLoad(1.5); c1.setBootTime(1072746384);
        c2.setMac("00:0c:11:f0:43:02");c2.setName("SSG2"); c2.setAddress("192.128.1.101"); c2.setAvgLoad(1.8); c2.setBootTime(1072656394);
        c3.setMac("00:0c:11:f0:43:03");c3.setName("SSG3"); c3.setAddress("192.128.1.102"); c3.setAvgLoad(0); c3.setBootTime(1072746404);
        c4.setMac("00:0c:11:f0:43:04");c4.setName("SSG4"); c4.setAddress("192.128.2.10"); c4.setAvgLoad(1.1); c4.setBootTime(1072776414);
        c5.setMac("00:0c:11:f0:43:05");c5.setName("SSG5"); c5.setAddress("192.128.2.11"); c5.setAvgLoad(2.1); c5.setBootTime(1072746484);
        c6.setMac("00:0c:11:f0:43:06");c6.setName("SSG6"); c6.setAddress("192.128.3.1"); c6.setAvgLoad(0.8); c6.setBootTime(1072736464);
        c7.setMac("00:0c:11:f0:43:07");c7.setName("SSG7"); c7.setAddress("192.128.3.2"); c7.setAvgLoad(0); c7.setBootTime(1072808010);
        c8.setMac("00:0c:11:f0:43:08");c8.setName("SSG8"); c8.setAddress("192.128.3.3"); c8.setAvgLoad(0); c8.setBootTime(1072808325);

        cluster[0] = c1;
        cluster[1] = c2;
        cluster[2] = c3;
        cluster[3] = c4;
        cluster[4] = c5;
        cluster[5] = c6;
        cluster[6] = c7;
        cluster[7] = c8;
        return cluster;
    }

    @Override
    public CollectionUpdate<ClusterNodeInfo> getClusterNodesUpdate(int oldVersionID) throws FindException {
        return clusterNodesUpdateProducer.createUpdate(oldVersionID);
    }

    @Override
    public ServiceUsage[] getServiceUsage() {
        ServiceUsage[] serviceUsage = new ServiceUsage[10];

        ServiceUsage s1 = new ServiceUsage();
        ServiceUsage s2 = new ServiceUsage();
        ServiceUsage s3 = new ServiceUsage();
        ServiceUsage s4 = new ServiceUsage();
        ServiceUsage s5 = new ServiceUsage();
        ServiceUsage s6 = new ServiceUsage();
        ServiceUsage s7 = new ServiceUsage();
        ServiceUsage s8 = new ServiceUsage();
        ServiceUsage s9 = new ServiceUsage();
        ServiceUsage s10 = new ServiceUsage();

        s1.setServiceid(new Goid(0,1234)); s1.setNodeid("00:0c:11:f0:43:01"); s1.setRequests(1000); s1.setAuthorized(998); s1.setCompleted(998);
        s2.setServiceid(new Goid(0,1235)); s2.setNodeid("00:0c:11:f0:43:01"); s2.setRequests(500); s2.setAuthorized(497); s2.setCompleted(497);
        s3.setServiceid(new Goid(0,1234)); s3.setNodeid("00:0c:11:f0:43:02"); s3.setRequests(1100); s3.setAuthorized(1008); s3.setCompleted(1008);
        s4.setServiceid(new Goid(0,1235)); s4.setNodeid("00:0c:11:f0:43:02"); s4.setRequests(600); s4.setAuthorized(567); s4.setCompleted(567);
        s5.setServiceid(new Goid(0,1234)); s5.setNodeid("00:0c:11:f0:43:04"); s5.setRequests(1200); s5.setAuthorized(1158); s5.setCompleted(1158);
        s6.setServiceid(new Goid(0,1235)); s6.setNodeid("00:0c:11:f0:43:04"); s6.setRequests(700); s6.setAuthorized(689); s6.setCompleted(689);
        s7.setServiceid(new Goid(0,1234)); s7.setNodeid("00:0c:11:f0:43:05"); s7.setRequests(1300); s7.setAuthorized(1230); s7.setCompleted(1230);
        s8.setServiceid(new Goid(0,1235)); s8.setNodeid("00:0c:11:f0:43:05"); s8.setRequests(800); s8.setAuthorized(755); s8.setCompleted(755);
        s9.setServiceid(new Goid(0,1234)); s9.setNodeid("00:0c:11:f0:43:06"); s9.setRequests(1400); s9.setAuthorized(1298); s9.setCompleted(1298);
        s10.setServiceid(new Goid(0,1235)); s10.setNodeid("00:0c:11:f0:43:06"); s10.setRequests(900); s10.setAuthorized(905); s10.setCompleted(905);

        serviceUsage[0] = s1;
        serviceUsage[1] = s2;
        serviceUsage[2] = s3;
        serviceUsage[3] = s4;
        serviceUsage[4] = s5;
        serviceUsage[5] = s6;
        serviceUsage[6] = s7;
        serviceUsage[7] = s8;
        serviceUsage[8] = s9;
        serviceUsage[9] = s10;

        return serviceUsage;
    }

    @Override
    public void changeNodeName(String nodeid, String newName) throws UpdateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeStaleNode(String nodeid) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public java.util.Date getCurrentClusterSystemTime() {
        return Calendar.getInstance().getTime();
    }

    @Override
    public String getSelfNodeName() {
        return "No name from test stub";
    }

    @Override
    public Collection<ClusterProperty> getAllProperties() throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map getKnownProperties() {
        throw new UnsupportedOperationException();        
    }

    @Override
    public Collection<ClusterPropertyDescriptor> getAllPropertyDescriptors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClusterProperty findPropertyByName(String key) throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Goid saveProperty(ClusterProperty clusterProperty) throws SaveException, UpdateException, DeleteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProperty(ClusterProperty clusterProperty) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompositeLicense getCompositeLicense() {
        return license;
    }

    @Override
    public long getLicenseExpiryWarningPeriod() {
        return 0;
    }

    @Override
    public FeatureLicense createLicense(LicenseDocument document) throws InvalidLicenseException {
        throw new InvalidLicenseException("Not implemented");
    }

    @Override
    public void validateLicense(FeatureLicense license) throws InvalidLicenseException {
        throw new InvalidLicenseException("Not implemented");
    }

    @Override
    public void installLicense(FeatureLicense license) throws LicenseInstallationException {
        throw new LicenseInstallationException("Not implemented");
    }

    @Override
    public void uninstallLicense(FeatureLicense license) throws LicenseRemovalException {
        throw new LicenseRemovalException("Not implemented");
    }

    @Override
    public boolean isMetricsEnabled() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMetricsFineInterval() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<MetricsSummaryBin> summarizeByPeriod(final String nodeId, final Goid[] serviceOids, final Integer resolution, final Long minPeriodStart, final Long maxPeriodStart, final boolean includeEmpty) throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<MetricsSummaryBin> summarizeLatestByPeriod(final String nodeId, final Goid[] serviceOids, final Integer resolution, final long duration, final boolean includeEmpty) throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MetricsSummaryBin summarizeLatest(final String nodeId, final Goid[] serviceOids, final int resolution, final int duration, final boolean includeEmpty) throws FindException {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public List<String> getConfiguredDateFormats() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<Pair<String, Pattern>> getAutoDateFormats() {
        return Collections.emptyList();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Collection<ModuleInfo> getAssertionModuleInfo() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Pair<String, String>> getExtensionInterfaceInstances() {
        return Collections.emptyList();
    }

    @Override
    public boolean isExtensionInterfaceAvailable(String interfaceClassname, String instanceIdentifier) {
        return false;
    }

    @Override
    public Either<Throwable,Option<Object>> invokeExtensionMethod(String interfaceClassname, String targetObjectId, String methodName, Class[] parameterTypes, Object[] arguments) throws ClassNotFoundException, NoSuchMethodException {
        throw new ClassNotFoundException("Not implemented in stub");
    }

    @Override
    public String getHardwareCapability(String capability) {
        return null;
    }

    @Override
    public Serializable getHardwareCapabilityProperty(String capability, String property) throws NoSuchCapabilityException, NoSuchPropertyException {
        throw new NoSuchCapabilityException();
    }

    @Override
    public void putHardwareCapabilityProperty(String capability, String property, Serializable value) throws NoSuchCapabilityException, NoSuchPropertyException, ClassCastException, IllegalArgumentException {
        throw new NoSuchCapabilityException();
    }

    @Override
    public void testHardwareTokenAvailability(String capability, int slotNum, char[] tokenPin) throws NoSuchCapabilityException {
        throw new NoSuchCapabilityException();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<TrustedEsm> getTrustedEsmInstances() throws FindException {
        return Collections.emptyList();
    }

    @Override
    public void deleteTrustedEsmInstance(long trustedEsmOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteTrustedEsmUserMapping(long trustedEsmUserOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<TrustedEsmUser> getTrustedEsmUserMappings(long trustedEsmId) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public FailoverStrategy[] getAllFailoverStrategies() {
        return FailoverStrategyFactory.getFailoverStrategyNames();
    }

}
