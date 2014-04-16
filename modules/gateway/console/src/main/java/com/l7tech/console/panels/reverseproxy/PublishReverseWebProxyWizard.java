package com.l7tech.console.panels.reverseproxy;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.builder.PolicyBuilder;
import com.l7tech.policy.variable.DataType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard which guides the user in setting up a service and policy for a reverse web proxy.
 */
public class PublishReverseWebProxyWizard extends AbstractPublishServiceWizard {
    private static final Logger logger = Logger.getLogger(PublishReverseWebProxyWizard.class.getName());
    private ReverseWebProxyConfigurationPanel configPanel;
    private ReverseWebProxyConfig config;
    private static final String WEB_APP_HOST = "webAppHost";
    private static final String WEB_APP_HOST_ENCODED = "webAppHostEncoded";
    private static final String QUERY = "query";
    private static final String LOCATION = "location";
    private static final String RESPONSE_COOKIE_OVERWRITE_PATH = "response.cookie.overwritePath";
    private static final String RESPONSE_COOKIE_OVERWRITE_DOMAIN = "response.cookie.overwriteDomain";
    private static final String $_QUERY = "${" + QUERY + "}";
    private static final String $_WEB_APP_HOST_ENCODED = "${" + WEB_APP_HOST_ENCODED + "}";
    private static final String $_WEB_APP_HOST = "${" + WEB_APP_HOST + "}";
    private static final String $_REQUEST_URL_HOST = "${request.url.host}";
    private static final String $_REQUEST_URL_PORT = "${request.url.port}";
    private static final String $_HOST_AND_PORT = $_REQUEST_URL_HOST + ":" + $_REQUEST_URL_PORT;
    private static final String $_REQUEST_URL_QUERY = "${request.url.query}";
    private static final String $_REQUEST_URL_PATH = "${request.url.path}";

    public static PublishReverseWebProxyWizard getInstance(@NotNull final Frame parent) {
        final ReverseWebProxyConfigurationPanel configPanel = new ReverseWebProxyConfigurationPanel();
        configPanel.setNextPanel(null);
        return new PublishReverseWebProxyWizard(parent, configPanel);
    }

