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
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class CreateIdentityProviderWizard extends Wizard {
    static final Logger log = Logger.getLogger(CreateIdentityProviderWizard.class.getName());

    public CreateIdentityProviderWizard(Frame parent, final WizardStepPanel panel) {
        super(parent, panel);
        setResizable(true);
        setTitle("Identity Provider Creation Wizard");
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

    protected final JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EtchedBorder());
        buttonPanel.add(getButtonBack());
        buttonPanel.add(getButtonNext());
        buttonPanel.add(getButtonFinish());
        buttonPanel.add(getButtonCancel());
        buttonPanel.add(getButtonHelp());
        return buttonPanel;
    }

}
