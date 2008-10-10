package com.l7tech.server.transport.email;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.transport.email.EmailListener;

import java.util.List;

/**
 * the interface for objects that manage EmailListener objects.
 */
public interface EmailListenerManager extends EntityManager<EmailListener, EntityHeader> {
    public List<EmailListener> getEmailListenersForNode(final String clusterNodeId) throws FindException;

    public List<EmailListener> stealSubscriptionsFromDeadNodes(final String clusterNodeId);

    public void updateLastPolled( final long emailListenerOid ) throws UpdateException;
}
