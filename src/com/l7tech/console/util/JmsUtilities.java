/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.util;

import com.l7tech.common.transport.jms.JmsAdmin;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility code that is used by the JMS GUI components.
 */
public class JmsUtilities {
    /**
     * Get the current list of JMS endpoints from the server.
     * @param outboundOnly if true, only queues set to the Outbound direction will be returned.
     * @return an ArrayList of JmsQueue instances.  Each JmsQueue will have a non-null connection, but might have
     *         a null endpoint if the server was configured outside of the current version of this GUI.  The
     *         returned JmsConnection instances might be shared across multiple JmsEndpoint instances, even though
     *         the current GUI provides no way to configure Queues this way.
     */
    public static List loadJmsQueues(boolean outboundOnly) {
        try {
            ArrayList jmsQueues = new ArrayList();

            JmsAdmin.JmsTuple[] tuples = Registry.getDefault().getJmsManager().findAllTuples();
            for ( int i = 0; i < tuples.length; i++ ) {
                JmsAdmin.JmsTuple tuple = tuples[i];
                if ( !(outboundOnly && tuple.getEndpoint().isMessageSource() ) )
                    jmsQueues.add( tuple );
            }
            return jmsQueues;
        } catch (Exception e) {
            throw new RuntimeException("Unable to look up list of known JMS Queues", e);
        }
    }
}
