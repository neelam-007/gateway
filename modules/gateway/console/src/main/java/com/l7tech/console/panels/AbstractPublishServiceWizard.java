package com.l7tech.console.panels;

import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.folder.Folder;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.EventListenerList;
import java.awt.*;

/**
 * Parent class for all wizards that publish services.
 */
public abstract class AbstractPublishServiceWizard extends Wizard {
    private EventListenerList localListenerList = new EventListenerList();

    public AbstractPublishServiceWizard(@NotNull final Frame parent, @NotNull final WizardStepPanel firstPanel) {
        super(parent, firstPanel);
    }

    public abstract void setFolder(@NotNull final Folder folder);

    /**
     * Add the EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        localListenerList.add(EntityListener.class, listener);
    }

    /**
     * Remove the the EntityListener
     *
     * @param listener the EntityListener
     */
    public void removeEntityListener(EntityListener listener) {
        localListenerList.remove(EntityListener.class, listener);
    }

    public void notify(@NotNull final EntityHeader header) {
        final EntityEvent event = new EntityEvent(this, header);
        final EntityListener[] listeners = localListenerList.getListeners(EntityListener.class);
        for (EntityListener listener : listeners) {
            listener.entityAdded(event);
        }
    }
}
