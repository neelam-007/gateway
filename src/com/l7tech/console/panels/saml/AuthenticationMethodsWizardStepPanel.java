/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

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
    private JCheckBox[] allMethods;
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
        Collection lm = new ArrayList();
        lm.add(checkBoxPasswordMethod);
        lm.add(checkBoxKerberosMethod);
        lm.add(checkBoxSrpMethod);
        lm.add(checkBoxHardwareTokenMethod);
        lm.add(checkBoxSslTlsCLientCertMethod);
        lm.add(checkBoxX509PublicKey);
        lm.add(checkBoxPgpPublicKey);
        lm.add(checkBoxSpkiPublicKey);
        lm.add(checkBoxXkmsPublicKey);
        lm.add(checkBoxXmlDigitalSignatureMethod);
        lm.add(checkBoxMethodUnspecified);
        allMethods = (JCheckBox[])lm.toArray(new JCheckBox[] {});
        buttonSelectAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < allMethods.length; i++) {
                    JCheckBox allMethod = allMethods[i];
                    allMethod.setSelected(true);
                }
            }
        });
        buttonSelectNone.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < allMethods.length; i++) {
                    JCheckBox allMethod = allMethods[i];
                    allMethod.setSelected(false);
                }
            }
        });
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Authentication Methods";
    }
}