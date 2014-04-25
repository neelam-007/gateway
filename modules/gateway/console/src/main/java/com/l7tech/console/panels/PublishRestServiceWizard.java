package com.l7tech.console.panels;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Wizard that guides the administrator through the publication of a RESTful Service Proxy.
 */
public class PublishRestServiceWizard extends AbstractPublishServiceWizard {
    private static final Logger logger = Logger.getLogger(PublishRestServiceWizard.class.getName());
    private IdentityProviderWizardPanel authorizationPanel;

    public static PublishRestServiceWizard getInstance(final Frame parent) {
        IdentityProviderWizardPanel authorizationPanel = null;
        RestServiceDeploymentPanel restServiceDeploymentPanel = new RestServiceDeploymentPanel(null);
        if (ConsoleLicenseManager.getInstance().isAuthenticationEnabled()) {
            authorizationPanel = new IdentityProviderWizardPanel(false);
            RestServiceInfoPanel restServiceInfoPanel = new RestServiceInfoPanel(authorizationPanel);
            restServiceDeploymentPanel.setNextPanel(restServiceInfoPanel);
        }
        PublishRestServiceWizard output = new PublishRestServiceWizard(parent, restServiceDeploymentPanel);
        output.authorizationPanel = authorizationPanel;
        return output;
    }

    private PublishRestServiceWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel, "Publish REST Service Proxy Wizard");
        this.wizardInput = new RestServiceConfig();
    }

    @Override
    protected void finish(final ActionEvent evt) {
        super.finish(evt);
        if (wizardInput instanceof RestServiceConfig) {
            RestServiceConfig config = (RestServiceConfig) wizardInput;
            //deploy all services
            for (RestServiceInfo rsi : config.getServices()) {
                deployService(rsi);
            }
        }
    }

    private Assertion createRegexAssertion(final RestServiceInfo serviceInfo) {
        Regex regexAssertion = new Regex();
        regexAssertion.setRegexName("Remove Gateway URL from ${request.http.path}");
        regexAssertion.setCaseInsensitive(true);
        String gatewayUrl = serviceInfo.getGatewayUrl();
        try {
            //url encode the string. This is needed to handle urls with non standard characters. SSG-7898
            gatewayUrl = URLEncoder.encode(gatewayUrl, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            logger.log(Level.WARNING, "Could not encode gateway url: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        gatewayUrl = normalizeUrl(gatewayUrl);
        if (gatewayUrl.endsWith("/")) gatewayUrl = gatewayUrl.substring(0, gatewayUrl.length() - 1);
        regexAssertion.setRegex(Pattern.quote(gatewayUrl) + "(.*)");
        regexAssertion.setAutoTarget(false);
        regexAssertion.setTarget(TargetMessageType.OTHER);
        regexAssertion.setOtherTargetMessageVariable("request.http.uri");
        regexAssertion.setIncludeEntireExpressionCapture(false);
        regexAssertion.setCaptureVar("uri");
        return regexAssertion;
    }

    private void deployService(final RestServiceInfo serviceInfo) {
        final PublishedService service = new PublishedService();
        final ArrayList<Assertion> allAssertions = new ArrayList<Assertion>();
        try {
            // get the assertions from the all assertion and security zone
            if (authorizationPanel != null) {
                authorizationPanel.readSettings(allAssertions);
                service.setSecurityZone(authorizationPanel.getSelectedSecurityZone());
            }
            AllAssertion policy = new AllAssertion(allAssertions);

            String gatewayUrl = serviceInfo.getGatewayUrl();
            //if we have a custom gateway url, make sure to remove it so that it is routed to the
            //correct REST endpoint
            String remotePath = "${request.http.uri}";
            if (!gatewayUrl.isEmpty()) {
                policy.addChild(createRegexAssertion(serviceInfo));
                remotePath = "${uri}";
            }
            String backendUrl = serviceInfo.getBackendUrl();
            if (backendUrl.endsWith("/")) backendUrl = backendUrl.substring(0, backendUrl.length() - 1);
            policy.addChild(new HttpRoutingAssertion(backendUrl + remotePath + "${request.url.query}"));

            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            WspWriter.writePolicy(policy, bo);
            service.setFolder(folder.orSome(TopComponents.getInstance().getRootNode().getFolder()));
            final Policy servicePolicy = new Policy(PolicyType.PRIVATE_SERVICE, null, bo.toString(), false);
            // service policy inherits same security zone as the service
            servicePolicy.setSecurityZone(authorizationPanel.getSelectedSecurityZone());
            service.setPolicy(servicePolicy);
            service.setSoap(false);
            service.setWssProcessingEnabled(false);
            service.setHttpMethods(EnumSet.of(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE));
            service.setName(serviceInfo.getServiceName());
            gatewayUrl = normalizeUrl(gatewayUrl);
            gatewayUrl = gatewayUrl.endsWith("/") ? gatewayUrl + "*" : gatewayUrl + "/*";
            service.setRoutingUri(gatewayUrl);
            checkResolutionConflictAndSave(service);
        } catch (Exception e) {
            DialogDisplayer.display(new JOptionPane("Error saving the service '" + service.getName() + "'"), this, "Error", null);
        }
    }

    private String normalizeUrl(final String url) {
        String gatewayUrl = url;
        if (gatewayUrl != null) {
            gatewayUrl = gatewayUrl.replaceAll("\\\\+", "/").replaceAll("/{2,}", "/");
            if (!gatewayUrl.startsWith("/")) {
                gatewayUrl = "/" + gatewayUrl;
            }
        }
        return gatewayUrl;
    }

    public class RestServiceConfig {
        private boolean isManualEntry = true;
        private java.util.List<RestServiceInfo> services;

        public boolean isManualEntry() {
            return isManualEntry;
        }

        public void setManualEntry(final boolean manualEntry) {
            isManualEntry = manualEntry;
        }

        public List<RestServiceInfo> getServices() {
            return services;
        }

        public void setServices(final List<RestServiceInfo> services) {
            this.services = services;
        }
    }

    public static class RestServiceInfo {
        private final String serviceName;
        private final String backendUrl;
        private final String gatewayUrl;

        public static RestServiceInfo build(final String serviceName, final String backendUrl, final String gatewayUrl) {
            return new RestServiceInfo(serviceName, backendUrl, gatewayUrl);
        }

        private RestServiceInfo(final String serviceName, final String backendUrl, final String gatewayUrl) {
            this.serviceName = serviceName;
            this.backendUrl = backendUrl;
            this.gatewayUrl = gatewayUrl;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getBackendUrl() {
            return backendUrl;
        }

        public String getGatewayUrl() {
            return gatewayUrl;
        }
    }
}
