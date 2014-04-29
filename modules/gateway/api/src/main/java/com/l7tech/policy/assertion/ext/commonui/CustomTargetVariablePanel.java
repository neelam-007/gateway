package com.l7tech.policy.assertion.ext.commonui;

import javax.swing.*;
import javax.swing.event.ChangeListener;

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
     * Sets whether or not the value will be read.
     * <p>
     * Default is false.
     *
     * @param valueWillBeRead true if value will be read, false otherwise
     */
    void setValueWillBeRead(boolean valueWillBeRead);

    /**
     * Sets whether or not the value will be written.
     * <p>
     * Default is true.
     *
     * @param valueWillBeWritten true if value will be written, false otherwise
     */
    void setValueWillBeWritten(boolean valueWillBeWritten);

    /**
     * Sets whether or not to allow variable syntax such as array de-referencing (eg, "foo[4]").
     * <p>
     * Default is false.
     *
     * @param alwaysPermitSyntax true if additional syntax should always be allowed, false otherwise
     */
    void setAlwaysPermitSyntax(boolean alwaysPermitSyntax);

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
     * Adds a <code>ChangeListener</code>.
     * <p>
     * The <code>ChangeListener</code> will receive a <code>ChangeEvent</code>
     * when a change has been made in the target variable text field.
     *
     * @param listener the <code>ChangeListener</code> that is to be notified
     */
    void addChangeListener(ChangeListener listener);

    /**
     * Removes a <code>ChangeListener</code>.
     *
     * @param listener the <code>ChangeListener</code> to remove
     */
    void removeChangeListener(ChangeListener listener);

    /**
     * Gets the panel.
     *
     * @return the panel
     */
    JPanel getPanel();
}