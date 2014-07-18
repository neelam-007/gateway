package com.l7tech.server.util;

import com.l7tech.common.http.*;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugId;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.*;

import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;
import static org.junit.Assert.*;

public class HttpForwardingRuleEnforcerJavaTest {
    private static final String TARGET_DOMAIN = "localhost";
    private static final String SOAP_MESSAGE = "<?xml version=\"1.0\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
            "  <soap:Header>\n" +
            "  </soap:Header>\n" +
            "  <soap:Body>\n" +
            "    <m:GetStockPrice xmlns:m=\"http://www.example.org/stock\">\n" +
            "      <m:StockName>IBM</m:StockName>\n" +
            "    </m:GetStockPrice>\n" +
            "  </soap:Body>\n" +
            "</soap:Envelope>";
    private Message request;
    private Message response;
    private GenericHttpRequestParams requestParams;
    private PolicyEnforcementContext context;
    private HttpPassthroughRuleSet ruleSet;
    private List<HttpPassthroughRule> rules;
    private Audit audit;
    private StashManager stashManager;
    private Map<String, String> vars;
    private HeadersKnob responseHeadersKnob;
    private List<GenericHttpHeader> responseHeaders;

    @Before
    public void setup() throws Exception {
        stashManager = new ByteArrayStashManager();
        request = new Message(stashManager, ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream("<test></test>".getBytes()));
        response = new Message();
        requestParams = new GenericHttpRequestParams();
        response.attachHttpResponseKnob(new HttpServletResponseKnob(new MockHttpServletResponse()));
        responseHeadersKnob = new HeadersKnobSupport();
        response.attachKnob(responseHeadersKnob, HeadersKnob.class);
        responseHeaders = new ArrayList<>();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        ruleSet = new HttpPassthroughRuleSet();
        ruleSet.setForwardAll(true);
        rules = new ArrayList<>();
        audit = new TestAudit();
        vars = new HashMap<>();
    }

    @Test
    public void requestNotInitialized() throws Exception {
        final Message uninitialized = new Message();
        final PolicyEnforcementContext uninitializedContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(uninitialized, response);
        HttpForwardingRuleEnforcer.handleRequestHeaders(uninitialized, requestParams, uninitializedContext, ruleSet, audit, null, null);
        assertTrue(requestParams.getExtraHeaders().isEmpty());
    }

    @Test
    public void forwardAllRequestHeaders() throws Exception {
        request.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        final List<HttpHeader> extraHeaders = requestParams.getExtraHeaders();
        assertEquals(1, extraHeaders.size());
        assertEquals("foo", extraHeaders.get(0).getName());
        assertEquals("bar", extraHeaders.get(0).getFullValue());
    }

