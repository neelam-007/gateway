/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.gui.widgets.TargetMessagePanel;
import com.l7tech.common.gui.widgets.ValidatedPanel;
import com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/** @author alex */
public class ReplayProtectionPropertiesPanel extends ValidatedPanel<RequestWssReplayProtection> {
    private TargetMessagePanel targetMessagePanel;
    private JPanel mainPanel;
    private final RequestWssReplayProtection assertion;

    private volatile boolean targetPanelValid = true;

    public ReplayProtectionPropertiesPanel(RequestWssReplayProtection assertion) {
        this.assertion = assertion;
        init();
    }

    protected RequestWssReplayProtection getModel() {
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
    protected String getSyntaxError(RequestWssReplayProtection model) {
        if (!targetPanelValid) return "Variable name is required";
        return null;
    }
}
