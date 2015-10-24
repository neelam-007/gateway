package com.l7tech.external.assertions.swagger.console;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.console.logging.PermissionDeniedErrorHandler;
import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.swagger.SwaggerAssertion;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.List;
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

    private static final String HTTP_METHOD_COMPARISON_ASSERTION_POLICY_FILE = "httpMethodComparisonAssertion.xml";
    private static final String CACHE_LOOKUP_ASSERTION_POLICY_FILE = "cacheLookupAssertion.xml";
    private static final String CACHE_STORE_ASSERTION_POLICY_FILE = "cacheStoreAssertion.xml";

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

            service.setPolicy(generateServicePolicy());

            // set security zones
            service.getPolicy().setSecurityZone(wizardInput.getSecurityZone());
            service.setSecurityZone(wizardInput.getSecurityZone());

            checkResolutionConflictAndSave(service);
        } catch (final Exception e) {
            handlePublishingError(e);
        }
    }

    private Policy generateServicePolicy() throws IOException {
        Policy servicePolicy;

        AllAssertion policyAssertions = new AllAssertion();

        policyAssertions.setChildren(generateServiceSettingsVariables());
        policyAssertions.addChild(generateSwaggerDocDownloadAndCacheBranch());

        OneOrMoreAssertion retrieveDocOrRouteBranch = new OneOrMoreAssertion(CollectionUtils.list(
                generateSwaggerDocRequestBranch(), generateValidationAndRoutingBranch()
        ));

        policyAssertions.addChild(retrieveDocOrRouteBranch);

        try (ByteArrayOutputStream bo = new ByteArrayOutputStream()) {
            WspWriter.writePolicy(policyAssertions, bo);
            servicePolicy = new Policy(PolicyType.PRIVATE_SERVICE, null, bo.toString(), false);
        }

        return servicePolicy;
    }

    private OneOrMoreAssertion generateSwaggerDocDownloadAndCacheBranch() throws IOException {
        OneOrMoreAssertion swaggerDocDownloadAndCacheBranch;
        String cacheRetrieveXml = readPolicyFile(CACHE_LOOKUP_ASSERTION_POLICY_FILE);
        Assertion cacheRetrieveAssertion =
                WspReader.getDefault().parsePermissively(cacheRetrieveXml, WspReader.Visibility.includeDisabled);

        AllAssertion swaggerDocDownloadBranch = generateSwaggerDocDownloadBranch();

        swaggerDocDownloadAndCacheBranch =
                new OneOrMoreAssertion(CollectionUtils.list(cacheRetrieveAssertion, swaggerDocDownloadBranch));
        return swaggerDocDownloadAndCacheBranch;
    }

    private AllAssertion generateSwaggerDocDownloadBranch() throws IOException {
        AllAssertion branch = new AllAssertion();

        AuditDetailAssertion auditDetailAssertion = new AuditDetailAssertion();
        auditDetailAssertion.setLevel("WARNING");
        auditDetailAssertion.setDetail("Obtaining Swagger document from: ${swagger.docUrl}");

        final HttpRoutingAssertion routingAssertion = new HttpRoutingAssertion("${swagger.docUrl}");
        routingAssertion.setFollowRedirects(true);
        routingAssertion.setHttpMethod(HttpMethod.GET);
        routingAssertion.getRequestHeaderRules().setForwardAll(true);
        routingAssertion.getRequestParamRules().setForwardAll(true);
        routingAssertion.getResponseHeaderRules().setForwardAll(true);
        routingAssertion.setResponseMsgDest("swaggerDoc");

        String cacheStorageAssertionXml = readPolicyFile(CACHE_STORE_ASSERTION_POLICY_FILE);
        Assertion cacheStorageAssertion = WspReader.getDefault()
                .parsePermissively(cacheStorageAssertionXml, WspReader.Visibility.includeDisabled);

        branch.setChildren(CollectionUtils.list(
                auditDetailAssertion, routingAssertion, cacheStorageAssertion
        ));

        return branch;
    }

    private AllAssertion generateSwaggerDocRequestBranch() throws IOException {
        AllAssertion branch = new AllAssertion();

        Regex regexAssertion = new Regex();
        regexAssertion.setAutoTarget(false);
        regexAssertion.setCaseInsensitive(true);
        regexAssertion.setIncludeEntireExpressionCapture(false);
        regexAssertion.setOtherTargetMessageVariable("request.http.uri");
        regexAssertion.setRegex("^.*/swagger.json$");
        regexAssertion.setRegexName("Swagger Document Request");
        regexAssertion.setReplacement("");
        regexAssertion.setTarget(TargetMessageType.OTHER);

        String httpMethodComparisonXml = readPolicyFile(HTTP_METHOD_COMPARISON_ASSERTION_POLICY_FILE);
        Assertion httpMethodComparisonAssertion =
                WspReader.getDefault().parsePermissively(httpMethodComparisonXml, WspReader.Visibility.includeDisabled);

        HardcodedResponseAssertion hardcodedResponseAssertion = new HardcodedResponseAssertion();
        hardcodedResponseAssertion.setBase64ResponseBody(HexUtils.encodeBase64("${swaggerDoc}".getBytes()));
        hardcodedResponseAssertion.setResponseContentType("application/json");

        branch.setChildren(CollectionUtils.list(
                regexAssertion, httpMethodComparisonAssertion, hardcodedResponseAssertion
        ));

        return branch;
    }

    private List<SetVariableAssertion> generateServiceSettingsVariables() {
        return CollectionUtils.list(
                new SetVariableAssertion("swagger.host", wizardInput.getApiHost()),
                new SetVariableAssertion("swagger.baseUri", wizardInput.getApiBasePath()),
                new SetVariableAssertion("swagger.docUrl", wizardInput.getDocumentUrl())
        );
    }

    private AllAssertion generateValidationAndRoutingBranch() {
        AllAssertion branch = new AllAssertion();

        SwaggerAssertion swaggerAssertion = new SwaggerAssertion();
        swaggerAssertion.setSwaggerDoc("${swaggerDoc}");
        swaggerAssertion.setValidatePath(wizardInput.isValidatePath());
        swaggerAssertion.setValidateMethod(wizardInput.isValidateMethod());
        swaggerAssertion.setValidateScheme(wizardInput.isValidateScheme());
        swaggerAssertion.setRequireSecurityCredentials(wizardInput.isRequireSecurityCredentials());

        final HttpRoutingAssertion routingAssertion = new HttpRoutingAssertion("${request.url.protocol}://" +
                "${swagger.host}" + "${swagger.baseUri}" + "${sw.apiUri}${request.url.query}");
        routingAssertion.getRequestHeaderRules().setForwardAll(true);
        routingAssertion.getResponseHeaderRules().setForwardAll(true);

        branch.setChildren(CollectionUtils.list(swaggerAssertion, routingAssertion));

        return branch;
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
