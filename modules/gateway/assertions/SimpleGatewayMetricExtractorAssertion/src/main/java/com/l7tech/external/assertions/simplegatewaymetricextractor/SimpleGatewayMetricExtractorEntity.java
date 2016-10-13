package com.l7tech.external.assertions.simplegatewaymetricextractor;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.search.Dependency;

public class SimpleGatewayMetricExtractorEntity extends GenericEntity {
    private String serviceNameFilter;
    private Goid serviceId;
    private Goid simpleGatewayMetricExtractorEntityId;

    public String getServiceNameFilter() {
        return serviceNameFilter;
    }

    public void setServiceNameFilter(String serviceNameFilter) {
        this.serviceNameFilter = serviceNameFilter;
    }

    @Dependency(methodReturnType = Dependency.MethodReturnType.GOID, type = Dependency.DependencyType.SERVICE)
    public Goid getServiceId() {
        return serviceId;
    }

    public void setServiceId(Goid serviceId) {
        this.serviceId = serviceId;
    }

    @Dependency(methodReturnType = Dependency.MethodReturnType.GOID, type = Dependency.DependencyType.GENERIC)
    public Goid getSimpleGatewayMetricExtractorEntityId() {
        return simpleGatewayMetricExtractorEntityId;
    }

    public void setSimpleGatewayMetricExtractorEntityId(Goid simpleGatewayMetricExtractorEntityId) {
        this.simpleGatewayMetricExtractorEntityId = simpleGatewayMetricExtractorEntityId;
    }
}
