package com.l7tech.console.panels;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.console.logging.PermissionDeniedErrorHandler;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard that guides the administrator through the publication of a non-soap service.
 */
public class PublishNonSoapServiceWizard extends AbstractPublishServiceWizard {
    private static final Logger logger = Logger.getLogger(PublishNonSoapServiceWizard.class.getName());

    public static PublishNonSoapServiceWizard getInstance(Frame parent) {
        IdentityProviderWizardPanel panel2 = null;
        NonSoapServicePanel panel1 = new NonSoapServicePanel(null);
        if (ConsoleLicenseManager.getInstance().isAuthenticationEnabled()) {
            panel2 = new IdentityProviderWizardPanel(false);
            panel1.setNextPanel(panel2);
        }
        PublishNonSoapServiceWizard output = new PublishNonSoapServiceWizard(parent, panel1);
        output.panel1 = panel1;
        output.panel2 = panel2;
        return output;
    }

    public PublishNonSoapServiceWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel, "Publish Web API Wizard");
    }

    @Override
    protected void finish(final ActionEvent evt) {
        final PublishedService service = new PublishedService();
        ArrayList<Assertion> allAssertions = new ArrayList<Assertion>();
        try {
            // get the assertions from the all assertion
            if (panel2 != null) {
                panel2.readSettings(allAssertions);
                service.setSecurityZone(panel2.getSelectedSecurityZone());
            }
            AllAssertion policy = new AllAssertion(allAssertions);
            if (panel1.getDownstreamURL() != null)
                policy.addChild(new HttpRoutingAssertion(panel1.getDownstreamURL()));
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            WspWriter.writePolicy(policy, bo);
            service.setFolder(folder.orSome(TopComponents.getInstance().getRootNode().getFolder()));
            final Policy servicePolicy = new Policy(PolicyType.PRIVATE_SERVICE, null, bo.toString(), false);
            // service policy inherits same security zone as the service
            servicePolicy.setSecurityZone(panel2.getSelectedSecurityZone());
            service.setPolicy(servicePolicy);
            service.setSoap(false);
            service.setWssProcessingEnabled(false);
            // xml application are not like soap. by default, not just post is allowed
            service.setHttpMethods(EnumSet.of(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE));
            service.setName(panel1.getPublishedServiceName());
            service.setRoutingUri(panel1.getRoutingURI());
            checkResolutionConflictAndSave(service);
        } catch (Exception e) {
            handlePublishError(service, e);
        }
    }

    private void handlePublishError(final PublishedService service, final Exception e) {
        final String message = "Unable to save the service '" + service.getName() + "'\n";
        logger.log(Level.INFO, message, ExceptionUtils.getDebugException(e));
        if (e instanceof PermissionDeniedException) {
            PermissionDeniedErrorHandler.showMessageDialog((PermissionDeniedException) e, logger);
        } else {
            JOptionPane.showMessageDialog(this,
                    message,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private IdentityProviderWizardPanel panel2; // may be null if no authentication enabled by current license
    private NonSoapServicePanel panel1;
}
