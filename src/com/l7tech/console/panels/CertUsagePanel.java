package com.l7tech.console.panels;

import com.l7tech.common.security.TrustedCert;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

        signingClientCertCheckBox.addFocusListener(new FocusAdapter() {
            public void focusGained( FocusEvent e ) { setClientDesc(); }
        });
        signingClientCertCheckBox.addMouseListener(new MouseAdapter() {
            public void mouseEntered( MouseEvent e ) { setClientDesc(); }
        });

        signingServerCertCheckBox.addFocusListener(new FocusAdapter() {
            public void focusGained( FocusEvent e ) { setServerDesc(); }
        });
        signingServerCertCheckBox.addMouseListener(new MouseAdapter() {
            public void mouseEntered( MouseEvent e ) { setServerDesc(); }
        });

        signingSAMLTokenCheckBox.addFocusListener(new FocusAdapter() {
            public void focusGained( FocusEvent e ) { setSamlDesc(); }
        });
        signingSAMLTokenCheckBox.addMouseListener(new MouseAdapter() {
            public void mouseEntered( MouseEvent e ) { setSamlDesc(); }
        });

        outboundSSLConnCheckBox.addFocusListener(new FocusAdapter() {
            public void focusGained( FocusEvent e ) { setSslDesc(); }
        });
        outboundSSLConnCheckBox.addMouseListener(new MouseAdapter() {
            public void mouseEntered( MouseEvent e ) { setSslDesc(); }
        });
    }

    private String currentDescription = DEFAULT_DESC;

    private void setSslDesc() {
        currentDescription = resources.getString("usage.desc.ssl");
        notifyListeners();
    }

    private void setSamlDesc() {
        currentDescription = resources.getString("usage.desc.saml");
        notifyListeners();
    }

    private void setServerDesc() {
        currentDescription = resources.getString("usage.desc.server");
        notifyListeners();
    }

    private void setClientDesc() {
        currentDescription = resources.getString("usage.desc.client");
        notifyListeners();
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
        return currentDescription;
    }

     /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */
     public boolean canFinish() {
        return true;
    }

    private static final String DEFAULT_DESC = "This panel allows you to select the the usage options for the certificate. Any combination of the options in the list is allowed.";
}