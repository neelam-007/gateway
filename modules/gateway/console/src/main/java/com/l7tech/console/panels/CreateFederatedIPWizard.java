package com.l7tech.console.panels;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;

import java.awt.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class CreateFederatedIPWizard extends CreateIdentityProviderWizard {
    public CreateFederatedIPWizard(Frame parent, WizardStepPanel panel, FederatedIdentityProviderConfig fipConfig) {
        super(parent, panel, fipConfig == null ? new FederatedIdentityProviderConfig() : fipConfig, fipConfig != null);
        ((IdentityProviderConfig) wizardInput).setTypeVal(IdentityProviderType.FEDERATED.toVal());
    }
}
