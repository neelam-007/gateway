package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.identity.IdentityProviderConfig;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/*
 * This class provides a wizard for users to edit the selected identity provider.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class EditIdentityProviderWizard extends Wizard {
    static final Logger log = Logger.getLogger(EditIdentityProviderWizard.class.getName());

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

        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(EditIdentityProviderWizard.this);
            }
        });

    }

}
