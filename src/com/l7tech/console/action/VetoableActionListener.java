package com.l7tech.console.action;

import java.awt.event.ActionListener;

/**
 * A <code>VetoableActionListener</code> is registered with the action
 * that the listener wishes to control.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public interface VetoableActionListener extends ActionListener {
    /**
     * The listener will be invoked before the action is invioked.
     *
     * @param event the action event to be performed
     * @throws ActionVetoException if the recipient wishes to stop
     *         (not perform) the action.
     */
    void actionWillPerform(VetoableActionEvent event)
      throws ActionVetoException;
}
