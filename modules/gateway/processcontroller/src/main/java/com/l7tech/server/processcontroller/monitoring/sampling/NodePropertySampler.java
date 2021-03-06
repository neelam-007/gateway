/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.server.processcontroller.ProcessController;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.Serializable;

public abstract class NodePropertySampler<V extends Serializable> extends PropertySampler<V> {
    protected final ProcessController processController;
    protected final ConfigService configService;

    protected NodePropertySampler(ComponentType componentType, String componentId, String propertyName, ApplicationContext spring) {
        super(componentType, componentId, propertyName);
        this.processController = spring.getBean("processController", ProcessController.class);
        this.configService = spring.getBean("configService", ConfigService.class);
    }

    protected <T> T invokeNodeApi(Functions.UnaryThrows<T, NodeApi, Exception> callable) throws Exception {
        return processController.callNodeApi(null, callable);
    }

    @Override
    public void close() throws IOException { }
}
