/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.GoidUpgradeMapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private JCheckBox enforceHostnameVerificationCheckBox;
    private JLabel keystoreLabel;

    private final ActionListener enableDisableListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            enableOrDisableComponents();
        }
    };

    public MQSeriesQueueExtraPropertiesPanel(final Properties properties) {
        setLayout(new BorderLayout());
        add(mainPanel);

        sslCheckbox.addActionListener(enableDisableListener);
        clientAuthCheckbox.addActionListener(enableDisableListener);
        keystoreComboBox.addActionListener(enableDisableListener);
        keystoreComboBox.selectDefaultSsl();

        setProperties(properties);
    }

    @Override
    public void setProperties(final Properties properties) {
        if (properties != null) {
            sslCheckbox.setSelected(properties.getProperty(JmsConnection.PROP_CUSTOMIZER) != null);
            final String sclientAuth = properties.getProperty(JmsConnection.PROP_QUEUE_USE_CLIENT_AUTH);
            final boolean clientAuth = sclientAuth == null || "true".equals(sclientAuth);
            clientAuthCheckbox.setSelected(clientAuth);
            final String skid = properties.getProperty(JmsConnection.PROP_QUEUE_SSG_KEYSTORE_ID);
            if (skid != null) {
                final String alias = properties.getProperty(JmsConnection.PROP_QUEUE_SSG_KEY_ALIAS);
                final Goid keystoreId = GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, skid);
                keystoreComboBox.select(keystoreId, alias);
            }
        }

        enableOrDisableComponents();
    }

    @Override
    public Properties getProperties() {
        final Properties properties = new Properties();

        if (sslCheckbox.isSelected()) {
            properties.setProperty(JmsConnection.PROP_CUSTOMIZER, "com.l7tech.server.transport.jms.prov.MQSeriesCustomizer");
            if (clientAuthCheckbox.isSelected()) {
                properties.setProperty(JmsConnection.PROP_QUEUE_USE_CLIENT_AUTH, "true");
                String keyAlias = keystoreComboBox.getSelectedKeyAlias();
                Goid keyStoreId = keystoreComboBox.getSelectedKeystoreId();
                if ( keyAlias != null && !Goid.isDefault(keyStoreId) ) {
                    properties.setProperty(JmsConnection.PROP_QUEUE_SSG_KEY_ALIAS, keyAlias);
                    properties.setProperty(JmsConnection.PROP_QUEUE_SSG_KEYSTORE_ID, Goid.toString(keyStoreId));
                }
            } else {
                properties.setProperty(JmsConnection.PROP_QUEUE_USE_CLIENT_AUTH, "false"); // Null is a special case
            }
        }

        return properties;
    }

    @Override
    public boolean isKnownProperty( final String propertyName ) {
        return false;
    }

    private void enableOrDisableComponents() {
        final boolean ssl = sslCheckbox.isSelected();
        clientAuthCheckbox.setEnabled(ssl);

        final boolean client = ssl && clientAuthCheckbox.isSelected();
        keystoreLabel.setEnabled(client);
        keystoreComboBox.setEnabled(client);

        fireStateChanged();
    }

    @Override
    public boolean validatePanel() {
        return true;
    }
}