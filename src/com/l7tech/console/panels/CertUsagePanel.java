package com.l7tech.console.panels;

import com.l7tech.common.security.TrustedCert;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertUsagePanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JPanel certUsagePane;
    private JCheckBox signingServerCertCheckBox;
    private JCheckBox signingSAMLTokenCheckBox;
    private JCheckBox signingClientCertCheckBox;
    private JCheckBox outboundSSLConnCheckBox;
    private static Logger logger = Logger.getLogger(CertUsagePanel.class.getName());
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());

    public CertUsagePanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
    }

    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) {

        if (settings != null) {

            if (settings instanceof TrustedCert) {
                TrustedCert tc = (TrustedCert) settings;

                tc.setTrustedForSigningClientCerts(signingClientCertCheckBox.isSelected());
                tc.setTrustedForSigningSamlTokens(signingSAMLTokenCheckBox.isSelected());
                tc.setTrustedForSigningServerCerts(signingServerCertCheckBox.isSelected());
                tc.setTrustedForSsl(outboundSSLConnCheckBox.isSelected());
            }
        }
    }

    /**
      * Populate the configuration data from the wizard input object to the visual components of the panel.
      *
      * @param settings The current value of configuration items in the wizard input object.
      * @throws IllegalArgumentException if the data provided by the wizard are not valid.
      */
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (!(settings instanceof TrustedCert))
            throw new IllegalArgumentException("The settings object must be TrustedCert");

        TrustedCert trustedCert = (TrustedCert) settings;

        X509Certificate cert = null;
        try {
            cert = trustedCert.getCertificate();
        } catch (CertificateException e) {
             logger.warning(resources.getString("cert.decode.error"));
            JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.decode.error"),
                                           resources.getString("save.error.title"),
                                           JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            logger.warning(resources.getString("cert.decode.error"));
            JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.decode.error"),
                                           resources.getString("save.error.title"),
                                           JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Specify Certificate Options";
    }

    /**
     * Provide the description for the step being taken on this panel.
     *
     * @return  String  The descritpion of the step.
     */
    public String getDescription() {
        return "Select one or more certificate usage options:\n" +
                "\n" +
                "<p>Outbound SSL Connections: Using HTTPS, the SecureSpan Gateway can connect to \n" +
                "protected Web services hosted on SSL servers that use this certificate. If a \n" +
                "Web service's SSL uses a self-signed certificate, then that certificate must \n" +
                "be imported with this option.\n" +
                "\n" +
                "<p>Signing Certificates for Outbound SSL Connections: Using HTTPS, the \n" +
                "SecureSpan Gateway can connect to protected Web services hosted on SSL servers \n" +
                "whose certificates were signed by this certificate authority. If several \n" +
                "protected Web services' SSL server certificates are signed using an in-house \n" +
                "certificate authority, then the SecureSpan Gateway can connect to all of them \n" +
                "if the certificate authority's certificate is imported with this option.\n" +
                "\n" +
                "<p>Signing Client Certificates: A Federated Identity Provider can be configured \n" +
                "to authorize identities whose X.509 certificates were signed by this \n" +
                "certificate authority certificate.\n" +
                "\n" +
                "<p>Signing SAML Tokens: A Federated Identity Provider can be configured to \n" +
                "authorize identities whose SAML Tokens were signed by this certificate.";
    }

     /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */
     public boolean canFinish() {
        return true;
    }
}