package com.l7tech.console.event;

import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;


/**
 * An abstract adapter class for receiving container events. The methods
 * in this class are empty. This class exists as convenience for creating
 * listener objects.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class ContainerVetoableAdapter extends ContainerAdapter
  implements VetoableContainerListener {
    /**
     * Invoked when a component has to be added to the container.
     * @param e     the container event
     * @throws ContainerVetoException if the recipient wishes to stop
     *         (not perform) the action.
     */
    public void componentWillAdd(ContainerEvent e) throws ContainerVetoException {
    }

    /**
     * Invoked when a component has to be removed from to the container.
     * @param e     the container event
     * @throws ContainerVetoException if the recipient wishes to stop
     *         (not perform) the action.
     */
    public void componentWillRemove(ContainerEvent e) throws ContainerVetoException {
    }
}
