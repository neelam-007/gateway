package com.l7tech.policy.assertion.ext.commonui;

import javax.swing.*;

/**
 * The target variable panel.
 */
public interface CustomTargetVariablePanel {

    /**
     *  Sets whether or not this component is enabled.
     *
     * @param enabled true if this component should be enabled, false otherwise
     */
    void setEnabled (boolean enabled);

    /**
     *  Sets whether or not empty entry is valid.
     *
     * @param acceptEmpty true if empty entry is valid, false otherwise
     */
    void setAcceptEmpty(boolean acceptEmpty);

    /**
     * Checks if the entry is valid.
     *
     * @return true if the entry is valid, false otherwise
     */
    boolean isEntryValid();

    /**
     *  Sets suffixes.
     *
     * @param suffixes suffixes
     */
    void setSuffixes(String[] suffixes);

    /**
     * Sets the variable.
     *
     * @param var variable
     */
    void setVariable(String var);

    /**
     * Gets the variable.
     *
     * @return the variable
     */
    String getVariable();

    /**
     * Gets the panel.
     *
     * @return the panel
     */
    JPanel getPanel();
}