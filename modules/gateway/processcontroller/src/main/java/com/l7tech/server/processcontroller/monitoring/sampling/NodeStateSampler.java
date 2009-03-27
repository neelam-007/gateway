/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;
import com.l7tech.server.management.api.node.NodeApi;
import com.l7tech.server.management.config.monitoring.ComponentType;
import com.l7tech.server.processcontroller.ProcessController;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.springframework.context.ApplicationContext;

import javax.xml.ws.WebServiceException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

class NodeStateSampler extends NodePropertySampler<NodeStateType> {
    private static final Logger logger = Logger.getLogger(NodeStateSampler.class.getName());

    public NodeStateSampler(String componentId, ApplicationContext spring) {
        super(ComponentType.NODE, componentId, BuiltinMonitorables.NODE_STATE.getName(), spring);
    }

    public NodeStateType sample() throws PropertySamplingException {
        try {
            return invokeNodeApi(new Functions.UnaryThrows<NodeStateType, NodeApi, Exception>() {
                @Override
                public NodeStateType call(NodeApi nodeApi) {
                    return nodeApi.getNodeStatus().getType();
                }
            });
        } catch (ProcessController.TemporarilyUnavailableException e) {
            logger.log(Level.INFO, MessageFormat.format("Node state temporarily unavailable (returning last known state, {0})", e.getType()));
            return e.getType();
        } catch (WebServiceException e) {
            logger.log(Level.WARNING, "Couldn't get node state: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return NodeStateType.UNKNOWN;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't get node state: " + ExceptionUtils.getMessage(e), e);
            return NodeStateType.UNKNOWN;
        }
    }
}
