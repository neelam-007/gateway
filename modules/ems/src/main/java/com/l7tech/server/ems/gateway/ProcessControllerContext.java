package com.l7tech.server.ems.gateway;


import com.l7tech.server.DefaultKey;
import com.l7tech.server.management.api.monitoring.MonitoringApi;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.util.SyspropUtil;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

/**
 * GatewayContext provides access from the ESM to APIs offered by a single Gateway node and its Process Controller.
 */
public class ProcessControllerContext extends ApiContext {
    private static final String PROP_CONTROLLER_URL = "com.l7tech.esm.controllerUrl";
    private static final String PROP_MONITORING_URL = "com.l7tech.esm.monitoringUrl";
    private static final String CONTROLLER_URL = SyspropUtil.getString(PROP_CONTROLLER_URL, "https://{0}:{1}/services/nodeManagementApi");
    private static final String MONITORING_URL = SyspropUtil.getString(PROP_MONITORING_URL, "https://{0}:{1}/services/monitoringApi");

    private final String controllerUrl;
    private final String monitoringUrl;
    private final AtomicReference<NodeManagementApi> managementApi = new AtomicReference<NodeManagementApi>();
    private final AtomicReference<MonitoringApi> monitoringApi = new AtomicReference<MonitoringApi>();

    /**
     * Create a ProcessControllerContext that uses the given host/port for services.
     *
     * @param defaultKey the ESM's SSL private key.  Required so the ESM can authenticate to the API servers.
     * @param host An individual cluster node's IP address or hostname (and NOT the cluster's front-end load balancer hostname).  Required.
     * @param processControllerPort The process controller port.  Required.
     * @param esmId The ID for the EM
     */
    public ProcessControllerContext( final DefaultKey defaultKey, final String host, final int processControllerPort, final String esmId ) {
        super(defaultKey, host, esmId, null);
        if ( host == null ) throw new IllegalArgumentException("host is required");
        if ( esmId == null ) throw new IllegalArgumentException("esmId is required");
        controllerUrl = MessageFormat.format(CONTROLLER_URL, host, Integer.toString(processControllerPort));
        monitoringUrl = MessageFormat.format(MONITORING_URL, host, Integer.toString(processControllerPort));
    }

    public NodeManagementApi getManagementApi() {
        return getApi(managementApi, NodeManagementApi.class, controllerUrl);
    }

    public MonitoringApi getMonitoringApi() {
        return getApi(monitoringApi, MonitoringApi.class, monitoringUrl);
    }
}
