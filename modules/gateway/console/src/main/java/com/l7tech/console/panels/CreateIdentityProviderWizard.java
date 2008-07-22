package com.l7tech.console.panels;


import com.l7tech.console.action.Actions;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.gui.util.Utilities;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/*
 * This class provides a wizard for users to add a new identity provider.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id: CreateIdentityProviderWizard.java,v 1.5 2004/02/07 01:47:58 fpang Exp
 */

public class CreateIdentityProviderWizard extends IdentityProviderWizard {

    /**
     * Constructor
     *
     * @param parent The parent frame object reference.
     * @param panel  The panel attached to this wizard. This is the panel to be displayed when the wizard is shown.
     * @param wizardInput Can be used to pass in initial settings for the wizard to use
     */
    public CreateIdentityProviderWizard(Frame parent, final WizardStepPanel panel, LdapIdentityProviderConfig wizardInput) {
        super(parent, panel);
        setResizable(true);
        setTitle("Create LDAP Identity Provider Wizard");
        setShowDescription(false);
        Utilities.setEscKeyStrokeDisposes(this);

        if(wizardInput == null) {
            this.wizardInput = new LdapIdentityProviderConfig();
            ((IdentityProviderConfig)this.wizardInput).setTypeVal(IdentityProviderType.LDAP.toVal());
        } else {
            this.wizardInput = wizardInput;
            ((IdentityProviderConfig)this.wizardInput).setTypeVal(IdentityProviderType.LDAP.toVal());
            panel.readSettings(wizardInput, true);
        }

        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(CreateIdentityProviderWizard.this);
            }
        });

    }

}
