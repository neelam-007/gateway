package com.l7tech.console.action;

import javax.swing.*;

/**
 * This class provides default implementations for the application
 * <code>Action</code> interface. Additional properties  for
 * <code>Action</code> are defined.
 *
 * The developer need only subclass this abstract class and
 * define the <code>actionPerformed</code> method.
 *
 * @version 1.1
 * @author
 * @see javax.swing.Action
 * @see AbstractAction
 */
public abstract class BaseAction extends AbstractAction {
    /* this class classloader */
    final ClassLoader cl = getClass().getClassLoader();
    /** the resource path for the actions */
    public static final String RESOURCE_PATH = "com/l7tech/console/resources";

    public static final String LARGE_ICON = "LargeIcon";

    /**
     * Defines an <code>Action</code> object with the specified
     * description string and no icon.
     */
    public BaseAction(String name) {
        super(name);
    }

    /**
     * Defines an <code>Action</code> object with the specified
     * description string and a the specified icon.
     */
    public BaseAction(String name, String icon) {
        this(name);
        putValue(Action.SMALL_ICON, new ImageIcon(cl.getResource(icon)));
    }

    /**
     * Defines an <code>Action</code> object with the specified
     * description string and a the specified icon.
     */
    public BaseAction(String name, Icon icon) {
        super(name, icon);
    }


    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public abstract void performAction();

    /* Implementation of method of javax.swing.Action interface.
    * Delegates the execution to performAction method.
    *
    * @param ev ignored
    */
    public void actionPerformed(java.awt.event.ActionEvent ev) {
        performAction();
    }
}
