package com.l7tech.console.action;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 
 */
public class ActionManager {
    private static ActionManager instance = new ActionManager();

    /**
     * this clas cannot be isntantiated
     */
    private ActionManager() {}

    /**
     * @return the singleton instance
     */
    public static ActionManager getInstance() {
        return instance;
    }

    public void invokeAction(Action action, ActionEvent event) {
        action.actionPerformed(event);
    }

    /**
     * perform the action passed
     *
     * @param a the action to invoke
     */
    public void invokeAction(Action a) {
        invokeAction(a, null);
    }
}
