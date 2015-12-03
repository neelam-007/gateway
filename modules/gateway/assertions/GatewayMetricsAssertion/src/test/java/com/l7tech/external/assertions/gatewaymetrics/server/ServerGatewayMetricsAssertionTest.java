package com.l7tech.external.assertions.gatewaymetrics.server;

import com.l7tech.external.assertions.gatewaymetrics.GatewayMetricsAssertion;
import com.l7tech.external.assertions.gatewaymetrics.IntervalTimeUnit;
import com.l7tech.external.assertions.gatewaymetrics.IntervalType;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ServiceUsageManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.service.ServiceMetricsManager;
import com.l7tech.server.service.ServiceMetricsServices;
import com.l7tech.util.GoidUpgradeMapper;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the GatewayMetricsAssertion.
 */
public class ServerGatewayMetricsAssertionTest {

    private ApplicationContext applicationContext;
    private PolicyEnforcementContext peCtx;
    private GatewayMetricsAssertion assertion;
    private Collection<ClusterNodeInfo> clusterNodeInfos;
    private ClusterInfoManager clusterInfoManager;
    private ServiceMetricsManager metricsManager;
    private ServiceMetricsServices metricsServices;
    private ServiceUsageManager statsManager;
    private ServiceManager serviceManager;
    private PublishedService service;
    private String serviceName = "http://www.abundanttech.com/webservices/deadoralive";
    private String[] variablesUsed = new String[]{"clusterVar", "serviceVar", "resolutionVar"};

    @Before
    public void setUp() throws FindException {
        // Get the spring app context
        applicationContext = mock(ApplicationContext.class);
        peCtx = mock(PolicyEnforcementContext.class);
        assertion = mock(GatewayMetricsAssertion.class);
        clusterInfoManager = mock(ClusterInfoManager.class);
        metricsManager = mock(ServiceMetricsManager.class);
        metricsServices = mock(ServiceMetricsServices.class);
        statsManager = mock(ServiceUsageManager.class);
        serviceManager = mock(ServiceManager.class);
        service = mock(PublishedService.class);

        clusterNodeInfos = new ArrayList<ClusterNodeInfo>() {{
            add(new ClusterNodeInfo() {{
                setNodeIdentifier("TestNode-main");
                setMac("00:0c:11:f0:43:01");
                setName("SSG1");
                setAddress("192.128.1.100");
                setAvgLoad(1.5);
                setBootTime(System.currentTimeMillis());
            }});
        }};

        when(applicationContext.getBean("clusterInfoManager")).thenReturn(clusterInfoManager);
        when(applicationContext.getBean("serviceMetricsManager")).thenReturn(metricsManager);
        when(applicationContext.getBean("serviceMetricsServices")).thenReturn(metricsServices);
        when(applicationContext.getBean("serviceUsageManager")).thenReturn(statsManager);
        when(applicationContext.getBean("serviceManager")).thenReturn(serviceManager);

        when(clusterInfoManager.retrieveClusterStatus()).thenReturn(clusterNodeInfos);

        when(metricsServices.getFineInterval()).thenReturn(600000);
        when(metricsServices.isEnabled()).thenReturn(false);
        when(serviceManager.findByUniqueName(serviceName)).thenReturn(service);
        when(service.getGoid()).thenReturn(PublishedService.DEFAULT_GOID);

        when(assertion.getVariablesUsed()).thenReturn(variablesUsed);
        when(assertion.getClusterNodeVariable()).thenReturn("${clusterVar}");
        when(assertion.getPublishedServiceVariable()).thenReturn("${serviceVar}");
        when(assertion.getResolutionVariable()).thenReturn("${resolutionVar}");
        when(assertion.getIntervalType()).thenReturn(IntervalType.MOST_RECENT);
    }

