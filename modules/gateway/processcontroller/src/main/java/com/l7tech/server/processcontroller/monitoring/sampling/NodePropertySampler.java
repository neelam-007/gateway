/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.server.management.api.monitoring.NodeStatus;
import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.server.processcontroller.ProcessController;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

public abstract class NodePropertySampler<V extends Serializable> extends PropertySampler<V> {
    protected NodeApi nodeApi;
    private final ProcessController processController;

    protected NodePropertySampler(ComponentType componentType, String componentId, String propertyName, ApplicationContext spring) {
        super(componentType, componentId, propertyName);
        this.processController = (ProcessController) spring.getBean("processController", ProcessController.class);
    }

    protected <T> T invokeNodeApi(Functions.UnaryThrows<T, NodeApi, Exception> callable) throws Exception {
        final Map<String,NodeStatus> nodes = processController.listNodes();
        if (nodes.isEmpty()) throw new IOException("No nodes are currently known to the Process Controller");
        return processController.callNodeApi(null, callable);
    }

    @Override
    public void close() throws IOException { }
}
