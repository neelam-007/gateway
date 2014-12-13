package com.l7tech.external.assertions.retrieveservicewsdl.console;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.logging.PermissionDeniedErrorHandler;
import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.panels.ServicePropertiesDialog;
import com.l7tech.console.panels.SoapServiceRoutingURIEditor;
import com.l7tech.console.panels.WizardStepPanel;
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
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.logging.Level;
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

        addValidationRulesDefinedInWizardStepPanel(firstPanel.getClass(), firstPanel);

        addWizardListener(new WizardAdapter() {
            @Override
            public void wizardFinished(WizardEvent e) {
                publishWsdlQueryHandler();
            }
        });
    }

    public static PublishWsdlQueryHandlerWizard getInstance(@NotNull final Frame parent) {
        return new PublishWsdlQueryHandlerWizard(parent, new WsdlQueryHandlerConfigurationPanel(null));
    }

    private void publishWsdlQueryHandler() {
        PublishedService handlerService = new PublishedService();

        handlerService.setFolder(folder.orSome(TopComponents.getInstance().getRootNode().getFolder()));
        handlerService.setName(wizardInput.getServiceName());
        handlerService.setRoutingUri(wizardInput.getRoutingUri());

        handlerService.getPolicy().setXml(generateServicePolicyXml());

        handlerService.setSoap(false);

        final Frame parent = TopComponents.getInstance().getTopParent();

//        service.setSecurityZone(wizardInput.getSelectedSecurityZone());
//        service.getPolicy().setSecurityZone(wizardInput.getSelectedSecurityZone());

        saveNonSoapServiceWithResolutionCheck(parent, handlerService);
    }

    /**
     * performs service URI resolution check and publishes the service
     * @param parent The parent for any dialogs (may be null)
     * @param service  The service to be saved (required)
     */
    private void saveNonSoapServiceWithResolutionCheck(final Frame parent, final PublishedService service) {    // TODO jwilliams: clean up
        try {
            // set supported http methods
            service.setHttpMethods(EnumSet.of(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE));

            final Runnable saver = new Runnable(){
                @Override
                public void run() {
                    try {
                        Goid goid = Registry.getDefault().getServiceManager().savePublishedService(service);
                        Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                        service.setGoid(goid);
                        Thread.sleep(1000);
                        PublishWsdlQueryHandlerWizard.this.notify(new ServiceHeader(service));
                    } catch ( Exception e ) {
                        handlePublishServiceError(parent, service, e);
                    }
                }
            };

            //check the service URI resolution conflict
            if ( ServicePropertiesDialog.hasResolutionConflict(service, null) ) {
                final String message =
                        "Resolution parameters conflict for service '" + service.getName() + "'\n" +
                                "because an existing service is already using the URI " + service.getRoutingUri() + "\n\n" +
                                "Would you like to publish this service using a different routing URI?";
                DialogDisplayer.showConfirmDialog(parent, message, "Service Resolution Conflict", JOptionPane.YES_NO_CANCEL_OPTION, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(final int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            // get new routing URI
                            final SoapServiceRoutingURIEditor dlg = new SoapServiceRoutingURIEditor(parent, service);
                            DialogDisplayer.display(dlg, new Runnable() {
                                @Override
                                public void run() {
                                    if (dlg.wasSubjectAffected()) {
                                        saveNonSoapServiceWithResolutionCheck(parent, service);
                                    } else {
                                        saver.run();
                                    }
                                }
                            });
                        } else if (option == JOptionPane.NO_OPTION) {
                            saver.run();
                        }
                    }
                });
            } else {
                saver.run();
            }
        } catch (Exception e) {
            handlePublishServiceError(parent, service, e);
        }
    }

    private void handlePublishServiceError(final Frame parent, final PublishedService service, final Exception e) {
        final String message = "Unable to save the service '" + service.getName() + "'\n";

        logger.log(Level.INFO, message, ExceptionUtils.getDebugException(e));

        if (e instanceof PermissionDeniedException) {
            PermissionDeniedErrorHandler.showMessageDialog((PermissionDeniedException) e, logger);
        } else {
            JOptionPane.showMessageDialog(parent,   // TODO jwilliams: do with existing util/helper class
                    message,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String generateServicePolicyXml() {    // TODO jwilliams: implement policy building - currently returns a stub
        return WspWriter.getPolicyXml(
                new AllAssertion(Arrays.<Assertion>asList(
                        new AuditDetailAssertion("Success! Published \"" + wizardInput.getServiceName() + "\"."))));
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
