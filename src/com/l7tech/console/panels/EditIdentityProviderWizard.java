package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.common.util.Locator;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Locale;
import java.util.ResourceBundle;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;

/*
 * This class provides a wizard for users to edit the selected identity provider.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class EditIdentityProviderWizard extends Wizard {
    static final Logger log = Logger.getLogger(EditIdentityProviderWizard.class.getName());
    private JButton buttonTest = null;
    private ResourceBundle resources = null;

    /**
     * Constructor
     *
     * @param parent The parent frame object reference.
     * @param panel  The panel attached to this wizard. This is the panel to be displayed when the wizard is shown.
     * @param iProvider  The identity provider configuration to be edited.
     */
    public EditIdentityProviderWizard(Frame parent, WizardStepPanel panel, IdentityProviderConfig iProvider) {
        super(parent, panel);
        setResizable(true);
        setTitle("Edit Identity Provider Properties Wizard");
        setShowDescription(false);

        // store the current settings
        wizardInput = iProvider;

        initResources();

        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(EditIdentityProviderWizard.this);
            }
        });

    }

    protected final JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EtchedBorder());
        buttonPanel.add(getButtonBack());
        buttonPanel.add(getButtonNext());
        buttonPanel.add(getButtonTest());
        buttonPanel.add(getButtonFinish());
        buttonPanel.add(getButtonCancel());
        buttonPanel.add(getButtonHelp());
        return buttonPanel;
    }

    protected JButton getButtonTest() {
        if (buttonTest == null) {
            buttonTest = new JButton();
            buttonTest.setText("Test");
            buttonTest.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {

                    // store the current inputs
                    getSelectedWizardPanel().storeSettings(wizardInput);

                    // test the settings
                    testSettings();
                }
            });
        }
        return buttonTest;
    }

    /**
      * Loads locale-specific resources: strings  etc
      */
     private void initResources() {
         Locale locale = Locale.getDefault();

         resources = ResourceBundle.getBundle("com.l7tech.console.resources.IdentityProviderDialog", locale);
     }

    private void testSettings() {

        String errorMsg = null;
        try {
            getProviderConfigManager().test((IdentityProviderConfig) wizardInput);
        } catch (InvalidIdProviderCfgException e) {
            errorMsg = e.getMessage();
        } catch (RuntimeException e) {
            errorMsg = resources.getString("test.error.runtime") + "\n" + e.getMessage();
        }
        if (errorMsg == null) {
            JOptionPane.showMessageDialog(this, resources.getString("test.res.ok"),
                    resources.getString("test.res.title"),
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, errorMsg,
                    resources.getString("test.res.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private IdentityProviderConfigManager getProviderConfigManager()
      throws RuntimeException {
        IdentityProviderConfigManager ipc =
          (IdentityProviderConfigManager)Locator.
          getDefault().lookup(IdentityProviderConfigManager.class);
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }

        return ipc;
    }

}
