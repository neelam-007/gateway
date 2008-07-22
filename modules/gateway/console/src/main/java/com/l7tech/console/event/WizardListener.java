package com.l7tech.console.event;

import java.util.EventListener;

/**
 * The implementations of this interface listend for the wizard
 * events.
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public interface WizardListener extends EventListener {
    /**
     * Invoked when the wizard page has been changed.
     *
     * @param e the event describing the selection change
     */
    void wizardSelectionChanged(WizardEvent e);

    /**
     * Invoked when the wizard has finished.
     *
     * @param e the event describing the wizard finish
     */
    void wizardFinished(WizardEvent e);

    /**
     * Invoked when the wizard has been cancelled.
     *
     * @param e the event describinng the wizard cancel
     */
    void wizardCanceled(WizardEvent e);
}
