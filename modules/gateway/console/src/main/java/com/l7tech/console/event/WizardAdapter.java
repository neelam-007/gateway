package com.l7tech.console.event;

/**
 * The abstract adapter which receives wizard events. The methods
 * in this class are empty; this class is provided as a convenience
 * for easily creating listeners by extending this class and overriding
 * only the methods of interest.
 * <p>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class WizardAdapter implements WizardListener {
    /**
     * Invoked when the wizard page has been changed.
     *
     * @param e the event describing the selection change
     */
    public void wizardSelectionChanged(WizardEvent e) {
    }

    /**
     * Invoked when the wizard has finished.
     *
     * @param e the event describing the wizard finish
     */
    public void wizardFinished(WizardEvent e) {
    }

    /**
     * Invoked when the wizard has been cancelled.
     *
     * @param e the event describinng the wizard cancel
     */
    public void wizardCanceled(WizardEvent e) {
    }
}
