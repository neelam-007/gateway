/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id: SamlPolicyAssertionWizard.java 10161 2005-03-10 00:54:28Z emil $
 */
package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The <code>SamlpAssertionWizard</code> is the base class that drives the
 * configuration of the SAMLP assertion properties.
 *
 * @author vchan
 */
public class SamlpAssertionWizard extends Wizard {

    /**
     *
     */
    protected AssertionMode mode;

    //- PRIVATE
    private final boolean readOnly;

    //- PUBLIC

    /**
     * Creates new wizard
     */
    public SamlpAssertionWizard(SamlProtocolAssertion assertion, Frame parent, WizardStepPanel panel, AssertionMode mode, boolean readOnly) {

        super(parent, panel);

        if (assertion == null) {
            throw new IllegalArgumentException();
        }

        this.readOnly = readOnly;
        this.wizardInput = assertion;
        this.mode = mode;
//        setTitle(getDialogTitle());

        setTitle(assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString());

        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(SamlpAssertionWizard.this);
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


    /**
     * Returns the dialog's main title.
     *
     * @return String for the dialog title
     */
//    protected abstract String getDialogTitle();

 }