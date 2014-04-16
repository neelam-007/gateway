package com.l7tech.console.panels.reverseproxy;

import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.builder.PolicyBuilder;
import com.l7tech.policy.variable.DataType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PublishReverseWebProxyWizardTest {
    private ReverseWebProxyConfig config;
    @Mock
    private PolicyBuilder builder;

    @Before
    public void setup() throws Exception {
        config = new TestableReverseWebProxyConfig();
        when(builder.setContextVariable(anyString(), anyString())).thenReturn(builder);
        when(builder.setContextVariable(anyString(), anyString(), any(DataType.class), anyString())).thenReturn(builder);
        when(builder.urlEncode(anyString(), anyString())).thenReturn(builder);
        when(builder.regex(any(TargetMessageType.class), anyString(), anyString(), anyString())).thenReturn(builder);
        when(builder.replaceHttpCookieNames(any(TargetMessageType.class), anyString(), anyString(), anyString())).thenReturn(builder);
        when(builder.replaceHttpCookieDomains(any(TargetMessageType.class), anyString(), anyString(), anyString())).thenReturn(builder);
        when(builder.routeForwardAll(anyString(), anyBoolean())).thenReturn(builder);
        when(builder.rewriteHeader(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyString())).thenReturn(builder);
        when(builder.rewriteHtml(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyString())).thenReturn(builder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullWebAppHost() throws Exception {
        PublishReverseWebProxyWizard.buildPolicyXml(config, builder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyWebAppHost() throws Exception {
        config.setWebAppHost("");
        PublishReverseWebProxyWizard.buildPolicyXml(config, builder);
    }

    @Test
    public void defaultGeneric() throws Exception {
        config.setWebAppHost("default-generic.l7tech.com");
        PublishReverseWebProxyWizard.buildPolicyXml(config, builder);

        // constants
        verify(builder).setContextVariable("webAppHost", "default-generic.l7tech.com");
        verify(builder).setContextVariable("response.cookie.overwriteDomain", "false");
        verify(builder).setContextVariable("response.cookie.overwritePath", "false");
        verify(builder).setContextVariable("query", "${request.url.query}");

        // url rewriting + route
        verify(builder).replaceHttpCookieDomains(TargetMessageType.REQUEST, null, "${request.url.host}", "${webAppHost}");
        verify(builder).routeForwardAll("http://${webAppHost}${request.url.path}${" + "query" + "}", false);
        verify(builder).replaceHttpCookieDomains(TargetMessageType.RESPONSE, null, "${webAppHost}", "${request.url.host}");
        verify(builder).rewriteHeader(TargetMessageType.RESPONSE, null, "location", "${webAppHost}", "${request.url.host}:${request.url.port}");
        verify(builder).regex(TargetMessageType.RESPONSE, null, "${webAppHost}", "${request.url.host}:${request.url.port}");

        verify(builder, never()).urlEncode(anyString(), anyString());
        verify(builder, never()).regex(eq(TargetMessageType.OTHER), anyString(), anyString(), anyString());
        verify(builder, never()).replaceHttpCookieNames(any(TargetMessageType.class), anyString(), anyString(), anyString());
        verify(builder, never()).rewriteHtml(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void minimalGeneric() throws Exception {
        config.setWebAppHost("default-generic.l7tech.com");
        config.setRewriteCookies(false);
        config.setRewriteLocationHeader(false);
        config.setRewriteResponseContent(false);
        PublishReverseWebProxyWizard.buildPolicyXml(config, builder);

        // constants + route
        verify(builder).setContextVariable("webAppHost", "default-generic.l7tech.com");
        verify(builder).setContextVariable("response.cookie.overwriteDomain", "false");
        verify(builder).setContextVariable("response.cookie.overwritePath", "false");
        verify(builder).setContextVariable("query", "${request.url.query}");
        verify(builder).routeForwardAll("http://${webAppHost}${request.url.path}${query}", false);

        verify(builder, never()).urlEncode(anyString(), anyString());
        verify(builder, never()).replaceHttpCookieDomains(any(TargetMessageType.class), anyString(), anyString(), anyString());
        verify(builder, never()).replaceHttpCookieNames(any(TargetMessageType.class), anyString(), anyString(), anyString());
        verify(builder, never()).regex(any(TargetMessageType.class), anyString(), anyString(), anyString());
        verify(builder, never()).rewriteHeader(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyString());
        verify(builder, never()).rewriteHtml(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void genericViaHttps() throws Exception {
        config.setWebAppHost("default-generic.l7tech.com");
        config.setUseHttps(true);
        PublishReverseWebProxyWizard.buildPolicyXml(config, builder);

        verify(builder).routeForwardAll("https://${webAppHost}${request.url.path}${query}", false);
    }

    @Test
    public void genericRewriteSpecificTags() throws Exception {
        config.setWebAppHost("default-generic.l7tech.com");
        config.setHtmlTagsToRewrite("p,script");
        PublishReverseWebProxyWizard.buildPolicyXml(config, builder);

        verify(builder).rewriteHtml(TargetMessageType.RESPONSE, null, "${webAppHost}", "${request.url.host}:${request.url.port}", "p,script");
        verify(builder, never()).regex(eq(TargetMessageType.RESPONSE), anyString(), anyString(), anyString());
    }

    @Test
    public void defaultSharepoint() throws Exception {
        config.setWebAppHost("default-sharepoint.l7tech.com");
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.SHAREPOINT);
        PublishReverseWebProxyWizard.buildPolicyXml(config, builder);

        // constants
        verify(builder).setContextVariable("webAppHost", "default-sharepoint.l7tech.com");
        verify(builder).setContextVariable("response.cookie.overwriteDomain", "false");
        verify(builder).setContextVariable("response.cookie.overwritePath", "false");
        verify(builder).setContextVariable("query", "${request.url.query}");

        // encoding
        verify(builder).urlEncode("webAppHost", "webAppHostEncoded");
        verify(builder).regex(TargetMessageType.OTHER, "webAppHostEncoded", "\\.", "%2E");
        verify(builder).regex(TargetMessageType.OTHER, "query", "\\{", "%7B");
        verify(builder).regex(TargetMessageType.OTHER, "query", "\\}", "%7D");

        // url rewriting + route
        verify(builder).replaceHttpCookieNames(TargetMessageType.REQUEST, null, "${request.url.host}%3A${request.url.port}", "${webAppHostEncoded}");
        verify(builder).replaceHttpCookieDomains(TargetMessageType.REQUEST, null, "${request.url.host}", "${webAppHost}");
        verify(builder).routeForwardAll("http://${webAppHost}${request.url.path}${query}", false);
        verify(builder).replaceHttpCookieNames(TargetMessageType.RESPONSE, null, "${webAppHostEncoded}", "${request.url.host}%3A${request.url.port}");
        verify(builder).replaceHttpCookieDomains(TargetMessageType.RESPONSE, null, "${webAppHost}", "${request.url.host}");
        verify(builder).rewriteHeader(TargetMessageType.RESPONSE, null, "location", "${webAppHost}", "${request.url.host}:${request.url.port}");
        verify(builder).regex(TargetMessageType.RESPONSE, null, "${webAppHost}", "${request.url.host}:${request.url.port}");

        verify(builder, never()).rewriteHtml(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void minimalSharepoint() throws Exception {
        config.setWebAppHost("default-sharepoint.l7tech.com");
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.SHAREPOINT);
        config.setRewriteCookies(false);
        config.setRewriteLocationHeader(false);
        config.setRewriteResponseContent(false);
        PublishReverseWebProxyWizard.buildPolicyXml(config, builder);

        // constants
        verify(builder).setContextVariable("webAppHost", "default-sharepoint.l7tech.com");
        verify(builder).setContextVariable("response.cookie.overwriteDomain", "false");
        verify(builder).setContextVariable("response.cookie.overwritePath", "false");
        verify(builder).setContextVariable("query", "${request.url.query}");

        // encoding + route
        verify(builder).urlEncode("webAppHost", "webAppHostEncoded");
        verify(builder).regex(TargetMessageType.OTHER, "webAppHostEncoded", "\\.", "%2E");
        verify(builder).regex(TargetMessageType.OTHER, "query", "\\{", "%7B");
        verify(builder).regex(TargetMessageType.OTHER, "query", "\\}", "%7D");
        verify(builder).routeForwardAll("http://${webAppHost}${request.url.path}${query}", false);

        verify(builder, never()).replaceHttpCookieDomains(any(TargetMessageType.class), anyString(), anyString(), anyString());
        verify(builder, never()).replaceHttpCookieNames(any(TargetMessageType.class), anyString(), anyString(), anyString());
        verify(builder, never()).rewriteHeader(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyString());
        verify(builder, never()).regex(eq(TargetMessageType.RESPONSE), anyString(), anyString(), anyString());
        verify(builder, never()).rewriteHtml(any(TargetMessageType.class), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void sharepointViaHttps() throws Exception {
        config.setWebAppHost("default-sharepoint.l7tech.com");
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.SHAREPOINT);
        config.setUseHttps(true);
        PublishReverseWebProxyWizard.buildPolicyXml(config, builder);

        verify(builder).routeForwardAll("https://${webAppHost}${request.url.path}${query}", false);
    }

    @Test
    public void sharepointRewriteSpecificTags() throws Exception {
        config.setWebAppHost("default-sharepoint.l7tech.com");
        config.setWebAppType(ReverseWebProxyConfig.WebApplicationType.SHAREPOINT);
        config.setHtmlTagsToRewrite("p,script");
        PublishReverseWebProxyWizard.buildPolicyXml(config, builder);

        verify(builder).rewriteHtml(TargetMessageType.RESPONSE, null, "${webAppHost}", "${request.url.host}:${request.url.port}", "p,script");
        verify(builder, never()).regex(eq(TargetMessageType.RESPONSE), anyString(), anyString(), anyString());
    }

    private class TestableReverseWebProxyConfig extends ReverseWebProxyConfig {
        @Override
        protected Folder getDefaultFolder() {
            return new Folder("root", null);
        }
    }
}
