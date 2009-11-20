package com.l7tech.console.panels;

import com.l7tech.gui.util.FontUtil;
import com.l7tech.security.cert.TrustedCert;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertUsagePanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JCheckBox signingServerCertCheckBox;
    private JCheckBox signingSAMLTokenCheckBox;
    private JCheckBox signingClientCertCheckBox;
    private JCheckBox outboundSSLConnCheckBox;
    private JCheckBox samlAttestingEntityCheckBox;
    private JLabel headerLabel;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());

    private CertValidationPanel certValidationPanel;

    public CertUsagePanel(WizardStepPanel next) {
        super(next);
        initialize();
        if (next instanceof CertValidationPanel) certValidationPanel = (CertValidationPanel)next;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        FontUtil.resizeFont(headerLabel, 1.2);

        ActionListener sslOptionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (certValidationPanel != null)
                    certValidationPanel.setVerifySslHostnameCheckBoxEnabled (
                        signingServerCertCheckBox.isSelected() || outboundSSLConnCheckBox.isSelected());
            }
        };
        signingServerCertCheckBox.addActionListener(sslOptionListener);
        outboundSSLConnCheckBox.addActionListener(sslOptionListener);
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
                tc.setTrustedAsSamlIssuer(signingSAMLTokenCheckBox.isSelected());
                tc.setTrustedForSigningServerCerts(signingServerCertCheckBox.isSelected());
                tc.setTrustedForSsl(outboundSSLConnCheckBox.isSelected());
                tc.setTrustedAsSamlAttestingEntity(samlAttestingEntityCheckBox.isSelected());

                tc.setTrustAnchor(certValidationPanel == null? true : certValidationPanel.isTrustAnchor());
                tc.setVerifyHostname(certValidationPanel == null? (tc.isTrustedForSsl() || tc.isTrustedForSigningServerCerts()) : certValidationPanel.isVerifyHostname());
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
        return  resources.getString("usage.desc");
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