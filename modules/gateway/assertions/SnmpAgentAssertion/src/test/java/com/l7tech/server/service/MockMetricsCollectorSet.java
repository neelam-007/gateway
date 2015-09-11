package com.l7tech.server.service;

import com.l7tech.gateway.common.service.ServiceState;
import com.l7tech.objectmodel.Goid;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: rseminoff
 * Date: 5/15/12
 * Time: 2:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class MockMetricsCollectorSet extends ServiceMetrics.MetricsCollectorSet {
    MockMetricsCollectorSet(final Goid serviceOid, final ServiceState serviceState, final ServiceMetrics.MetricsCollector summaryMetrics, final Map<ServiceMetrics.MetricsDetailKey, ServiceMetrics.MetricsCollector> detailMetrics) {
        super(serviceOid, serviceState, summaryMetrics, detailMetrics);
        System.out.println("*** CALL *** MockMetricsCollectorSet: constructor(long, ServiceState, MetricsCollector, Map)");
    }

    MockMetricsCollectorSet(final Goid serviceOid, final long startTime, final long endTime, final ServiceMetrics.MetricsCollector summaryMetrics, final Map<ServiceMetrics.MetricsDetailKey, ServiceMetrics.MetricsCollector> detailMetrics) {
        super(serviceOid, startTime, endTime, summaryMetrics, detailMetrics);
        System.out.println("*** CALL *** MockMetricsCollectorSet: constructor(long, long, long, MetricsCollector, Map)");
    }
}
