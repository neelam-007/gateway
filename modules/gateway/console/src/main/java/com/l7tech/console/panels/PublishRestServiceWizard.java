package com.l7tech.console.panels;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.EntityEvent;
import com.l7tech.console.event.EntityListener;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.Option;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Wizard that guides the administrator through the publication of a RESTful Service Proxy.
 */
public class PublishRestServiceWizard extends Wizard {
    private IdentityProviderWizardPanel authorizationPanel;

    private Option<Folder> folder = Option.none();

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

    public PublishRestServiceWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        setTitle("Publish REST Service Proxy Wizard");

        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(PublishRestServiceWizard.this);
            }
        });
        this.wizardInput = new RestServiceConfig();
    }

    @Override
    protected void finish( final ActionEvent evt ) {
        super.finish(evt);
        if(wizardInput instanceof RestServiceConfig){
            RestServiceConfig config = (RestServiceConfig)wizardInput;
            //deploy all services
            for(RestServiceInfo rsi : config.getServices()){
                deployService(rsi);
            }
        }
    }

    private Assertion createRegexAssertion(final RestServiceInfo serviceInfo){
        Regex regexAssertion = new Regex();
        regexAssertion.setRegexName("Remove Gateway URL from ${request.http.path}");
        regexAssertion.setCaseInsensitive(true);
        String gatewayUrl = normalizeUrl(serviceInfo.getGatewayUrl());
        if(gatewayUrl.endsWith("/")) gatewayUrl = gatewayUrl.substring(0, gatewayUrl.length() - 1);
        regexAssertion.setRegex(Pattern.quote(gatewayUrl) + "(.*)");
        regexAssertion.setAutoTarget(false);
        regexAssertion.setTarget(TargetMessageType.OTHER);
        regexAssertion.setOtherTargetMessageVariable("request.http.uri");
        regexAssertion.setIncludeEntireExpressionCapture(false);
        regexAssertion.setCaptureVar("uri");
        return regexAssertion;
    }

    private void deployService(final RestServiceInfo serviceInfo){
        final PublishedService service = new PublishedService();
        final ArrayList<Assertion> allAssertions = new ArrayList<Assertion>();
        try {
            // get the assertions from the all assertion
            if (authorizationPanel != null) {
                authorizationPanel.readSettings(allAssertions);
            }
            AllAssertion policy = new AllAssertion(allAssertions);

            String gatewayUrl = serviceInfo.getGatewayUrl();
            //if we have a custom gateway url, make sure to remove it so that it is routed to the
            //correct REST endpoint
            String remotePath = "${request.http.uri}";
            if(!gatewayUrl.isEmpty()){
                policy.addChild(createRegexAssertion(serviceInfo));
                remotePath = "${uri}";
            }
            String backendUrl = serviceInfo.getBackendUrl();
            if(backendUrl.endsWith("/")) backendUrl = backendUrl.substring(0, backendUrl.length() - 1);
            policy.addChild(new HttpRoutingAssertion(backendUrl + remotePath + "${request.url.query}"));

            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            WspWriter.writePolicy(policy, bo);
            service.setFolder(folder.orSome(TopComponents.getInstance().getRootNode().getFolder()));
            service.setPolicy(new Policy(PolicyType.PRIVATE_SERVICE, null, bo.toString(), false));
            service.setSoap(false);
            service.setWssProcessingEnabled(false);
            service.setHttpMethods(EnumSet.of(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE));
            service.setName(serviceInfo.getServiceName());
            gatewayUrl = normalizeUrl(gatewayUrl);
            gatewayUrl = gatewayUrl.endsWith("/") ? gatewayUrl + "*" : gatewayUrl + "/*";
            service.setRoutingUri(gatewayUrl);
            final Runnable saver = new Runnable(){
                @Override
                public void run() {
                    try {
                        long oid = Registry.getDefault().getServiceManager().savePublishedService(service);
                        Registry.getDefault().getSecurityProvider().refreshPermissionCache();
                        service.setOid(oid);
                        PublishRestServiceWizard.this.notify(new ServiceHeader(service));
                    } catch ( Exception e ) {
                        DialogDisplayer.display(new JOptionPane("Error saving the service '" + service.getName() + "'"), getParent(), "Error", null);
                    }
                }
            };

            if ( ServicePropertiesDialog.hasResolutionConflict( service, null ) ) {
                final String message =
                        "Resolution parameters conflict for service '" + service.getName() + "'\n" +
                                "because an existing service is already using the URI " + service.getRoutingUri() + "\n\n" +
                                "Do you want to save this service?";
                DialogDisplayer.showConfirmDialog(this, message, "Service Resolution Conflict", JOptionPane.YES_NO_OPTION, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(final int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            saver.run();
                        }
                    }
                });
            } else {
                saver.run();
            }
        } catch (Exception e) {
            DialogDisplayer.display(new JOptionPane("Error saving the service '" + service.getName() +"'"), this, "Error", null);
        }
    }

    private void notify(EntityHeader header) {
        EntityEvent event = new EntityEvent(this, header);
        EntityListener[] listeners = listenerList.getListeners(EntityListener.class);
        for (EntityListener listener : listeners) {
            listener.entityAdded(event);
        }
    }

    private String normalizeUrl(final String url){
        String gatewayUrl = url;
        if(gatewayUrl != null){
            gatewayUrl = gatewayUrl.replaceAll("\\\\+", "/").replaceAll("/{2,}", "/");
            if(!gatewayUrl.startsWith("/")){
                gatewayUrl = "/" + gatewayUrl;
            }
        }
        return gatewayUrl;
    }
    /**
     * add the EntityListener
     *
     * @param listener the EntityListener
     */
    public void addEntityListener(EntityListener listener) {
        listenerList.add(EntityListener.class, listener);
    }

    /**
     * remove the the EntityListener
     *
     * @param listener the EntityListener
     */
    public void removeEntityListener(EntityListener listener) {
        listenerList.remove(EntityListener.class, listener);
    }

    /**
     * Set the Folder for the service.
     *
     * @param folder The folder to use (required)
     */
    public void setFolder( @NotNull final Folder folder ) {
        this.folder = Option.some( folder );
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

        public static RestServiceInfo build(final String serviceName, final String backendUrl, final String gatewayUrl){
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
