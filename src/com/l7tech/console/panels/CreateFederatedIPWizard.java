package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class CreateFederatedIPWizard extends IdentityProviderWizard {

    public static final String NAME = "Create Federated Identity Provider";

    public CreateFederatedIPWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        TopComponents.getInstance().registerComponent(CreateFederatedIPWizard.NAME, this);
        setResizable(true);
        setTitle("Create Federated Identity Provider Wizard");
        setShowDescription(false);
        Actions.setEscKeyStrokeDisposes(this);
        // create a holder for the new identity provider
        // NOTE: we only support creating LDAP provider

       //todo:
        //  wizardInput = new LdapIdentityProviderConfig();

       // ((IdentityProviderConfig)wizardInput).setTypeVal(IdentityProviderType.LDAP.toVal());

        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(CreateFederatedIPWizard.this);
            }
        });

    }

    protected void finish(ActionEvent evt) {
        TopComponents.getInstance().unregisterComponent(CreateFederatedIPWizard.NAME);
        super.finish(evt);
    }
}
