/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;

import javax.swing.*;
import java.awt.*;

/**
 * The <code>WizardStepPanel</code> that allows selection of SAML
 * authentication methods.
 * @author emil
 * @version Jan 20, 2005
 */
public class AuthenticationMethodsWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JTextField securityDomainTextField;
    private JCheckBox checkBoxPasswordMethod;
    private JCheckBox checkBoxKerberosMethod;
    private JCheckBox checkBoxSrpMethod;
    private JCheckBox checkBoxHardwareTokenMethod;
    private JCheckBox checkBoxSslTlsCLientCertMethod;
    private JCheckBox checkBoxX509PublicKey;
    private JCheckBox checkBoxPgpPublicKey;
    private JCheckBox checkBoxSpkiPublicKey;
    private JCheckBox checkBoxXkmsPublicKey;
    private JCheckBox checkBoxXmlDigitalSignatureMethod;
    private JCheckBox checkBoxMethodUnspecified;
    private JButton buttonSelectAll;
    private JButton buttonSelectNone;
    private JLabel titleLabel;

    /**
     * Creates new form WizardPanel
     */
    public AuthenticationMethodsWizardStepPanel(WizardStepPanel next) {
        super(next);
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        initialize();

    }

    private void initialize() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Authentication Methods";
    }
}