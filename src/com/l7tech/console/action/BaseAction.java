package com.l7tech.console.action;

import com.l7tech.common.gui.util.ImageCache;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

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
    static final Logger log = Logger.getLogger(BaseAction.class.getName());

    public static final String LARGE_ICON = "LargeIcon";
    private EventListenerList listenerList = new EventListenerList();

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
     * Adds an <code>ActionListener</code> to the button.
     * @param l the <code>ActionListener</code> to be added
     */
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    /**
     * Removes an <code>ActionListener</code> from the button.
     * If the listener is the currently set <code>Action</code>
     * for the button, then the <code>Action</code>
     * is set to <code>null</code>.
     *
     * @param l the listener to be removed
     */
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }


    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created using the <code>event</code> 
     * parameter.
     *
     * @param event  the <code>ActionEvent</code> object
     * @see javax.swing.event.EventListenerList
     */
    protected void fireActionPerformed(ActionEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event == null");
        }
        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = null;
        for (int i = listeners.length - 1; i >= 0; i--) {
            if (listeners[i] == ActionListener.class) {
                // Lazily create the event:
                if (e == null) {
                    String actionCommand = event.getActionCommand();
                    if (actionCommand == null) {
                        actionCommand = getName();
                    }
                    e = new ActionEvent(this,
                      ActionEvent.ACTION_PERFORMED,
                      actionCommand,
                      event.getWhen(),
                      event.getModifiers());
                }
                ((ActionListener)listeners[i + 1]).actionPerformed(e);
            }
        }
    }


    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created using the <code>event</code> 
     * parameter.
     *
     * @param event  the <code>ActionEvent</code> object
     * @see javax.swing.event.EventListenerList
     */
    protected void fireActionWillPerform(VetoableActionEvent event) {
        Object[] listeners = listenerList.getListenerList();
        VetoableActionEvent e = null;
        for (int i = listeners.length - 1; i >= 0; i--) {
            if (listeners[i] == VetoableActionListener.class) {
                // Lazily create the event:
                if (e == null) {
                    String actionCommand = event.getActionCommand();
                    if (actionCommand == null) {
                        actionCommand = getName();
                    }
                    e = new VetoableActionEvent(this,
                      VetoableActionEvent.ACTION_WILL_PERFORM,
                      actionCommand,
                      event.getWhen(),
                      event.getModifiers());
                }

                try {
                    ((VetoableActionListener)listeners[i + 1]).actionWillPerform(e);
                } catch (ActionVetoException ex) {
                    // vetoed
                }
            }
        }
    }


    /**
     * loads the icon specified by subclass iconResource()
     * implementation.
     *
     * @return the <code>ImageIcon</code> or null if not found
     */
    public final Image getIcon() {
        final String name = iconResource();
        if (name == null) return null;
        return ImageCache.getInstance().getIcon(name);

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


    /**
     * Actually perform the action.
     */
    protected abstract void performAction();

    /**
     * The method that invokes the action programatically 
     */
    public final void invoke() {
        actionPerformed(new ActionEvent(this,
              ActionEvent.ACTION_PERFORMED,
              getName()));

    }

    /**
     * Implementation of method of javax.swing.Action interface.
     * Delegates the execution to performAction method.
     *
     * @param ev ignored
     */
    public void actionPerformed(ActionEvent ev) {
        if (ev == null) {
            ev = new ActionEvent(this,
              ActionEvent.ACTION_PERFORMED,
              getName());
        }
        VetoableActionEvent vev = VetoableActionEvent.create(ev);
        fireActionWillPerform(vev);
        performAction();
        fireActionPerformed(ev);
    }
}
