package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class CreateFederatedIPWizard extends Wizard {

    public CreateFederatedIPWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);

        setResizable(true);
        setTitle("Create Federated Identity Provider Wizard");
        setShowDescription(false);
        Actions.setEscKeyStrokeDisposes(this);
        
        wizardInput = new FederatedIdentityProviderConfig();

        ((IdentityProviderConfig)wizardInput).setTypeVal(IdentityProviderType.FEDERATED.toVal());

        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(CreateFederatedIPWizard.this);
            }
        });

    }
}
