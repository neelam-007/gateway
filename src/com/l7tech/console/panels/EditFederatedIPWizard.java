package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;

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

    /**
     * Constructor
     *
     * @param parent The parent frame object reference.
     * @param panel  The panel attached to this wizard. This is the panel to be displayed when the wizard is shown.
     * @param iProvider  The identity provider configuration to be edited.
     */
     public EditFederatedIPWizard(Frame parent, WizardStepPanel panel, FederatedIdentityProviderConfig iProvider) {
        super(parent, panel);

        setResizable(true);
        setTitle("Edit Federated Identity Provider Wizard");
        setShowDescription(false);
        Actions.setEscKeyStrokeDisposes(this);

        wizardInput = iProvider;

        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(EditFederatedIPWizard.this);
            }
        });

    }

}