/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;

import java.awt.*;

/**
 * The <code>AuthenticationStatementWizard</code> drives the configuration
 * of the SAML authentication statement constraints.
 *
 * @author emil
 * @version Jan 18, 2005
 */
public class AuthenticationStatementWizard extends Wizard {
    /**
     * Creates new wizard
     */
    public AuthenticationStatementWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
    }
 }