package com.l7tech.console.panels;

import com.l7tech.console.event.WeakEventListenerList;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.EventListener;

/**
 * <code>JPanel</code> that represent a step in the wizard extend the
 * <code>WizardStepPanel</code>.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class WizardStepPanel extends JPanel {
    private EventListenerList listenerList = new WeakEventListenerList();
    private WizardStepPanel nextPanel;

    /** Creates new form WizardPanel */
    public WizardStepPanel(WizardStepPanel next) {
        this.nextPanel = next;
    }

    public final boolean hasNextPanel() {
        return nextPanel != null;
    }

    public final WizardStepPanel nextPanel() {
        return nextPanel;
    }


    /**
     * @return the wizard step description
     */
    public abstract String getDescription();

    /**
     * Test whether the step is finished and it is safe to proceed to the next
     * one.
     * If the step is valid, the "Next" (or "Finish") button will be enabled.
     *
     * @return true if the panel is valid, false otherwis
     */
    public abstract boolean isValid();


    /**
     * Test whether the step is finished and it is safe to advance to the next
     * one.
     *
     * @return true if the panel is valid, false otherwis
     */

    public boolean canAdvance() {
        return true;
    }

    /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */

    public boolean canFinish() {
        return true;
    }


    /**
     * Add a listener to changes of the panel's validity.
     * The default is a simple implementation that supports a single
     * listener.
     * For multiple listener support override the behaviour.
     *
     * @param l the listener to add
     * @see #isValid
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Remove a listener to changes of the panel's validity.
     *
     * The default is a simple implementation that supports a single
     * listener.
     *
     * @param l the listener to remove
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * notify listeners of the state change
     */
    protected void notifyListeners() {
        ChangeEvent event = new ChangeEvent(this);
        EventListener[] listeners = listenerList.getListeners(ChangeListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((ChangeListener)listeners[i]).stateChanged(event);
        }
    }


    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     */
    public void readSettings(Object settings)
      throws IllegalArgumentException {
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     *
     * This is a noop version that subclasses implement.
     *
     * @exception IllegalArgumentException if the the data provided
     * by the wizard are not valid.
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings)
      throws IllegalArgumentException {
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "implement " + getClass() + "getStepLabel()";
    }

    private ChangeListener changeListener;
}
