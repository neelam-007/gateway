/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.TibcoEmsConstants;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;

/**
 * A sub-panel for configuring additional Queue connection settings for TIBCO EMS;
 * to be inserted into <code>queueExtraPropertiesOuterPanel</code> of
 * {@link JmsQueuePropertiesDialog} when TIBCO EMS is selected.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class TibcoEmsQueueExtraPropertiesPanel extends JmsExtraPropertiesPanel {
    private JPanel mainPanel;
    private JCheckBox useSslCheckBox;
    private JCheckBox useSslForClientAuthOnlyCheckBox;
    private JCheckBox verifyServerCertCheckBox;
    private JCheckBox verifyServerHostNameCheckBox;
    private JCheckBox useCertForClientAuthCheckBox;
    private PrivateKeysComboBox clientCertsComboBox;

    public TibcoEmsQueueExtraPropertiesPanel(final Properties properties) {
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

        verifyServerCertCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableComponents();
            }
        });

        useCertForClientAuthCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableComponents();
            }
        });

        clientCertsComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
            }
        });

        setProperties(properties);
    }

    @Override
    public void setProperties(final Properties properties) {
        if (properties != null) {
            // We borrow property keys defined in com.tibco.tibjms.naming.TibjmsSSL to transmit
            // configurations to com.l7tech.server.transport.jms.prov.TibcoConnectionFactoryCustomizer.
            useSslCheckBox.setSelected(properties.getProperty(JmsConnection.PROP_CUSTOMIZER) != null);
            useSslForClientAuthOnlyCheckBox.setSelected(strToBool(properties.getProperty(TibcoEmsConstants.TibjmsSSL.AUTH_ONLY)));
            verifyServerCertCheckBox.setSelected(strToBool(properties.getProperty(TibcoEmsConstants.TibjmsSSL.ENABLE_VERIFY_HOST)));
            verifyServerHostNameCheckBox.setSelected(strToBool(properties.getProperty(TibcoEmsConstants.TibjmsSSL.ENABLE_VERIFY_HOST_NAME)));
            useCertForClientAuthCheckBox.setSelected(properties.getProperty(TibcoEmsConstants.TibjmsSSL.IDENTITY) != null);
            
            Long keystoreId = Long.parseLong(properties.getProperty(JmsConnection.PROP_QUEUE_SSG_KEYSTORE_ID, "-1"));
            String keyAlias = keystoreId == -1? null : properties.getProperty(JmsConnection.PROP_QUEUE_SSG_KEY_ALIAS);
            int index = clientCertsComboBox.select(keystoreId, keyAlias);
            if (index < 0) clientCertsComboBox.setSelectedIndex(0);
        }

        enableOrDisableComponents();
    }

    @Override
    public Properties getProperties() {
        final Properties properties = new Properties();

        if (useSslCheckBox.isSelected()) {
            properties.setProperty(JmsConnection.PROP_CUSTOMIZER, "com.l7tech.server.transport.jms.prov.TibcoConnectionFactoryCustomizer");
        }

        properties.setProperty(TibcoEmsConstants.TibjmsSSL.AUTH_ONLY, boolToStr(useSslForClientAuthOnlyCheckBox.isSelected()));

        if (verifyServerCertCheckBox.isSelected()) {
            properties.setProperty(TibcoEmsConstants.TibjmsSSL.ENABLE_VERIFY_HOST, JmsConnection.VALUE_BOOLEAN_TRUE);
            properties.setProperty(TibcoEmsConstants.TibjmsSSL.TRUSTED_CERTIFICATES, JmsConnection.VALUE_TRUSTED_LIST);
        } else {
            properties.setProperty(TibcoEmsConstants.TibjmsSSL.ENABLE_VERIFY_HOST, JmsConnection.VALUE_BOOLEAN_FALSE);
        }

        properties.setProperty(TibcoEmsConstants.TibjmsSSL.ENABLE_VERIFY_HOST_NAME, boolToStr(verifyServerHostNameCheckBox.isSelected()));

        if (useCertForClientAuthCheckBox.isSelected()) {
            final String whichKey = "\t" + clientCertsComboBox.getSelectedKeystoreId() +
                                    "\t" + clientCertsComboBox.getSelectedKeyAlias();
            properties.setProperty(TibcoEmsConstants.TibjmsSSL.IDENTITY, JmsConnection.VALUE_KEYSTORE_BYTES + whichKey);
            properties.setProperty(TibcoEmsConstants.TibjmsSSL.PASSWORD, JmsConnection.VALUE_KEYSTORE_PASSWORD + whichKey);
            
            long selectedKeystoreId = clientCertsComboBox.getSelectedKeystoreId();
            properties.setProperty(JmsConnection.PROP_QUEUE_SSG_KEYSTORE_ID, Long.toString(selectedKeystoreId));

            String selectedKeyAlias;
            if (selectedKeystoreId == -1) {
                try {
                    SsgKeyEntry defaultSslKey = Registry.getDefault().getTrustedCertManager().findKeyEntry(null, -1);
                    selectedKeyAlias = defaultSslKey.getAlias();
                } catch (Exception e) {
                    throw new RuntimeException("Cannot find Default SSL Key", e);
                }
            } else {
                selectedKeyAlias = clientCertsComboBox.getSelectedKeyAlias();
            }
            properties.setProperty(JmsConnection.PROP_QUEUE_SSG_KEY_ALIAS, selectedKeyAlias);
        }

        return properties;
    }

    private void enableOrDisableComponents() {
        useSslForClientAuthOnlyCheckBox.setEnabled(useSslCheckBox.isSelected());
        verifyServerCertCheckBox.setEnabled(useSslCheckBox.isSelected());
        verifyServerHostNameCheckBox.setEnabled(verifyServerCertCheckBox.isEnabled() && verifyServerCertCheckBox.isSelected());
        useCertForClientAuthCheckBox.setEnabled(useSslCheckBox.isSelected());
        clientCertsComboBox.setEnabled(useCertForClientAuthCheckBox.isEnabled() && useCertForClientAuthCheckBox.isSelected());
        fireStateChanged();
    }

    @Override
    public boolean validatePanel() {
        boolean ok = true;
        if (useCertForClientAuthCheckBox.isEnabled() && useCertForClientAuthCheckBox.isSelected()) {
            if (clientCertsComboBox.getSelectedIndex() == -1) {
                ok = false;
            }
        }
        return ok;
    }

    private static boolean strToBool(final String s) {
        return JmsConnection.VALUE_BOOLEAN_TRUE.equals(s);
    }

    private static String boolToStr(final boolean b) {
        return b ? JmsConnection.VALUE_BOOLEAN_TRUE : JmsConnection.VALUE_BOOLEAN_FALSE;
    }

    private void createUIComponents() {
        // TIBCO EMS API cannot use private keys stored in hardware keystore.
        clientCertsComboBox = new PrivateKeysComboBox(false);
    }
}
