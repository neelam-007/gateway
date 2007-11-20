/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import com.l7tech.common.transport.jms.JmsConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;

/**
 * A sub-panel for configuring additional Queue connection settings for MQSeries;
 * to be inserted into <code>queueExtraPropertiesOuterPanel</code> of
 * {@link com.l7tech.console.panels.JmsQueuePropertiesDialog} when MQSeries is selected.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class MQSeriesQueueExtraPropertiesPanel extends JmsExtraPropertiesPanel {
    private JPanel mainPanel;
    private PrivateKeysComboBox keystoreComboBox;
    private JCheckBox clientAuthCheckbox;
    private JCheckBox sslCheckbox;
    private JLabel keystoreLabel;
    private JCheckBox enforceHostnameVerificationCheckBox;

    public MQSeriesQueueExtraPropertiesPanel(final Properties properties) {
        setLayout(new BorderLayout());
        add(mainPanel);

        sslCheckbox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableComponents();
            }
        });

        setProperties(properties);
    }

    @Override
    public void setProperties(final Properties properties) {
        if (properties != null) {
            sslCheckbox.setSelected(properties.getProperty(JmsConnection.PROP_CUSTOMIZER) != null);
        }

        enableOrDisableComponents();
    }

    @Override
    public Properties getProperties() {
        final Properties properties = new Properties();

        if (sslCheckbox.isSelected()) {
            properties.setProperty(JmsConnection.PROP_CUSTOMIZER, "com.l7tech.server.transport.jms.prov.MQSeriesCustomizer");
        }

        return properties;
    }

    private void enableOrDisableComponents() {
        fireStateChanged();
    }

    @Override
    public boolean validatePanel() {
        return true;
    }
}