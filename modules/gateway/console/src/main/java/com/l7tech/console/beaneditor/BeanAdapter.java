/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.beaneditor;

import java.beans.PropertyChangeEvent;

/**
 * An abstract adapter class for receiving bean editor events. The methods
 * in this class are empty. This class exists as convenience for creating
 * listener objects.
 *
 * @author emil
 * @version Feb 19, 2004
 */
public abstract class BeanAdapter implements BeanListener {
    /**
     * Fired when the bean edit is accepted.
     *
     * @param source the event source
     * @param bean   the bean being edited
     */
    public void onEditAccepted(Object source, Object bean) {
    }

    /**
     * Fired when the bean edit is cancelled.
     *
     * @param source the event source
     * @param bean   the bean being edited
     */
    public void onEditCancelled(Object source, Object bean) {
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt) {
    }
}