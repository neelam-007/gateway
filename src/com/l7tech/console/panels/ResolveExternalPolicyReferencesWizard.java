package com.l7tech.console.panels;

import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.IdProviderReference;

import java.awt.*;

/**
 * This wizard lets the administrator resolve external conflicts from a policy
 * being imported.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 26, 2004<br/>
 * $Id$<br/>
 */
public class ResolveExternalPolicyReferencesWizard extends Wizard {

    /**
     * Convenient method to create a wizard based on a number of problematic external references.
     * @param refs the external references that could not be automatically resolved.
     */
    public static ResolveExternalPolicyReferencesWizard fromReferences(Frame parent, ExternalReference[] refs) {
        if (refs == null || refs.length < 1) {
            throw new IllegalArgumentException("cannot create wizard without references.");
        }
        WizardStepPanel previousPanel = null;
        WizardStepPanel firstPanel = null;
        for (int i = 0; i < refs.length; i++) {
            WizardStepPanel panel = null;
            if (refs[i] instanceof IdProviderReference) {
                panel = new ResolveForeignIdentityProviderPanel(null, (IdProviderReference)(refs[i]));
            } else {
                // todo, other types of panels
            }
            if (panel != null) {
                if (firstPanel == null) {
                    firstPanel = panel;
                }
                if (previousPanel != null) {
                    previousPanel.setNextPanel(panel);
                }
                previousPanel = panel;
            }
        }
        return new ResolveExternalPolicyReferencesWizard(parent, firstPanel);
    }
    public ResolveExternalPolicyReferencesWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        setTitle("Resolve external dependencies from exported policy document");
    }
}
