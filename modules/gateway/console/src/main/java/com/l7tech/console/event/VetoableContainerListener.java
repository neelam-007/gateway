package com.l7tech.console.event;

import java.awt.event.ContainerListener;
import java.awt.event.ContainerEvent;

/**
 * The listener interface for receiving container events with
 * veto support.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public interface VetoableContainerListener extends ContainerListener {
    /**
     * Invoked when a component has to be added to the container.
     * @param e     the container event
     * @throws ContainerVetoException if the recipient wishes to stop
     *         (not perform) the action.
     */
    void componentWillAdd(ContainerEvent e) throws ContainerVetoException;
    /**
     * Invoked when a component has to be removed from to the container.
     * @param e     the container event
     * @throws ContainerVetoException if the recipient wishes to stop
     *         (not perform) the action.
     */
    void componentWillRemove(ContainerEvent e)  throws ContainerVetoException;;
}
