package com.l7tech.console.panels.reverseproxy;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.panels.AbstractPublishServiceWizard;
import com.l7tech.console.panels.IdentityProviderWizardPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.builder.PolicyBuilder;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.TargetMessageType.*;

/**
 * Wizard which guides the user in setting up a service and policy for a reverse web proxy.
 */
public class PublishReverseWebProxyWizard extends AbstractPublishServiceWizard {
    private static final Logger logger = Logger.getLogger(PublishReverseWebProxyWizard.class.getName());
    private static final String TITLE = "Publish Reverse Web Proxy Wizard";
    private static final String WEB_APP_HOST = "webAppHost";
    private static final String WEB_APP_HOST_ENCODED = "webAppHostEncoded";
    private static final String REQUEST_HOST = "requestHost";
    private static final String REQUEST_HOST_ENCODED = "requestHostEncoded";
    private static final String QUERY = "query";
    private static final String LOCATION = "location";
    private static final String RESPONSE_COOKIE_OVERWRITE_PATH = "response.cookie.overwritePath";
    private static final String RESPONSE_COOKIE_OVERWRITE_DOMAIN = "response.cookie.overwriteDomain";
    private static final String $_QUERY = "${" + QUERY + "}";
    private static final String $_WEB_APP_HOST_ENCODED = "${" + WEB_APP_HOST_ENCODED + "}";
    private static final String $_WEB_APP_HOST = "${" + WEB_APP_HOST + "}";
    private static final String $_REQUEST_HOST = "${" + REQUEST_HOST + "}";
    private static final String $_REQUEST_HOST_ENCODED = "${" + REQUEST_HOST_ENCODED + "}";
    private static final String $_REQUEST_URL_HOST = "${request.url.host}";
    private static final String $_REQUEST_URL_PORT = "${request.url.port}";
    private static final String $_HOST_AND_PORT = $_REQUEST_URL_HOST + ":" + $_REQUEST_URL_PORT;
    private static final String $_REQUEST_URL_QUERY = "${request.url.query}";
    private static final String $_REQUEST_URL_PATH = "${request.url.path}";
    private static final String OPTIONAL_PORT_80 = "(:80)?";
    private static final String CONSTANTS_COMMENT = "// CONSTANTS";
    private static final String REWRITE_REQ_COOKIE_NAMES_COMMENT = "// REWRITE REQUEST COOKIE NAMES";
    private static final String REWRITE_RESP_COOKIE_NAMES_COMMENT = "// REWRITE RESPONSE COOKIE NAMES";
    private static final String REWRITE_REQ_COOKIE_DOMAINS_COMMENT = "// REWRITE REQUEST COOKIE DOMAINS";
    private static final String REWRITE_RESP_COOKIE_DOMAINS_COMMENT = "// REWRITE RESPONSE COOKIE DOMAINS";
    private static final String REWRITE_LOCATION_COMMENT = "// REWRITE LOCATION HEADER";
    private static final String REWRITE_HOST_COMMENT = "// REWRITE HOST HEADER";
    private static final String REWRITE_REQUEST_COMMENT = "// REWRITE REQUEST BODY";
    private static final String REWRITE_ENCODED_REQUEST_COMMENT = "// REWRITE ENCODED REQUEST BODY";
    private static final String REWRITE_RESPONSE_COMMENT = "// REWRITE RESPONSE BODY";
    private static final String ENCODE_WEB_APP_HOST_COMMENT = "// ENCODE WEB APP HOST";
    private static final String ENCODE_REQUEST_HOST = "// ENCODE REQUEST HOST";
    private static final String ENCODE_DOT_COMMENT = "// ENCODE AND REPLACE '.'";
    private static final String ENCODE_OPEN_CURLY_COMMENT = "// ENCODE AND REPLACE '{' IN QUERY";
    private static final String ENCODE_CLOSE_CURLY_COMMENT = "// ENCODE AND REPLACE '}' IN QUERY";
    private static final String AUTHORIZATION_COMMENT = "// AUTHORIZATION";
    private static final String REWRITE_REQ_QUERY = "// REWRITE REQUEST QUERY";
    private ReverseWebProxyConfigurationPanel configPanel;
    private IdentityProviderWizardPanel authPanel;
    private ReverseWebProxyConfig config;

    public static PublishReverseWebProxyWizard getInstance(@NotNull final Frame parent) {
        final ReverseWebProxyConfigurationPanel configPanel = new ReverseWebProxyConfigurationPanel();
        IdentityProviderWizardPanel authPanel = null;
        if (Registry.getDefault().getLicenseManager().isAuthenticationEnabled()) {
            authPanel = new IdentityProviderWizardPanel(false);
        }
        configPanel.setNextPanel(authPanel);
        return new PublishReverseWebProxyWizard(parent, configPanel, authPanel);
    }

    private PublishReverseWebProxyWizard(@NotNull final Frame parent,
                                         @NotNull final ReverseWebProxyConfigurationPanel configPanel,
                                         @NotNull final IdentityProviderWizardPanel authPanel) {
        super(parent, configPanel, TITLE);
        this.config = new ReverseWebProxyConfig();
        this.wizardInput = config;
        this.configPanel = configPanel;
        this.authPanel = authPanel;
    }

