package com.l7tech.console.action;

import java.awt.event.ActionEvent;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 
 */
public class VetoableActionEvent extends ActionEvent {
    /**
     * This event id indicates that the action will be invoked.
     */
    public static final int ACTION_WILL_PERFORM = ACTION_PERFORMED + 1;

    public VetoableActionEvent(Object source, int id, String command) {
        super(source, id, command);
    }

    public VetoableActionEvent(Object source, int id, String command, long when, int modifiers) {
        super(source, id, command, when, modifiers);
    }

    /**
     * factory method that creates the vetoable action event from
     * an action event
     * @param ev the action event
     */
    public static VetoableActionEvent create(ActionEvent ev) {
        if (ev == null) {
            throw new IllegalArgumentException("event cannot be null");
        }
            return new
              VetoableActionEvent(
                ev.getSource(), ACTION_WILL_PERFORM,
                ev.getActionCommand(), ev.getWhen(), ev.getModifiers());
    }
}
