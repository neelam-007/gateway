/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.policy.assertion.xmlsec.WssReplayProtection;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/** @author alex */
public class ReplayProtectionPropertiesPanel extends ValidatedPanel<WssReplayProtection> {
    private TargetMessagePanel targetMessagePanel;
    private JPanel mainPanel;
    private final WssReplayProtection assertion;

    private volatile boolean targetPanelValid = true;

    public ReplayProtectionPropertiesPanel(WssReplayProtection assertion) {
        this.assertion = assertion;
        init();
    }

    protected WssReplayProtection getModel() {
        return assertion;
    }

    protected void initComponents() {
        targetMessagePanel.setModel(assertion);
        targetMessagePanel.addPropertyChangeListener("valid", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                targetPanelValid = Boolean.TRUE.equals(evt.getNewValue());
                firePropertyChange("value", null, evt.getNewValue());
                checkSyntax();
            }
        });
        checkSyntax();
        add(mainPanel, BorderLayout.CENTER);
    }

    public void focusFirstComponent() {
    }

    protected void doUpdateModel() {
        targetMessagePanel.updateModel(assertion);
    }

    @Override
    protected String getSyntaxError(WssReplayProtection model) {
        if (!targetPanelValid) return "Variable name is required";
        return null;
    }
}
