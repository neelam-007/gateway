package com.l7tech.portal.metrics;

public class ServiceMetricDetail extends BaseMetric{
    private long serviceMetricsId;
    private long mappingValuesId;

    public long getServiceMetricsId() {
        return serviceMetricsId;
    }

    public void setServiceMetricsId(long serviceMetricsId) {
        this.serviceMetricsId = serviceMetricsId;
    }

    public long getMappingValuesId() {
        return mappingValuesId;
    }

    public void setMappingValuesId(long mappingValuesId) {
        this.mappingValuesId = mappingValuesId;
    }
}
