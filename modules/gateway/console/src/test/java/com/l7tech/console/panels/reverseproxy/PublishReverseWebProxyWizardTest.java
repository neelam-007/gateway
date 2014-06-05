package com.l7tech.console.panels.reverseproxy;

import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.builder.PolicyBuilder;
import com.l7tech.policy.variable.DataType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PublishReverseWebProxyWizardTest {
    private ReverseWebProxyConfig config;
    @Mock
    private PolicyBuilder builder;

    @Before
    public void setup() throws Exception {
        config = new ReverseWebProxyConfig();
        when(builder.setContextVariable(anyString(), anyString())).thenReturn(builder);
        when(builder.setContextVariable(anyString(), anyString(), any(DataType.class), anyString())).thenReturn(builder);
        when(builder.urlEncode(anyString(), anyString(), anyString())).thenReturn(builder);
        when(builder.regex(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString())).thenReturn(builder);
        when(builder.replaceHttpCookieNames(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(builder);
        when(builder.replaceHttpCookieDomains(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(builder);
        when(builder.routeForwardAll(anyString(), anyBoolean())).thenReturn(builder);
        when(builder.rewriteHeader(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(builder);
        when(builder.rewriteHtml(any(TargetMessageType.class), anyString(), anySet(), anyString(), anyString(), anyString())).thenReturn(builder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullWebAppHost() throws Exception {
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyWebAppHost() throws Exception {
        config.setWebAppHost("");
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);
    }

    @Test
    public void defaultGeneric() throws Exception {
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.GENERIC);
        config.setWebAppHost("default-generic.l7tech.com");
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);

        // constants
        final Map<String, String> constants = new HashMap<>();
        constants.put("webAppHost", "default-generic.l7tech.com");
        constants.put("requestHost", "${request.url.host}:${request.url.port}");
        constants.put("response.cookie.overwriteDomain", "false");
        constants.put("response.cookie.overwritePath", "false");
        constants.put("query", "${request.url.query}");
        verify(builder).setContextVariables(constants, "// CONSTANTS");

        // url rewriting + route
        verify(builder).regex(TargetMessageType.REQUEST, null, "${requestHost}", "${webAppHost}", true, true, "// REWRITE REQUEST BODY");
        verify(builder).replaceHttpCookieDomains(TargetMessageType.REQUEST, null, "${request.url.host}", "${webAppHost}", true, "// REWRITE REQUEST COOKIE DOMAINS");
        verify(builder).routeForwardAll("http://${webAppHost}${request.url.path}${" + "query" + "}", false);
        verify(builder).replaceHttpCookieDomains(TargetMessageType.RESPONSE, null, "${webAppHost}", "${request.url.host}", true, "// REWRITE RESPONSE COOKIE DOMAINS");
        verify(builder).rewriteHeader(TargetMessageType.RESPONSE, null, "location", "${webAppHost}", "${requestHost}", true, "// REWRITE LOCATION HEADER");
        verify(builder).regex(TargetMessageType.RESPONSE, null, "${webAppHost}", "${requestHost}", true, true, "// REWRITE RESPONSE BODY");
        verify(builder).addOrReplaceHeader(TargetMessageType.REQUEST, null, "Host", "${requestHost}", true, false, "// REWRITE HOST HEADER");

        verify(builder, never()).urlEncode(anyString(), anyString(), anyString());
        verify(builder, never()).regex(eq(TargetMessageType.OTHER), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString());
        verify(builder, never()).replaceHttpCookieNames(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyBoolean(), anyString());
        verify(builder, never()).rewriteHtml(any(TargetMessageType.class), anyString(), anySet(), anyString(), anyString(), anyString());
    }

    @Test
    public void minimalGeneric() throws Exception {
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.GENERIC);
        config.setWebAppHost("default-generic.l7tech.com");
        config.setRewriteCookies(false);
        config.setRewriteLocationHeader(false);
        config.setRewriteHostHeader(false);
        config.setRewriteRequestContent(false);
        config.setRewriteResponseContent(false);
        config.setIncludeRequestPort(false);
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);

        // constants + route
        final Map<String, String> constants = new HashMap<>();
        constants.put("webAppHost", "default-generic.l7tech.com");
        constants.put("requestHost", "${request.url.host}");
        constants.put("response.cookie.overwriteDomain", "false");
        constants.put("response.cookie.overwritePath", "false");
        constants.put("query", "${request.url.query}");
        verify(builder).setContextVariables(constants, "// CONSTANTS");
        verify(builder).routeForwardAll("http://${webAppHost}${request.url.path}${query}", false);

        // disabled policy
        verify(builder).regex(TargetMessageType.REQUEST, null, "${requestHost}", "${webAppHost}", true, false, "// REWRITE REQUEST BODY");
        verify(builder).replaceHttpCookieDomains(TargetMessageType.REQUEST, null, "${request.url.host}", "${webAppHost}", false, "// REWRITE REQUEST COOKIE DOMAINS");
        verify(builder).addOrReplaceHeader(TargetMessageType.REQUEST, null, "Host", "${requestHost}", true, false, "// REWRITE HOST HEADER");
        verify(builder).replaceHttpCookieDomains(TargetMessageType.RESPONSE, null, "${webAppHost}", "${request.url.host}", false, "// REWRITE RESPONSE COOKIE DOMAINS");
        verify(builder).rewriteHeader(TargetMessageType.RESPONSE, null, "location", "${webAppHost}", "${requestHost}", false, "// REWRITE LOCATION HEADER");
        verify(builder).regex(TargetMessageType.RESPONSE, null, "${webAppHost}", "${requestHost}", true, false, "// REWRITE RESPONSE BODY");

        verify(builder, never()).replaceHttpCookieNames(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyBoolean(), anyString());
        verify(builder, never()).urlEncode(anyString(), anyString(), anyString());
        verify(builder, never()).rewriteHtml(any(TargetMessageType.class), anyString(), anySet(), anyString(), anyString(), anyString());
    }

    @Test
    public void genericViaHttps() throws Exception {
        config.setWebAppHost("default-generic.l7tech.com");
        config.setUseHttps(true);
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);

        verify(builder).routeForwardAll("https://${webAppHost}${request.url.path}${query}", false);
    }

    @Test
    public void genericRewriteSpecificTags() throws Exception {
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.GENERIC);
        config.setWebAppHost("default-generic.l7tech.com");
        config.setHtmlTagsToRewrite("p,script");
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);

        verify(builder).rewriteHtml(TargetMessageType.RESPONSE, null, Collections.singleton("${webAppHost}"), "${requestHost}", "p,script", "// REWRITE RESPONSE BODY");
        verify(builder, never()).regex(eq(TargetMessageType.RESPONSE), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString());
    }

    @Test
    public void genericWithAuthorization() throws Exception {
        config.setWebAppHost("default-generic.l7tech.com");
        final List<Assertion> authAssertions = Collections.<Assertion>singletonList(new AuthenticationAssertion());
        PublishReverseWebProxyWizard.buildPolicyXml(config, authAssertions, builder);
        verify(builder).appendAssertion(any(AllAssertion.class), eq("// AUTHORIZATION"));
    }

    @Test
    public void genericWithEmptyAuthorizationList() throws Exception {
        config.setWebAppHost("default-generic.l7tech.com");
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);
        verify(builder, never()).appendAssertion(any(Assertion.class), eq("// AUTHORIZATION"));
    }

    @Test
    public void defaultSharepoint() throws Exception {
        config.setWebAppHost("default-sharepoint.l7tech.com");
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.SHAREPOINT);
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);

        // constants
        final Map<String, String> constants = new HashMap<>();
        constants.put("webAppHost", "default-sharepoint.l7tech.com");
        constants.put("requestHost", "${request.url.host}:${request.url.port}");
        constants.put("response.cookie.overwriteDomain", "false");
        constants.put("response.cookie.overwritePath", "false");
        constants.put("query", "${request.url.query}");
        verify(builder).setContextVariables(constants, "// CONSTANTS");

        // encoding
        verify(builder).urlEncode("webAppHost", "webAppHostEncoded", "// ENCODE WEB APP HOST");
        verify(builder).regex(TargetMessageType.OTHER, "webAppHostEncoded", "\\.", "%2E", true, true, "// ENCODE AND REPLACE '.'");
        verify(builder).urlEncode("requestHost", "requestHostEncoded", "// ENCODE REQUEST HOST");
        verify(builder).regex(TargetMessageType.OTHER, "requestHostEncoded", "\\.", "%2E", true, true, "// ENCODE AND REPLACE '.'");
        verify(builder).regex(TargetMessageType.OTHER, "query", "\\{", "%7B", true, true, "// ENCODE AND REPLACE '{' IN QUERY");
        verify(builder).regex(TargetMessageType.OTHER, "query", "\\}", "%7D", true, true, "// ENCODE AND REPLACE '}' IN QUERY");
        verify(builder).regex(TargetMessageType.OTHER, "query", "${requestHostEncoded}", "${webAppHostEncoded}", true, true, "// REWRITE REQUEST QUERY");

        // url rewriting + route
        verify(builder).regex(TargetMessageType.REQUEST, null, "${requestHost}", "${webAppHost}", true, true, "// REWRITE REQUEST BODY");
        verify(builder).regex(TargetMessageType.REQUEST, null, "${requestHostEncoded}", "${webAppHostEncoded}", true, true, "// REWRITE ENCODED REQUEST BODY");
        verify(builder).replaceHttpCookieNames(TargetMessageType.REQUEST, null, "${requestHostEncoded}", "${webAppHostEncoded}", true, "// REWRITE REQUEST COOKIE NAMES");
        verify(builder).replaceHttpCookieDomains(TargetMessageType.REQUEST, null, "${request.url.host}", "${webAppHost}", true, "// REWRITE REQUEST COOKIE DOMAINS");
        verify(builder).routeForwardAll("http://${webAppHost}${request.url.path}${query}", false);
        verify(builder).replaceHttpCookieNames(TargetMessageType.RESPONSE, null, "${webAppHostEncoded}", "${requestHostEncoded}", true, "// REWRITE RESPONSE COOKIE NAMES");
        verify(builder).replaceHttpCookieDomains(TargetMessageType.RESPONSE, null, "${webAppHost}", "${request.url.host}", true, "// REWRITE RESPONSE COOKIE DOMAINS");
        verify(builder).rewriteHeader(TargetMessageType.RESPONSE, null, "location", "${webAppHost}", "${requestHost}", true, "// REWRITE LOCATION HEADER");
        verify(builder).regex(TargetMessageType.RESPONSE, null, "${webAppHost}(:80)?", "${requestHost}", true, true, "// REWRITE RESPONSE BODY");
        verify(builder).addOrReplaceHeader(TargetMessageType.REQUEST, null, "Host", "${requestHost}", true, false, "// REWRITE HOST HEADER");

        verify(builder, never()).rewriteHtml(any(TargetMessageType.class), anyString(), anySet(), anyString(), anyString(), anyString());
    }

    @Test
    public void minimalSharepoint() throws Exception {
        config.setWebAppHost("default-sharepoint.l7tech.com");
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.SHAREPOINT);
        config.setRewriteCookies(false);
        config.setRewriteLocationHeader(false);
        config.setRewriteHostHeader(false);
        config.setRewriteRequestContent(false);
        config.setRewriteResponseContent(false);
        config.setIncludeRequestPort(false);
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);

        // constants
        final Map<String, String> constants = new HashMap<>();
        constants.put("webAppHost", "default-sharepoint.l7tech.com");
        constants.put("requestHost", "${request.url.host}");
        constants.put("response.cookie.overwriteDomain", "false");
        constants.put("response.cookie.overwritePath", "false");
        constants.put("query", "${request.url.query}");
        verify(builder).setContextVariables(constants, "// CONSTANTS");

        // encoding + route
        verify(builder).urlEncode("webAppHost", "webAppHostEncoded", "// ENCODE WEB APP HOST");
        verify(builder).regex(TargetMessageType.OTHER, "webAppHostEncoded", "\\.", "%2E", true, true, "// ENCODE AND REPLACE '.'");
        verify(builder).urlEncode("requestHost", "requestHostEncoded", "// ENCODE REQUEST HOST");
        verify(builder).regex(TargetMessageType.OTHER, "requestHostEncoded", "\\.", "%2E", true, true, "// ENCODE AND REPLACE '.'");
        verify(builder).regex(TargetMessageType.OTHER, "query", "\\{", "%7B", true, true, "// ENCODE AND REPLACE '{' IN QUERY");
        verify(builder).regex(TargetMessageType.OTHER, "query", "\\}", "%7D", true, true, "// ENCODE AND REPLACE '}' IN QUERY");
        verify(builder).regex(TargetMessageType.OTHER, "query", "${requestHostEncoded}", "${webAppHostEncoded}", true, true, "// REWRITE REQUEST QUERY");
        verify(builder).routeForwardAll("http://${webAppHost}${request.url.path}${query}", false);

        // disabled policy
        verify(builder).regex(TargetMessageType.REQUEST, null, "${requestHost}", "${webAppHost}", true, false, "// REWRITE REQUEST BODY");
        verify(builder).regex(TargetMessageType.REQUEST, null, "${requestHostEncoded}", "${webAppHostEncoded}", true, false, "// REWRITE ENCODED REQUEST BODY");
        verify(builder).replaceHttpCookieNames(TargetMessageType.REQUEST, null, "${requestHostEncoded}", "${webAppHostEncoded}", false, "// REWRITE REQUEST COOKIE NAMES");
        verify(builder).addOrReplaceHeader(TargetMessageType.REQUEST, null, "Host", "${requestHost}", true, false, "// REWRITE HOST HEADER");
        verify(builder).replaceHttpCookieNames(TargetMessageType.RESPONSE, null, "${webAppHostEncoded}", "${requestHostEncoded}", false, "// REWRITE RESPONSE COOKIE NAMES");
        verify(builder).replaceHttpCookieDomains(TargetMessageType.REQUEST, null, "${request.url.host}", "${webAppHost}", false, "// REWRITE REQUEST COOKIE DOMAINS");
        verify(builder).replaceHttpCookieDomains(TargetMessageType.RESPONSE, null, "${webAppHost}", "${request.url.host}", false, "// REWRITE RESPONSE COOKIE DOMAINS");
        verify(builder).rewriteHeader(TargetMessageType.RESPONSE, null, "location", "${webAppHost}", "${requestHost}", false, "// REWRITE LOCATION HEADER");
        verify(builder).regex(TargetMessageType.RESPONSE, null, "${webAppHost}(:80)?", "${requestHost}", true, false, "// REWRITE RESPONSE BODY");

        verify(builder, never()).rewriteHtml(any(TargetMessageType.class), anyString(), anySet(), anyString(), anyString(), anyString());
    }

    @Test
    public void sharepointViaHttps() throws Exception {
        config.setWebAppHost("default-sharepoint.l7tech.com");
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.SHAREPOINT);
        config.setUseHttps(true);
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);

        verify(builder).routeForwardAll("https://${webAppHost}${request.url.path}${query}", false);
    }

    @Test
    public void sharepointRewriteSpecificTags() throws Exception {
        config.setWebAppHost("default-sharepoint.l7tech.com");
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.SHAREPOINT);
        config.setHtmlTagsToRewrite("p,script");
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);

        verify(builder).rewriteHtml(TargetMessageType.RESPONSE, null, new HashSet<>(Arrays.asList("${webAppHost}:80", "${webAppHost}")),
                "${requestHost}", "p,script", "// REWRITE RESPONSE BODY");
        verify(builder, never()).regex(eq(TargetMessageType.RESPONSE), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString());
    }

    @Test
    public void sharepointDoNotIncludeRequestPort() throws Exception {
        config.setWebAppHost("default-sharepoint.l7tech.com");
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.SHAREPOINT);
        config.setIncludeRequestPort(false);
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);

        // constants
        final Map<String, String> constants = new HashMap<>();
        constants.put("webAppHost", "default-sharepoint.l7tech.com");
        constants.put("requestHost", "${request.url.host}");
        constants.put("response.cookie.overwriteDomain", "false");
        constants.put("response.cookie.overwritePath", "false");
        constants.put("query", "${request.url.query}");
        verify(builder).setContextVariables(constants, "// CONSTANTS");

        // encoding
        verify(builder).urlEncode("webAppHost", "webAppHostEncoded", "// ENCODE WEB APP HOST");
        verify(builder).regex(TargetMessageType.OTHER, "webAppHostEncoded", "\\.", "%2E", true, true, "// ENCODE AND REPLACE '.'");
        verify(builder).urlEncode("requestHost", "requestHostEncoded", "// ENCODE REQUEST HOST");
        verify(builder).regex(TargetMessageType.OTHER, "requestHostEncoded", "\\.", "%2E", true, true, "// ENCODE AND REPLACE '.'");
        verify(builder).regex(TargetMessageType.OTHER, "query", "\\{", "%7B", true, true, "// ENCODE AND REPLACE '{' IN QUERY");
        verify(builder).regex(TargetMessageType.OTHER, "query", "\\}", "%7D", true, true, "// ENCODE AND REPLACE '}' IN QUERY");
        verify(builder).regex(TargetMessageType.OTHER, "query", "${requestHostEncoded}", "${webAppHostEncoded}", true, true, "// REWRITE REQUEST QUERY");

        // url rewriting + route
        verify(builder).regex(TargetMessageType.REQUEST, null, "${requestHost}", "${webAppHost}", true, true, "// REWRITE REQUEST BODY");
        verify(builder).regex(TargetMessageType.REQUEST, null, "${requestHostEncoded}", "${webAppHostEncoded}", true, true, "// REWRITE ENCODED REQUEST BODY");
        verify(builder).replaceHttpCookieNames(TargetMessageType.REQUEST, null, "${requestHostEncoded}", "${webAppHostEncoded}", true, "// REWRITE REQUEST COOKIE NAMES");
        verify(builder).replaceHttpCookieDomains(TargetMessageType.REQUEST, null, "${request.url.host}", "${webAppHost}", true, "// REWRITE REQUEST COOKIE DOMAINS");
        verify(builder).routeForwardAll("http://${webAppHost}${request.url.path}${query}", false);
        verify(builder).replaceHttpCookieNames(TargetMessageType.RESPONSE, null, "${webAppHostEncoded}", "${requestHostEncoded}", true, "// REWRITE RESPONSE COOKIE NAMES");
        verify(builder).replaceHttpCookieDomains(TargetMessageType.RESPONSE, null, "${webAppHost}", "${request.url.host}", true, "// REWRITE RESPONSE COOKIE DOMAINS");
        verify(builder).rewriteHeader(TargetMessageType.RESPONSE, null, "location", "${webAppHost}", "${requestHost}", true, "// REWRITE LOCATION HEADER");
        verify(builder).regex(TargetMessageType.RESPONSE, null, "${webAppHost}(:80)?", "${requestHost}", true, true, "// REWRITE RESPONSE BODY");
        verify(builder).addOrReplaceHeader(TargetMessageType.REQUEST, null, "Host", "${requestHost}", true, false, "// REWRITE HOST HEADER");

        verify(builder, never()).rewriteHtml(any(TargetMessageType.class), anyString(), anySet(), anyString(), anyString(), anyString());
    }

    @Test
    public void genericDoNotIncludeRequestPort() throws Exception {
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.GENERIC);
        config.setWebAppHost("default-generic.l7tech.com");
        config.setIncludeRequestPort(false);
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);

        // constants
        final Map<String, String> constants = new HashMap<>();
        constants.put("webAppHost", "default-generic.l7tech.com");
        constants.put("requestHost", "${request.url.host}");
        constants.put("response.cookie.overwriteDomain", "false");
        constants.put("response.cookie.overwritePath", "false");
        constants.put("query", "${request.url.query}");
        verify(builder).setContextVariables(constants, "// CONSTANTS");

        // url rewriting + route
        verify(builder).regex(TargetMessageType.REQUEST, null, "${requestHost}", "${webAppHost}", true, true, "// REWRITE REQUEST BODY");
        verify(builder).replaceHttpCookieDomains(TargetMessageType.REQUEST, null, "${request.url.host}", "${webAppHost}", true, "// REWRITE REQUEST COOKIE DOMAINS");
        verify(builder).routeForwardAll("http://${webAppHost}${request.url.path}${" + "query" + "}", false);
        verify(builder).replaceHttpCookieDomains(TargetMessageType.RESPONSE, null, "${webAppHost}", "${request.url.host}", true, "// REWRITE RESPONSE COOKIE DOMAINS");
        verify(builder).rewriteHeader(TargetMessageType.RESPONSE, null, "location", "${webAppHost}", "${requestHost}", true, "// REWRITE LOCATION HEADER");
        verify(builder).regex(TargetMessageType.RESPONSE, null, "${webAppHost}", "${requestHost}", true, true, "// REWRITE RESPONSE BODY");
        verify(builder).addOrReplaceHeader(TargetMessageType.REQUEST, null, "Host", "${requestHost}", true, false, "// REWRITE HOST HEADER");

        verify(builder, never()).urlEncode(anyString(), anyString(), anyString());
        verify(builder, never()).regex(eq(TargetMessageType.OTHER), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString());
        verify(builder, never()).replaceHttpCookieNames(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyBoolean(), anyString());
        verify(builder, never()).rewriteHtml(any(TargetMessageType.class), anyString(), anySet(), anyString(), anyString(), anyString());
    }

    @Test
    public void genericRewriteHost() throws Exception {
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.GENERIC);
        config.setWebAppHost("default-generic.l7tech.com");
        config.setRewriteHostHeader(true);
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);

        verify(builder).addOrReplaceHeader(TargetMessageType.REQUEST, null, "Host", "${requestHost}", true, true, "// REWRITE HOST HEADER");
    }

    @Test
    public void sharepointRewriteHost() throws Exception {
        config.setWebAppHost("default-sharepoint.l7tech.com");
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.SHAREPOINT);
        config.setRewriteHostHeader(true);
        PublishReverseWebProxyWizard.buildPolicyXml(config, Collections.<Assertion>emptyList(), builder);

        verify(builder).addOrReplaceHeader(TargetMessageType.REQUEST, null, "Host", "${requestHost}", true, true, "// REWRITE HOST HEADER");
    }
}
