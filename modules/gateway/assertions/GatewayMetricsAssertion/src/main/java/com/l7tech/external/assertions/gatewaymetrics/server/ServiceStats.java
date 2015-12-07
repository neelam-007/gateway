package com.l7tech.external.assertions.gatewaymetrics.server;

import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.gateway.common.service.MetricsSummaryBin;
import com.l7tech.objectmodel.Goid;

import java.util.List;
import java.util.Map;

/**
 * Data structure to keep track of Service Statistics for Gateway Metrics Assertion
 */
public class ServiceStats {

    private ServiceUsage[] serviceUsages;
    private Map<Goid, List<MetricsSummaryBin>> serviceMetricsMap;

    public ServiceUsage[] getServiceUsages() {
        return serviceUsages;
    }

    public void setServiceUsages(ServiceUsage[] serviceUsages) {
        this.serviceUsages = serviceUsages;
    }

    public void setServiceMetricsMap(Map<Goid, List<MetricsSummaryBin>> serviceMetricsMap) {
        this.serviceMetricsMap = serviceMetricsMap;
    }

    public Map<Goid, List<MetricsSummaryBin>> getServiceMetricsMap() {
        return serviceMetricsMap;
    }
}
