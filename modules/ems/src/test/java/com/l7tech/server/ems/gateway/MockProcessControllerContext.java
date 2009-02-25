package com.l7tech.server.ems.gateway;

import com.l7tech.server.management.api.monitoring.MonitoringApi;
import com.l7tech.server.management.api.monitoring.MonitoringApiStub;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.api.node.NodeManagementApiStub;
import com.l7tech.server.DefaultKey;

/**
 *
 */
public class MockProcessControllerContext extends ProcessControllerContext {
    private NodeManagementApi managementApi = new NodeManagementApiStub();
    private MonitoringApi monitoringApi = new MonitoringApiStub();

    public MockProcessControllerContext() {
        super(null, "testpchostname.example.com", 7111, "testesmid");
    }

    public MockProcessControllerContext(NodeManagementApi managementApi, MonitoringApi monitoringApi) {
        super(null, "testpchostname.example.com", 7111, "testesmid");
        this.managementApi = managementApi;
        this.monitoringApi = monitoringApi;
    }

    public MockProcessControllerContext(final String host, final String esmId) {
        super(null, host, 7111, esmId);
    }

    public MockProcessControllerContext(DefaultKey defaultKey, String host, int processControllerPort, String esmId, NodeManagementApi managementApi, MonitoringApi monitoringApi) {
        super(defaultKey, host, processControllerPort, esmId);
        this.managementApi = managementApi;
        this.monitoringApi = monitoringApi;
    }

    public NodeManagementApi getManagementApi() {
        return managementApi;
    }

    public void setManagementApi(NodeManagementApi managementApi) {
        this.managementApi = managementApi;
    }

    public MonitoringApi getMonitoringApi() {
        return monitoringApi;
    }

    public void setMonitoringApi(MonitoringApi monitoringApi) {
        this.monitoringApi = monitoringApi;
    }
}
