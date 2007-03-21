/*
 * Copyright (C) 2007 Layer 7 Technologies, Inc.
 */

package com.l7tech.console.panels;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListenerAdapter;

/**
 * A sub-panel for extra properties of TIBCO EMS; to be inserted into
 * {@link JmsQueuePropertiesDialog} when TIBCO EMS is selected.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class TibcoEmsExtraPropertiesPanel extends JmsExtraPropertiesPanel {
    private static final String VALUE_SSL = "ssl";
    private static final String VALUE_TRUE = "com.l7tech.server.jms.prop.boolean.true";
    private static final String VALUE_FALSE = "com.l7tech.server.jms.prop.boolean.false";
    private static final String VALUE_TRUSTED_CERTS = "com.l7tech.server.jms.prop.trustedcert.listx509";
    private static final String VALUE_KEYSTORE = "com.l7tech.server.jms.prop.keystore";
    private static final String VALUE_KEYSTORE_PASSWORD = "com.l7tech.server.jms.prop.keystore.password";
    private static final String CERT_PROP = "com.l7tech.server.jms.prop.certificate.subject";

    /** Same as com.tibco.tibjms.naming.TibjmsContext.SECURITY_PROTOCOL.
        Value is "ssl" if using SSL in TIBCO JNDI lookups. */
    private static final String SECURITY_PROTOCOL = "com.tibco.tibjms.naming.security_protocol";

    /** Same as com.tibco.tibjms.naming.TibjmsContext.SSL_AUTH_ONLY.
        A Boolean to specify if TIBCO EMS client should use SSL for authentication only. */
    private static final String SSL_AUTH_ONLY = "com.tibco.tibjms.naming.ssl_auth_only";

    /** Same as com.tibco.tibjms.naming.TibjmsContext.SSL_ENABLE_VERIFY_HOST.
        A Boolean to specify if TIBCO EMS client should verify server certificate. */
    private static final String SSL_ENABLE_VERIFY_HOST = "com.tibco.tibjms.naming.ssl_enable_verify_host";

    /** Same as com.tibco.tibjms.naming.TibjmsContext.SSL_ENABLE_VERIFY_HOST.
        A Boolean to specify if TIBCO EMS client should verify the common name in the server certificate; applies if SSL_ENABLE_VERIFY_HOST is true. */
    private static final String SSL_ENABLE_VERIFY_HOST_NAME = "com.tibco.tibjms.naming.ssl_enable_verify_hostname";

    /** Same as com.tibco.tibjms.naming.TibjmsContext.SSL_TRUSTED_CERTIFICATES.
        A Vector of trusted certificates for verifying TIBCO EMS server certificate; applies if SSL_ENABLE_VERIFY_HOST is true. */
    private static final String SSL_TRUSTED_CERTIFICATES = "com.tibco.tibjms.naming.ssl_trusted_certs";

    /** Same as com.tibco.tibjms.naming.TibjmsContext.SSL_IDENTITY.
        Keystore use in client authentication. */
    private static final String SSL_IDENTITY = "com.tibco.tibjms.naming.ssl_identity";

    /** Same as com.tibco.tibjms.naming.TibjmsContext.SSL_PASSWORD.
        Password for the private key in SSL_IDENTITY. */
    private static final String SSL_PASSWORD = "com.tibco.tibjms.naming.ssl_password";

    private JPanel mainPanel;
    private JCheckBox useSslCheckBox;
    private JCheckBox useSslForClientAuthOnlyCheckBox;
    private JCheckBox verifyServerCertCheckBox;
    private JCheckBox verifyServerHostNameCheckBox;
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
                        (JDialog)SwingUtilities.getAncestorOfClass(JDialog.class, TibcoEmsExtraPropertiesPanel.this), 
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

        setProperties(properties);
    }

    @Override
    public void setProperties(final Properties properties) {
        if (properties != null) {
            useSslCheckBox.setSelected("ssl".equals(properties.getProperty(SECURITY_PROTOCOL)));
            useSslForClientAuthOnlyCheckBox.setSelected(strToBool(properties.getProperty(SSL_AUTH_ONLY)));
            verifyServerCertCheckBox.setSelected(strToBool(properties.getProperty(SSL_ENABLE_VERIFY_HOST)));
            verifyServerHostNameCheckBox.setSelected(strToBool(properties.getProperty(SSL_ENABLE_VERIFY_HOST_NAME)));
            useCertForClientAuthCheckBox.setSelected(properties.getProperty(SSL_IDENTITY) != null);
            clientCertTextField.setText(properties.getProperty(CERT_PROP, ""));
        }

        enableOrDisableComponents();
    }

    @Override
    public Properties getProperties() {
        final Properties properties = new Properties();

        if (useSslCheckBox.isSelected()) {
            properties.setProperty(SECURITY_PROTOCOL, VALUE_SSL);
        }

        properties.setProperty(SSL_AUTH_ONLY, boolToStr(useSslForClientAuthOnlyCheckBox.isSelected()));

        if (verifyServerCertCheckBox.isSelected()) {
            properties.setProperty(SSL_ENABLE_VERIFY_HOST, VALUE_TRUE);
            properties.setProperty(SSL_TRUSTED_CERTIFICATES, VALUE_TRUSTED_CERTS);
        } else {
            properties.setProperty(SSL_ENABLE_VERIFY_HOST, VALUE_FALSE);
            properties.remove(SSL_TRUSTED_CERTIFICATES);
        }

        properties.setProperty(SSL_ENABLE_VERIFY_HOST_NAME, boolToStr(verifyServerHostNameCheckBox.isSelected()));

        if (useCertForClientAuthCheckBox.isSelected()) {
            properties.setProperty(SSL_IDENTITY, VALUE_KEYSTORE);
            properties.setProperty(SSL_PASSWORD, VALUE_KEYSTORE_PASSWORD);
            properties.setProperty(CERT_PROP, clientCertTextField.getText());
        } else {
            properties.remove(SSL_IDENTITY);
            properties.remove(SSL_PASSWORD);
            properties.remove(CERT_PROP);
        }

        return properties;
    }

    private void enableOrDisableComponents() {
        useSslForClientAuthOnlyCheckBox.setEnabled(useSslCheckBox.isSelected());
        verifyServerCertCheckBox.setEnabled(useSslCheckBox.isSelected());
        verifyServerHostNameCheckBox.setEnabled(verifyServerCertCheckBox.isEnabled() && verifyServerCertCheckBox.isSelected());
        useCertForClientAuthCheckBox.setEnabled(useSslCheckBox.isSelected());
        selectClientCertButton.setEnabled(useCertForClientAuthCheckBox.isEnabled() && useCertForClientAuthCheckBox.isSelected());
    }

    private static boolean strToBool(final String s) {
        return VALUE_TRUE.equals(s);
    }

    private static String boolToStr(final boolean b) {
        return b ? VALUE_TRUE : VALUE_FALSE;
    }
}
