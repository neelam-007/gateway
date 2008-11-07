/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.TargetMessageType;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;

/** @author alex */
public class TargetMessagePanel extends JPanel {
    private static final Pattern FIXSTART = Pattern.compile("\\s*(?:\\$\\{)?\\s*");
    private static final Pattern FIXEND = Pattern.compile("\\s*(?:\\})?\\s*");

    private JPanel mainPanel;
    private JRadioButton requestRadioButton;
    private JRadioButton responseRadioButton;
    private JRadioButton otherRadioButton;
    private JTextField otherMessageVariableTextfield;
    private boolean allowNonMessageVariables = false;

    private final RunOnChangeListener listener = new RunOnChangeListener(new Runnable() {
        public void run() {
            enableDisable();
            final boolean valid = !otherRadioButton.isSelected() || otherMessageVariableTextfield.getText().trim().length() > 0;
            firePropertyChange("valid", null, valid);
            if (valid) {
                final ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "updated", 0);
                for (ActionListener actionListener : listenerList.getListeners(ActionListener.class)) {
                    actionListener.actionPerformed(event);
                }
            }
        }
    });

    private void enableDisable() {
        otherMessageVariableTextfield.setEnabled(otherRadioButton.isSelected());
    }

    public TargetMessagePanel() {
        this("Target Message");
    }

    public TargetMessagePanel(String title) {
        setTitle(title);
        initComponents();
    }

    public void setAllowNonMessageVariables(boolean allowNonMessageVariables) {
        if (allowNonMessageVariables != this.allowNonMessageVariables) {
            otherRadioButton.setText(allowNonMessageVariables
                    ? "Other Context Variable:"
                    : "Other Message Variable");
            this.allowNonMessageVariables = allowNonMessageVariables;
        }
    }

    public void setTitle(String title) {
        Border border = getBorder();
        if (border instanceof TitledBorder) {
            TitledBorder titledBorder = (TitledBorder)border;
            titledBorder.setTitle(title);
        }
    }

    public String getTitle() {
        Border border = getBorder();
        if (border instanceof TitledBorder) {
            TitledBorder titledBorder = (TitledBorder)border;
            return titledBorder.getTitle();
        }
        throw new IllegalStateException();
    }

    /**
     * Configure the GUI controls to reflect the settings of the specified model object.
     *
     * @param model the object whose values are to be read to configure the GUI controls.  Required.
     */
    public void setModel(MessageTargetable model) {
        switch(model.getTarget()) {
            case REQUEST:
                requestRadioButton.setSelected(true);
                break;
            case RESPONSE:
                responseRadioButton.setSelected(true);
                break;
            case OTHER:
                otherRadioButton.setSelected(true);
                otherMessageVariableTextfield.setText(model.getOtherTargetMessageVariable());
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    protected void initComponents() {
        requestRadioButton.addActionListener(listener);
        responseRadioButton.addActionListener(listener);
        otherRadioButton.addActionListener(listener);
        otherMessageVariableTextfield.getDocument().addDocumentListener(listener);
        Utilities.attachDefaultContextMenu(otherMessageVariableTextfield);

        enableDisable();
        this.setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    public void setBorder(Border border) {
        if (mainPanel != null) mainPanel.setBorder(border);
    }

    @Override
    public Border getBorder() {
        return mainPanel == null ? null : mainPanel.getBorder();
    }

    private String getVariableName() {
        String varname = otherMessageVariableTextfield.getText().trim();
        // As a convenience to our poor confused users, we'll remove any ${ } surrounding the variable name
        varname = FIXSTART.matcher(varname).replaceAll("");
        varname = FIXEND.matcher(varname).replaceAll("");
        return varname;
    }

    /**
     * Update the specified model object to correspond to the values the user has set
     * by manipulating the GUI controls.
     *
     * @param model the object to update.  Required.
     */
    public void updateModel(MessageTargetable model) {
        final TargetMessageType type;
        final String var;
        if (requestRadioButton.isSelected()) {
            type = TargetMessageType.REQUEST;
            var = null;
        } else if (responseRadioButton.isSelected()) {
            type = TargetMessageType.RESPONSE;
            var = null;
        } else if (otherRadioButton.isSelected()) {
            type = TargetMessageType.OTHER;
            var = getVariableName();
        } else {
            throw new IllegalStateException();
        }

        model.setTarget(type);
        model.setOtherTargetMessageVariable(var);
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }
}
