package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.common.gui.util.Utilities;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class CreateFederatedIPWizard extends Wizard {

    public CreateFederatedIPWizard(Frame parent, WizardStepPanel panel, FederatedIdentityProviderConfig fipConfig) {
        super(parent, panel);

        setResizable(true);
        setTitle("Create Federated Identity Provider Wizard");
        setShowDescription(false);
        Utilities.setEscKeyStrokeDisposes(this);
        
        //check to verify that a federated identity provider config is provided.
        if ( fipConfig != null ) {
            wizardInput = fipConfig;    //set the wizard input to the one specified
            ((IdentityProviderConfig)wizardInput).setTypeVal(IdentityProviderType.FEDERATED.toVal());
            panel.readSettings(fipConfig, true);    //read in the settings into the panel
        }
        else {
            wizardInput = new FederatedIdentityProviderConfig();
            ((IdentityProviderConfig)wizardInput).setTypeVal(IdentityProviderType.FEDERATED.toVal());
        }

        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(CreateFederatedIPWizard.this);
            }
        });

    }

}
