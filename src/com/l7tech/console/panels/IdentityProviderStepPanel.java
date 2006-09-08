package com.l7tech.console.panels;

/*
 * This is a base class for the step panel of identity provider.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public abstract class IdentityProviderStepPanel extends WizardStepPanel{

    /**
     * Constructor
     *
     * @param next  The reference to the next step panel.
     */
    public IdentityProviderStepPanel(WizardStepPanel next) {
         super(next);
    }

    /**
     * Constructor
     *
     * @param next  The reference to the next step panel.
     * @param readOnly denotes if the editing controls on this form should be disabled - the default is false
     */public IdentityProviderStepPanel(WizardStepPanel next, boolean readOnly) {
         super(next, readOnly);
    }

    /**
     * Test whether the step panel allows testing the settings.
     *
     * @return true if the panel is valid, false otherwis
     */
    public boolean canTest() {
        return false;
    }
}
