/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;

/**
 * @author rmak
 * @since SecureSpan 3.7
 */
public class TibcoEmsExtraPropertiesPanel extends JmsExtraPropertiesPanel {
    private JPanel mainPanel;
    private JCheckBox useSslCheckBox;
    private JCheckBox useSslForClientAuthOnlyCheckBox;
    private JCheckBox verifyServerCertCheckBox;
    private JCheckBox useCertForClientAuthCheckBox;
    private JButton selectClientCertButton;
    private JTextField clientCertTextField;

    public TibcoEmsExtraPropertiesPanel(final Properties properties) {
        setLayout(new BorderLayout());
        add(mainPanel);

        useSslCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableComponents();
            }
        });

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

        // TODO wire up selectClientCertButton and clientCertTextField

        setProperties(properties);
    }

    @Override
    public void setProperties(final Properties properties) {
        // TODO apply properties to components


        enableOrDisableComponents();
    }

    @Override
    public Properties getProperties() {
        final Properties properties = new Properties();
        // TODO create properties from components


        return properties;
    }

    private void enableOrDisableComponents() {
        useSslForClientAuthOnlyCheckBox.setEnabled(useSslCheckBox.isSelected());
        verifyServerCertCheckBox.setEnabled(useSslCheckBox.isSelected());
        useCertForClientAuthCheckBox.setEnabled(useSslCheckBox.isSelected());
        selectClientCertButton.setEnabled(useCertForClientAuthCheckBox.isEnabled() && useCertForClientAuthCheckBox.isSelected());
    }
}
