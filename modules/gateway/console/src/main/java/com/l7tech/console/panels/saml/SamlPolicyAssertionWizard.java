/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id: SamlPolicyAssertionWizard.java 10161 2005-03-10 00:54:28Z emil $
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.JButton;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The <code>SamlPolicyAssertionWizard</code> drives the configuration
 * of the SAML constraints.
 *
 * @author emil
 * @version Jan 18, 2005
 */
public class SamlPolicyAssertionWizard extends Wizard {

    //- PUBLIC

    /**
     * Creates new wizard
     */
    public SamlPolicyAssertionWizard(SamlPolicyAssertion assertion, Frame parent, WizardStepPanel panel, boolean readOnly) {
        super(parent, panel);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        this.readOnly = readOnly;
        wizardInput = assertion;
        setTitle(assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString());

        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(SamlPolicyAssertionWizard.this);
            }
        });

    }

    //- PROTECTED

    @Override
    protected void updateWizardControls( final WizardStepPanel wsp ) {
        super.updateWizardControls( wsp );
        JButton finishButton = this.getButtonFinish();
        if ( finishButton.isEnabled() ) {
            finishButton.setEnabled( !readOnly );
        }
    }

    //- PRIVATE

    private final boolean readOnly;
 }