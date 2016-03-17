package com.l7tech.policy.solutionkit;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WARNING: this class is under development and is currently for CA internal use only.
 * This interface contract may change without notice.
 *
 * Provides an opportunity for the caller to hook into the Solution Kit Manager UI and provide customized UIs.
 * Methods to implement:
 *      initialize() - provide an opportunity to do initialization work.
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
     * Provide an opportunity to do initialization work.
     * @return an instance of itself
     */
    public SolutionKitManagerUi initialize() {
        // override to implement in sub class if required
        return this;
    }

    // onOk()

    /**
     * Keeps a map of all {@code SolutionKit}'s context objects (with key being the {@code SolutionKit} id/guid),
     * including the parent kit.
     */
    private final Map<String, SolutionKitManagerContext> contextMap = new LinkedHashMap<String, SolutionKitManagerContext>();

    /**
     * Get individual {@link SolutionKitManagerContext} for all {@code SolutionKit}'s in the skar,
     * mapped by the {@code SolutionKit} {@code GUID}.
     * <p/>
     * Use this method to retrieve info about other kits in the skar, like see their meta info,
     * check whether they've been selected for installation/upgrade by the user or even access their individual
     * key-value pairs.
     *
     * @return a read-only {@code Map} of individual {@link SolutionKitManagerContext}.
     */
    public final Map<String, SolutionKitManagerContext> getContextMap() {
        return contextMap;
    }
}