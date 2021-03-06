/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.management.api.monitoring.MonitoredPropertyStatus;
import com.l7tech.server.management.api.monitoring.NotificationAttempt;

import java.util.List;

public interface MonitoringKernel {
    void setConfiguration(MonitoringConfiguration config);

    List<MonitoredPropertyStatus> getCurrentPropertyStatuses();

    List<NotificationAttempt> getRecentNotificationAttempts(long sinceWhen);
}
