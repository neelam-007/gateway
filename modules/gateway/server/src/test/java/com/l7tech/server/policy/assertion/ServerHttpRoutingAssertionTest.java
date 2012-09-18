/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.http.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.transport.http.HttpAdmin;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.transport.http.HttpAdminImpl;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;

public class ServerHttpRoutingAssertionTest {

    @Test(expected = AssertionStatusException.class)
    @BugNumber(11385)
    public void testInvalidTimeout() throws Exception {
        final HttpRoutingAssertion assertion = new HttpRoutingAssertion();
        assertion.setTimeout("invalid");
        assertion.setProtectedServiceUrl("http://localhost:17380/testurl");
        final TestAudit testAudit = new TestAudit();
        final ServerHttpRoutingAssertion serverAssertion = new ServerHttpRoutingAssertion(assertion, ApplicationContexts.getTestApplicationContext());
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));
        final PolicyEnforcementContext policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        try {
            serverAssertion.checkRequest(policyContext);
        } catch (final AssertionStatusException e) {
            assertEquals(AssertionStatus.FAILED, e.getAssertionStatus());
            assertTrue(testAudit.isAuditPresent(AssertionMessages.HTTPROUTE_CONFIGURATION_ERROR));
            assertTrue(testAudit.isAuditPresentContaining("Read Timeout"));
            throw e;
        }

    }

    /**
     * Test that its possible to overwrite a Message variable with the contents of a response, when the message variable
     * has previously held the contents of a routing assertion response
     */
    @Test
    @BugNumber(8396)
    public void testOverwriteResponseVariable() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        final String responseVariable = "result";
        hra.setResponseMsgDest(responseVariable);
        hra.setProtectedServiceUrl("http://localhost:17380/testurl");

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        Message request = new Message(XmlUtil.stringAsDocument("<foo/>"));
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        HttpHeaders headers = new GenericHttpHeaders(new HttpHeader[]{
                new GenericHttpHeader("Content-Length", "6, 6"),
                new GenericHttpHeader("Content-Length", "6"),
                new GenericHttpHeader("Content-Type", "text/xml"),
        });

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

        final String expectedResponse = "<bar/>";
        testingHttpClientFactory.setMockHttpClient(
                new MockGenericHttpClient(200, headers, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes())));

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        routingAssertion.checkRequest(pec);
        final Object variable = pec.getVariable(responseVariable);
        Assert.assertNotNull("Response should be in the ${result} context variable", variable);
        Message response = (Message) variable;
        Assert.assertNotNull("Message should contain a response knob", response.getKnob(HttpResponseKnob.class));

        routingAssertion.checkRequest(pec);
        Assert.assertNotNull("Response should be in the ${result} context variable", variable);
        response = (Message) variable;
        Assert.assertNotNull("Message should contain a response knob", response.getKnob(HttpResponseKnob.class));

        final InputStream bodyAsInputStream = response.getMimeKnob().getEntireMessageBodyAsInputStream();
        final String responseAsString = new String(IOUtils.slurpStream(bodyAsInputStream));
        Assert.assertEquals("Incorrect response", expectedResponse, responseAsString);
    }

    @Test
    @BugNumber(10018)
    public void testPassthroughAuthHeaderNotDuplicated() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testPassthroughAuthHeaderNotDuplicated");

        // Configure to combine passthrough auth with pass through all application headers
        hra.setPassthroughHttpAuthentication(true);
        hra.setRequestHeaderRules(new HttpPassthroughRuleSet(true, new HttpPassthroughRule[]{}));

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        Message request = new Message(XmlUtil.stringAsDocument("<foo/>"));

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.addHeader("Authorization", "abcde=");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

        final Object[] requestRecordedExtraHeaders = {null};
        final PasswordAuthentication[] requestRecordedPasswordAuthentication = {null};

        final String expectedResponse = "<bar/>";
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[0]);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                requestRecordedExtraHeaders[0] = params.getExtraHeaders();
                requestRecordedPasswordAuthentication[0] = params.getPasswordAuthentication();
                return request;
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        routingAssertion.checkRequest(pec);

        PasswordAuthentication reqPasswordAuth = requestRecordedPasswordAuthentication[0];
        assertNull("Passthrough auth should be configured via passthrough of raw Authorization header as extra header", reqPasswordAuth);

        @SuppressWarnings({"unchecked"}) List<HttpHeader> reqExtraHeaders = (List<HttpHeader>) requestRecordedExtraHeaders[0];

        int numAuthHeaders = 0;
        for (HttpHeader header : reqExtraHeaders) {
            if ("authorization".equalsIgnoreCase(header.getName()))
                numAuthHeaders++;
        }

        assertEquals("Only one copy of the request's Authorization header shall be passed through when both passthroughHttpAuthentication and request header forwardAll are enabled",
                1, numAuthHeaders);
    }

    @Test
    @BugNumber(10795)
    public void testPassthroughAuthHeaderIgnoredIfCredentialsSpecified() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testPassthroughAuthHeader");

        // Pass through all headers
        hra.setRequestHeaderRules(new HttpPassthroughRuleSet(true, new HttpPassthroughRule[]{}));

        // Configure a back-end HTTP Basic password
        hra.setLogin("jimmy");
        hra.setPassword("qwert");

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        Message request = new Message(XmlUtil.stringAsDocument("<foo/>"));

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.addHeader("Authorization", "abcde=");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

        final Object[] requestRecordedExtraHeaders = {null};
        final PasswordAuthentication[] requestRecordedPasswordAuthentication = {null};

        final String expectedResponse = "<bar/>";
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[0]);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                requestRecordedExtraHeaders[0] = params.getExtraHeaders();
                requestRecordedPasswordAuthentication[0] = params.getPasswordAuthentication();
                return request;
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        routingAssertion.checkRequest(pec);

        PasswordAuthentication reqPasswordAuth = requestRecordedPasswordAuthentication[0];
        assertNotNull("Request should be explicitly configured to use HTTP Basic credentials", reqPasswordAuth);
        assertEquals("jimmy", reqPasswordAuth.getUserName());
        assertEquals("qwert", new String(reqPasswordAuth.getPassword()));

        @SuppressWarnings({"unchecked"}) List<HttpHeader> reqExtraHeaders = (List<HttpHeader>) requestRecordedExtraHeaders[0];

        int numAuthHeaders = 0;
        for (HttpHeader header : reqExtraHeaders) {
            if ("authorization".equalsIgnoreCase(header.getName()))
                numAuthHeaders++;
        }

        assertEquals("No authorization headers shall be passed through if request is configured with explicit HTTP Basic credentials",
                0, numAuthHeaders);
    }

    @Test
    @BugNumber(10712)
    public void testContentEncodingNotPassedThrough() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testContentEncodingNotPassedThrough");

        // Pass through all response headers
        hra.setResponseHeaderRules(new HttpPassthroughRuleSet(true, new HttpPassthroughRule[]{}));

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        Message request = new Message(XmlUtil.stringAsDocument("<foo/>"));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(new MockHttpServletRequest(new MockServletContext())));
        Message response = new Message();
        response.attachHttpResponseKnob(new HttpServletResponseKnob(new MockHttpServletResponse()));
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

        final byte[] expectedResponse = IOUtils.compressGzip("<bar/>".getBytes());
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[]{new GenericHttpHeader("Content-Encoding", "gzip")});
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, expectedResponse);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        AssertionStatus result = routingAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, result);

        assertEquals("<bar/>", new String(pec.getResponse().getMimeKnob().getFirstPart().getBytesIfAvailableOrSmallerThan(Integer.MAX_VALUE)));
        assertEquals(0, pec.getResponse().getHttpResponseKnob().getHeaderValues("content-encoding").length);
    }

    @Test
    @BugNumber(10629)
    public void testSoapHeaderPassthroughForNonSoapMessage() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testContentEncodingNotPassedThrough");

        // Pass through all request headers
        hra.setRequestHeaderRules(new HttpPassthroughRuleSet(true, new HttpPassthroughRule[]{}));

        // An incoming octet-stream request that happens to contain a SOAP envelope as its body, along with a SOAPAction header
        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        Message request = new Message(new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, TestDocuments.getInputStream(TestDocuments.PLACEORDER_CLEARTEXT));
        final MockHttpServletRequest hrequest = new MockHttpServletRequest(new MockServletContext());
        final String placeOrderSoapActionHeaderValue = "\"http://warehouse.acme.com/ws/placeOrder\"";
        hrequest.addHeader("SOAPAction", placeOrderSoapActionHeaderValue);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

        final Object[] requestRecordedExtraHeaders = {null};

        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[0]);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, "<bar/>".getBytes());
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                requestRecordedExtraHeaders[0] = params.getExtraHeaders();
                return request;
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        routingAssertion.checkRequest(pec);

        @SuppressWarnings({"unchecked"}) List<HttpHeader> reqExtraHeaders = (List<HttpHeader>) requestRecordedExtraHeaders[0];

        int numSoapAction = 0;
        for (HttpHeader header : reqExtraHeaders) {
            if ("soapaction".equalsIgnoreCase(header.getName())) {
                numSoapAction++;
                assertEquals(placeOrderSoapActionHeaderValue, header.getFullValue());
            }
        }

        assertEquals("A SOAPAction header shall have been passed through", 1, numSoapAction);
    }

    @Test
    @BugNumber(12060)
    /**
     * Test that when 'Pass through all parameters' is enabled, it should pass through all form parameters.
     */
    public void testPassThroughAllRequestParameters() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testPassThroughAllRequestParameters");

        //set to pass through all parameters
        hra.setRequestParamRules(new HttpPassthroughRuleSet(true, new HttpPassthroughRule[]{}));

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        Message request = new Message(XmlUtil.stringAsDocument("<foo/>"));

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
        hrequest.setMethod("POST");

        hrequest.setContent("foo=bar&hello=world".getBytes("UTF-8"));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);
        
        final String expectedResponse = "<bar/>";
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[0]);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));
        
        final Map<String, String> actualParameters = new HashMap<String, String>();
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                return mockClient.new MockGenericHttpRequest(){
                    @Override
                    public void addParameter(final String paramName, final String paramValue) throws IllegalArgumentException, IllegalStateException {
                        actualParameters.put(paramName, paramValue);
                    }
                };

            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        routingAssertion.checkRequest(pec);

        //actualParameters should contain foo & hello
        assertTrue("test for existence of 'foo' parameter", actualParameters.containsKey("foo"));
        assertTrue("test for existence of 'hello' parameter", actualParameters.containsKey("hello"));
    }

    @Test
    @BugNumber(12060)
    /**
     * Test that when 'Customize parameters to pass through' is enabled, it should only pass through the specified.
     */
    public void testPassThroughCustomizeRequestParameters() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testPassThroughCustomizeRequestParameters");

        //set to pass through certain parameters
        hra.setRequestParamRules(new HttpPassthroughRuleSet(false, new HttpPassthroughRule[]{
                new HttpPassthroughRule("foo", true, "foofoefum"),
                new HttpPassthroughRule("layer", false, "7")
        }));

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        Message request = new Message(XmlUtil.stringAsDocument("<foo/>"));

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
        hrequest.setMethod("POST");

        hrequest.setContent("foo=bar&hello=world&layer=seven".getBytes("UTF-8"));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

        final String expectedResponse = "<bar/>";
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[0]);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));

        final Map<String, String> actualParameters = new HashMap<String, String>();
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                return mockClient.new MockGenericHttpRequest(){
                    @Override
                    public void addParameter(final String paramName, final String paramValue) throws IllegalArgumentException, IllegalStateException {
                        actualParameters.put(paramName, paramValue);
                    }
                };
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        routingAssertion.checkRequest(pec);

        //actualParameters should contain foo and NOT hello
        assertTrue("test for existence of 'foo' parameter", actualParameters.containsKey("foo"));
        assertEquals("test for 'foo' parameter value change", "foofoefum", actualParameters.get("foo"));

        assertTrue("test for existence of 'layer' parameter", actualParameters.containsKey("layer"));
        assertEquals("test of 'seven' parameter value change (it should not change)", "seven", actualParameters.get("layer"));

        assertTrue("test for existence of 'hello' parameter (it should not exist)", !actualParameters.containsKey("hello"));
    }

    @Test
    @BugNumber(12060)
    /**
     * Test that when 'Customize parameters to pass through' is enabled AND there are no entries, it SHOULD NOT pass
     * any parameters.  Please see bug http://sarek.l7tech.com/bugzilla/show_bug.cgi?id=12060
     */
    public void testPassThroughAllRequestParametersDefault() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testPassThroughAllRequestParametersDefault");

        hra.setRequestParamRules(new HttpPassthroughRuleSet(false, new HttpPassthroughRule[]{}));

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        Message request = new Message(XmlUtil.stringAsDocument("<foo/>"));

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
        hrequest.setMethod("POST");

        hrequest.setContent("foo=bar&hello=world&layer=seven".getBytes("UTF-8"));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

        final String expectedResponse = "<bar/>";
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[0]);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));

        final Map<String, String> actualParameters = new HashMap<String, String>();
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                return mockClient.new MockGenericHttpRequest(){
                    @Override
                    public void addParameter(final String paramName, final String paramValue) throws IllegalArgumentException, IllegalStateException {
                        actualParameters.put(paramName, paramValue);
                    }
                };
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        routingAssertion.checkRequest(pec);

        assertTrue("there should be no parameters forwarded", actualParameters.isEmpty());
    }

    @BugNumber(11510)
    @Test
    public void testHttpVersion() throws Exception {
        final HttpRoutingAssertion assertion = new HttpRoutingAssertion();
        assertion.setHttpVersion(GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_0);
        assertion.setProtectedServiceUrl("http://localhost:17380/testurl");
        final TestAudit testAudit = new TestAudit();
        final ServerHttpRoutingAssertion serverAssertion = new ServerHttpRoutingAssertion(assertion, ApplicationContexts.getTestApplicationContext());
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));
        final PolicyEnforcementContext policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

        final String expectedResponse = "<bar/>";
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[0]);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));

        final Map<String, String> actualParameters = new HashMap<String, String>();
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                return mockClient.new MockGenericHttpRequest(){
                    @Override
                    public void addParameter(final String paramName, final String paramValue) throws IllegalArgumentException, IllegalStateException {
                        actualParameters.put(paramName, paramValue);
                    }
                };
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);

        serverAssertion.checkRequest(policyContext);
        assertEquals(mockClient.getParams().getHttpVersion(), GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_0);

        assertion.setHttpVersion(GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_1);
        serverAssertion.checkRequest(policyContext);
        assertEquals(mockClient.getParams().getHttpVersion(), GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_1);

        assertion.setHttpVersion(null);
        serverAssertion.checkRequest(policyContext);
        assertEquals(mockClient.getParams().getHttpVersion(), GenericHttpRequestParams.HttpVersion.HTTP_VERSION_1_1);

    }

    @Test
    @BugNumber(10829)
    /**
     * Test the http admin interface, we need to make sure it can be executed successfully and be able to catch exceptions since internally it uses @{link ServerHttpRoutingAssertion}
     */
    public void testHttpAdmin() throws Exception {
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        HttpAdminImpl admin = new HttpAdminImpl();
        admin.setApplicationContext(appContext);
        final String testUrl = "http://localhost:17380";
        final String should_fail_url = "http://localhost-dummy:8443";
        final String unknown_host_str = "Unknown Host";

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

        final String expectedResponse = "<ok/>";
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[0]);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));
        testingHttpClientFactory.setMockHttpClient(mockClient);

        final HttpRoutingAssertion hra = new HttpRoutingAssertion();
        try {
            final String message = "<test>message</test>";
            admin.testConnection(new String[]{testUrl}, message, hra);
        } catch (Exception e) {
            fail("testConnection method should not fail");
        }

        final MockGenericHttpClient mockClient2 = new MockGenericHttpClient(500, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));

        //let's simulate an unknown host error
        mockClient2.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                return mockClient2.new MockGenericHttpRequest() {
                    @Override
                    public GenericHttpResponse getResponse() throws GenericHttpException {
                        throw new GenericHttpException(unknown_host_str);
                    }
                };
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient2);

        try {
            final String message = "<test>message</test>";
            admin.testConnection(new String[]{should_fail_url}, message, hra);
            fail("testConnection method should have fail");
        } catch (HttpAdmin.HttpAdminException e) {
            //Problem routing to http://localhost-dummy:8443. Error msg: Unknown Host
            assertTrue(e.getSessionLog().indexOf(unknown_host_str)>0);
            assertTrue(e.getSessionLog().indexOf(should_fail_url)>0);
        } catch (Exception e){
            fail("should not have this exception");
        }
    }


    @Test
    @BugNumber(10050)
    public void testPassThroughUnauthorized() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testPassthroughUnauthorized");
        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        PolicyEnforcementContext pec = createTestPolicyEnforcementContext(401, hra, appContext);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.AUTH_REQUIRED, routingAssertion.checkRequest(pec));
    }


    @Test
    @BugNumber(10050)
    public void testPassThroughServerError() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testPassthroughServerError");
        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        PolicyEnforcementContext pec = createTestPolicyEnforcementContext(500, hra, appContext);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.FALSIFIED, routingAssertion.checkRequest(pec));
    }

    @Test
    @BugNumber(10050)
    public void testPassThroughSuccessful() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testPassthroughSucceeded");
        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        PolicyEnforcementContext pec = createTestPolicyEnforcementContext(200, hra, appContext);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));
    }

    private PolicyEnforcementContext createTestPolicyEnforcementContext( int status, HttpRoutingAssertion hra, ApplicationContext appContext) {
        // Configure to combine passthrough auth with pass through all application headers
        hra.setPassthroughHttpAuthentication(true);
        hra.setRequestHeaderRules(new HttpPassthroughRuleSet(true, new HttpPassthroughRule[]{}));

        Message request = new Message(XmlUtil.stringAsDocument("<foo/>"));

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.addHeader("Authorization", "NTLM JHY6798hjkaahjklkjsb");
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));

        final String expectedResponse = "<bar/>";
        MockHttpServletResponse hresponse = new MockHttpServletResponse();
        Message response = new Message(XmlUtil.stringAsDocument(expectedResponse));
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

        final Object[] requestRecordedExtraHeaders = {null};
        final PasswordAuthentication[] requestRecordedPasswordAuthentication = {null};

        final GenericHttpHeader[] genericHttpHeaders= {new GenericHttpHeader(HttpConstants.HEADER_WWW_AUTHENTICATE, "NTLM")};
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(genericHttpHeaders);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(status, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                requestRecordedExtraHeaders[0] = params.getExtraHeaders();
                requestRecordedPasswordAuthentication[0] = params.getPasswordAuthentication();
                return request;
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);
        return pec;
    }
}
