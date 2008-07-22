/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.TargetMessageType;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.Dimension;
import java.awt.BorderLayout;

/** @author alex */
public class TargetMessagePanel extends JPanel {
    private JPanel mainPanel;
    private JRadioButton requestRadioButton;
    private JRadioButton responseRadioButton;
    private JRadioButton otherRadioButton;
    private JTextField otherMessageVariableTextfield;

    private final RunOnChangeListener listener = new RunOnChangeListener(new Runnable() {
        public void run() {
            enableDisable();
            final boolean valid = !otherRadioButton.isSelected() || otherMessageVariableTextfield.getText().trim().length() > 0;
            firePropertyChange("valid", null, valid);
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

        enableDisable();
        this.setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

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
            var = otherMessageVariableTextfield.getText().trim();
        } else {
            throw new IllegalStateException();
        }

        model.setTarget(type);
        model.setOtherTargetMessageVariable(var);
    }
}
