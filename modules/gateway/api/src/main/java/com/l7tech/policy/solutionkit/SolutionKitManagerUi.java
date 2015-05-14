package com.l7tech.policy.solutionkit;

import javax.swing.*;

/**
 * WARNING: this class is under development and is currently for CA internal use only.
 * This interface contract may change without notice.
 *
 * Provides an opportunity for the caller to hook into the Solution Kit Manager UI and provide customized UIs.
 */
public abstract class SolutionKitManagerUi {
    /**
     * Parent panel is a customizable button panel in the Solution Kit Manager UI.
     */
    private JPanel parentPanel;

    /**
     * Provides data transport between the customized UI and the customized callback code.
     */
    private SolutionKitManagerContext context;

    public JPanel getParentPanel() {
        return parentPanel;
    }

    public void setParentPanel(JPanel parentPanel) {
        this.parentPanel = parentPanel;
    }

    public SolutionKitManagerContext getContext() {
        return context;
    }

    public void setContext(SolutionKitManagerContext context) {
        this.context = context;
    }

    /**
     * This button triggers the implementer's custom UI.
     * @return the button to show in the Solution Kit Manager
     */
    public JButton getButton() {
        // override to implement in sub class if required
        return null;
    }

    /**
     * Provide opportunity to do initialization work.
     * @return an instance of itself
     */
    public SolutionKitManagerUi initialize() {
        // override to implement in sub class if required
        return this;
    }

    // onOk()
}