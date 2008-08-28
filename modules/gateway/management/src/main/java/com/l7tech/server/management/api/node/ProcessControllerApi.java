/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.api.node;

import com.l7tech.gateway.common.transport.SsgConnector;

import javax.jws.WebService;

/**
 * API published by the Process Controller for use by the Node
 *
 * @author alex
 */
@WebService(name="ProcessControllerApi", targetNamespace = "http://ns.l7tech.com/secureSpan/5.0/component/processController/processControllerApi")
public interface ProcessControllerApi {
    /** Tells the PC that the SN has been told to create a new connector; the PC may need to make firewall changes. */
    void connectorCreated(SsgConnector connector);

    /** Tells the PC that the SN has been told to update an existing connector; the PC may need to make firewall changes. */
    void connectorUpdated(SsgConnector connector);

    /** Tells the PC that the SN has been told to delete a connector; the PC may need to make firewall changes. */
    void connectorDeleted(SsgConnector connector);

    /** Notifies the PC of an event that has occurred in the SN */
    void notifyEvent(EventSubscription sub, Object event);

    /** Called periodically by the SN to ensure that the PC is still alive */
    void ping();
}
