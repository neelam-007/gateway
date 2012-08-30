/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.RequireSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.security.saml.SamlConstants;

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

    private JCheckBox checkBoxAuthenticatedTelephony;
    private JCheckBox checkBoxInternetProtocol;
    private JCheckBox checkBoxInternetProtocolPassword;
    private JCheckBox checkBoxMobileOneFactorContract;
    private JCheckBox checkBoxMobileOneFactorUnregistered;
    private JCheckBox checkBoxMobileTwoFactorContract;
    private JCheckBox checkBoxMobileTwoFactorUnregistered;
    private JCheckBox checkBoxNomadTelephony;
    private JCheckBox checkBoxPasswordProtectedTransport;
    private JCheckBox checkBoxPersonalizedTelephony;
    private JCheckBox checkBoxPreviousSession;
    private JCheckBox checkBoxSmartcard;
    private JCheckBox checkBoxSmartcardPKI;
    private JCheckBox checkBoxSoftwarePKI;
    private JCheckBox checkBoxTelephony;
    private JCheckBox checkBoxTimeSyncToken;

    private JButton buttonSelectAll;
    private JButton buttonSelectNone;
    private JLabel titleLabel;
    private JCheckBox[] allMethods;
    private HashMap authenticationsMap = new HashMap();
    private boolean showTitleLabel;

    /**
     * Creates new form AuthenticationMethodsWizardStepPanel.
     */
    public AuthenticationMethodsWizardStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        initialize();
    }

    /**
     * Creates new form AuthenticationMethodsWizardStepPanel. Full constructor, that specifies
     * the owner dialog.
     */
    public AuthenticationMethodsWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog owner) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        setOwner(owner);
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
        RequireSaml assertion = (RequireSaml)settings;
        SamlAuthenticationStatement statement = assertion.getAuthenticationStatement();
        setSkipped(statement == null);
        if (statement == null) {
            return;
        }

        enableForVersion(assertion.getVersion()==null ? 1 : assertion.getVersion().intValue());

        for (int i = 0; i < allMethods.length; i++) {
            JCheckBox method = allMethods[i];
            if (method.isEnabled())
                method.setSelected(false);
        }

        String[] methods = statement.getAuthenticationMethods();
        for (int i = 0; i < methods.length; i++) {
            String method = methods[i];
            JCheckBox jc = (JCheckBox)authenticationsMap.get(method);
            if (jc == null) {
                throw new IllegalArgumentException("Unknown authentication method: "+method);
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
        RequireSaml assertion = (RequireSaml)settings;
         SamlAuthenticationStatement statement = assertion.getAuthenticationStatement();
         if (statement == null) {
             throw new IllegalArgumentException();
         }
        List methodsSelected = new ArrayList();
        for (Iterator iterator = authenticationsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
            entry.getKey();
            JCheckBox jc = (JCheckBox)entry.getValue();
            if (jc.isSelected() && jc.isEnabled()) {
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
        authenticationsMap.put(SamlConstants.PASSWORD_AUTHENTICATION, checkBoxPasswordMethod);
        authenticationsMap.put(SamlConstants.KERBEROS_AUTHENTICATION, checkBoxKerberosMethod);
        authenticationsMap.put(SamlConstants.SRP_AUTHENTICATION, checkBoxSrpMethod);
        authenticationsMap.put(SamlConstants.HARDWARE_TOKEN_AUTHENTICATION, checkBoxHardwareTokenMethod);
        authenticationsMap.put(SamlConstants.SSL_TLS_CERTIFICATE_AUTHENTICATION, checkBoxSslTlsCLientCertMethod);
        authenticationsMap.put(SamlConstants.X509_PKI_AUTHENTICATION, checkBoxX509PublicKey);
        authenticationsMap.put(SamlConstants.PGP_AUTHENTICATION, checkBoxPgpPublicKey);
        authenticationsMap.put(SamlConstants.SPKI_AUTHENTICATION, checkBoxSpkiPublicKey);
        authenticationsMap.put(SamlConstants.XKMS_AUTHENTICATION, checkBoxXkmsPublicKey);
        authenticationsMap.put(SamlConstants.XML_DSIG_AUTHENTICATION, checkBoxXmlDigitalSignatureMethod);
        authenticationsMap.put(SamlConstants.UNSPECIFIED_AUTHENTICATION, checkBoxMethodUnspecified);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_TELEPHONY_AUTH, checkBoxAuthenticatedTelephony);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_IP, checkBoxInternetProtocol);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_IPPASSWORD, checkBoxInternetProtocolPassword);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_MOBILE_1FACTOR_CONTRACT, checkBoxMobileOneFactorContract);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_MOBILE_1FACTOR_UNREG, checkBoxMobileOneFactorUnregistered);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_MOBILE_2FACTOR_CONTRACT, checkBoxMobileTwoFactorContract);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_MOBILE_2FACTOR_UNREG, checkBoxMobileTwoFactorUnregistered);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_TELEPHONY_NOMAD, checkBoxNomadTelephony);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_PASSWORD_PROTECTED, checkBoxPasswordProtectedTransport);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_TELEPHONY_PERSONALIZED, checkBoxPersonalizedTelephony);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_SESSION, checkBoxPreviousSession);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_SMARTCARD, checkBoxSmartcard);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_SMARTCARD_PKI, checkBoxSmartcardPKI);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_SOFTWARE_PKI, checkBoxSoftwarePKI);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_TELEPHONY, checkBoxTelephony);
        authenticationsMap.put(SamlConstants.AUTHENTICATION_SAML2_TIME_SYNC_TOKEN, checkBoxTimeSyncToken);

        allMethods = (JCheckBox[])authenticationsMap.values().toArray(new JCheckBox[] {});
        buttonSelectAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < allMethods.length; i++) {
                    JCheckBox method = allMethods[i];
                    if (method.isEnabled())
                        method.setSelected(true);
                }
            }
        });
        buttonSelectNone.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < allMethods.length; i++) {
                    JCheckBox method = allMethods[i];
                    if (method.isEnabled())
                        method.setSelected(false);
                }
            }
        });

        for (int i = 0; i < allMethods.length; i++) {
            JCheckBox method = allMethods[i];
            method.addChangeListener( new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    notifyListeners();
                }
            });
        }
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Authentication Methods";
    }

    /**
     * Test whether the step is finished and it is safe to advance to the next one.
     * A single authentication method must be specified.
     *
     * @return true if the panel is valid, false otherwis
     */
    @Override
    public boolean canAdvance() {
        for (int i = 0; i < allMethods.length; i++) {
            JCheckBox method = allMethods[i];
            if (method.isSelected()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return
          "<html>Specify one or more accepted authentication methods that the SAML statement must assert. " +
            "At least one authentication method must be selected</html>";
    }

    /**
     * Enable only the methods that are applicable for a given saml version(s)
     */
    private void enableForVersion(int samlVersion) {
        if (samlVersion == 0) {
            // enable all
            for (int i = 0; i < allMethods.length; i++) {
                JCheckBox method = allMethods[i];
                method.setEnabled(true);
            }
        }
        else if (samlVersion == 1) {
            HashMap v1Map = new HashMap(authenticationsMap);
            v1Map.keySet().retainAll(Arrays.asList(SamlConstants.ALL_AUTHENTICATIONS));
            for (int i = 0; i < allMethods.length; i++) {
                JCheckBox method = allMethods[i];
                method.setEnabled(v1Map.containsValue(method));
            }
        }
        else if (samlVersion == 2) {
            HashMap v2Map = new HashMap(authenticationsMap);
            Set v1Only = new HashSet(Arrays.asList(SamlConstants.ALL_AUTHENTICATIONS));
            v1Only.removeAll(SamlConstants.AUTH_MAP_SAML_1TO2.keySet());
            v2Map.keySet().removeAll(v1Only);
            for (int i = 0; i < allMethods.length; i++) {
                JCheckBox method = allMethods[i];
                method.setEnabled(v2Map.containsValue(method));
            }
        }
    }
}