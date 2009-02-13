/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;
import com.l7tech.server.management.api.monitoring.MonitorableProperty;
import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

public class AuditSizeSampler extends NodePropertySampler<Long> {
    private static final MonitorableProperty AUDIT_SIZE = BuiltinMonitorables.AUDIT_SIZE;

    public AuditSizeSampler(String componentId, ApplicationContext spring) {
        super(ComponentType.CLUSTER, componentId, AUDIT_SIZE.getName(), spring);
    }

    @Override
    public Long sample() throws PropertySamplingException {
        try {
            return invokeNodeApi(new Functions.UnaryThrows<Long, NodeApi, Exception>() {
                @Override
                public Long call(NodeApi nodeApi) throws Exception {
                    return Long.valueOf(nodeApi.getProperty(propertyName));
                }
            });
        } catch (Exception e) {
            throw new PropertySamplingException("Couldn't get audit size", e);
        }
    }
}
