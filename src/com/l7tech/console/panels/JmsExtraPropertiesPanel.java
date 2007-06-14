/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Properties;

/**
 * A sub-panel for configuring additional settings of a specific JMS provider
 * type; to be inserted into {@link JmsQueuePropertiesDialog} when that JMS
 * provider type is selected.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public abstract class JmsExtraPropertiesPanel extends JPanel {

    protected transient ChangeEvent changeEvent;

    /**
     * Applies given properties to initialize the view.
     */
    public abstract void setProperties(final Properties properties);

    /**
     * Gets properties out of the current view. 
     */
    public abstract Properties getProperties();

    /**
     * @return <code>true</code> if all settings on the panel are valid
     */
    public abstract boolean validatePanel();

    /**
     * Adds a <code>ChangeListener</code> to the panel to listen for changes
     * that affect validity. Upon notification, the listens should call
     * {@link #validatePanel} to compute validity.
     *
     * @param l the listener to be added
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Removes a <code>ChangeListener</code> from the panel.
     * @param l the listener to be removed
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * Returns an array of all the <code>ChangeListener</code>s registered
     * through {@link #addChangeListener}.
     *
     * @return all of the <code>ChangeListener</code>s added or an empty
     *         array if no listeners have been added
     */
    public ChangeListener[] getChangeListeners() {
        return (ChangeListener[])(listenerList.getListeners(
            ChangeListener.class));
    }

    /**
     * Notifies all listeners registered through {@link #addChangeListener}.
     */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event.
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i]==ChangeListener.class) {
                // Lazily create the event:
                if (changeEvent == null)
                    changeEvent = new ChangeEvent(this);
                ((ChangeListener)listeners[i+1]).stateChanged(changeEvent);
            }
        }
    }
}
