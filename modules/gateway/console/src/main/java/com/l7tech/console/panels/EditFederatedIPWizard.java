package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.gui.util.Utilities;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class EditFederatedIPWizard extends IdentityProviderWizard {
    /**
     * Constructor
     *
     * @param parent The parent frame object reference.
     * @param panel  The panel attached to this wizard. This is the panel to be displayed when the wizard is shown.
     * @param iProvider  The identity provider configuration to be edited.
     */
     public EditFederatedIPWizard(Frame parent, WizardStepPanel panel, FederatedIdentityProviderConfig iProvider) {
        super(parent, panel);
        wizardInput = iProvider;
        setupComponents();
    }

    public EditFederatedIPWizard(Frame parent, WizardStepPanel panel, FederatedIdentityProviderConfig iProvider, boolean readOnly) {
        super(parent, panel, readOnly);
        wizardInput = iProvider;
        setupComponents();
    }

    private void setupComponents() {
        setResizable(true);
        setTitle("Edit Federated Identity Provider Wizard");
        setShowDescription(false);
        getButtonTest().setVisible(false);
        Utilities.setEscKeyStrokeDisposes(this);

        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(EditFederatedIPWizard.this);
            }
        });
    }

}