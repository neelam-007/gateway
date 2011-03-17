package com.l7tech.server.processcontroller;

import com.l7tech.server.management.api.monitoring.MonitoredPropertyStatus;
import com.l7tech.server.management.api.monitoring.MonitoringApi;
import com.l7tech.server.management.api.monitoring.NodeStatus;
import com.l7tech.server.management.api.monitoring.NotificationAttempt;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.processcontroller.monitoring.MonitoringKernel;

import javax.inject.Inject;
import javax.jws.WebService;
import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

@WebService(endpointInterface="com.l7tech.server.management.api.monitoring.MonitoringApi")
public class MonitoringApiImpl implements MonitoringApi {
    private static final Logger logger = Logger.getLogger(MonitoringApiImpl.class.getName());

    @Inject
    private ConfigService configService;

    @Inject
    private ProcessController processController;

    @Inject
    private MonitoringKernel monitoringKernel;

    @Override
    public NodeStatus getNodeStatus(String nodeName) throws IOException {
        return processController.getNodeStatus(nodeName);
    }

    @Override
    public void pushMonitoringConfiguration(MonitoringConfiguration config) throws IOException {
        configService.pushMonitoringConfiguration(config);
    }

    @Override
    public List<MonitoredPropertyStatus> getCurrentPropertyStatuses() {
        try {
            return monitoringKernel.getCurrentPropertyStatuses();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't get properties", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<NotificationAttempt> getRecentNotificationAttempts(long sinceWhen) {
        return monitoringKernel.getRecentNotificationAttempts(sinceWhen);
    }
}
