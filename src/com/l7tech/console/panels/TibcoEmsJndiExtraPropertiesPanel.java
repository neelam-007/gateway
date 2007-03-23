/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.TibcoEmsConstants;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListenerAdapter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
    private JButton selectClientCertButton;
    private JTextField clientCertTextField;

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

        selectClientCertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                CertSearchPanel sp = new CertSearchPanel(
                        (JDialog)SwingUtilities.getAncestorOfClass(JDialog.class, TibcoEmsJndiExtraPropertiesPanel.this),
                        true);
                sp.addCertListener(new CertListenerAdapter() {
                    public void certSelected(CertEvent e) {
                        clientCertTextField.setText(e.getCert().getSubjectDn());
                    }
                });
                sp.pack();
                Utilities.centerOnParentWindow(sp);
                DialogDisplayer.display(sp);
            }
        });

        clientCertTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { fireStateChanged(); }
            public void removeUpdate(DocumentEvent e) { fireStateChanged(); }
            public void changedUpdate(DocumentEvent e) { fireStateChanged(); }
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
            clientCertTextField.setText(properties.getProperty(CERT_PROP, ""));
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
            properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_ENABLE_VERIFY_HOST, VALUE_TRUE);
            properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_TRUSTED_CERTIFICATES, VALUE_TRUSTED_CERTS);
        } else {
            properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_ENABLE_VERIFY_HOST, VALUE_FALSE);
        }

        properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_ENABLE_VERIFY_HOST_NAME, boolToStr(verifyServerHostNameCheckBox.isSelected()));

        if (useCertForClientAuthCheckBox.isSelected()) {
            properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_IDENTITY, VALUE_KEYSTORE);
            properties.setProperty(TibcoEmsConstants.TibjmsContext.SSL_PASSWORD, VALUE_KEYSTORE_PASSWORD);
            properties.setProperty(CERT_PROP, clientCertTextField.getText());
        }

        return properties;
    }

    private void enableOrDisableComponents() {
        useSslForClientAuthOnlyCheckBox.setEnabled(useSslCheckBox.isSelected());
        verifyServerCertCheckBox.setEnabled(useSslCheckBox.isSelected());
        verifyServerHostNameCheckBox.setEnabled(verifyServerCertCheckBox.isEnabled() && verifyServerCertCheckBox.isSelected());
        useCertForClientAuthCheckBox.setEnabled(useSslCheckBox.isSelected());
        selectClientCertButton.setEnabled(useCertForClientAuthCheckBox.isEnabled() && useCertForClientAuthCheckBox.isSelected());
        fireStateChanged();
    }

    @Override
    public boolean validatePanel() {
        boolean ok = true;
        if (useCertForClientAuthCheckBox.isEnabled() && useCertForClientAuthCheckBox.isSelected()) {
            if (clientCertTextField.getText().length() == 0) {
                ok = false;
            }
        }
        return ok;
    }

    private static boolean strToBool(final String s) {
        return VALUE_TRUE.equals(s);
    }

    private static String boolToStr(final boolean b) {
        return b ? VALUE_TRUE : VALUE_FALSE;
    }
}
