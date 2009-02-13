/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller;

import com.l7tech.server.management.api.monitoring.MonitoredPropertyStatus;
import com.l7tech.server.management.api.monitoring.MonitoringApi;
import com.l7tech.server.management.api.monitoring.NodeStatus;
import com.l7tech.server.management.config.monitoring.MonitoringConfiguration;
import com.l7tech.server.processcontroller.monitoring.MonitoringKernel;

import javax.annotation.Resource;
import javax.jws.WebService;
import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

@WebService(endpointInterface="com.l7tech.server.management.api.monitoring.MonitoringApi")
public class MonitoringApiImpl implements MonitoringApi {
    private static final Logger logger = Logger.getLogger(MonitoringApiImpl.class.getName());

    @Resource
    private ConfigService configService;

    @Resource
    private ProcessController processController;

    @Resource
    private MonitoringKernel monitoringKernel;

    @Override
    public NodeStatus getNodeStatus(String nodeName) throws IOException {
        return processController.getNodeStatus(nodeName);
    }

    @Override
    public void pushMonitoringConfiguration(MonitoringConfiguration config, boolean responsibleForClusterMonitoring) throws IOException {
        configService.pushMonitoringConfiguration(config, responsibleForClusterMonitoring);
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
}
