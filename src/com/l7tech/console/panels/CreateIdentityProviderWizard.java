package com.l7tech.console.panels;


import com.l7tech.console.action.Actions;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/*
 * This class provides a wizard for users to add a new identity provider.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id: CreateIdentityProviderWizard.java,v 1.5 2004/02/07 01:47:58 fpang Exp 
 */

public class CreateIdentityProviderWizard extends Wizard {
    static final Logger log = Logger.getLogger(CreateIdentityProviderWizard.class.getName());

    /**
     * Constructor
     *
     * @param parent  The parent frame object reference.
     * @param panel   The panel attached to this wizard. This is the panel to be displayed when the wizard is shown.
     */
    public CreateIdentityProviderWizard(Frame parent, final WizardStepPanel panel) {
        super(parent, panel);
        setResizable(true);
        setTitle("New Identity Provider Wizard");
        setShowDescription(false);

        // create a holder for the new identity provider
        // NOTE: we only support creating LDAP provider
        wizardInput = new LdapIdentityProviderConfig();
        ((IdentityProviderConfig) wizardInput).setTypeVal(IdentityProviderType.LDAP.toVal());

        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(CreateIdentityProviderWizard.this);
            }
        });

    }

}