    @Test
    public void forwardAllRequestHeadersFiltersNonPassThroughHeaders() throws Exception {
        request.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP, true);
        request.getHeadersKnob().addHeader("doNotPassThrough", "doNotPassThrough", HEADER_TYPE_HTTP, false);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        final List<HttpHeader> extraHeaders = requestParams.getExtraHeaders();
        assertEquals(1, extraHeaders.size());
        assertEquals("foo", extraHeaders.get(0).getName());
        assertEquals("bar", extraHeaders.get(0).getFullValue());
    }

    @Test
    public void forwardAllRequestHeadersMultipleWithSameName() throws Exception {
        request.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        request.getHeadersKnob().addHeader("foo", "bar2", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        final List<HttpHeader> extraHeaders = requestParams.getExtraHeaders();
        final Map<String, List<String>> headersMap = generateHeadersMap(extraHeaders);
        assertEquals(1, headersMap.size());
        assertEquals(2, headersMap.get("foo").size());
        assertTrue(headersMap.get("foo").contains("bar"));
        assertTrue(headersMap.get("foo").contains("bar2"));
    }

    @Test
    public void existingRequestHeaderReplaced() throws Exception {
        requestParams.addExtraHeader(new GenericHttpHeader("foo", "shouldBeReplaced"));
        request.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        request.getHeadersKnob().addHeader("foo", "bar2", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        final List<HttpHeader> extraHeaders = requestParams.getExtraHeaders();
        final Map<String, List<String>> headersMap = generateHeadersMap(extraHeaders);
        assertEquals(1, headersMap.size());
        assertEquals(2, headersMap.get("foo").size());
        assertTrue(headersMap.get("foo").contains("bar"));
        assertTrue(headersMap.get("foo").contains("bar2"));
    }

    @Test
    public void existingRequestHeaderReplacedCaseInsensitive() throws Exception {
        requestParams.addExtraHeader(new GenericHttpHeader("FOO", "shouldBeReplaced"));
        request.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        request.getHeadersKnob().addHeader("foo", "bar2", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        final List<HttpHeader> extraHeaders = requestParams.getExtraHeaders();
        final Map<String, List<String>> headersMap = generateHeadersMap(extraHeaders);
        assertEquals(1, headersMap.size());
        assertEquals(2, headersMap.get("foo").size());
        assertTrue(headersMap.get("foo").contains("bar"));
        assertTrue(headersMap.get("foo").contains("bar2"));
    }

    @Test
    public void existingRequestHeaderReplacedByRule() throws Exception {
        requestParams.addExtraHeader(new GenericHttpHeader("FOO", "shouldBeReplaced"));
        request.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        rules.add(new HttpPassthroughRule("foo", true, "bar2"));
        rules.add(new HttpPassthroughRule("foo", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        final List<HttpHeader> extraHeaders = requestParams.getExtraHeaders();
        final Map<String, List<String>> headersMap = generateHeadersMap(extraHeaders);
        assertEquals(1, headersMap.size());
        assertEquals(2, headersMap.get("foo").size());
        assertTrue(headersMap.get("foo").contains("bar"));
        assertTrue(headersMap.get("foo").contains("bar2"));
    }

    @Test
    public void soapActionFromRequestIgnoredIfNotInKnob() throws Exception {
        final Message soapRequest = generateSoapRequest();
        HttpForwardingRuleEnforcer.handleRequestHeaders(soapRequest, requestParams, PolicyEnforcementContextFactory.createPolicyEnforcementContext(soapRequest, response), ruleSet, audit, null, null);
        assertTrue(requestParams.getExtraHeaders().isEmpty());
    }

    @Test
    public void soapActionFromRequestHeadersKnob() throws Exception {
        request.getHeadersKnob().addHeader(SoapUtil.SOAPACTION, "test", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        final Map<String, List<String>> headersMap = generateHeadersMap(requestParams.getExtraHeaders());
        assertEquals(1, headersMap.get(SoapUtil.SOAPACTION).size());
        assertEquals("test", headersMap.get(SoapUtil.SOAPACTION).get(0));
    }

    @Test
    public void soapActionFromHeadersKnobHasPriorityOverRequest() throws Exception {
        final Message soapRequest = generateSoapRequest();
        // headers knob has priority
        soapRequest.getHeadersKnob().addHeader(SoapUtil.SOAPACTION, "priority", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(soapRequest, requestParams, PolicyEnforcementContextFactory.createPolicyEnforcementContext(soapRequest, response), ruleSet, audit, null, null);
        final Map<String, List<String>> headersMap = generateHeadersMap(requestParams.getExtraHeaders());
        assertEquals(1, headersMap.get(SoapUtil.SOAPACTION).size());
        assertEquals("priority", headersMap.get(SoapUtil.SOAPACTION).get(0));
    }

    @Test
    public void customizeRequestSoapAction() throws Exception {
        rules.add(new HttpPassthroughRule("SOAPAction", true, "customSoapAction"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        final Message soapRequest = generateSoapRequest();
        soapRequest.getHeadersKnob().addHeader(SoapUtil.SOAPACTION, "shouldBeReplaced", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(soapRequest, requestParams, PolicyEnforcementContextFactory.createPolicyEnforcementContext(soapRequest, response), ruleSet, audit, null, null);
        final Map<String, List<String>> headersMap = generateHeadersMap(requestParams.getExtraHeaders());
        assertEquals(1, headersMap.get(SoapUtil.SOAPACTION).size());
        assertEquals("customSoapAction", headersMap.get(SoapUtil.SOAPACTION).get(0));
    }

    @Test
    public void passThroughRequestSoapAction() throws Exception {
        rules.add(new HttpPassthroughRule("SOAPAction", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        final Message soapRequest = generateSoapRequest();
        soapRequest.getHeadersKnob().addHeader(SoapUtil.SOAPACTION, "testSoapAction", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(soapRequest, requestParams, PolicyEnforcementContextFactory.createPolicyEnforcementContext(soapRequest, response), ruleSet, audit, null, null);
        final Map<String, List<String>> headersMap = generateHeadersMap(requestParams.getExtraHeaders());
        assertEquals(1, headersMap.get(SoapUtil.SOAPACTION).size());
        assertEquals("testSoapAction", headersMap.get(SoapUtil.SOAPACTION).get(0));
    }

    @Test(expected = IOException.class)
    public void emptyRequestHeaderName() throws Exception {
        request.getHeadersKnob().addHeader("", "empty", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
    }

    @Test
    public void requestAuthHeader() throws Exception {
        request.getHeadersKnob().addHeader(HttpConstants.HEADER_AUTHORIZATION, "foo", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals(HttpConstants.HEADER_AUTHORIZATION, requestParams.getExtraHeaders().get(0).getName());
        assertEquals("foo", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestAuthHeaderSkippedIfPasswordAuthPresent() throws Exception {
        requestParams.setPasswordAuthentication(new PasswordAuthentication("foo", "bar".toCharArray()));
        request.getHeadersKnob().addHeader(HttpConstants.HEADER_AUTHORIZATION, "foo", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(0, requestParams.getExtraHeaders().size());
    }

    @Test
    public void requestCookies() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("a", "apple", 0, "/", TARGET_DOMAIN));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("b", "bear", 0, "/", TARGET_DOMAIN));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("Cookie", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("a=apple; b=bear", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestCookiesWithInvalidCookie() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("a", "apple", 0, "/", TARGET_DOMAIN));
        request.getHeadersKnob().addHeader("Cookie", "invalid", HeadersKnob.HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("Cookie", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("a=apple; invalid", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestCookieInvalid() throws Exception {
        request.getHeadersKnob().addHeader("Cookie", "invalid", HeadersKnob.HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("Cookie", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("invalid", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestGatewayManagedCookieHeadersNotPassed() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("a", "apple", 0, "/", TARGET_DOMAIN));
        request.getHttpCookiesKnob().addCookie(new HttpCookie(CookieUtils.PREFIX_GATEWAY_MANAGED + "foo", "bar", 0, "/", TARGET_DOMAIN));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("Cookie", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("a=apple", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestCookieHeadersDoesNotIncludePathOrDomain() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("a", "apple", 0, "/somePath", "someDomain"));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("Cookie", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("a=apple", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestCookieHeadersDoesNotIncludeSetCookieAttributes() throws Exception {
        request.getHttpCookiesKnob().addCookie(new HttpCookie("a", "apple", 1, "/somePath", "someDomain", 60, true, "someComment", true));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("Cookie", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("a=apple", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @BugId("SSG-8415")
    @Test
    public void requestCookiesCustomizedWithRule() throws Exception {
        rules.add(new HttpPassthroughRule("Cookie", true, "ruleName=ruleValue"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        request.getHttpCookiesKnob().addCookie(new HttpCookie("knobName", "knobValue", 0, "/", TARGET_DOMAIN));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("Cookie", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("ruleName=ruleValue", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    /**
     * If the request has a set-cookie header (which is actually a response header), treat it like any other header.
     */
    @BugId("SSG-8416")
    @Test
    public void requestWithSetCookieHeader() throws Exception {
        request.getHeadersKnob().addHeader("Set-Cookie", "foo=bar; version=1; domain=localhost; path=/; comment=test", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("Set-Cookie", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("foo=bar; version=1; domain=localhost; path=/; comment=test", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestRuleWithCustomValue() throws Exception {
        rules.add(new HttpPassthroughRule("foo", true, "custom"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("foo", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("custom", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestRuleWithCustomValueOverridesNonPassThroughHeader() throws Exception {
        // originally the header is set to not be passed through
        request.getHeadersKnob().addHeader("foo", "doNotPassThrough", HEADER_TYPE_HTTP, false);
        // however adding a custom value for the header should take precedence
        rules.add(new HttpPassthroughRule("foo", true, "custom"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("foo", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("custom", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestRuleWithContextVariableCustomValue() throws Exception {
        vars.put("custom", "customValue");
        rules.add(new HttpPassthroughRule("foo", true, "${custom}"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, vars, vars.keySet().toArray(new String[vars.size()]));
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("foo", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("customValue", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestRuleWithContextVariableCustomValueRetrievedFromContext() throws Exception {
        context.setVariable("custom", "customValue");
        rules.add(new HttpPassthroughRule("foo", true, "${custom}"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, new String[]{"custom"});
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("foo", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("customValue", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestRuleWithCustomValueReplacesOriginal() throws Exception {
        rules.add(new HttpPassthroughRule("foo", true, "custom"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        request.getHeadersKnob().addHeader("foo", "shouldBeReplaced", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("foo", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("custom", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestRuleWithOriginalValue() throws Exception {
        rules.add(new HttpPassthroughRule("foo", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        request.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("foo", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("bar", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestRuleWithOriginalValueDoNotPassThrough() throws Exception {
        rules.add(new HttpPassthroughRule("foo", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        request.getHeadersKnob().addHeader("foo", "doNotPassThrough", HEADER_TYPE_HTTP, false);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(0, requestParams.getExtraHeaders().size());
    }

    @Test
    public void requestRuleWithOriginalValueHeaderDoesNotExist() throws Exception {
        rules.add(new HttpPassthroughRule("foo", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(0, requestParams.getExtraHeaders().size());
    }

    @Test
    public void requestRulesFilterHeaders() throws Exception {
        rules.add(new HttpPassthroughRule("add", true, "shouldBeAdded"));
        rules.add(new HttpPassthroughRule("test", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        request.getHeadersKnob().addHeader("shouldBeFiltered", "shouldBeFiltered", HEADER_TYPE_HTTP);
        request.getHeadersKnob().addHeader("test", "shouldNotBeFiltered", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        final Map<String, List<String>> headers = generateHeadersMap(requestParams.getExtraHeaders());
        assertEquals(2, headers.size());
        assertEquals(1, headers.get("add").size());
        assertEquals("shouldBeAdded", headers.get("add").get(0));
        assertEquals(1, headers.get("test").size());
        assertEquals("shouldNotBeFiltered", headers.get("test").get(0));
        // ensure headers knob was not modified by routing
        assertEquals(2, request.getHeadersKnob().getHeaderNames().length);
        final List<String> headersOnKnob = Arrays.asList(request.getHeadersKnob().getHeaderNames());
        assertTrue(headersOnKnob.contains("shouldBeFiltered"));
        assertTrue(headersOnKnob.contains("test"));
        assertFalse(headersOnKnob.contains("add"));
    }

    @BugId("SSG-8918")
    @Test
    public void requestRulesDuplicateHeaders() throws Exception {
        rules.add(new HttpPassthroughRule("foo", true, "1"));
        rules.add(new HttpPassthroughRule("foo", true, "2"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        request.getHeadersKnob().addHeader("foo", "shouldBeOverwritten", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        final Map<String, List<String>> headers = generateHeadersMap(requestParams.getExtraHeaders());
        assertEquals(2, headers.get("foo").size());
        assertEquals("1", headers.get("foo").get(0));
        assertEquals("2", headers.get("foo").get(1));
    }

    @BugId("SSG-6543,SSG-8415")
    @Test
    public void requestRuleWithHost() throws Exception {
        rules.add(new HttpPassthroughRule("host", true, "fromRule"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        request.getHeadersKnob().addHeader("Host", "fromAddHeader", HEADER_TYPE_HTTP);
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals("fromRule", requestParams.getVirtualHost());
        assertEquals(0, requestParams.getExtraHeaders().size());
    }

    @Test(expected = IOException.class)
    public void requestRuleWithEmptyName() throws Exception {
        rules.add(new HttpPassthroughRule("", true, "uhOh"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
    }

    @Test
    public void requestRuleWithUserAgentOverridesExistingUserAgent() throws Exception {
        requestParams.addExtraHeader(new GenericHttpHeader("User-Agent", "shouldBeOverridden"));
        rules.add(new HttpPassthroughRule("User-Agent", true, "customUserAgent"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("User-Agent", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("customUserAgent", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void forwardAllResponseHeaders() throws Exception {
        responseHeaders.add(new GenericHttpHeader("foo", "bar"));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        assertEquals("foo", responseHeadersKnob.getHeaderNames()[0]);
        assertEquals(1, responseHeadersKnob.getHeaderValues("foo").length);
        assertEquals("bar", responseHeadersKnob.getHeaderValues("foo")[0]);
    }

    @Test
    public void specialResponseHeaders() throws Exception {
        for (final String specialHeader : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            responseHeaders.add(new GenericHttpHeader(specialHeader, "testValue"));
        }
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD.size(), responseHeadersKnob.getHeaderNames().length);
        final List<String> headerNames = Arrays.asList(responseHeadersKnob.getHeaderNames());
        for (final String specialHeader : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            // special headers should be included but flagged as non-passthrough
            assertTrue(headerNames.contains(specialHeader));
            assertFalse(responseHeadersKnob.getHeaders(specialHeader, HEADER_TYPE_HTTP).iterator().next().isPassThrough());
        }
    }

    @Test
    public void responseSetCookieHeaders() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "foo=bar"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "a=hasPath; path=/cookiePath"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "b=hasDomain; domain=cookieDomain"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "c=hasPathAndDomain; domain=cookieDomain; path=/cookiePath"));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        final Map<String, HttpCookie> cookiesMap = createCookiesMap(context.getResponse());
        assertEquals(4, cookiesMap.size());
        final HttpCookie fooCookie = cookiesMap.get("foo");
        assertEquals("bar", fooCookie.getCookieValue());
        assertEquals("localhost", fooCookie.getDomain());
        assertEquals("/test", fooCookie.getPath());
        final HttpCookie aCookie = cookiesMap.get("a");
        assertEquals("hasPath", aCookie.getCookieValue());
        assertEquals("localhost", aCookie.getDomain());
        assertEquals("/cookiePath", aCookie.getPath());
        final HttpCookie bCookie = cookiesMap.get("b");
        assertEquals("hasDomain", bCookie.getCookieValue());
        assertEquals("cookieDomain", bCookie.getDomain());
        assertEquals("/test", bCookie.getPath());
        final HttpCookie cCookie = cookiesMap.get("c");
        assertEquals("hasPathAndDomain", cCookie.getCookieValue());
        assertEquals("cookieDomain", cCookie.getDomain());
        assertEquals("/cookiePath", cCookie.getPath());
    }

    /**
     * For backwards compatibility, request domain and/or path should only be used if the response cookie domain and/or path is missing and the context is configured to overwrite the domain and/or path.
     */
    @BugId("SSG-8033")
    @Test
    public void responseSetCookieHeadersDoNotOverwriteDomain() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "foo=bar"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "a=hasPath; path=/cookiePath"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "b=hasDomain; domain=cookieDomain"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "c=hasPathAndDomain; domain=cookieDomain; path=/cookiePath"));
        context.setOverwriteResponseCookieDomain(false);
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        final Map<String, HttpCookie> cookiesMap = createCookiesMap(context.getResponse());
        assertEquals(4, cookiesMap.size());

        final HttpCookie fooCookie = cookiesMap.get("foo");
        assertEquals("bar", fooCookie.getCookieValue());
        assertNull(fooCookie.getDomain());
        assertEquals("/test", fooCookie.getPath());

        final HttpCookie aCookie = cookiesMap.get("a");
        assertEquals("hasPath", aCookie.getCookieValue());
        assertNull(aCookie.getDomain());
        assertEquals("/cookiePath", aCookie.getPath());

        final HttpCookie bCookie = cookiesMap.get("b");
        assertEquals("hasDomain", bCookie.getCookieValue());
        assertEquals("cookieDomain", bCookie.getDomain());
        assertEquals("/test", bCookie.getPath());

        final HttpCookie cCookie = cookiesMap.get("c");
        assertEquals("hasPathAndDomain", cCookie.getCookieValue());
        assertEquals("cookieDomain", cCookie.getDomain());
        assertEquals("/cookiePath", cCookie.getPath());
    }

    /**
     * For backwards compatibility, request domain and/or path should only be used if the response cookie domain and/or path is missing and the context is configured to overwrite the domain and/or path.
     */
    @BugId("SSG-8033")
    @Test
    public void responseSetCookieHeadersDoNotOverwritePath() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "foo=bar"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "a=hasPath; path=/cookiePath"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "b=hasDomain; domain=cookieDomain"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "c=hasPathAndDomain; domain=cookieDomain; path=/cookiePath"));
        context.setOverwriteResponseCookiePath(false);
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        final Map<String, HttpCookie> cookiesMap = createCookiesMap(context.getResponse());
        assertEquals(4, cookiesMap.size());

        final HttpCookie fooCookie = cookiesMap.get("foo");
        assertEquals("bar", fooCookie.getCookieValue());
        assertEquals("localhost", fooCookie.getDomain());
        assertNull(fooCookie.getPath());

        final HttpCookie aCookie = cookiesMap.get("a");
        assertEquals("hasPath", aCookie.getCookieValue());
        assertEquals("localhost", aCookie.getDomain());
        assertEquals("/cookiePath", aCookie.getPath());

        final HttpCookie bCookie = cookiesMap.get("b");
        assertEquals("hasDomain", bCookie.getCookieValue());
        assertEquals("cookieDomain", bCookie.getDomain());
        assertNull(bCookie.getPath());

        final HttpCookie cCookie = cookiesMap.get("c");
        assertEquals("hasPathAndDomain", cCookie.getCookieValue());
        assertEquals("cookieDomain", cCookie.getDomain());
        assertEquals("/cookiePath", cCookie.getPath());
    }

    /**
     * For backwards compatibility, request domain and/or path should only be used if the response cookie domain and/or path is missing and the context is configured to overwrite the domain and/or path.
     */
    @BugId("SSG-8033")
    @Test
    public void responseSetCookieHeadersDoNotOverwriteDomainOrPath() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "foo=bar"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "a=hasPath; path=/cookiePath"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "b=hasDomain; domain=cookieDomain"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "c=hasPathAndDomain; domain=cookieDomain; path=/cookiePath"));
        context.setOverwriteResponseCookieDomain(false);
        context.setOverwriteResponseCookiePath(false);
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        final Map<String, HttpCookie> cookiesMap = createCookiesMap(context.getResponse());
        assertEquals(4, cookiesMap.size());

        final HttpCookie fooCookie = cookiesMap.get("foo");
        assertEquals("bar", fooCookie.getCookieValue());
        assertNull(fooCookie.getDomain());
        assertNull(fooCookie.getPath());

        final HttpCookie aCookie = cookiesMap.get("a");
        assertEquals("hasPath", aCookie.getCookieValue());
        assertNull(aCookie.getDomain());
        assertEquals("/cookiePath", aCookie.getPath());

        final HttpCookie bCookie = cookiesMap.get("b");
        assertEquals("hasDomain", bCookie.getCookieValue());
        assertEquals("cookieDomain", bCookie.getDomain());
        assertNull(bCookie.getPath());

        final HttpCookie cCookie = cookiesMap.get("c");
        assertEquals("hasPathAndDomain", cCookie.getCookieValue());
        assertEquals("cookieDomain", cCookie.getDomain());
        assertEquals("/cookiePath", cCookie.getPath());
    }

    @Test
    public void responseSetCookieHeadersInvalidCookie() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "foo=bar"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "invalid"));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        final String[] cookieValues = responseHeadersKnob.getHeaderValues("Set-Cookie");
        assertEquals(2, cookieValues.length);
        assertEquals("foo=bar; Domain=localhost; Path=/test", cookieValues[0]);
        assertEquals("invalid", cookieValues[1]);
        final Map<String, HttpCookie> cookiesMap = createCookiesMap(context.getResponse());
        assertEquals(2, cookiesMap.size());
        assertEquals("bar", cookiesMap.get("foo").getCookieValue());
        assertEquals("", cookiesMap.get("invalid").getCookieValue());
    }

    @Test
    public void responseRuleCustomValue() {
        rules.add(new HttpPassthroughRule("foo", true, "bar"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        assertEquals(1, responseHeadersKnob.getHeaderValues("foo").length);
        assertEquals("bar", responseHeadersKnob.getHeaderValues("foo")[0]);
    }

    @Test
    public void responseRuleCustomValueFromContextVariable() {
        vars.put("custom", "customValue");
        rules.add(new HttpPassthroughRule("foo", true, "${custom}"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, vars, vars.keySet().toArray(new String[vars.size()]));
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        assertEquals(1, responseHeadersKnob.getHeaderValues("foo").length);
        assertEquals("customValue", responseHeadersKnob.getHeaderValues("foo")[0]);
    }

    @Test
    public void responseRuleCustomValueFromContextVariableRetrievedFromContext() {
        context.setVariable("custom", "customValue");
        rules.add(new HttpPassthroughRule("foo", true, "${custom}"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, new String[]{"custom"});
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        assertEquals(1, responseHeadersKnob.getHeaderValues("foo").length);
        assertEquals("customValue", responseHeadersKnob.getHeaderValues("foo")[0]);
    }

    @Test
    public void responseRuleOriginalValue() {
        responseHeaders.add(new GenericHttpHeader("foo", "bar"));
        rules.add(new HttpPassthroughRule("foo", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        assertEquals(1, responseHeadersKnob.getHeaderValues("foo").length);
        assertEquals("bar", responseHeadersKnob.getHeaderValues("foo")[0]);
    }

    @Test
    public void responseRuleOriginalValueNotFound() {
        rules.add(new HttpPassthroughRule("foo", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(0, responseHeadersKnob.getHeaderNames().length);
    }

    @Test
    public void responseRulesFilterHeaders() {
        responseHeaders.add(new GenericHttpHeader("notInRule", "shouldBeFiltered"));
        ruleSet.setForwardAll(false);
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(0, responseHeadersKnob.getHeaderNames().length);
    }

    @Test
    public void responseRuleWithSetCookie() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "foo=bar"));
        rules.add(new HttpPassthroughRule("Set-Cookie", false, "thisValueIsNotUsed"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        assertEquals(1, context.getResponse().getHttpCookiesKnob().getCookies().size());
        final HttpCookie cookie = context.getResponse().getHttpCookiesKnob().getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
    }

    @Test
    public void responseRuleWithSetCookieInvalidCookie() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        // set-cookie value is empty
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "invalid"));
        rules.add(new HttpPassthroughRule("Set-Cookie", false, "thisValueIsNotUsed"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        assertEquals("Set-Cookie", responseHeadersKnob.getHeaderNames()[0]);
        assertEquals("invalid", responseHeadersKnob.getHeaderValues("Set-Cookie")[0]);
        final Set<HttpCookie> cookies = context.getResponse().getHttpCookiesKnob().getCookies();
        assertEquals(1, cookies.size());
        final HttpCookie cookie = cookies.iterator().next();
        assertEquals("invalid", cookie.getCookieName());
        assertEquals("", cookie.getCookieValue());
    }

    @Test
    public void responseRulesWithDuplicateHeaders() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        responseHeaders.add(new GenericHttpHeader("foo", "shouldBeOverwritten"));
        rules.add(new HttpPassthroughRule("foo", true, "1"));
        rules.add(new HttpPassthroughRule("foo", true, "2"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), response, audit, ruleSet, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        final String[] values = responseHeadersKnob.getHeaderValues("foo");
        assertEquals(2, values.length);
        assertEquals("1", values[0]);
        assertEquals("2", values[1]);
    }

    @BugId("SSG-8561")
    @Test
    public void nonDefaultResponseSetCookieHeaders() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "foo=bar"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "a=hasPath; path=/cookiePath"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "b=hasDomain; domain=cookieDomain"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "c=hasPathAndDomain; domain=cookieDomain; path=/cookiePath"));
        final Message nonDefaultResponse = new Message();
        nonDefaultResponse.attachHttpResponseKnob(new HttpServletResponseKnob(new MockHttpServletResponse()));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), nonDefaultResponse, audit, ruleSet, context, requestParams, null, null);
        assertEquals(1, nonDefaultResponse.getHeadersKnob().getHeaderNames().length);
        final Map<String, HttpCookie> cookiesMap = createCookiesMap(nonDefaultResponse);
        assertEquals(4, cookiesMap.size());
        final HttpCookie fooCookie = cookiesMap.get("foo");
        assertEquals("bar", fooCookie.getCookieValue());
        assertEquals("localhost", fooCookie.getDomain());
        assertEquals("/test", fooCookie.getPath());
        final HttpCookie aCookie = cookiesMap.get("a");
        assertEquals("hasPath", aCookie.getCookieValue());
        assertEquals("localhost", aCookie.getDomain());
        assertEquals("/cookiePath", aCookie.getPath());
        final HttpCookie bCookie = cookiesMap.get("b");
        assertEquals("hasDomain", bCookie.getCookieValue());
        assertEquals("cookieDomain", bCookie.getDomain());
        assertEquals("/test", bCookie.getPath());
        final HttpCookie cCookie = cookiesMap.get("c");
        assertEquals("hasPathAndDomain", cCookie.getCookieValue());
        assertEquals("cookieDomain", cCookie.getDomain());
        assertEquals("/cookiePath", cCookie.getPath());
    }

    private Map<String, HttpCookie> createCookiesMap(final Message message) {
        final Map<String, HttpCookie> cookiesMap = new HashMap<>();
        for (final HttpCookie cookie : message.getHttpCookiesKnob().getCookies()) {
            cookiesMap.put(cookie.getCookieName(), cookie);
        }
        return cookiesMap;
    }

    private Map<String, List<String>> generateHeadersMap(List<HttpHeader> extraHeaders) {
        final Map<String, List<String>> headersMap = new HashMap<>();
        for (final HttpHeader header : extraHeaders) {
            if (!headersMap.containsKey(header.getName())) {
                headersMap.put(header.getName(), new ArrayList<String>());
            }
            headersMap.get(header.getName()).add(header.getFullValue());
        }
        return headersMap;
    }

    private Message generateSoapRequest() throws Exception {
        final Message soapRequest = new Message(stashManager, ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(SOAP_MESSAGE.getBytes()));
        final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setMethod("POST");
        mockRequest.addHeader(SoapUtil.SOAPACTION, "test");
        soapRequest.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));
        return soapRequest;
    }

    private HttpInboundResponseKnob createInboundKnob() {
        final HttpInboundResponseKnob inboundKnob = new HttpInboundResponseFacet();
        inboundKnob.setHeaderSource(new SimpleHttpHeadersHaver(new GenericHttpHeaders(responseHeaders.toArray(new GenericHttpHeader[responseHeaders.size()]))));
        return inboundKnob;
    }
}
