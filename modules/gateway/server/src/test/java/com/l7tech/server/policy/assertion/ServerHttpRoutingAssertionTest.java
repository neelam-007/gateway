/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.policy.assertion;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.l7tech.common.TestDocuments;
import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.IdentityBindingHttpConnectionManager;
import com.l7tech.common.io.PermissiveHostnameVerifier;
import com.l7tech.common.io.TestSSLSocketFactory;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.log.TestHandler;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.message.*;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.server.*;
import com.l7tech.server.identity.cert.TestTrustedCertManager;
import com.l7tech.server.identity.cert.TrustedCertServicesImpl;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.server.security.cert.TestCertValidationProcessor;
import com.l7tech.server.security.keystore.PermissiveKeyAccessFilter;
import com.l7tech.server.transport.http.SslClientHostnameVerifier;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.server.util.*;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

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

        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        routingAssertion.checkRequest(pec);
        final Map<String, String[]> actualParameters = pec.getRequest().getHttpRequestKnob().getParameterMap();
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

    @BugNumber(10257)
    @Test
    public void testConnectionBinding() throws Exception {
        //Setup Http server
        MockHttpServer httpServer = new MockHttpServer(17800);
        httpServer.start();

        try {
            //Setup applicationcontext
            final AssertionRegistry assertionRegistry = new AssertionRegistry();
            assertionRegistry.afterPropertiesSet();
            final ServerPolicyFactory serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
            final HttpClientFactory identityBindingHttpClientFactory = new HttpClientFactory();

            ApplicationContext ac = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
                put("assertionRegistry", assertionRegistry);
                put("policyFactory", serverPolicyFactory);
                put("auditFactory", new TestAudit().factory());
                put("messageProcessingEventChannel", new EventChannel());
                put("serverConfig", new ServerConfigStub());
                put("hostnameVerifier", new SslClientHostnameVerifier(new ServerConfigStub(), new TrustedCertServicesImpl(new TestTrustedCertManager())));
                put("stashManagerFactory", TestStashManagerFactory.getInstance());
                put("httpRoutingHttpClientFactory", identityBindingHttpClientFactory);
            }}));

            serverPolicyFactory.setApplicationContext(ac);
            //Simulate NTLM request
            List<HttpHeader> headers = new ArrayList<HttpHeader>();
            headers.add(new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, "NTLM"));

            //Simulate Connection ID
            Random r = new Random();
            final int connectionID = r.nextInt();
            HttpRequestKnobStub requestKnob = new HttpRequestKnobStub(headers) {
                @Override
                public Object getConnectionIdentifier() {
                    return connectionID;
                }
            };
            Message requestMessage = new Message();
            requestMessage.attachHttpRequestKnob(requestKnob);
            PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMessage, new Message());

            HttpRoutingAssertion assertion = new HttpRoutingAssertion();
            assertion.setPassthroughHttpAuthentication(true);
            assertion.setProtectedServiceUrl("http://localhost:" +httpServer.getPort() );

            ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);

            httpServer.setResponseCode(HttpsURLConnection.HTTP_UNAUTHORIZED);
            AssertionStatus result = sass.checkRequest(peCtx);
            assertEquals(peCtx.getVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_REASON_CODE), HttpsURLConnection.HTTP_UNAUTHORIZED);
            assertEquals(AssertionStatus.AUTH_REQUIRED, result);
            peCtx.close();

            //Make sure the connection is bound
            Map map = ((IdentityBindingHttpConnectionManager)identityBindingHttpClientFactory.getConnectionManager()).getConnectionsById();
            assertNotNull(map.get(connectionID));

            //Resubmit the request
            httpServer.setResponseCode(HttpsURLConnection.HTTP_OK);
            requestMessage = new Message();
            requestMessage.attachHttpRequestKnob(requestKnob);
            peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMessage, new Message());
            result = sass.checkRequest(peCtx);
            assertEquals(result, AssertionStatus.NONE);
            map = ((IdentityBindingHttpConnectionManager)identityBindingHttpClientFactory.getConnectionManager()).getConnectionsById();
            //Make sure the connection is bound
            assertNotNull(map.get(connectionID));
        } finally {
            httpServer.stop();
        }
    }


    @BugNumber(10257)
    @Test
    public void testConnectionWithoutBinding() throws Exception {
        //Setup Http server
        MockHttpServer httpServer = new MockHttpServer(17800);
        httpServer.start();

        try {
            //Setup applicationcontext
            final AssertionRegistry assertionRegistry = new AssertionRegistry();
            assertionRegistry.afterPropertiesSet();
            final ServerPolicyFactory serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
            final HttpClientFactory identityBindingHttpClientFactory = new HttpClientFactory();

            ApplicationContext ac = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
                put("assertionRegistry", assertionRegistry);
                put("policyFactory", serverPolicyFactory);
                put("auditFactory", new TestAudit().factory());
                put("messageProcessingEventChannel", new EventChannel());
                put("serverConfig", new ServerConfigStub());
                put("hostnameVerifier", new SslClientHostnameVerifier(new ServerConfigStub(), new TrustedCertServicesImpl(new TestTrustedCertManager())));
                put("stashManagerFactory", TestStashManagerFactory.getInstance());
                put("httpRoutingHttpClientFactory", identityBindingHttpClientFactory);
            }}));

            serverPolicyFactory.setApplicationContext(ac);
            //Simulate Basic request
            List<HttpHeader> headers = new ArrayList<HttpHeader>();
            headers.add(new GenericHttpHeader(HttpConstants.HEADER_AUTHORIZATION, "Basic"));

            //Simulate Connection ID
            Random r = new Random();
            final int connectionID = r.nextInt();
            HttpRequestKnobStub requestKnob = new HttpRequestKnobStub(headers) {
                @Override
                public Object getConnectionIdentifier() {
                    return connectionID;
                }
            };
            Message requestMessage = new Message();
            requestMessage.attachHttpRequestKnob(requestKnob);
            PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMessage, new Message());

            HttpRoutingAssertion assertion = new HttpRoutingAssertion();
            assertion.setPassthroughHttpAuthentication(true);
            assertion.setProtectedServiceUrl("http://localhost:" +httpServer.getPort());
            ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);

            httpServer.setResponseCode(HttpsURLConnection.HTTP_UNAUTHORIZED);
            AssertionStatus result = sass.checkRequest(peCtx);
            assertEquals(peCtx.getVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_REASON_CODE), HttpsURLConnection.HTTP_UNAUTHORIZED);
            assertEquals(AssertionStatus.AUTH_REQUIRED, result);
            peCtx.close();

            //Make sure the connection is not bound
            Map map = ((IdentityBindingHttpConnectionManager)identityBindingHttpClientFactory.getConnectionManager()).getConnectionsById();
            assertNull(map.get(connectionID));

        } finally {
            httpServer.stop();
        }
    }

    @BugNumber(13629)
    @Test
    public void testPassThroughAllRequestParametersWithInvalidBody() throws Exception {
        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testPassThroughAllRequestParametersWithInvalidBody");

        //set to pass through all parameters
        hra.setRequestParamRules(new HttpPassthroughRuleSet(true, new HttpPassthroughRule[]{}));
        byte[] data = "{foo:bar}".getBytes("UTF-8");

        Message request = new Message(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED, new ByteArrayInputStream(data));
        Message response = new Message(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED, new ByteArrayInputStream(data));
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setMethod("POST");
        hrequest.addHeader("Content-Type", "application/x-www-form-urlencoded");

        hrequest.setContent(data);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[0]);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED, (long)data.length, data);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                return mockClient.new MockGenericHttpRequest(){
                    @Override
                    public void setInputStreamFactory(final InputStreamFactory isf) {
                        try {
                            super.setInputStreamFactory(isf);
                            IOUtils.copyStream(isf.getInputStream(), expectedResult);
                        } catch (IOException e) {
                            //ignore
                        }
                    }
                };
            }
        });
        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        routingAssertion.checkRequest(pec);

        assertArrayEquals(expectedResult.toByteArray(), data);
    }

    private static class HttpClientFactory extends IdentityBindingHttpClientFactory {
        public HttpConnectionManager getConnectionManager() {
            return super.connectionManager;
        }
    }

    @Test
    public void testSSLConnection() throws Exception {

        //Setup the MockWebServer
        MockWebServer server = new MockWebServer();
        server.useHttps(new TestSSLSocketFactory(), false);
        server.enqueue(new MockResponse().setBody("test"));
        server.play();

        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        final ServerPolicyFactory serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        final HttpClientFactory identityBindingHttpClientFactory = new HttpClientFactory();

        KeyStore keyStore = TestDocuments.getMockSSLServerKeyStore();

        final String alias = keyStore.aliases().nextElement();
        final PrivateKey key = (PrivateKey)keyStore.getKey(alias, "7layer]".toCharArray());
        final X509Certificate[] chain = TestDocuments.toX509Certificate(keyStore.getCertificateChain(alias));

        final DefaultKey defaultKey = new TestDefaultKey(new SsgKeyEntry( -1, "alias", chain, key));

        TestTrustedCertManager trustedCertManager = new TestTrustedCertManager(defaultKey);
        TrustedCertServicesImpl trustedCertServices = new TrustedCertServicesImpl(trustedCertManager);
        TestCertValidationProcessor certValidationProcessor = new TestCertValidationProcessor();
        final SslClientTrustManager sslClientTrustManager = new SslClientTrustManager(trustedCertServices, certValidationProcessor, CertValidationProcessor.Facility.valueOf("ROUTING"));

        ApplicationContext ac = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("auditFactory", new TestAudit().factory());
            put("messageProcessingEventChannel", new EventChannel());
            put("serverConfig", new ServerConfigStub());
            put("hostnameVerifier", new PermissiveHostnameVerifier());
            put("stashManagerFactory", TestStashManagerFactory.getInstance());
            put("httpRoutingHttpClientFactory", identityBindingHttpClientFactory);
            put("defaultKey", defaultKey);
            put("routingTrustManager", sslClientTrustManager);
        }}));

        serverPolicyFactory.setApplicationContext(ac);

        //Simulate Connection ID
        Message requestMessage = new Message();
        PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMessage, new Message());

        HttpRoutingAssertion assertion = new HttpRoutingAssertion();
        assertion.setPassthroughHttpAuthentication(true);
        URL url = server.getUrl("/");
        assertion.setProtectedServiceUrl(url.toString());
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);

        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
        peCtx.close();

    }

    @Test
    public void testHttpTrace() throws Exception {

        Logger logger = Logger.getLogger("com.l7tech.server.routing.http.trace");
        logger.setLevel(Level.FINEST);
        logger.setUseParentHandlers(false);
        TestHandler testHandler = new TestHandler();
        logger.addHandler(testHandler);
        //Setup the MockWebServer
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("test"));
        server.play();

        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        final ServerPolicyFactory serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        final HttpClientFactory identityBindingHttpClientFactory = new HttpClientFactory();

        ApplicationContext ac = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("auditFactory", new TestAudit().factory());
            put("messageProcessingEventChannel", new EventChannel());
            put("serverConfig", new ServerConfigStub());
            put("hostnameVerifier", new PermissiveHostnameVerifier());
            put("stashManagerFactory", TestStashManagerFactory.getInstance());
            put("httpRoutingHttpClientFactory", identityBindingHttpClientFactory);
        }}));

        serverPolicyFactory.setApplicationContext(ac);

        //Simulate Connection ID
        Message requestMessage = new Message();
        PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMessage, new Message());

        HttpRoutingAssertion assertion = new HttpRoutingAssertion();
        URL url = server.getUrl("/");
        assertion.setProtectedServiceUrl(url.toString());
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);

        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
        assertTrue(TestHandler.isAuditPresentContaining("FINEST: http-in"));
        assertTrue(TestHandler.isAuditPresentContaining("FINEST: http-out"));
        peCtx.close();
    }


    @Test
    public void testHttpsTrace() throws Exception {

        Logger logger = Logger.getLogger("com.l7tech.server.routing.http.trace");
        logger.setLevel(Level.FINEST);
        logger.setUseParentHandlers(false);
        TestHandler testHandler = new TestHandler();
        logger.addHandler(testHandler);

        //Setup the MockWebServer
        MockWebServer server = new MockWebServer();
        server.useHttps(new TestSSLSocketFactory(), false);
        server.enqueue(new MockResponse().setBody("test"));
        server.play();

        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        final ServerPolicyFactory serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        final HttpClientFactory identityBindingHttpClientFactory = new HttpClientFactory();

        KeyStore keyStore = TestDocuments.getMockSSLServerKeyStore();

        final String alias = keyStore.aliases().nextElement();
        final PrivateKey key = (PrivateKey)keyStore.getKey(alias, "7layer]".toCharArray());
        final X509Certificate[] chain = TestDocuments.toX509Certificate(keyStore.getCertificateChain(alias));

        final DefaultKey defaultKey = new TestDefaultKey(new SsgKeyEntry( -1, "alias", chain, key));

        TestTrustedCertManager trustedCertManager = new TestTrustedCertManager(defaultKey);
        TrustedCertServicesImpl trustedCertServices = new TrustedCertServicesImpl(trustedCertManager);
        TestCertValidationProcessor certValidationProcessor = new TestCertValidationProcessor();
        final SslClientTrustManager sslClientTrustManager = new SslClientTrustManager(trustedCertServices, certValidationProcessor, CertValidationProcessor.Facility.valueOf("ROUTING"));

        ApplicationContext ac = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("auditFactory", new TestAudit().factory());
            put("messageProcessingEventChannel", new EventChannel());
            put("serverConfig", new ServerConfigStub());
            put("hostnameVerifier", new PermissiveHostnameVerifier());
            put("stashManagerFactory", TestStashManagerFactory.getInstance());
            put("httpRoutingHttpClientFactory", identityBindingHttpClientFactory);
            put("defaultKey", defaultKey);
            put("routingTrustManager", sslClientTrustManager);
        }}));

        serverPolicyFactory.setApplicationContext(ac);

        //Simulate Connection ID
        Message requestMessage = new Message();
        PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMessage, new Message());

        HttpRoutingAssertion assertion = new HttpRoutingAssertion();
        assertion.setPassthroughHttpAuthentication(true);
        URL url = server.getUrl("/");
        assertion.setProtectedServiceUrl(url.toString());
        ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);

        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.NONE, result);
        assertTrue(TestHandler.isAuditPresentContaining("FINEST: http-in"));
        assertTrue(TestHandler.isAuditPresentContaining("FINEST: http-out"));
        peCtx.close();

    }
}
