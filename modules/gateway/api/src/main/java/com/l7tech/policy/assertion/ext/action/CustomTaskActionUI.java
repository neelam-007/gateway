package com.l7tech.policy.assertion.ext.action;

import javax.swing.*;
import java.awt.*;

/**
 * Task action to display in the Tasks->Additional Actions menu in the Policy Manager UI.
 *
 * <code>CustomTaskActionUI</code> implementations are specified in the custom assertion configuration.
 */
public interface CustomTaskActionUI {

    /**
     * Gets the task action name.
     *
     * @return the task action name
     */
    String getName();

    /**
     * Gets the task action description.
     *
     * @return the task action description
     */
    String getDescription();

    /**
     * Gets the icon for the task action.
     *
     * @return the task action icon
     */
    ImageIcon getIcon();

    /**
     * Gets the task action dialog.
     * This dialog is displayed when this task action is selected in the Policy Manager UI.
     *
     * @param owner the frame from which the dialog is displayed
     * @return the task action dialog
     */
    JDialog getDialog (Frame owner);
}