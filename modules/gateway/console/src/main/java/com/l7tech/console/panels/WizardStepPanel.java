package com.l7tech.console.panels;

import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.gui.util.InputValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import java.util.EventListener;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>JPanel</code> that represent a step in the wizard extend the
 * <code>WizardStepPanel</code>.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class WizardStepPanel<ST> extends JPanel {
    protected JDialog owner;
    protected List<InputValidator.ValidationRule> validationRules = new ArrayList<InputValidator.ValidationRule>();
    private EventListenerList listenerList = new WeakEventListenerList();
    private WizardStepPanel<ST> nextPanel;
    private boolean showDescriptionPanel = true;
    private boolean skipped = false;

    private boolean readOnly = false;

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
    public WizardStepPanel(WizardStepPanel<ST> next) {
        this.nextPanel = next;
    }

    public WizardStepPanel(WizardStepPanel<ST> next, boolean readOnly) {
        this.nextPanel = next;
        this.readOnly = readOnly;
    }

    public void setNextPanel(WizardStepPanel<ST> next) {
        this.nextPanel = next;
    }

    public final boolean hasNextPanel() {
        return nextPanel != null;
    }

    public final WizardStepPanel<ST> nextPanel() {
        return nextPanel;
    }

    public List<InputValidator.ValidationRule> getValidationRules() {
        return validationRules;
    }

    /**
     * @return whether to show description panel
     */
    protected boolean isShowDescriptionPanel() {
        return showDescriptionPanel;
    }

    /**
     * The subclass step panels use this to set the step panel
     *
     * @param showDescriptionPanel true show the description panel, false otherwise
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
     * Note: This method can be called before the panel has been initialized with data and before the user has been
     * shown the dialog.
     * Warning: be careful of using {@link com.l7tech.gui.util.TextComponentPauseListenerManager#registerPauseListener}
     * with an event handler that can call canAdvance() indirectly through notifyListeners(). Pause events will be generated
     * before the panel containing the pause capable component has been shown. If canAdvance() has logic to stop
     * next from being enabled, then this sequence of events can disable the next button on the first panel of the wizard.
     * (Unless your running in debug mode in which case it may work some or all of the time).
     *
     * @return true if the panel is valid, false otherwise
     */
    public boolean canAdvance() {
        return true;
    }

    /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwise
     */
    public boolean canFinish() {
        return (canAdvance() || isSkipped())
                && (nextPanel == null || nextPanel.canFinish());
    }

    /**
     * Given the current settings, determine whether this step can be skipped. Default is false.
     *
     * Override to customize behaviour - settings should not be mutated.
     *
     * @param settings the current wizard input settings (may be null).
     * @return true if the step can be skipped, false otherwise.
     */
    public boolean canSkip(@Nullable final Object settings) {
        return false;
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
        for (EventListener listener : listeners) {
            ((ChangeListener) listener).stateChanged(event);
        }
    }

    /**
     * Called by the wizard to notify that the panel is active.
     */
    public void notifyActive() {
    }

    /**
     * Called by the wizard to notify that the panel is inactive.
     */
    public void notifyInactive() {
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * no-op version that subclasses implement.
     *
     * This method is called every time the step is displayed.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(ST settings)
      throws IllegalArgumentException {
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings. This is a
     * no-op version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @param acceptNewProvider if true, then settings will be read
     * the input settings have a valid OID or not
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(ST settings, boolean acceptNewProvider)
      throws IllegalArgumentException {
        readSettings(settings);
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
     * This is a no-op version that subclasses implement.
     *
     * This method is called every time the step is displayed.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void storeSettings(ST settings)
      throws IllegalArgumentException {
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "implement " + getClass() + ".getStepLabel()";
    }

    protected boolean isReadOnly() {
        return readOnly;
    }
}
