package com.l7tech.server.management.api.monitoring;

import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 *
 */
public class MonitoringApiStub implements MonitoringApi {
    public NodeStatus getNodeStatus(String nodeName) throws IOException {
        return new NodeStatus(NodeStateType.UNKNOWN, new Date(), new Date());
    }

    public void pushMonitoringConfiguration(MonitoringConfiguration config) throws IOException {
    }

    public List<MonitoredPropertyStatus> getCurrentPropertyStatuses() {
        return Collections.emptyList();
    }

    public List<NotificationAttempt> getRecentNotificationAttempts(long sinceWhen) {
        return Collections.emptyList();
    }
}
