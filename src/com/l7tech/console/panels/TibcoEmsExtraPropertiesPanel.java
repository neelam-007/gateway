/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Hashtable;

/**
 * @author rmak
 * @since SecureSpan 3.7
 */
public class TibcoEmsExtraPropertiesPanel extends JPanel {
    private JPanel mainPanel;
    private JCheckBox useSslCheckBox;
    private JCheckBox useSslForClientAuthOnlyCheckBox;
    private JCheckBox verifyServerCertCheckBox;
    private JCheckBox useCertForClientAuthCheckBox;
    private JTextField clientCertTextField;
    private JButton selectClientCertButton;

    public TibcoEmsExtraPropertiesPanel(Hashtable properties) {
        setLayout(new BorderLayout());
        add(mainPanel);

        useSslForClientAuthOnlyCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableComponents();
            }
        });

        useCertForClientAuthCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableComponents();
            }
        });

        setProperties(properties);
    }

    public void setProperties(Hashtable properties) {
        // TODO


        enableOrDisableComponents();
    }

    public Hashtable getProperties() {
        final Hashtable properties = new Hashtable();
        // TODO


        return properties;
    }

    private void enableOrDisableComponents() {
        useSslForClientAuthOnlyCheckBox.setEnabled(useSslCheckBox.isSelected());
        verifyServerCertCheckBox.setEnabled(useSslCheckBox.isSelected());
        useCertForClientAuthCheckBox.setEnabled(useSslCheckBox.isSelected());
        selectClientCertButton.setEnabled(useCertForClientAuthCheckBox.isEnabled() && useCertForClientAuthCheckBox.isSelected());
    }
}
