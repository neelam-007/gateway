/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.ext;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The <code>AssertionEditorSupport</code> class encapsulates
 * editing and edit outcome notifying for <code>AssertionEditor</code>.
 *
 * @author emil
 * @version Mar 22, 2005
 * @see com.l7tech.console.beaneditor.BeanListener
 */
public class AssertionEditorSupport {
    /**
     * listeners
     */
    private final Collection beanListeners = new ArrayList();
    private Object source;

    /**
     * @param source the component that is event source
     */
    public AssertionEditorSupport(Object source) {
        if (source == null) {
            throw new IllegalArgumentException();
        }
        this.source = source;
    }

    /**
     * Adds the bean listener to the list of bean listeners.
     *
     * @param listener the bean listener
     */
    public synchronized void addListener(EditListener listener) {
        beanListeners.add(listener);
    }

    /**
     * Removes the bean listener from the list of
     *
     * @param listener the bean listener
     */
    public synchronized void removeListener(EditListener listener) {
        beanListeners.remove(listener);
    }

    /**
     * Send the edit cancelled message to listeners
     *
     * @param edited the edited object
     */
    public void fireCancelled(Object edited) {
        EditListener[] listeners = (EditListener[])beanListeners.toArray(new EditListener[]{});
        for (int i = 0; i < listeners.length; i++) {
            EditListener listener = listeners[i];
            listener.onEditCancelled(source, edited);
        }
    }

    /**
     * Send the edit accepted message to listeners.
     *
     * @param edited the edited object
     */
    public void fireEditAccepted(Object edited) {
        EditListener[] listeners = (EditListener[])beanListeners.toArray(new EditListener[]{});
        for (int i = 0; i < listeners.length; i++) {
            EditListener listener = listeners[i];
            listener.onEditAccepted(source, edited);
        }
    }

}
