/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.policy.assertion;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.l7tech.common.TestDocuments;
import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.apache.components.HttpComponentsClient;
import com.l7tech.common.io.PermissiveHostnameVerifier;
import com.l7tech.common.io.TestSSLSocketFactory;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.log.TestHandler;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.message.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistentEntity;
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
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.server.security.cert.CertValidationProcessor;
import com.l7tech.server.security.cert.TestCertValidationProcessor;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.http.SslClientHostnameVerifier;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.server.util.*;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import org.apache.http.pool.PoolStats;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
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
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerHttpRoutingAssertionTest {

    private static final String CLIENT_FACTORY = "httpRoutingHttpClientFactory2";
    @Mock
    private ApplicationContext mockApplicationContext;
    @Mock
    private GenericHttpClientFactory mockClientFactory;
    @Mock
    private GenericHttpClient mockClient;
    @Mock
    private GenericHttpRequest mockHttpRequest;
    @Mock
    private GenericHttpResponse mockHttpResponse;
    @Mock
    private StashManagerFactory stashManagerFactory;
    @Mock
    private StashManager stashManager;

    @Before
    public void setup() throws Exception {
        when(mockApplicationContext.getBean("httpRoutingHttpClientFactory2", GenericHttpClientFactory.class)).thenReturn(mockClientFactory);
        when(mockApplicationContext.getBean("stashManagerFactory", StashManagerFactory.class)).thenReturn(stashManagerFactory);
        when(stashManagerFactory.createStashManager()).thenReturn(stashManager);
        when(mockClientFactory.createHttpClient(anyInt(), anyInt(), anyInt(), anyInt(), anyObject(), any(HttpProxyConfig.class))).thenReturn(mockClient);
        when(mockClient.createRequest(any(HttpMethod.class), any(GenericHttpRequestParams.class))).thenReturn(mockHttpRequest);
        when(mockHttpRequest.getResponse()).thenReturn(mockHttpResponse);
    }

    @After
    public void tearDown() throws Exception {
        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);
        testingHttpClientFactory.setMockHttpClient(null);
    }

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

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

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
        request.getHeadersKnob().addHeader("Authorization", "abcde=", HEADER_TYPE_HTTP);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

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

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

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

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

        final byte[] expectedResponse = IOUtils.compressGzip("<bar/>".getBytes());
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[]{new GenericHttpHeader("Content-Encoding", "gzip")});
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, expectedResponse);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        AssertionStatus result = routingAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, result);

        assertEquals("<bar/>", new String(pec.getResponse().getMimeKnob().getFirstPart().getBytesIfAvailableOrSmallerThan(Integer.MAX_VALUE)));
        final Collection<Header> contentEncodingHeaders = pec.getResponse().getHeadersKnob().getHeaders("content-encoding", HeadersKnob.HEADER_TYPE_HTTP);
        assertEquals(1, contentEncodingHeaders.size());
        assertFalse(contentEncodingHeaders.iterator().next().isPassThrough());
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
        request.getHeadersKnob().addHeader("SOAPACtion", placeOrderSoapActionHeaderValue, HEADER_TYPE_HTTP);
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

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
        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

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
        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

        final String expectedResponse = "<bar/>";
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[0]);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));

        final Map<String, String> actualParameters = new HashMap<String, String>();
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                return mockClient.new MockGenericHttpRequest() {
                    @Override
                    public void addParameters(List<String[]> parameters) throws IllegalArgumentException, IllegalStateException {
                        for (String[] parameter : parameters) {
                            actualParameters.put(parameter[0], parameter[1]);
                        }
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
        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

        final String expectedResponse = "<bar/>";
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[0]);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));

        final Map<String, String> actualParameters = new HashMap<String, String>();
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                return mockClient.new MockGenericHttpRequest() {
                    @Override
                    public void addParameters(List<String[]> parameters) throws IllegalArgumentException, IllegalStateException {
                        for (String[] parameter : parameters) {
                            actualParameters.put(parameter[0], parameter[1]);
                        }
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
        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

        final String expectedResponse = "<bar/>";
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(new GenericHttpHeader[0]);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));

        final Map<String, String> actualParameters = new HashMap<String, String>();
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                return mockClient.new MockGenericHttpRequest() {
                    @Override
                    public void addParameters(List<String[]> parameters) throws IllegalArgumentException, IllegalStateException {
                        for (String[] parameter : parameters) {
                            actualParameters.put(parameter[0], parameter[1]);
                        }
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

    @Test
    @BugId("SSG-5631")
    public void testCustomRequestMethod() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testCustomHttpMethod");
        hra.setHttpMethod(HttpMethod.OTHER);
        hra.setHttpMethodAsString("MyCustomMethodName");
        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        final HttpMethod[] sawMethod = {null};
        final String[] sawMethodStr = {null};
        PolicyEnforcementContext pec = createTestPolicyEnforcementContext(200, hra, appContext, new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                sawMethod[0] = method;
                sawMethodStr[0] = params.getMethodAsString();
                return null;
            }
        });

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));

        assertEquals(HttpMethod.OTHER, sawMethod[0]);
        assertEquals("MyCustomMethodName", sawMethodStr[0]);
    }

    @Test
    public void testOauth10AuthorizationHeader() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl( "http://localhost:17380/testOauth10AuthHeader" );
        hra.setAuthOauthVersion( "1.0" );
        hra.setAuthOauthTokenVar( "mytoken" );

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        final String[] authHeaderValue = { null };
        PolicyEnforcementContext pec = createAuthHeaderSnaggingTestPec( hra, appContext, authHeaderValue );

        pec.setVariable( "mytoken", "myspecialtoken" );

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));

        assertEquals( "OAuth myspecialtoken", authHeaderValue[0]);
    }

    @Test
    public void testOauth20AuthorizationHeader() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl( "http://localhost:17380/testOauth10AuthHeader" );
        hra.setAuthOauthVersion( "2.0" );
        hra.setAuthOauthTokenVar( "mytoken" );

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        final String[] authHeaderValue = { null };
        PolicyEnforcementContext pec = createAuthHeaderSnaggingTestPec( hra, appContext, authHeaderValue );

        pec.setVariable( "mytoken", "myspecialtoken" );

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));

        assertEquals( "Bearer myspecialtoken", authHeaderValue[0]);
    }

    @Test
    public void testOauthInvalidVersionAuthorizationHeader() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl( "http://localhost:17380/testOauthInvalidVersionAuthorizationHeader" );
        hra.setAuthOauthVersion( "ewrgwerg" );
        hra.setAuthOauthTokenVar( "mytoken" );

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        final String[] authHeaderValue = { null };
        PolicyEnforcementContext pec = createAuthHeaderSnaggingTestPec( hra, appContext, authHeaderValue );

        pec.setVariable( "mytoken", "myspecialtoken" );

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));

        assertEquals( "Bearer myspecialtoken", authHeaderValue[0]);
    }

    @Test
    public void testOauthDefaultVersionAuthorizationHeader() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl( "http://localhost:17380/testOauthDefaultVersionAuthorizationHeader" );
        hra.setAuthOauthTokenVar( "mytoken" );

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        final String[] authHeaderValue = { null };
        PolicyEnforcementContext pec = createAuthHeaderSnaggingTestPec( hra, appContext, authHeaderValue );

        pec.setVariable( "mytoken", "myspecialtoken" );

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));

        assertEquals( "Bearer myspecialtoken", authHeaderValue[0]);
    }


    private PolicyEnforcementContext createAuthHeaderSnaggingTestPec( HttpRoutingAssertion hra, ApplicationContext appContext, final String[] authHeaderValueCollector ) {
        return createTestPolicyEnforcementContext(200, hra, appContext, new MockGenericHttpClient.CreateRequestListener() {
                @Override
                public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                    for ( HttpHeader head : params.getExtraHeaders() ) {
                        if ( "Authorization".equalsIgnoreCase( head.getName() ) ) {
                            if ( authHeaderValueCollector[0] != null )
                                fail( "More than one authorization header present" );
                            authHeaderValueCollector[0] = head.getFullValue();
                        }
                    }
                    return null;
                }
            });
    }


    @Test
    @BugId("SSG-8361")
    public void testCustomRequestMethodContextVariable() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testCustomHttpMethod");
        hra.setHttpMethod(HttpMethod.OTHER);
        hra.setHttpMethodAsString("${contextVar}");
        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        final HttpMethod[] sawMethod = {null};
        final String[] sawMethodStr = {null};
        PolicyEnforcementContext pec = createTestPolicyEnforcementContext(200, hra, appContext, new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                sawMethod[0] = method;
                sawMethodStr[0] = params.getMethodAsString();
                return null;
            }
        });

        pec.setVariable("contextVar", "MethodNameFromVar");

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));

        assertEquals(HttpMethod.OTHER, sawMethod[0]);
        assertEquals("MethodNameFromVar", sawMethodStr[0]);
    }

    private PolicyEnforcementContext createTestPolicyEnforcementContext(int status, HttpRoutingAssertion hra, ApplicationContext appContext) {
        return createTestPolicyEnforcementContext(status, hra, appContext, null);
    }

    private PolicyEnforcementContext createTestPolicyEnforcementContext(int status, HttpRoutingAssertion hra, ApplicationContext appContext, MockGenericHttpClient.CreateRequestListener createRequestListener) {
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

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

        final GenericHttpHeader[] genericHttpHeaders = {new GenericHttpHeader(HttpConstants.HEADER_WWW_AUTHENTICATE, "NTLM")};
        final GenericHttpHeaders responseHeaders = new GenericHttpHeaders(genericHttpHeaders);
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(status, responseHeaders, ContentTypeHeader.XML_DEFAULT, 6L, (expectedResponse.getBytes()));
        mockClient.setCreateRequestListener(createRequestListener);
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
                put(CLIENT_FACTORY, identityBindingHttpClientFactory);
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
            Message responseMessage = new Message();
            requestMessage.attachHttpRequestKnob(requestKnob);
            PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMessage, responseMessage);

            HttpRoutingAssertion assertion = new HttpRoutingAssertion();
            assertion.setPassthroughHttpAuthentication(true);
            assertion.setProtectedServiceUrl("http://localhost:" + httpServer.getPort());

            ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);

            httpServer.setResponseCode(HttpsURLConnection.HTTP_UNAUTHORIZED);
            AssertionStatus result = sass.checkRequest(peCtx);
            assertEquals(peCtx.getVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_REASON_CODE), HttpsURLConnection.HTTP_UNAUTHORIZED);
            assertEquals(AssertionStatus.AUTH_REQUIRED, result);
            IOUtils.copyStream(responseMessage.getMimeKnob().getEntireMessageBodyAsInputStream(), System.out);
            peCtx.close();

            //Resubmit the request
            httpServer.setResponseCode(HttpsURLConnection.HTTP_OK);
            requestMessage = new Message();
            responseMessage = new Message();
            requestMessage.attachHttpRequestKnob(requestKnob);
            peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMessage, responseMessage);
            result = sass.checkRequest(peCtx);
            IOUtils.copyStream(responseMessage.getMimeKnob().getEntireMessageBodyAsInputStream(), System.out);
            assertEquals(result, AssertionStatus.NONE);
            //Make sure the connection is bound, re-use the connection
            PoolStats stats = identityBindingHttpClientFactory.getStats();
            assertEquals(1, stats.getAvailable());
            assertEquals(1, httpServer.getServedClient().size());

            //another request with different connection id
            final int connectionID2 = r.nextInt();
            requestKnob = new HttpRequestKnobStub(headers) {
                @Override
                public Object getConnectionIdentifier() {
                    return connectionID2;
                }
            };
            requestMessage = new Message();
            responseMessage = new Message();
            requestMessage.attachHttpRequestKnob(requestKnob);
            peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMessage, responseMessage);

            httpServer.setResponseCode(HttpsURLConnection.HTTP_UNAUTHORIZED);
            result = sass.checkRequest(peCtx);
            IOUtils.copyStream(responseMessage.getMimeKnob().getEntireMessageBodyAsInputStream(), System.out);
            assertEquals(peCtx.getVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_REASON_CODE), HttpsURLConnection.HTTP_UNAUTHORIZED);
            assertEquals(AssertionStatus.AUTH_REQUIRED, result);
            peCtx.close();
            stats = identityBindingHttpClientFactory.getStats();
            assertEquals(2, stats.getAvailable());


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
                put(CLIENT_FACTORY, identityBindingHttpClientFactory);
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
            Message responseMessage = new Message();
            requestMessage.attachHttpRequestKnob(requestKnob);
            PolicyEnforcementContext peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMessage, responseMessage);

            HttpRoutingAssertion assertion = new HttpRoutingAssertion();
            assertion.setPassthroughHttpAuthentication(true);
            assertion.setProtectedServiceUrl("http://localhost:" + httpServer.getPort());
            ServerAssertion sass = serverPolicyFactory.compilePolicy(assertion, false);

            //First Request
            httpServer.setResponseCode(HttpsURLConnection.HTTP_UNAUTHORIZED);
            AssertionStatus result = sass.checkRequest(peCtx);
            IOUtils.copyStream(responseMessage.getMimeKnob().getEntireMessageBodyAsInputStream(), System.out);
            assertEquals(peCtx.getVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_REASON_CODE), HttpsURLConnection.HTTP_UNAUTHORIZED);
            assertEquals(AssertionStatus.AUTH_REQUIRED, result);
            peCtx.close();

            //Another request
            final int connectionID2 = r.nextInt();
            requestKnob = new HttpRequestKnobStub(headers) {
                @Override
                public Object getConnectionIdentifier() {
                    return connectionID2;
                }
            };
            requestMessage = new Message();
            responseMessage = new Message();
            requestMessage.attachHttpRequestKnob(requestKnob);
            peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(requestMessage, responseMessage);

            httpServer.setResponseCode(HttpsURLConnection.HTTP_UNAUTHORIZED);
            result = sass.checkRequest(peCtx);
            IOUtils.copyStream(responseMessage.getMimeKnob().getEntireMessageBodyAsInputStream(), System.out);
            assertEquals(peCtx.getVariable(HttpRoutingAssertion.VAR_HTTP_ROUTING_REASON_CODE), HttpsURLConnection.HTTP_UNAUTHORIZED);
            assertEquals(AssertionStatus.AUTH_REQUIRED, result);
            peCtx.close();

            PoolStats stats = identityBindingHttpClientFactory.getStats();
            //Pooled, so it should reuse existing connection
            assertEquals(1, stats.getAvailable());

        } finally {
            httpServer.stop();
        }
    }

    @BugNumber(13629)
    @Test
    public void testPassThroughAllRequestParametersWithInvalidBody() throws Exception {
        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();
        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);
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
        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, responseHeaders, ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED, (long) data.length, data);
        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ByteArrayOutputStream expectedResult = new ByteArrayOutputStream();
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                return mockClient.new MockGenericHttpRequest() {
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

        public PoolStats getStats() {
            return getTotalStats();
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
        final PrivateKey key = (PrivateKey) keyStore.getKey(alias, "7layer]".toCharArray());
        final X509Certificate[] chain = TestDocuments.toX509Certificate(keyStore.getCertificateChain(alias));

        final DefaultKey defaultKey = new TestDefaultKey(new SsgKeyEntry(PersistentEntity.DEFAULT_GOID, "alias", chain, key));

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

        Logger logger = Logger.getLogger("com.l7tech.server.routing.https.trace");
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
        final PrivateKey key = (PrivateKey) keyStore.getKey(alias, "7layer]".toCharArray());
        final X509Certificate[] chain = TestDocuments.toX509Certificate(keyStore.getCertificateChain(alias));

        final DefaultKey defaultKey = new TestDefaultKey(new SsgKeyEntry(PersistentEntity.DEFAULT_GOID, "alias", chain, key));

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
        assertTrue(TestHandler.isAuditPresentContaining("FINEST: https-in"));
        assertTrue(TestHandler.isAuditPresentContaining("FINEST: https-out"));
        peCtx.close();

    }

    @Test
    public void responseHeadersSetOnHeadersKnob() throws Exception {
        final HttpRoutingAssertion assertion = new HttpRoutingAssertion();
        assertion.getResponseHeaderRules().setForwardAll(true);
        assertion.setProtectedServiceUrl("http://localhost:8080/test");
        final ServerHttpRoutingAssertion serverAssertion = new ServerHttpRoutingAssertion(assertion, mockApplicationContext);

        final List<HttpHeader> responseHeaders = new ArrayList<>();
        responseHeaders.add(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_ENCODING, HttpConstants.ENCODING_UTF8));
        responseHeaders.add(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/plain"));
        responseHeaders.add(new GenericHttpHeader("foo", "bar"));
        when(mockHttpResponse.getHeaders()).thenReturn(new GenericHttpHeaders(responseHeaders.toArray(new HttpHeader[responseHeaders.size()])));
        final Message response = new Message();
        response.attachHttpResponseKnob(new HttpServletResponseKnob(new MockHttpServletResponse()));
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), response);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, assertionStatus);
        final HeadersKnob responseHeadersKnob = context.getResponse().getHeadersKnob();
        assertEquals(3, responseHeadersKnob.getHeaderNames(HeadersKnob.HEADER_TYPE_HTTP).length);
        final String[] fooValues = responseHeadersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, fooValues.length);
        assertEquals("bar", fooValues[0]);
        final String[] contentTypeValues = responseHeadersKnob.getHeaderValues(HttpConstants.HEADER_CONTENT_TYPE, HEADER_TYPE_HTTP);
        assertEquals(1, contentTypeValues.length);
        assertEquals("text/plain", contentTypeValues[0]);
        final String[] contentEncodingValues = responseHeadersKnob.getHeaderValues(HttpConstants.HEADER_CONTENT_ENCODING, HEADER_TYPE_HTTP);
        assertEquals(1, contentEncodingValues.length);
        assertEquals(HttpConstants.ENCODING_UTF8, contentEncodingValues[0]);
    }

    @BugId("SSG-8528")
    @Test
    public void gzipResponseZeroContentLength() throws Exception {
        final HttpRoutingAssertion assertion = new HttpRoutingAssertion();
        assertion.setProtectedServiceUrl("http://localhost:8080/test");
        final ServerHttpRoutingAssertion serverAssertion = new ServerHttpRoutingAssertion(assertion, mockApplicationContext);
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), response);
        final List<HttpHeader> responseHeaders = new ArrayList<>();
        responseHeaders.add(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_ENCODING, "gzip"));
        responseHeaders.add(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/plain"));
        responseHeaders.add(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_LENGTH, "0"));
        when(mockHttpResponse.getHeaders()).thenReturn(new GenericHttpHeaders(responseHeaders.toArray(new HttpHeader[responseHeaders.size()])));
        when(mockHttpResponse.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertTrue(response.isInitialized());
    }

    @Test
    public void gzipResponseNoContentLength() throws Exception {
        final HttpRoutingAssertion assertion = new HttpRoutingAssertion();
        assertion.setProtectedServiceUrl("http://localhost:8080/test");
        final ServerHttpRoutingAssertion serverAssertion = new ServerHttpRoutingAssertion(assertion, mockApplicationContext);
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), response);
        final List<HttpHeader> responseHeaders = new ArrayList<>();
        responseHeaders.add(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_ENCODING, "gzip"));
        responseHeaders.add(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/plain"));

        when(mockHttpResponse.getHeaders()).thenReturn(new GenericHttpHeaders(responseHeaders.toArray(new HttpHeader[responseHeaders.size()])));
        when(mockHttpResponse.getInputStream()).thenReturn(new ByteArrayInputStream(IOUtils.compressGzip("test".getBytes())));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertTrue(response.isInitialized());
    }

    @Test
    public void followRedirectsForGetRequest() throws Exception {
        final HttpRoutingAssertion route = new HttpRoutingAssertion();
        route.setProtectedServiceUrl("http://localhost:17380/testurl");
        // route method is auto-detected
        route.setHttpMethod(null);
        route.setFollowRedirects(true);

        final Message getRequest = new Message();
        final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setMethod("GET");
        getRequest.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        final List<HttpHeader> responseHeaders = new ArrayList<>();
        responseHeaders.add(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/plain"));
        when(mockHttpResponse.getHeaders()).thenReturn(new GenericHttpHeaders(responseHeaders.toArray(new HttpHeader[responseHeaders.size()])));

        when(mockClient.createRequest(any(HttpMethod.class), any(GenericHttpRequestParams.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final GenericHttpRequestParams params = (GenericHttpRequestParams) invocation.getArguments()[1];
                // ensure follow redirects is set on params
                assertTrue(params.isFollowRedirects());
                return mockHttpRequest;
            }
        });

        final ServerHttpRoutingAssertion serverRoute = new ServerHttpRoutingAssertion(route, mockApplicationContext);
        assertEquals(AssertionStatus.NONE, serverRoute.checkRequest(PolicyEnforcementContextFactory.createPolicyEnforcementContext(getRequest, new Message())));
    }

    @BugId("SSG-8905")
    @Test
    public void followRedirectsForForcedGetRequest() throws Exception {
        final HttpRoutingAssertion forcedGetRoute = new HttpRoutingAssertion();
        forcedGetRoute.setProtectedServiceUrl("http://localhost:17380/testurl");
        forcedGetRoute.setHttpMethod(HttpMethod.GET);
        forcedGetRoute.setFollowRedirects(true);

        final Message getRequest = new Message();
        final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setMethod("GET");
        getRequest.attachHttpRequestKnob(new HttpServletRequestKnob(mockRequest));

        final List<HttpHeader> responseHeaders = new ArrayList<>();
        responseHeaders.add(new GenericHttpHeader(HttpConstants.HEADER_CONTENT_TYPE, "text/plain"));
        when(mockHttpResponse.getHeaders()).thenReturn(new GenericHttpHeaders(responseHeaders.toArray(new HttpHeader[responseHeaders.size()])));

        when(mockClient.createRequest(any(HttpMethod.class), any(GenericHttpRequestParams.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final GenericHttpRequestParams params = (GenericHttpRequestParams) invocation.getArguments()[1];
                // ensure follow redirects is set on params
                assertTrue(params.isFollowRedirects());
                return mockHttpRequest;
            }
        });

        final ServerHttpRoutingAssertion serverRoute = new ServerHttpRoutingAssertion(forcedGetRoute, mockApplicationContext);
        assertEquals(AssertionStatus.NONE, serverRoute.checkRequest(PolicyEnforcementContextFactory.createPolicyEnforcementContext(getRequest, new Message())));
    }

    @BugId("SSG-9355")
    @Test
    public void testRoutingWithNoBodyContentTypeHeader_overrideContentType() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl( "http://localhost:17380/tesOverrideContentTypeHeader1" );
        hra.setOverrideContentType(true);

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        PolicyEnforcementContext pec = createContentTypeTestPec(hra, appContext, "PUT",
                new Header[] {new Header("Content-Type", "text/html", HeadersKnob.HEADER_TYPE_HTTP)},
                new String[] {"text/html"}, true);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));
    }

    @BugId("SSG-9355")
    @Test
    public void testRoutingNoContentTypeHeader() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl( "http://localhost:17380/tesOverrideContentTypeHeader2" );
        hra.setOverrideContentType(false);

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        PolicyEnforcementContext pec = createContentTypeTestPec(hra, appContext, "PUT", null, new String[] {ContentTypeHeader.XML_DEFAULT.getFullValue()}, false);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));
    }

    @BugId("SSG-9355")
    @Test
    public void testRoutingWithContentTypeHeaderNoPassThrough_overrideContentType() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl( "http://localhost:17380/tesOverridetContentTypeHeader3" );
        hra.setOverrideContentType(true);

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        PolicyEnforcementContext pec = createContentTypeTestPec(hra, appContext, "PUT",
                new Header[] {new Header("Content-Type", "text/html", HeadersKnob.HEADER_TYPE_HTTP, false)},
                new String[] {"text/html"}, false);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));
    }

    @BugId("SSG-9355")
    @Test
    public void testRoutingNoContentTypeHeader_overrideContentType() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl( "http://localhost:17380/tesOverridetContentTypeHeader4" );
        hra.setOverrideContentType(true);

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        PolicyEnforcementContext pec = createContentTypeTestPec(hra, appContext, "PUT",
                new Header[] {new Header("My-Type", "blah", HeadersKnob.HEADER_TYPE_HTTP)}, new String[0], false);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));
    }

    @BugId("SSG-9355")
    @Test
    public void testRoutingWithContentTypeHeader_duplicate() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl( "http://localhost:17380/tesOverridetContentTypeHeader5" );
        hra.setOverrideContentType(false);

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        PolicyEnforcementContext pec = createContentTypeTestPec(hra, appContext, "POST",
                new Header[] {new Header("Content-Type", "text/html", HeadersKnob.HEADER_TYPE_HTTP)},
                new String[] {"text/html", ContentTypeHeader.XML_DEFAULT.getFullValue()}, false);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));
    }

    @BugId("SSG-9355")
    @Test
    public void testRoutingNoContentTypeHeader_GET() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl( "http://localhost:17380/tesOverridetContentTypeHeader2" );
        hra.setOverrideContentType(false);

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        PolicyEnforcementContext pec = createContentTypeTestPec(hra, appContext, "GET", null, new String[0], false);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(hra, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));
    }

    @BugId("SSG-13709")
    @Test
    public void testMethodFromContextVariable_HEAD_BodyForcedIncluded() throws Exception {
        String requestBody = "<foo/>";

        HttpRoutingAssertion assertion = new HttpRoutingAssertion();
        assertion.setProtectedServiceUrl("http://localhost:8080/test");
        assertion.setHttpMethod(HttpMethod.OTHER);
        assertion.setHttpMethodAsString("${contextVar}");
        assertion.setForceIncludeRequestBody(true);

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(new MockServletContext());
        mockHttpServletRequest.setMethod("GET");

        Message request = new Message(XmlUtil.stringAsDocument(requestBody));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockHttpServletRequest));
        request.getHeadersKnob().addHeader("Content-Length", "6", HeadersKnob.HEADER_TYPE_HTTP);
        request.getHeadersKnob().addHeader("Content-Type", "text/xml", HeadersKnob.HEADER_TYPE_HTTP);

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        pec.setVariable("contextVar", "HEAD");

        TestingHttpClientFactory testingHttpClientFactory =
                appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

        final MockGenericHttpClient mockClient =
                new MockGenericHttpClient(200, new GenericHttpHeaders(new HttpHeader[]{}), null, 0L, new byte[0]);

        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(assertion, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));

        assertEquals(requestBody, new String(mockClient.getRequestBody()));
    }

    @BugId("SSG-13709")
    @Test
    public void testMethodFromContextVariable_HEAD_BodyNotIncluded() throws Exception {
        String requestBody = "<foo/>";

        HttpRoutingAssertion assertion = new HttpRoutingAssertion();
        assertion.setProtectedServiceUrl("http://localhost:8080/test");
        assertion.setHttpMethod(HttpMethod.OTHER);
        assertion.setHttpMethodAsString("${contextVar}");
        assertion.setForceIncludeRequestBody(false);

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(new MockServletContext());
        mockHttpServletRequest.setMethod("GET");

        Message request = new Message(XmlUtil.stringAsDocument(requestBody));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockHttpServletRequest));
        request.getHeadersKnob().addHeader("Content-Length", "6", HeadersKnob.HEADER_TYPE_HTTP);
        request.getHeadersKnob().addHeader("Content-Type", "text/xml", HeadersKnob.HEADER_TYPE_HTTP);

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        pec.setVariable("contextVar", "HEAD");

        TestingHttpClientFactory testingHttpClientFactory =
                appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

        final MockGenericHttpClient mockClient =
                new MockGenericHttpClient(200, new GenericHttpHeaders(new HttpHeader[]{}), null, 0L, new byte[0]);

        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(assertion, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));

        assertNull(mockClient.getRequestBody());
    }

    @BugId("SSG-13709")
    @Test
    public void testMethodFromContextVariable_PUT_BodyIncluded() throws Exception {
        String requestBody = "<foo/>";

        HttpRoutingAssertion assertion = new HttpRoutingAssertion();
        assertion.setProtectedServiceUrl("http://localhost:8080/test");
        assertion.setHttpMethod(HttpMethod.OTHER);
        assertion.setHttpMethodAsString("${contextVar}");
        assertion.setForceIncludeRequestBody(false);

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(new MockServletContext());
        mockHttpServletRequest.setMethod("GET");

        Message request = new Message(XmlUtil.stringAsDocument(requestBody));
        request.attachHttpRequestKnob(new HttpServletRequestKnob(mockHttpServletRequest));
        request.getHeadersKnob().addHeader("Content-Length", "6", HeadersKnob.HEADER_TYPE_HTTP);
        request.getHeadersKnob().addHeader("Content-Type", "text/xml", HeadersKnob.HEADER_TYPE_HTTP);

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        pec.setVariable("contextVar", "PUT");

        TestingHttpClientFactory testingHttpClientFactory =
                appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

        final MockGenericHttpClient mockClient =
                new MockGenericHttpClient(200, new GenericHttpHeaders(new HttpHeader[]{}), null, 0L, new byte[0]);

        testingHttpClientFactory.setMockHttpClient(mockClient);

        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(assertion, appContext);
        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));

        assertEquals(requestBody, new String(mockClient.getRequestBody()));
    }

    @BugId("SSG-7885")
    @Test
    public void testProxyConfigGetsResolvedFromContextVariables() throws Exception {
        ServerVariables.setSecurePasswordManager(new SecurePasswordManagerBuilder()
                .addPassword("proxypassword", "passwordOfProxy")
                .build());

        // let's build a ServerHttpRoutingAssertion that tries to contact a proxy
        final ServerHttpRoutingAssertion routingAssertion = new ServerHttpRoutingAssertion(
                new HttpRoutingAssertionBuilder()
                        .withProtectedServiceUrl("http://localhost:8080/test").withHttpMethod(HttpMethod.POST)
                        .withProxyHost("${proxyHost}").withProxyPort("${proxyPort}").withProxyUsername("${proxyUserName}")
                        .withProxyPassword("${secpass.proxypassword.plaintext}")
                        .build(),
                ApplicationContexts.getTestApplicationContext()
        );

        // let's build a fake HttpComponentsClient that returns a status of 200 and does nothing else
        GenericHttpRequest request = new GenericHttpRequestBuilder()
                .withResponse(new GenericHttpResponseBuilder().withStatus(200).withHeaders(mock(HttpHeaders.class))
                        .build())
                .build();
        final HttpComponentsClient client = mock(HttpComponentsClient.class);
        when(client.createRequest(any(HttpMethod.class), any(GenericHttpRequestParams.class))).thenReturn(request);

        // we need a container where we can keep track of the ProxyConfig object passed to the client factory
        final HttpProxyConfig[] proxyConfigPassedIn = new HttpProxyConfig[1];

        // let's override client factory
        routingAssertion.setHttpClientFactory(new GenericHttpClientFactory() {
            @Override
            public GenericHttpClient createHttpClient() {
                return null;
            }
            @Override
            public GenericHttpClient createHttpClient(int hostConnections, int totalConnections, int connectTimeout,
                    int timeout, Object identity, @Nullable HttpProxyConfig proxyConfig) {
                proxyConfigPassedIn[0] = proxyConfig;
                return client;
            }
        });

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        pec.setVariable("proxyHost", "proxy.com");
        pec.setVariable("proxyPort", "1330");
        pec.setVariable("proxyUsername", "userNameOfProxy");
        pec.setVariable("proxypassword", "passwordOfProxy");

        assertEquals(AssertionStatus.NONE, routingAssertion.checkRequest(pec));
        assertNotNull("Proxy config was not passed into createClient method", proxyConfigPassedIn[0]);
        assertEquals("Wrong proxy config was pass into createClient method",
                new HttpProxyConfig("proxy.com", 1330, "userNameOfProxy", "passwordOfProxy"),
                proxyConfigPassedIn[0]);
    }

    static class SecurePasswordManagerBuilder {
        private final Map<String, String> passwords;

        SecurePasswordManagerBuilder() {
            passwords = new HashMap<>();
        }

        SecurePasswordManagerBuilder addPassword(String key, String password) {
            passwords.put(key, password);
            return this;
        }

        SecurePasswordManager build() throws FindException, ParseException {
            SecurePasswordManager manager = mock(SecurePasswordManager.class);
            for (Map.Entry<String, String> entry : passwords.entrySet()) {
                SecurePassword password = new SecurePassword(entry.getKey());
                password.setUsageFromVariable(true);
                password.setEncodedPassword(entry.getValue());
                when(manager.findByUniqueName(entry.getKey())).thenReturn(password);
                when(manager.decryptPassword(entry.getValue())).thenReturn(entry.getValue().toCharArray());
            }

            return manager;
        }
    }

    static class HttpRoutingAssertionBuilder {
        private final HttpRoutingAssertion assertion;

        HttpRoutingAssertionBuilder() {
            this.assertion = new HttpRoutingAssertion();
        }

        HttpRoutingAssertionBuilder withProtectedServiceUrl(String url) {
            assertion.setProtectedServiceUrl(url);
            return this;
        }

        HttpRoutingAssertionBuilder withHttpMethod(HttpMethod method) {
            assertion.setHttpMethod(method);
            return this;
        }

        HttpRoutingAssertionBuilder withProxyHost(String host) {
            assertion.setProxyHost(host);
            return this;
        }

        HttpRoutingAssertionBuilder withProxyPort(String port) {
            assertion.setProxyPort(port);
            return this;
        }

        HttpRoutingAssertionBuilder withProxyUsername(String username) {
            assertion.setProxyUsername(username);
            return this;
        }

        HttpRoutingAssertionBuilder withProxyPassword(String password) {
            assertion.setProxyPassword(password);
            return this;
        }

        HttpRoutingAssertion build() {
            return assertion;
        }
    }

    static class GenericHttpResponseBuilder {
        private int status;
        private HttpHeaders headers;

        GenericHttpResponseBuilder withStatus(int status) {
            this.status = status;
            return this;
        }

        GenericHttpResponseBuilder withHeaders(HttpHeaders headers) {
            this.headers = headers;
            return this;
        }

        GenericHttpResponse build() throws GenericHttpException {
            GenericHttpResponse response = mock(GenericHttpResponse.class);
            when(response.getStatus()).thenReturn(status);
            when(response.getHeaders()).thenReturn(headers);
            return response;
        }
    }

    static class GenericHttpRequestBuilder {
        private GenericHttpResponse response;

        GenericHttpRequestBuilder withResponse(GenericHttpResponse response) throws GenericHttpException {
            this.response = response;
            return this;
        }

        GenericHttpRequest build() throws GenericHttpException {
            GenericHttpRequest request = mock(GenericHttpRequest.class);
            when(request.getResponse()).thenReturn(response);
            return request;
        }
    }

    static class MockHttpServletRequestBuilder {
        private final MockHttpServletRequest request;

        MockHttpServletRequestBuilder() {
            request = new MockHttpServletRequest();
        }

        MockHttpServletRequestBuilder withMethod(String method) {
            request.setMethod(method);
            return this;
        }

        MockHttpServletRequest build() {
            return request;
        }
    }

    private PolicyEnforcementContext createContentTypeTestPec(HttpRoutingAssertion hra, ApplicationContext appContext, String method, final Header[] contentTypeHeaders, final String[] contentTypeHeaderValueCollector, boolean nobody) throws Exception {
        // Configure to combine passthrough
        hra.setPassthroughHttpAuthentication(true);
        hra.setRequestHeaderRules(new HttpPassthroughRuleSet(true, new HttpPassthroughRule[]{}));

        Message request = nobody? new Message() : new Message(XmlUtil.stringAsDocument("<foo/>"));

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setMethod(method);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        if(contentTypeHeaders != null) {
            for (Header header : contentTypeHeaders) {
                request.getHeadersKnob().addHeader(header.getKey(), header.getValue(), header.getType(), header.isPassThrough());
            }
        }

        final String body = "<bar/>";
        Message response = new Message(XmlUtil.stringAsDocument(body));

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        TestingHttpClientFactory testingHttpClientFactory = appContext.getBean(CLIENT_FACTORY, TestingHttpClientFactory.class);

        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200, null, ContentTypeHeader.XML_DEFAULT, (long)body.length(), body.getBytes());
        mockClient.setCreateRequestListener(new MockGenericHttpClient.CreateRequestListener() {
            @Override
            public MockGenericHttpClient.MockGenericHttpRequest onCreateRequest(HttpMethod method, GenericHttpRequestParams params, MockGenericHttpClient.MockGenericHttpRequest request) {
                List<String> headerValList = new ArrayList<>();
                for (HttpHeader head : params.getExtraHeaders()) {
                    if (HttpConstants.HEADER_CONTENT_TYPE.equalsIgnoreCase(head.getName())) {
                        headerValList.add(head.getFullValue());
                    }
                }
                Collections.sort(headerValList);//always have predictable order
                assertArrayEquals(contentTypeHeaderValueCollector, headerValList.toArray(new String[0]));
                return null;
            }
        });
        testingHttpClientFactory.setMockHttpClient(mockClient);
        return pec;

    }

}
