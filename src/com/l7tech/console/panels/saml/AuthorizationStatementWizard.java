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
 * The <code>AuthorizationStatementStatementWizard</code> drives the configuration
 * of the SAML authorization statement constraints.
 *
 * @author emil
 * @version Jan 18, 2005
 */
public class AuthorizationStatementWizard extends Wizard {
    /**
     * Creates new wizard
     */
    public AuthorizationStatementWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        setTitle("SAML Authorization Statement Constraints Wizard");
    }
 }