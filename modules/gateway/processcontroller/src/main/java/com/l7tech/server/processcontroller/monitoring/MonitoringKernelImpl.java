/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring;

import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.management.config.monitoring.NotificationRule;
import com.l7tech.server.management.config.monitoring.PropertyTrigger;
import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.util.Pair;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MonitoringKernelImpl implements MonitoringKernel {
    private final List<Pair<PropertySampler, List<NotificationRule>>> config = new CopyOnWriteArrayList<Pair<PropertySampler, List<NotificationRule>>>();

    private final Map<PropertyTrigger, PropertySampler> propertySamplers = new ConcurrentHashMap<PropertyTrigger, PropertySampler>();
    private final Map<NotificationRule, Notifier> notifiers = new ConcurrentHashMap<NotificationRule, Notifier>();

    private volatile MonitoringConfiguration currentConfiguration = null;

    @Resource
    private ConfigService configService;

    void checkConfig() {
        MonitoringConfiguration newConfiguration = configService.getCurrentMonitoringConfiguration();
        if (currentConfiguration == null) {
            currentConfiguration = newConfiguration;
            return;
        }

        if (newConfiguration.equals(currentConfiguration)) {
            return;
        }
    }
}
