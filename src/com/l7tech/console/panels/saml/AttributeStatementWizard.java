/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;

import java.awt.*;

/**
 * The <code>AttributeStatementWizard</code> drives the configuration
 * of the SAML attribute statement constraints.
 *
 * @author emil
 * @version Jan 18, 2005
 */
public class AttributeStatementWizard extends Wizard {
    /**
     * Creates new wizard
     */
    public AttributeStatementWizard(SamlAttributeStatement assertion, Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        wizardInput = assertion;
        setTitle("SAML Attribute Statement Constraints Wizard");
    }

 }