    private PublishReverseWebProxyWizard(@NotNull final Frame parent, @NotNull final ReverseWebProxyConfigurationPanel configPanel) {
        super(parent, configPanel);
        setTitle("Publish Reverse Web Proxy");
        this.config = new ReverseWebProxyConfig();
        this.wizardInput = config;
        this.configPanel = configPanel;
        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(PublishReverseWebProxyWizard.this);
            }
        });
    }

    @Override
    protected void finish(final ActionEvent evt) {
        configPanel.storeSettings(wizardInput);
        if (wizardInput instanceof ReverseWebProxyConfig) {
            final ReverseWebProxyConfig config = (ReverseWebProxyConfig) wizardInput;
            try {
                validateFinishedConfig(config);
                final PublishedService service = new PublishedService();
                service.setName(config.getName());
                service.setSoap(false);
                service.setFolder(config.getFolder());
                service.setHttpMethods(new HashSet<>(Arrays.asList(HttpMethod.values())));
                String routingUri = config.getRoutingUri();
                if (!routingUri.startsWith("/")) {
                    routingUri = "/" + routingUri;
                }
                service.setRoutingUri(routingUri);
                final PolicyBuilder builder = new PolicyBuilder();
                buildPolicyXml(config, builder);
                service.getPolicy().setXml(XmlUtil.nodeToFormattedString(builder.getPolicy()));
                final Goid goid = Registry.getDefault().getServiceManager().savePublishedService(service);
                service.setGoid(goid);
                notify(new ServiceHeader(service));
            } catch (final IllegalArgumentException | IOException | ObjectModelException | VersionException | PolicyAssertionException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Error publishing reverse web proxy.", "Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }
        super.finish(evt);
    }

    private void validateFinishedConfig(final ReverseWebProxyConfig config) throws IllegalArgumentException {
        if (StringUtils.isBlank(config.getWebAppHost())) {
            throw new IllegalArgumentException("Missing web app host");
        } else if (StringUtils.isBlank(config.getName())) {
            throw new IllegalArgumentException("Missing service name");
        } else if (StringUtils.isBlank(config.getRoutingUri())) {
            throw new IllegalArgumentException("Missing routing uri");
        }
    }

    static void buildPolicyXml(@NotNull final ReverseWebProxyConfig config, @NotNull final PolicyBuilder builder) throws IOException {
        if (StringUtils.isBlank(config.getWebAppHost())) {
            throw new IllegalArgumentException("Configured web app host is null or empty");
        }
        // set constants
        builder.setContextVariable(WEB_APP_HOST, config.getWebAppHost())
                .setContextVariable(RESPONSE_COOKIE_OVERWRITE_PATH, "false")
                .setContextVariable(RESPONSE_COOKIE_OVERWRITE_DOMAIN, "false")
                .setContextVariable(QUERY, $_REQUEST_URL_QUERY);

        if (config.getWebAppType() == ReverseWebProxyConfig.WebApplicationType.SHAREPOINT) {
            // encode '.', '{' and '}'
            builder.urlEncode(WEB_APP_HOST, WEB_APP_HOST_ENCODED)
                    .regex(TargetMessageType.OTHER, WEB_APP_HOST_ENCODED, "\\.", "%2E")
                    .regex(TargetMessageType.OTHER, QUERY, "\\{", "%7B")
                    .regex(TargetMessageType.OTHER, QUERY, "\\}", "%7D");
        }

        if (config.isRewriteCookies()) {
            // handle request cookies
            if (config.getWebAppType() == ReverseWebProxyConfig.WebApplicationType.SHAREPOINT) {
                builder.replaceHttpCookieNames(TargetMessageType.REQUEST, null, $_REQUEST_URL_HOST + "%3A" + $_REQUEST_URL_PORT, $_WEB_APP_HOST_ENCODED);
            }
            builder.replaceHttpCookieDomains(TargetMessageType.REQUEST, null, $_REQUEST_URL_HOST, $_WEB_APP_HOST);
        }

        // route to web app
        final String protocol = config.isUseHttps() ? "https" : "http";
        builder.routeForwardAll(protocol + "://" + $_WEB_APP_HOST + $_REQUEST_URL_PATH + $_QUERY, false);

        if (config.isRewriteCookies()) {
            // handle response cookies
            if (config.getWebAppType() == ReverseWebProxyConfig.WebApplicationType.SHAREPOINT) {
                // sharepoint cookie names may contain the encoded webb app host
                builder.replaceHttpCookieNames(TargetMessageType.RESPONSE, null, $_WEB_APP_HOST_ENCODED, $_REQUEST_URL_HOST + "%3A" + $_REQUEST_URL_PORT);
            }
            builder.replaceHttpCookieDomains(TargetMessageType.RESPONSE, null, $_WEB_APP_HOST, $_REQUEST_URL_HOST);
        }

        if (config.isRewriteLocationHeader()) {
            // handle redirects
            builder.rewriteHeader(TargetMessageType.RESPONSE, null, LOCATION, $_WEB_APP_HOST, $_HOST_AND_PORT);
        }

        if (config.isRewriteResponseContent()) {
            // replace instances of web app host in response body
            if (StringUtils.isNotBlank(config.getHtmlTagsToRewrite())) {
                builder.rewriteHtml(TargetMessageType.RESPONSE, null, $_WEB_APP_HOST, $_HOST_AND_PORT, config.getHtmlTagsToRewrite());
            } else {
                // blanket rewrite
                builder.regex(TargetMessageType.RESPONSE, null, $_WEB_APP_HOST, $_HOST_AND_PORT);
            }
        }
    }

    @Override
    public void setFolder(@NotNull Folder folder) {
        config.setFolder(folder);
    }
}