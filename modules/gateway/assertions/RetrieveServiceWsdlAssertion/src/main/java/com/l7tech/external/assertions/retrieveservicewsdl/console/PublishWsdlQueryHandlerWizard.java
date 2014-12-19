package com.l7tech.external.assertions.retrieveservicewsdl.console;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.logging.PermissionDeniedErrorHandler;
import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.UUID;
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

    private static final String HANDLER_SERVICE_POLICY_FILE = "WsdlQueryHandlerPolicy.xml";
    private static final String AUTHENTICATION_FRAGMENT_POLICY_FILE = "WsdlQueryAuthenticationFragmentPolicy.xml";
    private static final String REDIRECTION_FRAGMENT_POLICY_FILE = "WsdlQueryInterceptionFragmentPolicy.xml";

    protected static final String AUTHENTICATION_FRAGMENT_NAME_SUFFIX = " - Authentication";
    protected static final String REDIRECTION_FRAGMENT_NAME_SUFFIX = " - Query Redirection";

    private PublishWsdlQueryHandlerWizard(@NotNull Frame parent,
                                          @NotNull WizardStepPanel<WsdlQueryHandlerConfig> firstPanel) {
        super(parent, firstPanel, new WsdlQueryHandlerConfig(), "Publish WSDL Query Handler Wizard");

        initWizard();
    }

    public static PublishWsdlQueryHandlerWizard getInstance(@NotNull final Frame parent) {
        return new PublishWsdlQueryHandlerWizard(parent, new WsdlQueryHandlerConfigurationPanel(null));
    }

    private void initWizard() {
        addValidationRulesDefinedInWizardStepPanel(getSelectedWizardPanel().getClass(), getSelectedWizardPanel());

        addWizardListener(new WizardAdapter() {
            @Override
            public void wizardFinished(WizardEvent e) {
                try {
                    publishWsdlQueryHandler();
                } catch (Exception ex) {
                    handlePublishingError(ex);
                }
            }
        });
    }

    private void publishWsdlQueryHandler() throws IOException, SAXException {
        final Folder targetFolder = getTargetFolder();

        final Policy authFragment = createAuthenticationFragment(targetFolder);
        final PublishedService handlerService = createHandlerService(targetFolder, authFragment.getGuid());
        final Policy redirectionFragment = createRedirectionFragment(targetFolder);

        final Runnable publisher = new Runnable() {
            @Override
            public void run() {
                try {
                    Registry.getDefault().getPolicyAdmin().savePolicy(authFragment);

                    Goid handlerServiceGoid = Registry.getDefault().getServiceManager().savePublishedService(handlerService);
                    handlerService.setGoid(handlerServiceGoid);

                    Registry.getDefault().getPolicyAdmin().savePolicy(redirectionFragment);
                    Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                    Thread.sleep(1000);

                    PublishWsdlQueryHandlerWizard.this.notify(new ServiceHeader(handlerService));
                } catch (final PermissionDeniedException e) {
                    PermissionDeniedErrorHandler.showMessageDialog(e, logger);
                } catch (final Exception e) {
                    handlePublishingError(e);
                }
            }
        };

        checkResolutionConflict(handlerService, publisher);
    }

    private Policy createAuthenticationFragment(Folder targetFolder) throws IOException {
        String policyXml = readPolicyFile(AUTHENTICATION_FRAGMENT_POLICY_FILE);
        String policyName = wizardInput.getServiceName() + AUTHENTICATION_FRAGMENT_NAME_SUFFIX;

        final Policy fragment = new Policy(PolicyType.INCLUDE_FRAGMENT, policyName, policyXml, false);

        fragment.setFolder(targetFolder);
        fragment.setGuid(UUID.randomUUID().toString());

//        fragment.setSecurityZone(wizardInput.getSelectedSecurityZone());

        return fragment;
    }

    private Policy createRedirectionFragment(Folder targetFolder) throws IOException, SAXException {
        String policyXml = readPolicyFile(REDIRECTION_FRAGMENT_POLICY_FILE);
        String policyName = wizardInput.getServiceName() + REDIRECTION_FRAGMENT_NAME_SUFFIX;

        String updatedPolicyXml =
                updatePolicyElementAttribute(policyXml, "Uri", "stringValue", wizardInput.getRoutingUri());

        final Policy fragment = new Policy(PolicyType.INCLUDE_FRAGMENT, policyName, updatedPolicyXml, false);

        fragment.setFolder(targetFolder);
        fragment.setGuid(UUID.randomUUID().toString());

//        fragment.setSecurityZone(wizardInput.getSelectedSecurityZone());

        return fragment;
    }

    private PublishedService createHandlerService(Folder targetFolder, String authFragmentGuid) throws IOException, SAXException {
        final PublishedService handlerService = new PublishedService();

        handlerService.setFolder(targetFolder);
        handlerService.setName(wizardInput.getServiceName());
        handlerService.setRoutingUri(wizardInput.getRoutingUri());
        handlerService.setSoap(false);
        handlerService.setHttpMethods(EnumSet.of(HttpMethod.GET));

        String policyXml = readPolicyFile(HANDLER_SERVICE_POLICY_FILE);
        String updatedPolicyXml =
                updatePolicyElementAttribute(policyXml, "PolicyGuid", "stringValue", authFragmentGuid);

        handlerService.getPolicy().setXml(updatedPolicyXml);

//        handlerService.setSecurityZone(wizardInput.getSelectedSecurityZone());
//        handlerService.getPolicy().setSecurityZone(wizardInput.getSelectedSecurityZone());

        return handlerService;
    }

    public static String updatePolicyElementAttribute(String policyXml, String elementTagName,
                                                      String attributeName, String attributeValue)
            throws SAXException, IOException {
        Document policyDocument = XmlUtil.parse(policyXml);

        NodeList nodes = policyDocument.getElementsByTagNameNS(WspConstants.L7_POLICY_NS, elementTagName);

        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            element.setAttribute(attributeName, attributeValue);
        }

        return XmlUtil.nodeToFormattedString(policyDocument);
    }

    private String readPolicyFile(final String policyResourceFile) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(policyResourceFile)) {
            if (inputStream == null) {
                throw new IOException("Policy resource file does not exist: " + policyResourceFile);
            }

            return new String(IOUtils.slurpStream(inputStream));
        }
    }

    private Folder getTargetFolder() {
        return folder.orSome(TopComponents.getInstance().getRootNode().getFolder());
    }

    private void handlePublishingError(Exception e) {
        logger.log(Level.WARNING, e.getMessage(), ExceptionUtils.getDebugException(e));

        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                "Error publishing WSDL Query Handler.", "Error", JOptionPane.ERROR_MESSAGE, null);
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
