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
    protected JDialog owner;
    private EventListenerList listenerList = new WeakEventListenerList();
    private WizardStepPanel nextPanel;
    private boolean showDescriptionPanel = true;
    private boolean skipped = false;

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    public JDialog getOwner() {
        return owner;
    }

    protected void setOwner(JDialog owner) {
        this.owner = owner;
    }

    /**
     * Creates new form WizardPanel
     */
    public WizardStepPanel(WizardStepPanel next) {
        this.nextPanel = next;
    }

    public void setNextPanel(WizardStepPanel next) {
        this.nextPanel = next;
    }

    public final boolean hasNextPanel() {
        return nextPanel != null;
    }

    public final WizardStepPanel nextPanel() {
        return nextPanel;
    }

    /**
     * @return whether to show description panel
     */
    protected boolean isShowDescriptionPanel() {
        return showDescriptionPanel;
    }

    /**
         * The sublass step panels use this to set the step panel
         *
         * @param showDescriptionPanel true show the dscri[ption panel, false otherwise
         */
    protected void setShowDescriptionPanel(boolean showDescriptionPanel) {
        this.showDescriptionPanel = showDescriptionPanel;
    }

    /**
     * Perform any panel-specific last-second checking at the time the user presses the "Next"
     * (or "Finish") button
     * while this panel is showing.  The panel may veto the action by returning false here.
     * Since this method is called in response to user input it may take possibly-lengthy actions
     * such as downloading a remote file.
     * <p/>
     * This differs from canAdvance() in that it is called when the user actually hits the Next button,
     * whereas canAdvance() is used to determine if the Next button is even enabled.
     *
     * @return true if it is safe to advance to the next step; false if not (and the user may have
     *         been pestered with an error dialog).
     */
    public boolean onNextButton() {
        return true;
    }


    /**
     * Test whether the step is finished and it is safe to advance to the next one.  This method
     * should return quickly.
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
     */
    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Remove a listener to changes of the panel's validity.
     * <p/>
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
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(Object settings)
      throws IllegalArgumentException {
    }


    public String getDescription() {
        return "";
    }


    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     * <p/>
     * This is a noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void storeSettings(Object settings)
      throws IllegalArgumentException {
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "implement " + getClass() + ".getStepLabel()";
    }
}
