/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.node;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

/**
 * API published by the Process Controller for use by the Service Node
 *
 * @author alex
 */
public interface ProcessControllerApi {
    /** Tells the PC that the SN has been told to create a new connector; the PC may need to make firewall changes. */
    void createConnector(SsgConnector connector) throws SaveException;

    /** Tells the PC that the SN has been told to update an existing connector; the PC may need to make firewall changes. */
    void updateConnector(SsgConnector connector) throws UpdateException;

    /** Tells the PC that the SN has been told to delete a connector; the PC may need to make firewall changes. */
    void deleteConnector(SsgConnector connector) throws DeleteException;

    /** Notifies the PC of an event that has occurred in the SN */
    void notifyEvent(EventSubscription sub, Object event);
}
