package com.l7tech.server.service;

import com.l7tech.external.assertions.snmpagent.server.MockSnmpValues;
import com.l7tech.gateway.common.service.MetricsBin;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.ServiceState;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;

import java.util.*;


/**
 * User: rseminoff
 * Date: 5/15/12
 */
public class MockServiceMetricsManager implements ServiceMetricsManager {

    @Override
    public Collection<MetricsSummaryBin> summarizeByPeriod(String nodeId, Goid[] serviceGoids, Integer resolution, Long minPeriodStart, Long maxPeriodStart, boolean includeEmpty) throws FindException {
        System.out.println("*** CALL *** MockServiceMetricsManager: summarizeByPeriod()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<Goid, MetricsSummaryBin> summarizeByService(String nodeId, Integer resolution, Long minPeriodStart, Long maxPeriodStart, Goid[] serviceGoids, boolean includeEmpty) throws FindException {
        System.out.println("*** CALL *** MockServiceMetricsManager: summarizeByService()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MetricsSummaryBin summarizeLatest(final String clusterNodeId, final Goid[] serviceGoids, int resolution, int duration, boolean includeEmpty) throws FindException {

        ArrayList<MetricsBin> theBin = new ArrayList<MetricsBin>() {{
           add(new MetricsBin() {{
               setSumBackendResponseTime(getSumBackendResponseTime() + MockSnmpValues.TEST_MIN_BACKEND_POLICY_RESPONSE_FINE * MockSnmpValues.TEST_REQUESTS_RECEIVED_FINE);
               setSumFrontendResponseTime(getSumFrontendResponseTime() + MockSnmpValues.TEST_MIN_FRONTEND_POLICY_RESPONSE_FINE * MockSnmpValues.TEST_REQUESTS_RECEIVED_FINE);
               setNumAttemptedRequest(MockSnmpValues.TEST_REQUESTS_RECEIVED_FINE);
               setNumAuthorizedRequest(MockSnmpValues.TEST_REQUESTS_AUTHORIZED_UNPROCESSED_FINE);
               setNumCompletedRequest(MockSnmpValues.TEST_REQUESTS_AUTHORIZED_PROCESSED_FINE);

           }});
        }};

        MetricsSummaryBin summary = new MetricsSummaryBin(theBin);
        final long summaryPeriodEnd = MetricsBin.periodStartFor(resolution, MockSnmpValues.FINE_MS_VALUE, System.currentTimeMillis());
        final long summaryPeriodStart = summaryPeriodEnd - duration;

        summary.setPeriodStart(summaryPeriodStart);
        summary.setInterval(duration);
        summary.setEndTime(summaryPeriodEnd);

        return summary;


    }

    @Override
    public ServiceState getCreatedOrUpdatedServiceState(Goid goid) throws FindException {
        System.out.println("*** CALL *** MockServiceMetricsManager: getCreatedOrUpdatedServiceState()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Integer delete(long oldestSurvivor, int resolution) {
        System.out.println("*** CALL *** MockServiceMetricsManager: delete()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<ServiceHeader> findAllServiceHeaders() throws FindException {
        System.out.println("*** CALL *** MockServiceMetricsManager: findAllServiceHeaders()");
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doFlush(ServiceMetrics.MetricsCollectorSet metricsSet, MetricsBin bin) {
        System.out.println("*** CALL *** MockServiceMetricsManager: doFlush()");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createHourlyBin(Goid serviceGoid, ServiceState state, long startTime) throws SaveException {
        System.out.println("*** CALL *** MockServiceMetricsManager: createHourlyBin()");
//To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createDailyBin(Goid serviceGoid, ServiceState state, long startTime) throws SaveException {
        System.out.println("*** CALL *** MockServiceMetricsManager: createDailyBin()");
//To change body of implemented methods use File | Settings | File Templates.
    }

}
