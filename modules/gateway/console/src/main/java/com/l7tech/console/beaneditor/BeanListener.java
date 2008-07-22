package com.l7tech.console.beaneditor;

import java.beans.PropertyChangeListener;

/**
 * Listener to changes with Entities.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public interface BeanListener extends PropertyChangeListener {
    /**
     * Fired when the bean edit is accepted.
     *
     * @param source the event source
     * @param bean   the bean being edited
     */
    void onEditAccepted(Object source, Object bean);

    /**
     * Fired when the bean edit is cancelled.
     *
     * @param source the event source
     * @param bean   the bean being edited
     */
    void onEditCancelled(Object source, Object bean);

}
