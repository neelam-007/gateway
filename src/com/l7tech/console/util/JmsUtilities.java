/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.util;

import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.jms.JmsEndpoint;

import javax.swing.*;
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

    /** Utilities class for representing a JMS Queue inside a list or combo box model. */
    public static class QueueItem {
        private JmsAdmin.JmsTuple queue;

        QueueItem(JmsAdmin.JmsTuple q) {
            this.queue = q;
        }

        public String toString() {
            return queue.getEndpoint().getName() + " on " + queue.getConnection().getJndiUrl();
        }

        public JmsAdmin.JmsTuple getQueue() {
            return queue;
        }
    }

    /**
     * Obtain the current list of JMS Queues from the server as a QueueItem array.
     * This can be easily and immediately be turned into a list or combo box model.
     */
    public static QueueItem[] loadQueueItems() {
        java.util.List queues = JmsUtilities.loadJmsQueues(true);
        QueueItem[] items = new QueueItem[queues.size()];
        for (int i = 0; i < queues.size(); ++i)
            items[i] = new QueueItem((JmsAdmin.JmsTuple) queues.get(i));
        return items;
    }

    /**
     * Adjust the specified combo box so that the specified JMS endpoint is the selected item, if possible.
     * If the specified JMS endpoint is not present in the combo box's model the combo box selection
     * will be cleared.  The combo box model must not change while this method is running, and must
     * return instances of QueueItem.
     * @param cb the JComboBox to adjust
     * @param endpointOid the OID of the endpoint to select, or null to clear the selection.
     */
    public static void selectEndpoint(JComboBox cb, Long endpointOid) {
        if (endpointOid == null ||
            endpointOid.longValue() == JmsEndpoint.DEFAULT_OID) {
            cb.setSelectedIndex(-1);
            return;
        }

        for (int i = 0, size = cb.getModel().getSize(); i < size; i++) {
            QueueItem item = (QueueItem) cb.getModel().getElementAt(i);
            if (item.getQueue().getEndpoint().getOid() == endpointOid.longValue()) {
                cb.setSelectedItem(item);
                return;
            }
        }

        cb.setSelectedIndex(-1);
    }

    /**
     * Adjust the specified combo box so that the specified JMS endpoint is the selected item, if possible.
     * If the specified JMS endpoint is not present in the combo box's model the combo box selection
     * will be cleared.  The combo box model must not change while this method is running, and must
     * return instances of QueueItem.
     * @param cb the JComboBox to adjust
     * @param endpoint the endpoint to select, or null to clear the selection.
     */
    public static void selectEndpoint(JComboBox cb, JmsEndpoint endpoint) {
        selectEndpoint(cb, (Long) (endpoint == null ? null : new Long(endpoint.getOid())));
    }
}
