package com.l7tech.console.panels;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.common.util.Locator;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.util.ResourceBundle;
import java.util.Locale;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/*
 * This is a base class for create/edit identity provider wizard.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class IdentityProviderWizard extends Wizard {
    static private JButton buttonTest = null;
    private ResourceBundle resources = null;

    /**
     * Constructor
     *
     * @param parent  The parent frame object reference.
     * @param panel   The panel attached to this wizard. This is the panel to be displayed when the wizard is shown.
     */
    public IdentityProviderWizard(Frame parent, final WizardStepPanel panel) {
        super(parent, panel);
        initResources();
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
            buttonTest.setEnabled(false);
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
                (IdentityProviderConfigManager) Locator.
                getDefault().lookup(IdentityProviderConfigManager.class);
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }

        return ipc;
    }

    /**
     * updates the wizard controls with the state from the
     * panel parameter
     * Default Wizard deals with the standard buttons. Wizards
     * that provide more controls (wizard buttons for example)
     * override this method.
     *
     * @param wp the wizard panel
     */
    protected void updateWizardControls(WizardStepPanel wp) {
        if(wp instanceof IdentityProviderStepPanel) {
            buttonTest.setEnabled(((IdentityProviderStepPanel)wp).canTest());
        }
        super.updateWizardControls(wp);
    }

}

