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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.*;

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
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        ruleSet = new HttpPassthroughRuleSet();
        ruleSet.setForwardAll(true);
        rules = new ArrayList<>();
        audit = new TestAudit();
        vars = new HashMap<>();
        responseHeadersKnob = new HeadersKnobSupport();
        responseHeaders = new ArrayList<>();
    }

    @Test
    public void requestNotInitialized() throws Exception {
        final Message uninitialized = new Message();
        final PolicyEnforcementContext uninitializedContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(uninitialized, response);
        HttpForwardingRuleEnforcer.handleRequestHeaders(uninitialized, requestParams, uninitializedContext, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertTrue(requestParams.getExtraHeaders().isEmpty());
    }

    @Test
    public void forwardAllRequestHeaders() throws Exception {
        request.getHeadersKnob().addHeader("foo", "bar");
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        final List<HttpHeader> extraHeaders = requestParams.getExtraHeaders();
        assertEquals(1, extraHeaders.size());
        assertEquals("foo", extraHeaders.get(0).getName());
        assertEquals("bar", extraHeaders.get(0).getFullValue());
    }

    @Test
    public void forwardAllRequestHeadersMultipleWithSameName() throws Exception {
        request.getHeadersKnob().addHeader("foo", "bar");
        request.getHeadersKnob().addHeader("foo", "bar2");
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
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
        request.getHeadersKnob().addHeader("foo", "bar");
        request.getHeadersKnob().addHeader("foo", "bar2");
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
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
        HttpForwardingRuleEnforcer.handleRequestHeaders(soapRequest, requestParams, PolicyEnforcementContextFactory.createPolicyEnforcementContext(soapRequest, response), TARGET_DOMAIN, ruleSet, audit, null, null);
        assertTrue(requestParams.getExtraHeaders().isEmpty());
    }

    @Test
    public void soapActionFromRequestHeadersKnob() throws Exception {
        request.getHeadersKnob().addHeader(SoapUtil.SOAPACTION, "test");
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        final Map<String, List<String>> headersMap = generateHeadersMap(requestParams.getExtraHeaders());
        assertEquals(1, headersMap.get(SoapUtil.SOAPACTION).size());
        assertEquals("test", headersMap.get(SoapUtil.SOAPACTION).get(0));
    }

    @Test
    public void soapActionFromHeadersKnobHasPriorityOverRequest() throws Exception {
        final Message soapRequest = generateSoapRequest();
        // headers knob has priority
        soapRequest.getHeadersKnob().addHeader(SoapUtil.SOAPACTION, "priority");
        HttpForwardingRuleEnforcer.handleRequestHeaders(soapRequest, requestParams, PolicyEnforcementContextFactory.createPolicyEnforcementContext(soapRequest, response), TARGET_DOMAIN, ruleSet, audit, null, null);
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
        soapRequest.getHeadersKnob().addHeader(SoapUtil.SOAPACTION, "shouldBeReplaced");
        HttpForwardingRuleEnforcer.handleRequestHeaders(soapRequest, requestParams, PolicyEnforcementContextFactory.createPolicyEnforcementContext(soapRequest, response), TARGET_DOMAIN, ruleSet, audit, null, null);
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
        soapRequest.getHeadersKnob().addHeader(SoapUtil.SOAPACTION, "testSoapAction");
        HttpForwardingRuleEnforcer.handleRequestHeaders(soapRequest, requestParams, PolicyEnforcementContextFactory.createPolicyEnforcementContext(soapRequest, response), TARGET_DOMAIN, ruleSet, audit, null, null);
        final Map<String, List<String>> headersMap = generateHeadersMap(requestParams.getExtraHeaders());
        assertEquals(1, headersMap.get(SoapUtil.SOAPACTION).size());
        assertEquals("testSoapAction", headersMap.get(SoapUtil.SOAPACTION).get(0));
    }

    @Test(expected = IOException.class)
    public void emptyRequestHeaderName() throws Exception {
        request.getHeadersKnob().addHeader("", "empty");
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
    }

    @Test
    public void requestAuthHeader() throws Exception {
        request.getHeadersKnob().addHeader(HttpConstants.HEADER_AUTHORIZATION, "foo");
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals(HttpConstants.HEADER_AUTHORIZATION, requestParams.getExtraHeaders().get(0).getName());
        assertEquals("foo", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestAuthHeaderSkippedIfPasswordAuthPresent() throws Exception {
        requestParams.setPasswordAuthentication(new PasswordAuthentication("foo", "bar".toCharArray()));
        request.getHeadersKnob().addHeader(HttpConstants.HEADER_AUTHORIZATION, "foo");
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(0, requestParams.getExtraHeaders().size());
    }

    @Test
    public void requestCookieHeaders() throws Exception {
        // as long as the request has at last one Cookie header, cookies will be retrieved from the context and passed along
        request.getHeadersKnob().addHeader("Cookie", "a=apple");
        request.getHeadersKnob().addHeader("Cookie", "b=bear");
        context.addCookie(new HttpCookie("a", "apple", 0, "/", TARGET_DOMAIN));
        context.addCookie(new HttpCookie("b", "bear", 0, "/", TARGET_DOMAIN));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("Cookie", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("a=apple; b=bear", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestCookies() throws Exception {
        // if there are cookies in context, but no request Cookie header, we should still pass through the cookies in the context
        context.addCookie(new HttpCookie("a", "apple", 0, "/", TARGET_DOMAIN));
        context.addCookie(new HttpCookie("b", "bear", 0, "/", TARGET_DOMAIN));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("Cookie", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("a=apple; b=bear", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestGatewayManagedCookieHeadersNotPassed() throws Exception {
        request.getHeadersKnob().addHeader("Cookie", "a=apple");
        request.getHeadersKnob().addHeader("Cookie", CookieUtils.PREFIX_GATEWAY_MANAGED + "foo=bar");
        context.addCookie(new HttpCookie("a", "apple", 0, "/", TARGET_DOMAIN));
        context.addCookie(new HttpCookie(CookieUtils.PREFIX_GATEWAY_MANAGED + "foo", "bar", 0, "/", TARGET_DOMAIN));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("Cookie", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("a=apple", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestCookieHeadersDoesNotIncludePathOrDomain() throws Exception {
        request.getHeadersKnob().addHeader("Cookie", "a=apple");
        context.addCookie(new HttpCookie("a", "apple", 0, "/somePath", "someDomain"));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("Cookie", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("a=apple", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestRuleWithCustomValue() throws Exception {
        rules.add(new HttpPassthroughRule("foo", true, "custom"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
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
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, vars, vars.keySet().toArray(new String[vars.size()]));
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
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, new String[]{"custom"});
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("foo", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("customValue", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestRuleWithCustomValueReplacesOriginal() throws Exception {
        rules.add(new HttpPassthroughRule("foo", true, "custom"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        request.getHeadersKnob().addHeader("foo", "shouldBeReplaced");
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("foo", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("custom", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestRuleWithOriginalValue() throws Exception {
        rules.add(new HttpPassthroughRule("foo", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        request.getHeadersKnob().addHeader("foo", "bar");
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("foo", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("bar", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void requestRuleWithOriginalValueHeaderDoesNotExist() throws Exception {
        rules.add(new HttpPassthroughRule("foo", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(0, requestParams.getExtraHeaders().size());
    }

    @Test
    public void requestRulesFilterHeaders() throws Exception {
        rules.add(new HttpPassthroughRule("add", true, "shouldBeAdded"));
        rules.add(new HttpPassthroughRule("test", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        request.getHeadersKnob().addHeader("shouldBeFiltered", "shouldBeFiltered");
        request.getHeadersKnob().addHeader("test", "shouldNotBeFiltered");
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
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

    @BugId("SSG-6543")
    @Test
    public void requestRuleWithHostIgnored() throws Exception {
        rules.add(new HttpPassthroughRule("host", true, "customHost"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(0, requestParams.getExtraHeaders().size());
    }

    @Test(expected = IOException.class)
    public void requestRuleWithEmptyName() throws Exception {
        rules.add(new HttpPassthroughRule("", true, "uhOh"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
    }

    @Test
    public void requestRuleWithUserAgentOverridesExistingUserAgent() throws Exception {
        requestParams.addExtraHeader(new GenericHttpHeader("User-Agent", "shouldBeOverridden"));
        rules.add(new HttpPassthroughRule("User-Agent", true, "customUserAgent"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("User-Agent", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("customUserAgent", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void forwardAllResponseHeaders() throws Exception {
        responseHeaders.add(new GenericHttpHeader("foo", "bar"));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        assertEquals("foo", responseHeadersKnob.getHeaderNames()[0]);
        assertEquals(1, responseHeadersKnob.getHeaderValues("foo").length);
        assertEquals("bar", responseHeadersKnob.getHeaderValues("foo")[0]);
    }

    @Test
    public void passThroughSpecialResponseHeaders() throws Exception {
        for (final String specialHeader : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            responseHeaders.add(new GenericHttpHeader(specialHeader, "testValue"));
        }
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, null);
        assertEquals(HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD.size(), responseHeadersKnob.getHeaderNames().length);
        final List<String> headerNames = Arrays.asList(responseHeadersKnob.getHeaderNames());
        for (final String specialHeader : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            assertTrue(headerNames.contains(specialHeader));
        }
    }

    @Test
    public void passThroughSpecialResponseHeadersCaseInsensitive() throws Exception {
        for (final String specialHeader : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            responseHeaders.add(new GenericHttpHeader(specialHeader.toUpperCase(), "testValue"));
        }
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, null);
        assertEquals(HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD.size(), responseHeadersKnob.getHeaderNames().length);
        final List<String> headerNames = Arrays.asList(responseHeadersKnob.getHeaderNames());
        for (final String specialHeader : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            assertTrue(headerNames.contains(specialHeader.toUpperCase()));
        }
    }

    @Test
    public void doNotPassThroughSpecialResponseHeaders() throws Exception {
        for (final String specialHeader : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            responseHeaders.add(new GenericHttpHeader(specialHeader, "testValue"));
        }
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, false, context, requestParams, null, null);
        assertEquals(0, responseHeadersKnob.getHeaderNames().length);
    }

    @Test
    public void responseSetCookieHeaders() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "foo=bar"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "key=value"));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, null);
        assertEquals(0, responseHeadersKnob.getHeaderNames().length);
        final Map<String, String> cookiesMap = createCookiesMap(context);
        assertEquals(2, cookiesMap.size());
        assertEquals("bar", cookiesMap.get("foo"));
        assertEquals("value", cookiesMap.get("key"));
    }

    @Test
    public void responseSetCookieHeadersInvalidCookie() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "foo=bar"));
        // invalid cookie should be filtered
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", ""));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, null);
        assertEquals(0, responseHeadersKnob.getHeaderNames().length);
        final Map<String, String> cookiesMap = createCookiesMap(context);
        assertEquals(1, cookiesMap.size());
        assertEquals("bar", cookiesMap.get("foo"));
    }

    @Test
    public void doNotForwardAllResponseHeadersButForwardSpecialHeaders() {
        responseHeaders.add(new GenericHttpHeader("foo", "shouldNotBePassed"));
        ruleSet.setForwardAll(false);
        for (final String specialHeader : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            responseHeaders.add(new GenericHttpHeader(specialHeader, "testValue"));
        }
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, null);
        assertEquals(HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD.size(), responseHeadersKnob.getHeaderNames().length);
        final List<String> headerNames = Arrays.asList(responseHeadersKnob.getHeaderNames());
        for (final String specialHeader : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            assertTrue(headerNames.contains(specialHeader));
        }
        assertFalse(headerNames.contains("foo"));
    }

    @Test
    public void responseRuleCustomValue() {
        rules.add(new HttpPassthroughRule("foo", true, "bar"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, null);
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
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, vars, vars.keySet().toArray(new String[vars.size()]));
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
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, new String[]{"custom"});
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
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, null);
        assertEquals(1, responseHeadersKnob.getHeaderNames().length);
        assertEquals(1, responseHeadersKnob.getHeaderValues("foo").length);
        assertEquals("bar", responseHeadersKnob.getHeaderValues("foo")[0]);
    }

    @Test
    public void responseRuleOriginalValueNotFound() {
        rules.add(new HttpPassthroughRule("foo", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, null);
        assertEquals(0, responseHeadersKnob.getHeaderNames().length);
    }

    @Test
    public void responseRulesFilterHeaders() {
        responseHeaders.add(new GenericHttpHeader("notInRule", "shouldBeFiltered"));
        ruleSet.setForwardAll(false);
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, null);
        assertEquals(0, responseHeadersKnob.getHeaderNames().length);
    }

    @Test
    public void responseRuleWithSetCookie() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", "foo=bar"));
        rules.add(new HttpPassthroughRule("Set-Cookie", false, "thisValueIsNotUsed"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, null);
        assertEquals(0, responseHeadersKnob.getHeaderNames().length);
        assertEquals(1, context.getCookies().size());
        final HttpCookie cookie = context.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
    }

    @Test
    public void responseRuleWithSetCookieInvalidCookie() throws Exception {
        requestParams.setTargetUrl(new URL("http://localhost:8080/test"));
        // set-cookie value is empty
        responseHeaders.add(new GenericHttpHeader("Set-Cookie", ""));
        rules.add(new HttpPassthroughRule("Set-Cookie", false, "thisValueIsNotUsed"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleResponseHeaders(createInboundKnob(), responseHeadersKnob, audit, ruleSet, true, context, requestParams, null, null);
        assertEquals(0, responseHeadersKnob.getHeaderNames().length);
        assertEquals(0, context.getCookies().size());
    }

    private Map<String, String> createCookiesMap(final PolicyEnforcementContext context) {
        final Map<String, String> cookiesMap = new HashMap<>();
        for (final HttpCookie cookie : context.getCookies()) {
            cookiesMap.put(cookie.getCookieName(), cookie.getCookieValue());
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
