package com.l7tech.server.transport.email;

import com.l7tech.objectmodel.*;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailListenerState;

import java.util.List;

/**
 * the interface for objects that manage EmailListener objects.
 */
public interface EmailListenerManager extends EntityManager<EmailListener, EntityHeader> {
    public List<EmailListener> getEmailListenersForNode(final String clusterNodeId) throws FindException;

    public List<EmailListener> stealSubscriptionsFromDeadNodes(final String clusterNodeId);

    public void updateLastPolled( final Goid emailListenerGoid ) throws UpdateException;

    /**
     * Update email listener's state.
     *
     * @param state Email Listener state
     * @throws UpdateException  Failed to update state
     */
    public void updateState(final EmailListenerState state) throws UpdateException;
}