    @Test
    public void testMetrics() throws Exception {
        when(assertion.getClusterNodeId()).thenReturn(null);
        when(assertion.getPublishedServiceGoid()).thenReturn(GoidUpgradeMapper.mapOid(EntityType.SERVICE, -1L));
        when(assertion.getResolution()).thenReturn(MetricsBin.RES_DAILY);
        when(assertion.getIntervalType()).thenReturn(IntervalType.MOST_RECENT);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testDefinedClusterName() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("clustervar", "SSG1");
        vars.put("servicevar", "ALL_SERVICES");
        vars.put("resolutionvar", "DAILY");

        when(peCtx.getVariableMap(eq(variablesUsed), any(Audit.class))).thenReturn(vars);
        when(assertion.getUseVariables()).thenReturn(true);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testDefinedServiceName() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("clustervar", "ALL_NODES");
        vars.put("servicevar", "http://www.abundanttech.com/webservices/deadoralive");
        vars.put("resolutionvar", "DAILY");

        when(peCtx.getVariableMap(eq(variablesUsed), any(Audit.class))).thenReturn(vars);
        when(assertion.getUseVariables()).thenReturn(true);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testDefinedDailyResolutionName() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("clustervar", "ALL_NODES");
        vars.put("servicevar", "ALL_SERVICES");
        vars.put("resolutionvar", "DAILY");

        when(peCtx.getVariableMap(eq(variablesUsed), any(Audit.class))).thenReturn(vars);
        when(assertion.getUseVariables()).thenReturn(true);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testDefinedClusterName_NotContextVariable() throws Exception {
        when(assertion.getUseVariables()).thenReturn(true);
        when(assertion.getClusterNodeVariable()).thenReturn("SSG1");
        when(assertion.getPublishedServiceVariable()).thenReturn("ALL_SERVICES");
        when(assertion.getResolutionVariable()).thenReturn("DAILY");

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testDefinedServiceName_NotContextVariable() throws Exception {
        when(assertion.getUseVariables()).thenReturn(true);
        when(assertion.getClusterNodeVariable()).thenReturn("ALL_NODES");
        when(assertion.getPublishedServiceVariable()).thenReturn("http://www.abundanttech.com/webservices/deadoralive");
        when(assertion.getResolutionVariable()).thenReturn("DAILY");

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testDefinedDailyResolutionName_NotContextVariable() throws Exception {
        when(assertion.getUseVariables()).thenReturn(true);
        when(assertion.getClusterNodeVariable()).thenReturn("ALL_NODES");
        when(assertion.getPublishedServiceVariable()).thenReturn("ALL_SERVICES");
        when(assertion.getResolutionVariable()).thenReturn("DAILY");

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testDefinedHourlyResolutionName() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("clustervar", "ALL_NODES");
        vars.put("servicevar", "ALL_SERVICES");
        vars.put("resolutionvar", "HOURLY");

        when(peCtx.getVariableMap(eq(variablesUsed), any(Audit.class))).thenReturn(vars);
        when(assertion.getUseVariables()).thenReturn(true);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testDefinedFineResolutionName() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("clustervar", "ALL_NODES");
        vars.put("servicevar", "ALL_SERVICES");
        vars.put("resolutionvar", "FINE");

        when(peCtx.getVariableMap(eq(variablesUsed), any(Audit.class))).thenReturn(vars);
        when(assertion.getUseVariables()).thenReturn(true);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testGetRecentNumberOfIntervals_FineResolution() throws Exception {
        when(assertion.getUseVariables()).thenReturn(false);
        when(assertion.getClusterNodeId()).thenReturn(null);
        when(assertion.getPublishedServiceGoid()).thenReturn(GoidUpgradeMapper.mapOid(EntityType.SERVICE, -1L));
        when(assertion.getResolution()).thenReturn(MetricsBin.RES_FINE);
        when(assertion.getIntervalType()).thenReturn(IntervalType.RECENT_NUMBER_OF_INTERVALS);
        when(assertion.getNumberOfRecentIntervals()).thenReturn("10");

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testGetRecentNumberOfIntervals_HourlyResolution() throws Exception {
        when(assertion.getUseVariables()).thenReturn(false);
        when(assertion.getClusterNodeId()).thenReturn(null);
        when(assertion.getPublishedServiceGoid()).thenReturn(GoidUpgradeMapper.mapOid(EntityType.SERVICE, -1L));
        when(assertion.getResolution()).thenReturn(MetricsBin.RES_HOURLY);
        when(assertion.getIntervalType()).thenReturn(IntervalType.RECENT_NUMBER_OF_INTERVALS);
        when(assertion.getNumberOfRecentIntervals()).thenReturn("10");

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testGetRecentNumberOfIntervals_DailyResolution() throws Exception {
        when(assertion.getUseVariables()).thenReturn(false);
        when(assertion.getClusterNodeId()).thenReturn(null);
        when(assertion.getPublishedServiceGoid()).thenReturn(GoidUpgradeMapper.mapOid(EntityType.SERVICE, -1L));
        when(assertion.getResolution()).thenReturn(MetricsBin.RES_DAILY);
        when(assertion.getIntervalType()).thenReturn(IntervalType.RECENT_NUMBER_OF_INTERVALS);
        when(assertion.getNumberOfRecentIntervals()).thenReturn("10");

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testGetRecentNumberOfIntervals_ContextVariables() throws Exception {
        when(assertion.getUseVariables()).thenReturn(false);
        when(assertion.getClusterNodeId()).thenReturn(null);
        when(assertion.getPublishedServiceGoid()).thenReturn(GoidUpgradeMapper.mapOid(EntityType.SERVICE, -1L));
        when(assertion.getResolution()).thenReturn(MetricsBin.RES_FINE);
        when(assertion.getIntervalType()).thenReturn(IntervalType.RECENT_NUMBER_OF_INTERVALS);
        when(assertion.getNumberOfRecentIntervals()).thenReturn("${number_of_intervals}");

        Map<String, Object> vars = new HashMap<>();
        vars.put("number_of_intervals", "10");
        when(peCtx.getVariableMap(eq(variablesUsed), any(Audit.class))).thenReturn(vars);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testGetRecentIntervalsWithinTimePeriod_FineResolution() throws Exception {
        when(assertion.getUseVariables()).thenReturn(false);
        when(assertion.getClusterNodeId()).thenReturn(null);
        when(assertion.getPublishedServiceGoid()).thenReturn(GoidUpgradeMapper.mapOid(EntityType.SERVICE, -1L));
        when(assertion.getResolution()).thenReturn(MetricsBin.RES_FINE);
        when(assertion.getIntervalType()).thenReturn(IntervalType.RECENT_INTERVALS_WITHIN_TIME_PERIOD);
        when(assertion.getNumberOfRecentIntervalsWithinTimePeriod()).thenReturn("10");
        when(assertion.getIntervalTimeUnit()).thenReturn(IntervalTimeUnit.MINUTES);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testGetRecentIntervalsWithinTimePeriod_HourlyResolution() throws Exception {
        when(assertion.getUseVariables()).thenReturn(false);
        when(assertion.getClusterNodeId()).thenReturn(null);
        when(assertion.getPublishedServiceGoid()).thenReturn(GoidUpgradeMapper.mapOid(EntityType.SERVICE, -1L));
        when(assertion.getResolution()).thenReturn(MetricsBin.RES_HOURLY);
        when(assertion.getIntervalType()).thenReturn(IntervalType.RECENT_INTERVALS_WITHIN_TIME_PERIOD);
        when(assertion.getNumberOfRecentIntervalsWithinTimePeriod()).thenReturn("60");
        when(assertion.getIntervalTimeUnit()).thenReturn(IntervalTimeUnit.HOURS);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testGetRecentIntervalsWithinTimePeriod_DailyResolution() throws Exception {
        when(assertion.getUseVariables()).thenReturn(false);
        when(assertion.getClusterNodeId()).thenReturn(null);
        when(assertion.getPublishedServiceGoid()).thenReturn(GoidUpgradeMapper.mapOid(EntityType.SERVICE, -1L));
        when(assertion.getResolution()).thenReturn(MetricsBin.RES_DAILY);
        when(assertion.getIntervalType()).thenReturn(IntervalType.RECENT_INTERVALS_WITHIN_TIME_PERIOD);
        when(assertion.getNumberOfRecentIntervalsWithinTimePeriod()).thenReturn("60");
        when(assertion.getIntervalTimeUnit()).thenReturn(IntervalTimeUnit.DAYS);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testGetRecentIntervalsWithinTimePeriod_ContextVariables() throws Exception {
        when(assertion.getUseVariables()).thenReturn(false);
        when(assertion.getClusterNodeId()).thenReturn(null);
        when(assertion.getPublishedServiceGoid()).thenReturn(GoidUpgradeMapper.mapOid(EntityType.SERVICE, -1L));
        when(assertion.getResolution()).thenReturn(MetricsBin.RES_FINE);
        when(assertion.getIntervalType()).thenReturn(IntervalType.RECENT_INTERVALS_WITHIN_TIME_PERIOD);
        when(assertion.getNumberOfRecentIntervalsWithinTimePeriod()).thenReturn("${interval_time_period}");
        when(assertion.getIntervalTimeUnit()).thenReturn(IntervalTimeUnit.SECONDS);

        Map<String, Object> vars = new HashMap<>();
        vars.put("interval_time_period", "600");
        when(peCtx.getVariableMap(eq(variablesUsed), any(Audit.class))).thenReturn(vars);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testUndefinedClusterName() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("clustervar", "GatewayClusterNode0001");
        vars.put("servicevar", "ALL_SERVICES");
        vars.put("resolutionvar", "DAILY");

        when(peCtx.getVariableMap(eq(variablesUsed), any(Audit.class))).thenReturn(vars);
        when(assertion.getUseVariables()).thenReturn(true);


        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.FAILED, status);    // It's supposed to fail.  That's a pass.
    }

    @Test
    public void testUndefinedServiceName() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("clustervar", "ALL_NODES");
        vars.put("servicevar", "Service-12378asdfzxcv@#IUA23@)#(qlKJASDFOIu!@JISFAF;L");
        vars.put("resolutionvar", "DAILY");

        when(peCtx.getVariableMap(eq(variablesUsed), any(Audit.class))).thenReturn(vars);
        when(assertion.getUseVariables()).thenReturn(true);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.FAILED, status);    // It's supposed to fail.  That's a pass.
    }

    @Test
    public void testUndefinedResolutionName() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("clustervar", "ALL_NODES");
        vars.put("servicevar", "ALL_SERVICES");
        vars.put("resolutionvar", "RIGHT_NOW");

        when(peCtx.getVariableMap(eq(variablesUsed), any(Audit.class))).thenReturn(vars);
        when(assertion.getUseVariables()).thenReturn(true);

        ServerGatewayMetricsAssertion server = new ServerGatewayMetricsAssertion(assertion, applicationContext);
        AssertionStatus status = server.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.FAILED, status);    // It's supposed to fail.  That's a pass.
    }


}