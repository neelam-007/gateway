/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.action.Actions;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * The <code>RequestWssSamlStatementWizard</code> drives the configuration
 * of the SAML constraints.
 *
 * @author emil
 * @version Jan 18, 2005
 */
public class RequestWssSamlStatementWizard extends Wizard {
    /**
     * Creates new wizard
     */
    public RequestWssSamlStatementWizard(RequestWssSaml assertion, Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        wizardInput = assertion;
        setTitle("SAML Constraints Wizard");


        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(RequestWssSamlStatementWizard.this);
            }
        });

    }
 }