/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.client.gui.dialogs;

import com.l7tech.gui.widgets.PropertyPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Content of SSG property dialog tab for "Bridge Policy".
 */
class BridgePolicyPanel extends JPanel {
    private JPanel rootPanel;
    private JCheckBox cbHeaderPassthrough;
    private JCheckBox cbUseSsl;
    private PropertyPanel propertyPanel;

    public BridgePolicyPanel(final boolean allowUseSsl) {
        setLayout(new BorderLayout());

        if (!allowUseSsl) {
            cbUseSsl.setEnabled(false);
            cbUseSsl.setSelected(false);
            cbUseSsl.setVisible(false);
        }

        propertyPanel.setTitle("Additional Properties");
        propertyPanel.setPropertyEditTitle("Client Policy Property");

        add(rootPanel, BorderLayout.CENTER);
    }

    boolean isHeaderPassthrough() {
        return cbHeaderPassthrough.isSelected();
    }

    void setHeaderPassthrough(boolean passthrough) {
        cbHeaderPassthrough.setSelected(passthrough);
    }

    boolean isUseSslByDefault() {
        return cbUseSsl.isSelected();
    }

    void setUseSslByDefault(boolean ssl) {
        cbUseSsl.setSelected(ssl);
    }

    Map<String, String> getProperties() {
        return propertyPanel.getProperties();
    }

    void setProperties(Map<String, String> newprops) {
        propertyPanel.setProperties(newprops);
    }

    private void createUIComponents() {
        propertyPanel = new PropertyPanel();
    }
}
