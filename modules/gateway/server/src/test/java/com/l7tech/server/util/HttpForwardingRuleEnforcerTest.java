package com.l7tech.server.util;

import com.l7tech.common.http.*;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class HttpForwardingRuleEnforcerTest {
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
    public void soapActionFromRequest() throws Exception {
        final Message soapRequest = generateSoapRequest();
        HttpForwardingRuleEnforcer.handleRequestHeaders(soapRequest, requestParams, PolicyEnforcementContextFactory.createPolicyEnforcementContext(soapRequest, response), TARGET_DOMAIN, ruleSet, audit, null, null);
        final Map<String, List<String>> headersMap = generateHeadersMap(requestParams.getExtraHeaders());
        assertEquals(1, headersMap.get(SoapUtil.SOAPACTION).size());
        assertEquals("test", headersMap.get(SoapUtil.SOAPACTION).get(0));
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
    public void requestCookieHeadersReplacesExistingCookie() throws Exception {
        requestParams.addExtraHeader(new GenericHttpHeader("Cookie", "c=shouldBeReplaced"));
        request.getHeadersKnob().addHeader("Cookie", "a=apple");
        context.addCookie(new HttpCookie("a", "apple", 0, "/", TARGET_DOMAIN));
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
    public void ruleWithCustomValue() throws Exception {
        rules.add(new HttpPassthroughRule("foo", true, "custom"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("foo", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("custom", requestParams.getExtraHeaders().get(0).getFullValue());
    }

    @Test
    public void ruleWithContextVariableCustomValue() throws Exception {
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
    public void ruleWithContextVariableCustomValueRetrievedFromContext() throws Exception {
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
    public void ruleWithCustomValueReplacesOriginal() throws Exception {
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
    public void ruleWithOriginalValue() throws Exception {
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
    public void ruleWithOriginalValueHeaderDoesNotExist() throws Exception {
        rules.add(new HttpPassthroughRule("foo", false, null));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(0, requestParams.getExtraHeaders().size());
    }

    @Test
    public void rulesFilterHeaders() throws Exception {
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
    }

    @BugId("SSG-6543")
    @Test
    public void ruleWithHostIgnored() throws Exception {
        rules.add(new HttpPassthroughRule("host", true, "customHost"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(0, requestParams.getExtraHeaders().size());
    }

    @Test(expected = IOException.class)
    public void ruleWithEmptyName() throws Exception {
        rules.add(new HttpPassthroughRule("", true, "uhOh"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
    }

    @Test
    public void ruleWithUserAgentOverridesExistingUserAgent() throws Exception {
        requestParams.addExtraHeader(new GenericHttpHeader("User-Agent", "shouldBeOverridden"));
        rules.add(new HttpPassthroughRule("User-Agent", true, "customUserAgent"));
        ruleSet.setForwardAll(false);
        ruleSet.setRules(rules.toArray(new HttpPassthroughRule[rules.size()]));
        HttpForwardingRuleEnforcer.handleRequestHeaders(request, requestParams, context, TARGET_DOMAIN, ruleSet, audit, null, null);
        assertEquals(1, requestParams.getExtraHeaders().size());
        assertEquals("User-Agent", requestParams.getExtraHeaders().get(0).getName());
        assertEquals("customUserAgent", requestParams.getExtraHeaders().get(0).getFullValue());
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
}
