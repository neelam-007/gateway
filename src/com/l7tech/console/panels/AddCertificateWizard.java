package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.common.security.TrustedCert;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class AddCertificateWizard extends CertificateWizard {

    /**
     * Constructor
     *
     * @param parent The parent frame object reference.
     * @param panel  The panel attached to this wizard. This is the panel to be displayed when the wizard is shown.
     */
    public AddCertificateWizard(Frame parent, final WizardStepPanel panel) {
        super(parent, panel);
        setResizable(true);
        setTitle("Add Certificate Wizard");
        setShowDescription(false);
        Actions.setEscKeyStrokeDisposes(this);

        // create a holder for the new identity provider
        // NOTE: we only support creating LDAP provider

        wizardInput = new TrustedCert();

        pack();

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(AddCertificateWizard.this);
            }
        });
    }
}
