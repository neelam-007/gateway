package com.l7tech.cluster;

import com.l7tech.gateway.common.License;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.emstrust.TrustedEms;
import com.l7tech.gateway.common.emstrust.TrustedEmsUser;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterPropertyDescriptor;
import com.l7tech.util.CollectionUpdate;
import com.l7tech.util.CollectionUpdateProducer;
import com.l7tech.objectmodel.*;
import com.l7tech.server.GatewayFeatureSets;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/*
 * Test stub for ClusterStatusAdmin interface
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */

public class ClusterStatusAdminStub implements ClusterStatusAdmin {
    private static License license = null;

    private CollectionUpdateProducer<ClusterNodeInfo, FindException> clusterNodesUpdateProducer =
            new CollectionUpdateProducer<ClusterNodeInfo, FindException>(5000, 10, null) {
                protected Collection<ClusterNodeInfo> getCollection() throws FindException {
                    return Arrays.asList(getClusterStatus());
                }
            };

    public String getCurrentClusterTimeZone() {
        throw new UnsupportedOperationException();
    }

    public boolean isCluster() {
        return false;
    }
    
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

    public CollectionUpdate<ClusterNodeInfo> getClusterNodesUpdate(int oldVersionID) throws FindException {
        return clusterNodesUpdateProducer.createUpdate(oldVersionID);
    }

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

        s1.setServiceid(1234); s1.setNodeid("00:0c:11:f0:43:01"); s1.setRequests(1000); s1.setAuthorized(998); s1.setCompleted(998);
        s2.setServiceid(1235); s2.setNodeid("00:0c:11:f0:43:01"); s2.setRequests(500); s2.setAuthorized(497); s2.setCompleted(497);
        s3.setServiceid(1234); s3.setNodeid("00:0c:11:f0:43:02"); s3.setRequests(1100); s3.setAuthorized(1008); s3.setCompleted(1008);
        s4.setServiceid(1235); s4.setNodeid("00:0c:11:f0:43:02"); s4.setRequests(600); s4.setAuthorized(567); s4.setCompleted(567);
        s5.setServiceid(1234); s5.setNodeid("00:0c:11:f0:43:04"); s5.setRequests(1200); s5.setAuthorized(1158); s5.setCompleted(1158);
        s6.setServiceid(1235); s6.setNodeid("00:0c:11:f0:43:04"); s6.setRequests(700); s6.setAuthorized(689); s6.setCompleted(689);
        s7.setServiceid(1234); s7.setNodeid("00:0c:11:f0:43:05"); s7.setRequests(1300); s7.setAuthorized(1230); s7.setCompleted(1230);
        s8.setServiceid(1235); s8.setNodeid("00:0c:11:f0:43:05"); s8.setRequests(800); s8.setAuthorized(755); s8.setCompleted(755);
        s9.setServiceid(1234); s9.setNodeid("00:0c:11:f0:43:06"); s9.setRequests(1400); s9.setAuthorized(1298); s9.setCompleted(1298);
        s10.setServiceid(1235); s10.setNodeid("00:0c:11:f0:43:06"); s10.setRequests(900); s10.setAuthorized(905); s10.setCompleted(905);

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

    public void changeNodeName(String nodeid, String newName) throws UpdateException {
        throw new UnsupportedOperationException();
    }

    public void removeStaleNode(String nodeid) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    public java.util.Date getCurrentClusterSystemTime() {
        return Calendar.getInstance().getTime();
    }

    public String getSelfNodeName() {
        return "No name from test stub";
    }

    public Collection<ClusterProperty> getAllProperties() throws FindException {
        throw new UnsupportedOperationException();
    }

    public Map getKnownProperties() {
        throw new UnsupportedOperationException();        
    }

    public Collection<ClusterPropertyDescriptor> getAllPropertyDescriptors() {
        throw new UnsupportedOperationException();
    }

    public ClusterProperty findPropertyByName(String key) throws FindException {
        throw new UnsupportedOperationException();
    }

    public long saveProperty(ClusterProperty clusterProperty) throws SaveException, UpdateException, DeleteException {
        throw new UnsupportedOperationException();
    }

    public void deleteProperty(ClusterProperty clusterProperty) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    public License getCurrentLicense() throws InvalidLicenseException {
        return license;
    }

    public long getLicenseExpiryWarningPeriod() {
        return 0;
    }

    public void installNewLicense(String newLicenseXml) throws InvalidLicenseException {
        try {
            license = new License(newLicenseXml, null, GatewayFeatureSets.getFeatureSetExpander());
        } catch (Exception e) {
            throw new InvalidLicenseException(e.getMessage(), e);
        }
    }

    public boolean isMetricsEnabled() {
        throw new UnsupportedOperationException();
    }

    public int getMetricsFineInterval() {
        throw new UnsupportedOperationException();
    }

    public Collection<MetricsSummaryBin> summarizeByPeriod(final String nodeId, final long[] serviceOids, final Integer resolution, final Long minPeriodStart, final Long maxPeriodStart, final boolean includeEmpty) throws FindException {
        throw new UnsupportedOperationException();
    }

    public Collection<MetricsSummaryBin> summarizeLatestByPeriod(final String nodeId, final long[] serviceOids, final Integer resolution, final long duration, final boolean includeEmpty) throws FindException {
        throw new UnsupportedOperationException();
    }

    public MetricsSummaryBin summarizeLatest(final String nodeId, final long[] serviceOids, final int resolution, final int duration, final boolean includeEmpty) throws FindException {
        throw new UnsupportedOperationException();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Collection<ModuleInfo> getAssertionModuleInfo() {
        return Collections.emptyList();
    }

    public String getHardwareCapability(String capability) {
        return null;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<TrustedEms> getTrustedEmsInstances() throws FindException {
        return Collections.emptyList();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Secured(types = EntityType.TRUSTED_EMS, stereotype = MethodStereotype.DELETE_BY_ID, relevantArg = 0)
    public void deleteTrustedEmsInstance(long trustedEmsOid) {
        throw new UnsupportedOperationException();
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    @Secured(types = EntityType.TRUSTED_EMS_USER, stereotype = MethodStereotype.DELETE_BY_ID, relevantArg = 0)
    public void deleteTrustedEmsUserMapping(long trustedEmsUserOid) {
        throw new UnsupportedOperationException();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<TrustedEmsUser> getTrustedEmsUserMappings(long trustedEmsId) throws FindException {
        return Collections.emptyList();
    }
}