    @Override
    protected void finish(final ActionEvent evt) {
        configPanel.storeSettings(wizardInput);
        if (wizardInput instanceof ReverseWebProxyConfig) {
            final ReverseWebProxyConfig config = (ReverseWebProxyConfig) wizardInput;
            try {
                validateFinishedConfig(config);
                final PublishedService service = createService(config);
                checkResolutionConflictAndSave(service);
            } catch (final IllegalArgumentException | IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                        "Error publishing reverse web proxy.", "Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }
    }

    private PublishedService createService(final ReverseWebProxyConfig config) throws IOException {
        final PublishedService service = new PublishedService();
        service.setName(config.getName());
        service.setSoap(false);
        service.setWssProcessingEnabled(false);
        service.setFolder((com.l7tech.objectmodel.folder.Folder) folder.orSome(TopComponents.getInstance().getRootNode().getFolder()));
        service.setHttpMethods(new HashSet<>(Arrays.asList(HttpMethod.values())));
        String routingUri = config.getRoutingUri();
        if (!routingUri.startsWith("/")) {
            routingUri = "/" + routingUri;
        }
        service.setRoutingUri(routingUri);
        final ArrayList<Assertion> authAssertions = new ArrayList<>();
        if (authPanel != null) {
            authPanel.readSettings(authAssertions);
            service.setSecurityZone(authPanel.getSelectedSecurityZone());
            service.getPolicy().setSecurityZone(authPanel.getSelectedSecurityZone());
        }
        final PolicyBuilder builder = new PolicyBuilder();
        buildPolicyXml(config, authAssertions, builder);
        service.getPolicy().setXml(XmlUtil.nodeToFormattedString(builder.getPolicy()));
        return service;
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

    static void buildPolicyXml(@NotNull final ReverseWebProxyConfig config, @NotNull final List<Assertion> authAssertions, @NotNull final PolicyBuilder builder) throws IOException {
        if (StringUtils.isBlank(config.getWebAppHost())) {
            throw new IllegalArgumentException("Configured web app host is null or empty");
        }
        authorize(authAssertions, builder);
        setConstants(config, builder);
        encodeSpecialCharacters(config, builder);
        rewriteQuery(config, builder);
        rewriteRequestBody(config, builder);
        handleRequestCookies(config, builder);
        builder.addOrReplaceHeader(REQUEST, null, "Host", $_REQUEST_HOST, true, config.isRewriteHostHeader(), REWRITE_HOST_COMMENT);
        // route to web app
        final String protocol = config.isUseHttps() ? "https" : "http";
        builder.routeForwardAll(protocol + "://" + $_WEB_APP_HOST + $_REQUEST_URL_PATH + $_QUERY, false);
        handleResponseCookies(config, builder);
        // handle redirects
        builder.rewriteHeader(RESPONSE, null, LOCATION, $_WEB_APP_HOST, $_REQUEST_HOST, config.isRewriteLocationHeader(), REWRITE_LOCATION_COMMENT);
        rewriteResponseBody(config, builder);
    }

    private static void rewriteRequestBody(final ReverseWebProxyConfig config, final PolicyBuilder builder) {
        builder.regex(REQUEST, null, $_REQUEST_HOST, $_WEB_APP_HOST, true, config.isRewriteRequestContent(), REWRITE_REQUEST_COMMENT);
        if (config.getWebAppType() == ReverseWebProxyConfig.WebApplicationType.SHAREPOINT) {
            builder.regex(REQUEST, null, $_REQUEST_HOST_ENCODED, $_WEB_APP_HOST_ENCODED, true, config.isRewriteRequestContent(), REWRITE_ENCODED_REQUEST_COMMENT);
        }
    }

    private static void rewriteQuery(final ReverseWebProxyConfig config, final PolicyBuilder builder) {
        if (config.getWebAppType() == ReverseWebProxyConfig.WebApplicationType.SHAREPOINT) {
            final List<Regex> assertions = new ArrayList<>();
            assertions.add(PolicyBuilder.createRegexAssertion(OTHER, QUERY, $_REQUEST_HOST, $_WEB_APP_HOST, true, config.isRewriteQuery()));
            assertions.add(PolicyBuilder.createRegexAssertion(OTHER, QUERY, $_REQUEST_HOST_ENCODED, $_WEB_APP_HOST_ENCODED, true, config.isRewriteQuery()));
            builder.multipleRegex(assertions, config.isRewriteQuery(), REWRITE_REQ_QUERY);
        } else {
            builder.regex(OTHER, QUERY, $_REQUEST_HOST, $_WEB_APP_HOST, true, config.isRewriteQuery(), REWRITE_REQ_QUERY);
        }
    }

    private static void authorize(final List<Assertion> authAssertions, final PolicyBuilder builder) {
        if (!authAssertions.isEmpty()) {
            final AllAssertion allAuth = new AllAssertion(authAssertions);
            builder.appendAssertion(allAuth, AUTHORIZATION_COMMENT);
        }
    }

    private static void rewriteResponseBody(final ReverseWebProxyConfig config, final PolicyBuilder builder) throws IOException {
        if (config.isRewriteResponseContent() && StringUtils.isNotBlank(config.getHtmlTagsToRewrite())) {
            Set<String> search = new HashSet<>();
            if (config.getWebAppType() == ReverseWebProxyConfig.WebApplicationType.SHAREPOINT && !mayContainPort(config)) {
                search.add($_WEB_APP_HOST + ":80");
            }
            search.add($_WEB_APP_HOST);
            builder.rewriteHtml(RESPONSE, null, search, $_REQUEST_HOST, config.getHtmlTagsToRewrite(),
                    REWRITE_RESPONSE_COMMENT);
        } else {
            // blanket rewrite
            String regex = $_WEB_APP_HOST;
            if (config.getWebAppType() == ReverseWebProxyConfig.WebApplicationType.SHAREPOINT && !mayContainPort(config)) {
                regex = regex + OPTIONAL_PORT_80;
            }
            builder.regex(RESPONSE, null, regex, $_REQUEST_HOST, true, config.isRewriteResponseContent(),
                    REWRITE_RESPONSE_COMMENT);
        }
    }

    private static boolean mayContainPort(final ReverseWebProxyConfig config) {
        return config.getWebAppHost().contains(":");
    }

    private static void handleResponseCookies(final ReverseWebProxyConfig config, final PolicyBuilder builder) throws IOException {
        if (config.getWebAppType() == ReverseWebProxyConfig.WebApplicationType.SHAREPOINT) {
            // sharepoint cookie names may contain the encoded webb app host
            builder.replaceHttpCookieNames(RESPONSE, null, $_WEB_APP_HOST_ENCODED, $_REQUEST_HOST_ENCODED,
                    config.isRewriteCookies(), REWRITE_RESP_COOKIE_NAMES_COMMENT);
        }
        builder.replaceHttpCookieDomains(RESPONSE, null, $_WEB_APP_HOST, $_REQUEST_URL_HOST, config.isRewriteCookies(),
                REWRITE_RESP_COOKIE_DOMAINS_COMMENT);
    }

    private static void handleRequestCookies(final ReverseWebProxyConfig config, final PolicyBuilder builder) throws IOException {
        if (config.getWebAppType() == ReverseWebProxyConfig.WebApplicationType.SHAREPOINT) {
            // sharepoint cookie names may contain the encoded webb app host
            builder.replaceHttpCookieNames(REQUEST, null, $_REQUEST_HOST_ENCODED,
                    $_WEB_APP_HOST_ENCODED, config.isRewriteCookies(), REWRITE_REQ_COOKIE_NAMES_COMMENT);
        }
        builder.replaceHttpCookieDomains(REQUEST, null, $_REQUEST_URL_HOST, $_WEB_APP_HOST, config.isRewriteCookies(),
                REWRITE_REQ_COOKIE_DOMAINS_COMMENT);
    }

    private static void encodeSpecialCharacters(final ReverseWebProxyConfig config, final PolicyBuilder builder) throws IOException {
        if (config.getWebAppType() == ReverseWebProxyConfig.WebApplicationType.SHAREPOINT) {
            // encode web app host, including '.'
            builder.urlEncode(WEB_APP_HOST, WEB_APP_HOST_ENCODED, ENCODE_WEB_APP_HOST_COMMENT)
                    .regex(OTHER, WEB_APP_HOST_ENCODED, "\\.", "%2E", true, true, ENCODE_DOT_COMMENT);
            // encode request host : port, including '.'
            builder.urlEncode(REQUEST_HOST, REQUEST_HOST_ENCODED, ENCODE_REQUEST_HOST)
                    .regex(OTHER, REQUEST_HOST_ENCODED, "\\.", "%2E", true, true, ENCODE_DOT_COMMENT);
            // encode '{' and '}'
            builder.regex(OTHER, QUERY, "\\{", "%7B", true, true, ENCODE_OPEN_CURLY_COMMENT)
                    .regex(OTHER, QUERY, "\\}", "%7D", true, true, ENCODE_CLOSE_CURLY_COMMENT);
        }
    }

    private static void setConstants(final ReverseWebProxyConfig config, final PolicyBuilder builder) throws IOException {
        final Map<String, String> constants = new HashMap<>();
        constants.put(WEB_APP_HOST, config.getWebAppHost());
        constants.put(REQUEST_HOST, config.isIncludeRequestPort() ? $_HOST_AND_PORT : $_REQUEST_URL_HOST);
        constants.put(RESPONSE_COOKIE_OVERWRITE_PATH, "false");
        constants.put(RESPONSE_COOKIE_OVERWRITE_DOMAIN, "false");
        constants.put(QUERY, $_REQUEST_URL_QUERY);
        builder.setContextVariables(constants, CONSTANTS_COMMENT);
    }
}