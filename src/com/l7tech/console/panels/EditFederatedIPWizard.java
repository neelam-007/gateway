package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.action.Actions;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class EditFederatedIPWizard extends IdentityProviderWizard {

    public static final String NAME = "Edit Federated Identity Provider";

    public EditFederatedIPWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);

        // unregister the old wizard if any
        TopComponents.getInstance().unregisterComponent(EditFederatedIPWizard.NAME);

        TopComponents.getInstance().registerComponent(EditFederatedIPWizard.NAME, this);
        setResizable(true);
        setTitle("Edit Federated Identity Provider Wizard");
        setShowDescription(false);
        Actions.setEscKeyStrokeDisposes(this);
        // create a holder for the new identity provider
        // NOTE: we only support creating LDAP provider

       //todo:
        //wizardInput = new LdapIdentityProviderConfig();

       // ((IdentityProviderConfig)wizardInput).setTypeVal(IdentityProviderType.LDAP.toVal());

        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(EditFederatedIPWizard.this);
            }
        });

    }

    protected void finish(ActionEvent evt) {
        TopComponents.getInstance().unregisterComponent(EditFederatedIPWizard.NAME);             
        super.finish(evt);
    }
}