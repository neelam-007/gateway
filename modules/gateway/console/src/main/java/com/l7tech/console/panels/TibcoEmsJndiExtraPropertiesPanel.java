/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.TibcoEmsConstants;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.util.GoidUpgradeMapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Properties;

/**
 * A sub-panel for configuring additional JNDI settings for TIBCO EMS; to be
 * inserted into <code>jndiExtraPropertiesOuterPanel</code> of
 * {@link JmsQueuePropertiesDialog} when TIBCO EMS is selected.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class TibcoEmsJndiExtraPropertiesPanel extends JmsExtraPropertiesPanel {
    private JPanel mainPanel;
    private JCheckBox useSslCheckBox;
    private JCheckBox useSslForClientAuthOnlyCheckBox;
    private JCheckBox verifyServerCertCheckBox;
    private JCheckBox verifyServerHostNameCheckBox;
    private JCheckBox useCertForClientAuthCheckBox;
    private PrivateKeysComboBox clientCertsComboBox;

    public TibcoEmsJndiExtraPropertiesPanel(final Properties properties) {
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
            useSslCheckBox.setSelected(TibcoEmsConstants.SSL.equals(properties.getProperty(TibcoEmsConstants.TibjmsContext.SECURITY_PROTOCOL)));
            useSslForClientAuthOnlyCheckBox.setSelected(strToBool(properties.getProperty(TibcoEmsConstants.TibjmsContext.SSL_AUTH_ONLY)));
            verifyServerCertCheckBox.setSelected(strToBool(properties.getProperty(TibcoEmsConstants.TibjmsContext.SSL_ENABLE_VERIFY_HOST)));
            verifyServerHostNameCheckBox.setSelected(strToBool(properties.getProperty(TibcoEmsConstants.TibjmsContext.SSL_ENABLE_VERIFY_HOST_NAME)));
            useCertForClientAuthCheckBox.setSelected(properties.getProperty(TibcoEmsConstants.TibjmsContext.SSL_IDENTITY) != null);

            Goid keystoreId = GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, properties.getProperty(JmsConnection.PROP_JNDI_SSG_KEYSTORE_ID, PersistentEntity.DEFAULT_GOID.toHexString()));
            String keyAlias = Goid.isDefault(keystoreId)? null : properties.getProperty(JmsConnection.PROP_JNDI_SSG_KEY_ALIAS);
            int index = clientCertsComboBox.select(keystoreId, keyAlias);
            if (index < 0) clientCertsComboBox.setSelectedIndex(0);
        }

        enableOrDisableComponents();
    }

    @Override
    public Properties getProperties() {
        final Properties properties = new Properties();

        if (useSslCheckBox.isSelected()) {
            properties.setProperty(TibcoEmsConstants.TibjmsContext.SECURITY_PROTOCOL, TibcoEmsConstants.SSL);
        }

        properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_AUTH_ONLY, boolToStr(useSslForClientAuthOnlyCheckBox.isSelected()));

        if (verifyServerCertCheckBox.isSelected()) {
            properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_ENABLE_VERIFY_HOST, JmsConnection.VALUE_BOOLEAN_TRUE);
            properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_TRUSTED_CERTIFICATES, JmsConnection.VALUE_TRUSTED_LIST);
        } else {
            properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_ENABLE_VERIFY_HOST, JmsConnection.VALUE_BOOLEAN_FALSE);
        }

        properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_ENABLE_VERIFY_HOST_NAME, boolToStr(verifyServerHostNameCheckBox.isSelected()));

        if (useCertForClientAuthCheckBox.isSelected()) {
            final String whichKey = "\t" + clientCertsComboBox.getSelectedKeystoreId() +
                                    "\t" + clientCertsComboBox.getSelectedKeyAlias();
            properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_IDENTITY, JmsConnection.VALUE_KEYSTORE + whichKey);
            properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_PASSWORD, JmsConnection.VALUE_KEYSTORE_PASSWORD + whichKey);

            Goid selectedKeystoreId = clientCertsComboBox.getSelectedKeystoreId();
            properties.setProperty(JmsConnection.PROP_JNDI_SSG_KEYSTORE_ID, selectedKeystoreId.toHexString());

            String selectedKeyAlias;
            if (Goid.isDefault(selectedKeystoreId)) {
                try {
                    SsgKeyEntry defaultSslKey = Registry.getDefault().getTrustedCertManager().findKeyEntry(null, PersistentEntity.DEFAULT_GOID);
                    selectedKeyAlias = defaultSslKey.getAlias();
                } catch (Exception e) {
                    throw new RuntimeException("Cannot find Default SSL Key", e);
                }
            } else {
                selectedKeyAlias = clientCertsComboBox.getSelectedKeyAlias();
            }
            properties.setProperty(JmsConnection.PROP_JNDI_SSG_KEY_ALIAS, selectedKeyAlias);
        }

        return properties;
    }

    @Override
    public boolean isKnownProperty( final String propertyName ) {
        return TibcoEmsConstants.TibjmsContext.SECURITY_PROTOCOL.equals( propertyName ) ||
               TibcoEmsConstants.TibjmsContext.SSL_AUTH_ONLY.equals( propertyName )  ||
               TibcoEmsConstants.TibjmsContext.SSL_ENABLE_VERIFY_HOST.equals( propertyName )  ||
               TibcoEmsConstants.TibjmsContext.SSL_TRUSTED_CERTIFICATES.equals( propertyName )  ||
               TibcoEmsConstants.TibjmsContext.SSL_ENABLE_VERIFY_HOST_NAME.equals( propertyName )  ||
               TibcoEmsConstants.TibjmsContext.SSL_IDENTITY.equals( propertyName )  ||
               TibcoEmsConstants.TibjmsContext.SSL_PASSWORD.equals( propertyName ) ;
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
        clientCertsComboBox = new PrivateKeysComboBox(false, false, false);
    }
}
