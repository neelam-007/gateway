package com.l7tech.external.assertions.swagger.console;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.logging.PermissionDeniedErrorHandler;
import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.Folder;
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
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.swagger.console.PublishSwaggerServiceWizard.SwaggerServiceConfig;

/**
 * Wizard for configuration and installation of Swagger services.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class PublishSwaggerServiceWizard extends AbstractPublishServiceWizard<SwaggerServiceConfig> {
    private static final Logger logger = Logger.getLogger(PublishSwaggerServiceWizard.class.getName());

    private static final String TEMPLATE_SERVICE_POLICY_FILE = "SwaggerServiceTemplatePolicy.xml";

    protected PublishSwaggerServiceWizard(@NotNull Frame parent,
                                          @NotNull WizardStepPanel<SwaggerServiceConfig> firstPanel) {
        super(parent, firstPanel, new SwaggerServiceConfig(), "Publish Swagger Service Wizard");

        initWizard();
    }

    public static PublishSwaggerServiceWizard getInstance(@NotNull final Frame parent) {
        SwaggerServiceConfigurationPanel configurationPanel = new SwaggerServiceConfigurationPanel(null);
        SwaggerDocumentPanel documentPanel = new SwaggerDocumentPanel(configurationPanel);

        return new PublishSwaggerServiceWizard(parent, documentPanel);
    }

    private void initWizard() {
        addValidationRulesDefinedInWizardStepPanel(getSelectedWizardPanel().getClass(), getSelectedWizardPanel());
    }

    @Override
    protected void finish(final ActionEvent evt) {
        getSelectedWizardPanel().storeSettings(wizardInput);

        try {
            final PublishedService service = new PublishedService();

            service.setFolder(getTargetFolder());
            service.setName(wizardInput.getServiceName());
            service.setRoutingUri("/" + wizardInput.getRoutingUri());
            service.setSoap(false);
            service.setHttpMethods(EnumSet.of(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE));

            String policyXml = readPolicyFile(TEMPLATE_SERVICE_POLICY_FILE);

            // TODO jwilliams: update template policy with defaults
            String updatedPolicyXml = "" + policyXml;
//                updatePolicyElementAttribute(policyXml, "PolicyGuid", "stringValue", "");

            service.getPolicy().setXml(updatedPolicyXml);
            service.getPolicy().setSecurityZone(wizardInput.getSecurityZone());
            service.setSecurityZone(wizardInput.getSecurityZone());

            checkResolutionConflictAndSave(service);
        } catch (final Exception e) {
            handlePublishingError(e);
        }
    }

    private void handlePublishingError(Exception e) {
        logger.log(Level.WARNING, e.getMessage(), ExceptionUtils.getDebugException(e));

        if (e instanceof PermissionDeniedException) {
            PermissionDeniedErrorHandler.showMessageDialog((PermissionDeniedException) e, logger);
        } else {
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                    "Error publishing Swagger service.", "Error", JOptionPane.ERROR_MESSAGE, null);
        }

        e.printStackTrace();
    }

    private String updatePolicyElementAttribute(String policyXml, String elementTagName,
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

    public static class SwaggerServiceConfig {
        private String serviceName;
        private String routingUri;
        private SecurityZone securityZone = null;
        private String documentUrl;
        private String apiHost;
        private String apiBasePath;
        private String apiTitle;
        private boolean validatePath;
        private boolean validateMethod;
        private boolean validateScheme;
        private boolean requireSecurityCredentials;

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

        public SecurityZone getSecurityZone() {
            return securityZone;
        }

        public void setSecurityZone(SecurityZone securityZone) {
            this.securityZone = securityZone;
        }

        public String getDocumentUrl() {
            return documentUrl;
        }

        public void setDocumentUrl(String documentUrl) {
            this.documentUrl = documentUrl;
        }

        public String getApiHost() {
            return apiHost;
        }

        public void setApiHost(String apiHost) {
            this.apiHost = apiHost;
        }

        public String getApiBasePath() {
            return apiBasePath;
        }

        public void setApiBasePath(String apiBasePath) {
            this.apiBasePath = apiBasePath;
        }

        public String getApiTitle() {
            return apiTitle;
        }

        public void setApiTitle(String apiTitle) {
            this.apiTitle = apiTitle;
        }

        public boolean isValidatePath() {
            return validatePath;
        }

        public void setValidatePath(boolean validatePath) {
            this.validatePath = validatePath;
        }

        public boolean isValidateMethod() {
            return validateMethod;
        }

        public void setValidateMethod(boolean validateMethod) {
            this.validateMethod = validateMethod;
        }

        public boolean isValidateScheme() {
            return validateScheme;
        }

        public void setValidateScheme(boolean validateScheme) {
            this.validateScheme = validateScheme;
        }

        public boolean isRequireSecurityCredentials() {
            return requireSecurityCredentials;
        }

        public void setRequireSecurityCredentials(boolean requireSecurityCredentials) {
            this.requireSecurityCredentials = requireSecurityCredentials;
        }
    }
}
