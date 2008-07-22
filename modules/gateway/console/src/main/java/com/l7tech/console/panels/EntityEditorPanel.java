package com.l7tech.console.panels;

import com.l7tech.console.event.EntityListener;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import java.util.EventListener;

/**
 * abstract class for implementing panels that allow editing of bean data
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public abstract class EntityEditorPanel extends JPanel {

    protected EntityEditorPanel() {
    }

    /**
     * add the EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        listenerList.add(EntityListener.class, listener);
    }

    /**
     * remove the the EntityListener
     *
     * @param listener the EntityListener
     */
    public void removeEntityListener(EntityListener listener) {
        listenerList.remove(EntityListener.class, listener);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type (entity update).
     *
     * @param eh the entity associated with the event
     */
    public void fireEntityUpdate(EntityHeader eh) {
        EntityEvent event = new EntityEvent(this, eh);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((EntityListener) listeners[i]).entityUpdated(event);
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type (entity update).
     *
     * @param eh the entity associated with the event
     */
    public void fireEntityAdded(EntityHeader eh) {
        EntityEvent event = new EntityEvent(this, eh);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((EntityListener) listeners[i]).entityAdded(event);
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type (entity update).
     *
     * @param eh the entity associated with the event
     */
    public void fireEntityRemoved(EntityHeader eh) {
        EntityEvent event = new EntityEvent(this, eh);
        EventListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((EntityListener) listeners[i]).entityAdded(event);
        }
    }

    /**
     * invoke edit on the object
     * @param entity  the object representing the entoty
     */
    public abstract void edit(Object entity);

}
