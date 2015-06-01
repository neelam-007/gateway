package com.l7tech.policy.solutionkit;

import javax.swing.*;

/**
 * WARNING: this class is under development and is currently for CA internal use only.
 * This interface contract may change without notice.
 *
 * Provides an opportunity for the caller to hook into the Solution Kit Manager UI and provide customized UIs.
 * Methods to implement:
 *      initialize() - provide an opportunity to do initialization work (e.g. set your custom data object).
 *      createButton(...) - implement the button to launch your main screen.
 */
public abstract class SolutionKitManagerUi {

    public SolutionKitManagerUi() {
        // default constructor needed to create new instance (e.g. Class.newInstance())
    }

    /**
     * Provides data transport between the customized UI and the customized callback code.
     */
    private SolutionKitManagerContext context = new SolutionKitManagerContext();


    public SolutionKitManagerContext getContext() {
        return context;
    }

    public void setContext(SolutionKitManagerContext context) {
        this.context = context;
    }

    /**
     * Implement the button to launch your main screen.
     * @param parentPanel Parent panel is a customizable button panel in the Solution Kit Manager UI.
     * @return the button to show in the Solution Kit Manager
     */
    abstract public JButton createButton(final JPanel parentPanel);

    /**
     * Provide an opportunity to do initialization work (e.g. set your custom data object).
     * @return an instance of itself
     */
    public SolutionKitManagerUi initialize() {
        // override to implement in sub class if required
        return this;
    }

    // onOk()
}