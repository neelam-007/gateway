package com.l7tech.server.management.api.node;

import com.l7tech.objectmodel.FindException;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.api.monitoring.NodeStatus;

import java.util.Date;

/**
 *
 */
public class NodeApiStub implements NodeApi {
    @Override
    public void shutdown() {
    }

    @Override
    public void ping() {
    }

    @Override
    public NodeStatus getNodeStatus() {
        return new NodeStatus(NodeStateType.UNKNOWN, new Date(), new Date());
    }

    @Override
    public String getProperty(String propertyId) throws UnsupportedPropertyException, FindException {
        throw new UnsupportedPropertyException(propertyId);
    }
}
