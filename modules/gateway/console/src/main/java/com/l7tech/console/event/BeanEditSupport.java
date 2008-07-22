/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.event;

import com.l7tech.console.beaneditor.BeanListener;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;

/**
 * The <code>BeanEditSupport</code> class encapsulates editing and
 * edit outcome notifying.
 *
 * The instances are usually created inside dialogs and frames,
 * with dialog or frame forms as a source. The forms may then
 * offer the <code>getBeanEditorSupport()</code> to allow clients
 * to interact with the bean editor support.
 *
 * @author emil
 * @version Mar 22, 2005
 * @see com.l7tech.console.beaneditor.BeanListener
 */
public class BeanEditSupport {
    /**
     * listeners
     */
    private final PropertyChangeSupport beanListeners;
    private Object source;

    /**
     *
     * @param source  the component that is event source
     */
    public BeanEditSupport(Object source) {
        if (source == null) {
            throw new IllegalArgumentException();
        }
        this.source = source;
        beanListeners = new PropertyChangeSupport(source);
    }

    /**
     * Adds the bean listener to the list of bean listeners.
     *
     * @param listener the bean listener
     */
    public synchronized void addBeanListener(BeanListener listener) {
        beanListeners.addPropertyChangeListener(listener);
    }

    /**
     * Removes the bean listener from the list of
     *
     * @param listener the bean listener
     */
    public synchronized void removeBeanListener(BeanListener listener) {
        beanListeners.removePropertyChangeListener(listener);
    }

    /**
     * Send the edit cancelled message to listeners
     *
     * @param edited the edited object
     */
    public void fireCancelled(Object edited) {
        PropertyChangeListener[] listeners = beanListeners.getPropertyChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            PropertyChangeListener listener = listeners[i];
            ((BeanListener)listener).onEditCancelled(source, edited);
        }
    }

    /**
     * Send the edit accepted message to listeners.
     *
     * @param edited  the edited object
     */
    public void fireEditAccepted(Object edited) {
        PropertyChangeListener[] listeners = beanListeners.getPropertyChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            PropertyChangeListener listener = listeners[i];
            ((BeanListener)listener).onEditAccepted(source, edited);
        }
    }



}
