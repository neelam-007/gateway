/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.util;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;

import java.util.ArrayList;

/**
 * Utility code that is used by the JMS GUI components.
 */
public class JmsUtilities {
    /**
     * Get the current list of JMS endpoints from the server.
     * @param includeEmptyConnections if true, a JmsQueue will be returned for each JmsConnection that has no JmsEndpoints.
     * @param outboundOnly if true, only queues set to the Outbound direction will be returned.
     * @return an ArrayList of JmsQueue instances.  Each JmsQueue will have a non-null connection, but might have
     *         a null endpoint if the server was configured outside of the current version of this GUI.  The
     *         returned JmsConnection instances might be shared across multiple JmsEndpoint instances, even though
     *         the current GUI provides no way to configure Queues this way.
     */
    public static ArrayList loadJmsQueues(boolean includeEmptyConnections, boolean outboundOnly) {
        try {
            ArrayList jmsQueues = new ArrayList();

            JmsConnection[] connections = Registry.getDefault().getJmsManager().findAllConnections();
            for (int i = 0; i < connections.length; i++) {
                JmsConnection connection = connections[i];
                JmsEndpoint[] endpoints = Registry.getDefault().getJmsManager().getEndpointsForConnection(connection.getOid());
                if (endpoints.length > 0)
                    for (int k = 0; k < endpoints.length; k++) {
                        JmsEndpoint endpoint = endpoints[k];
                        if (!(endpoint.isMessageSource() && outboundOnly))
                            jmsQueues.add(new JmsQueue(connection, endpoint));
                    }
                else {
                    if (includeEmptyConnections && !outboundOnly)
                    jmsQueues.add(new JmsQueue(connection, null)); // shouldn't happen in normal operation
                }
            }
            return jmsQueues;
        } catch (Exception e) {
            throw new RuntimeException("Unable to look up list of known JMS Queues", e);
        }
    }

    public static class JmsQueue {
        public JmsConnection connection;
        public JmsEndpoint endpoint;
        JmsQueue(JmsConnection connection, JmsEndpoint endpoint) {
            this.connection = connection;
            this.endpoint = endpoint;
        };
    }
}
