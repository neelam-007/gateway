/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.common.security.saml.SamlConstants;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * The <code>WizardStepPanel</code> that allows selection of SAML
 * authentication methods.
 * @author emil
 * @version Jan 20, 2005
 */
public class AuthenticationMethodsWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
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
    private HashMap authenticationsMap = new HashMap();
    private boolean showTitleLabel;

    /**
     * Creates new form WizardPanel. Full constructor, specifiy
     */
    public AuthenticationMethodsWizardStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }
    /**
     * Creates new form WizardPanel with default optins
     */
    public AuthenticationMethodsWizardStepPanel(WizardStepPanel next) {
        this(next, true);
    }

    /**
     * Provides the wizard with the current data--either
     * the default data or already-modified settings.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        RequestWssSaml assertion = (RequestWssSaml)settings;
        SamlAuthenticationStatement statement = assertion.getAuthenticationStatement();
        if (statement == null) {
            setSkipped(true);
            return;
        }

        for (int i = 0; i < allMethods.length; i++) {
            JCheckBox method = allMethods[i];
            method.setSelected(false);
        }

        String[] methods = statement.getAuthenticationMethods();
        for (int i = 0; i < methods.length; i++) {
            String method = methods[i];
            JCheckBox jc = (JCheckBox)authenticationsMap.get(method);
            if (jc == null) {
                throw new IllegalArgumentException("No corresponding widget for "+method);
            }
            jc.setSelected(true);
        }
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     * <p/>
     * This is a noop version that subclasses implement.
     *
     * @param settings the object representing wizard panel state
     * @throws IllegalArgumentException if the the data provided
     *                                  by the wizard are not valid.
     */
    public void storeSettings(Object settings) throws IllegalArgumentException {
        RequestWssSaml assertion = (RequestWssSaml)settings;
         SamlAuthenticationStatement statement = assertion.getAuthenticationStatement();
         if (statement == null) {
             throw new IllegalArgumentException();
         }
        List methodsSelected = new ArrayList();
        for (Iterator iterator = authenticationsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            entry.getKey();
            JCheckBox jc = (JCheckBox)entry.getValue();
            if (jc.isSelected()) {
                methodsSelected.add(entry.getKey().toString());
            }
        }
        statement.setAuthenticationMethods((String[])methodsSelected.toArray(new String[] {}));
    }

    private void initialize() {
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }
        Collection lm = new ArrayList();
        lm.add(checkBoxPasswordMethod);
        authenticationsMap.put(SamlConstants.PASSWORD_AUTHENTICATION, checkBoxPasswordMethod);
        lm.add(checkBoxKerberosMethod);
        authenticationsMap.put(SamlConstants.KERBEROS_AUTHENTICATION, checkBoxKerberosMethod);
        lm.add(checkBoxSrpMethod);
        authenticationsMap.put(SamlConstants.SRP_AUTHENTICATION, checkBoxSrpMethod);
        lm.add(checkBoxHardwareTokenMethod);
        authenticationsMap.put(SamlConstants.HARDWARE_TOKEN_AUTHENTICATION, checkBoxHardwareTokenMethod);
        lm.add(checkBoxSslTlsCLientCertMethod);
        authenticationsMap.put(SamlConstants.SSL_TLS_CERTIFICATE_AUTHENTICATION, checkBoxSslTlsCLientCertMethod);
        lm.add(checkBoxX509PublicKey);
        authenticationsMap.put(SamlConstants.X509_PKI_AUTHENTICATION, checkBoxX509PublicKey);
        lm.add(checkBoxPgpPublicKey);
        authenticationsMap.put(SamlConstants.PGP_AUTHENTICATION, checkBoxPgpPublicKey);
        lm.add(checkBoxSpkiPublicKey);
        authenticationsMap.put(SamlConstants.SPKI_AUTHENTICATION, checkBoxSpkiPublicKey);
        lm.add(checkBoxXkmsPublicKey);
        authenticationsMap.put(SamlConstants.XKMS_AUTHENTICATION, checkBoxXkmsPublicKey);
        lm.add(checkBoxXmlDigitalSignatureMethod);
        authenticationsMap.put(SamlConstants.XML_DSIG_AUTHENTICATION, checkBoxXmlDigitalSignatureMethod);
        lm.add(checkBoxMethodUnspecified);
        authenticationsMap.put(SamlConstants.UNSPECIFIED_AUTHENTICATION, checkBoxMethodUnspecified);

        allMethods = (JCheckBox[])lm.toArray(new JCheckBox[] {});
        buttonSelectAll.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < allMethods.length; i++) {
                    JCheckBox method = allMethods[i];
                    method.setSelected(true);
                }
            }
        });
        buttonSelectNone.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < allMethods.length; i++) {
                    JCheckBox method = allMethods[i];
                    method.setSelected(false);
                }
            }
        });

        for (int i = 0; i < allMethods.length; i++) {
            JCheckBox method = allMethods[i];
            method.addChangeListener( new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    notifyListeners();
                }
            });
        }
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Authentication Methods";
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.
     * A single authentication method must be specified.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canAdvance() {
        for (int i = 0; i < allMethods.length; i++) {
            JCheckBox method = allMethods[i];
            if (method.isSelected()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test whether the step is finished and it is safe to finish the wizard.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canFinish() {
        return false;
    }

    public String getDescription() {
        return
          "<html>Specify one or more accepted authentication methods that the SAML statement must assert. " +
            "At least one authentication method must be selected</html>";
    }
}