package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.policy.exporter.*;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This wizard lets the administrator resolve external conflicts from a policy
 * being imported.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 26, 2004<br/>
 */
public class ResolveExternalPolicyReferencesWizard extends Wizard {
    private static final Logger logger = Logger.getLogger(ResolveExternalPolicyReferencesWizard.class.getName());

    /**
     * Convenient method to create a wizard based on a number of problematic external references.
     * @param refs the external references that could not be automatically resolved.
     */
    public static ResolveExternalPolicyReferencesWizard fromReferences(Frame parent, ExternalReference[] refs) throws IOException {
        if (refs == null || refs.length < 1) {
            throw new IllegalArgumentException("cannot create wizard without references.");
        }
        WizardStepPanel previousPanel = null;
        WizardStepPanel firstPanel = null;
        for (ExternalReference ref : refs) {
            WizardStepPanel panel = null;

            // If a external reference is handled by ExternalReferenceFactory, then use the factory to get the WizardStepPanel.
            boolean found = false;
            Set<ExternalReferenceFactory> factories = getExternalReferenceFactories();
            if (factories != null && !factories.isEmpty()) {
                for (ExternalReferenceFactory factory : factories) {
                    if (factory.matchByExternalReference(ref.getClass().getName())) {
                        panel = (WizardStepPanel) factory.getResolveExternalReferenceWizardStepPanel(ref);
                        found = true;
                        break;
                    }
                }
            }
            // If the external reference is NOT handled by ExternalReferenceFactory, then directly create a specific WizardStepPanel.
            if (! found) {
                if (ref instanceof IdProviderReference) {
                    panel = new ResolveForeignIdentityProviderPanel(null, (IdProviderReference) (ref));
                } else if (ref instanceof JMSEndpointReference) {
                    panel = new ResolveForeignJMSEndpointPanel(null, (JMSEndpointReference) (ref));
                } else if (ref instanceof CustomAssertionReference) {
                    panel = new ResolveForeignCustomAssertionPanel(null, (CustomAssertionReference) (ref));
                } else if (ref instanceof ExternalSchemaReference) {
                    panel = new ResolveExternalSchemaReferencePanel(null, (ExternalSchemaReference) (ref));
                } else if (ref instanceof IncludedPolicyReference) {
                    try {
                        panel = new ResolveForeignIncludedPolicyPanel(null, (IncludedPolicyReference) (ref));
                    } catch (ResolveForeignIncludedPolicyPanel.NoLongerApplicableException e) {
                        // Skip this reference, since the conflict has gone away
                        panel = null;
                    }
                } else if (ref instanceof TrustedCertReference) {
                    panel = new ResolveForeignTrustedCertificatePanel(null, (TrustedCertReference) (ref));
                } else if (ref instanceof PrivateKeyReference) {
                    panel = new ResolvePrivateKeyPanel(null, (PrivateKeyReference) (ref));
                } else if (ref instanceof JdbcConnectionReference) {
                    panel = new ResolveJdbcConnectionPanel(null, (JdbcConnectionReference) (ref));
                } else if (ref instanceof StoredPasswordReference) {
                    panel = new ResolveStoredPasswordPanel(null, (StoredPasswordReference) (ref));
                } else if (ref instanceof CustomKeyValueReference) {
                    if (((CustomKeyValueReference) ref).getEntitySerializer() != null) {
                        panel = new ResolveCustomKeyValueWithSerializerPanel(null, (CustomKeyValueReference) (ref));
                    } else {
                        panel = new ResolveCustomKeyValuePanel(null, (CustomKeyValueReference) (ref));
                    }
                    if (panel instanceof ResolveCustomKeyValuePanel) {
                        ((ResolveCustomKeyValuePanel)panel).initialize();
                    }
                } else if (ref instanceof GlobalResourceReference) { // must be after ExternalSchemaReference since that is a subclass
                    panel = new ResolveGlobalResourcePanel(null, (GlobalResourceReference) (ref));
                }
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
        setTitle("Resolve External Dependencies Wizard");

        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(ResolveExternalPolicyReferencesWizard.this);
            }
        });
    }

    private static Set<ExternalReferenceFactory> getExternalReferenceFactories() {
        Registry registry = Registry.getDefault();
        if (! registry.isAdminContextPresent()) {
            logger.warning("Cannot get Policy Exporter and Importer Admin due to no Admin Context present.");
            return null;
        } else {
            return registry.getPolicyAdmin().findAllExternalReferenceFactories();
        }
    }
}