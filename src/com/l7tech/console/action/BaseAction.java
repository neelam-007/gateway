package com.l7tech.console.action;

import com.l7tech.common.gui.util.ImageCache;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

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
    public static final String LARGE_ICON = "LargeIcon";

    /**
     * Default constructorr. Defines an <code>Action</code> object with
     * action name, icon and description form subclasses.
     */
    public BaseAction() {
        String name = getName();
        if (name != null) {
            putValue(Action.NAME, getName());
        }
        String desc = getDescription();
        if (desc != null) {
            putValue(Action.SHORT_DESCRIPTION, desc);
        }
        Image img = getIcon();
        if (img != null) {
            putValue(Action.SMALL_ICON, new ImageIcon(img));
        }
    }

    /**
     * loads the icon specified by subclass iconResource()
     * implementation.
     *
     * @return the <code>ImageIcon</code> or null if not found
     */
    public final Image getIcon() {
        return ImageCache.getInstance().getIcon(iconResource());

    }

    /**
     * @return the action name
     */
    public abstract String getName();

    /**
     * @return the aciton description
     */
    public abstract String getDescription();

    /**
     * subclasses override this method specifying the resource name
     */
    protected abstract String iconResource();


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
    public final void actionPerformed(ActionEvent ev) {
        performAction();
    }
}
