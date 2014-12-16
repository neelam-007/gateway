package com.l7tech.external.assertions.retrieveservicewsdl.console;

import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.logging.PermissionDeniedErrorHandler;
import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.retrieveservicewsdl.console.PublishWsdlQueryHandlerWizard.WsdlQueryHandlerConfig;

/**
 * Wizard for configuration and installation of WSDL Query Handler Service.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class PublishWsdlQueryHandlerWizard extends AbstractPublishServiceWizard<WsdlQueryHandlerConfig> {
    private static final Logger logger = Logger.getLogger(PublishWsdlQueryHandlerWizard.class.getName());

    private PublishWsdlQueryHandlerWizard(@NotNull Frame parent,
                                          @NotNull WizardStepPanel<WsdlQueryHandlerConfig> firstPanel) {
        super(parent, firstPanel, new WsdlQueryHandlerConfig(), "Publish WSDL Query Handler Wizard");

        initWizard();
    }

    public static PublishWsdlQueryHandlerWizard getInstance(@NotNull final Frame parent) {
        return new PublishWsdlQueryHandlerWizard(parent, new WsdlQueryHandlerConfigurationPanel(null));
    }

    private void initWizard() {
        ServicesAndPoliciesTree tree =
                (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);

        FolderNode selectedFolder = tree.getDeepestFolderNodeInSelectionPath();

        if (null != selectedFolder) {
            setFolder(selectedFolder.getFolder());
        }

        addValidationRulesDefinedInWizardStepPanel(getSelectedWizardPanel().getClass(), getSelectedWizardPanel());

        addWizardListener(new WizardAdapter() {
            @Override
            public void wizardFinished(WizardEvent e) {
                publishWsdlQueryHandler();
            }
        });
    }

    private void publishWsdlQueryHandler() {
        final PublishedService handlerService = new PublishedService();

        handlerService.setFolder(folder.orSome(TopComponents.getInstance().getRootNode().getFolder()));
        handlerService.setName(wizardInput.getServiceName());
        handlerService.setRoutingUri(wizardInput.getRoutingUri());
        handlerService.setSoap(false);

        handlerService.getPolicy().setXml(generateServicePolicyXml());

//        service.setSecurityZone(wizardInput.getSelectedSecurityZone());
//        service.getPolicy().setSecurityZone(wizardInput.getSelectedSecurityZone());

        checkResolutionConflict(handlerService, new Runnable() {
            @Override
            public void run() {
                try {
                    Goid goid = Registry.getDefault().getServiceManager().savePublishedService(handlerService);
                    Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                    handlerService.setGoid(goid);
                    Thread.sleep(1000);
                    PublishWsdlQueryHandlerWizard.this.notify(new ServiceHeader(handlerService));
                } catch (final Exception e) {
                    if (e instanceof PermissionDeniedException) {
                        PermissionDeniedErrorHandler.showMessageDialog((PermissionDeniedException) e, logger);
                    } else {
                        DialogDisplayer.display(new JOptionPane("Error saving the service '" +
                                handlerService.getName() + "'"), getParent(), "Error", null);
                    }
                }
            }
        });
    }

    private String generateServicePolicyXml() {    // TODO jwilliams: implement policy building - currently returns a stub
        return WspWriter.getPolicyXml(
                new AllAssertion(Arrays.<Assertion>asList(
                        new AuditDetailAssertion("Published \"" + wizardInput.getServiceName() + "\"."))));
    }

    public static class WsdlQueryHandlerConfig {
        private String serviceName = "WSDL Query Handler";
        private String routingUri = "/wsdlQueryHandler";

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getRoutingUri() {
            return routingUri;
        }

        public void setRoutingUri(String routingUri) {
            this.routingUri = routingUri;
        }
    }
}
