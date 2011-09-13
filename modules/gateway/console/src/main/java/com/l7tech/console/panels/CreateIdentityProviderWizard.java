package com.l7tech.console.panels;


import com.l7tech.console.action.Actions;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderConfig;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/*
 * This class provides a wizard for users to add a new identity provider.
 */
public class CreateIdentityProviderWizard extends IdentityProviderWizard {

    /**
     * Constructor
     *
     * @param parent The parent frame object reference.
     * @param panel  The panel attached to this wizard. This is the panel to be displayed when the wizard is shown.
     * @param readSettings true if settings should be read from the provided wizardInput before packing the wizard; false to leave them as-is.
     * @param wizardInput Can be used to pass in initial settings for the wizard to use
     */
    public CreateIdentityProviderWizard(Frame parent, final WizardStepPanel panel, @NotNull IdentityProviderConfig wizardInput, boolean readSettings) {
        super(parent, panel);
        setResizable(true);
        setTitle("Create LDAP Identity Provider Wizard");
        setShowDescription(false);
        Utilities.setEscKeyStrokeDisposes(this);

        this.wizardInput = wizardInput;
        if (readSettings) {
            panel.readSettings(wizardInput, true);
        }

        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(CreateIdentityProviderWizard.this);
            }
        });

    }

}
