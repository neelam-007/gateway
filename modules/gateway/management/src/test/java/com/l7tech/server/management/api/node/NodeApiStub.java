package com.l7tech.server.management.api.node;

import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.server.management.api.monitoring.NodeStatus;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

/**
 *
 */
public class NodeApiStub implements NodeApi {
    public void shutdown() {
    }

    public void ping() {
    }

    public NodeStatus getNodeStatus() {
        return new NodeStatus(NodeStateType.UNKNOWN, new Date(), new Date());
    }

    public Set<EventSubscription> subscribeEvents(Set<String> eventIds) throws UnsupportedEventException, SaveException {
        return Collections.emptySet();
    }

    public Set<EventSubscription> renewEventSubscriptions(Set<String> subscriptionIds) throws UpdateException {
        return Collections.emptySet();
    }

    public void releaseEventSubscriptions(Set<String> subscriptionIds) throws DeleteException {
    }

    public String getProperty(String propertyId) throws UnsupportedPropertyException, FindException {
        throw new UnsupportedPropertyException(propertyId);
    }
}
