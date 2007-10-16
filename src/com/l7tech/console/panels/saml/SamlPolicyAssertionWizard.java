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
    /**
     * Creates new wizard
     */
    public SamlPolicyAssertionWizard(SamlPolicyAssertion assertion, Frame parent, WizardStepPanel panel, boolean issueMode) {
        super(parent, panel);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        wizardInput = assertion;
        if (issueMode) {
            setTitle("SAML Issuer Assertion Wizard");
        } else {
            setTitle("SAML Constraints Wizard");
        }


        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(SamlPolicyAssertionWizard.this);
            }
        });

    }
 }