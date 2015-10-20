package com.l7tech.external.assertions.cors.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.cors.CORSAssertion;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.boot.GatewayPermissiveLoggingSecurityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test the CORSAssertion.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerCORSAssertionTest {

    private TestAudit testAudit;
    private SecurityManager originalSecurityManager;

    @Before
    public void setUp() {
        testAudit = new TestAudit();

        originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new GatewayPermissiveLoggingSecurityManager());
    }

    @After
    public void tearDown() throws Exception {
        System.setSecurityManager(originalSecurityManager);
    }
    
    @Test
    public void testNonCorsRequest_NoOriginHeader_CorsVariablesFalse() throws Exception {
        Message request = createRequest(HttpMethod.OPTIONS.toString());

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.FALSIFIED, result);
        Assert.assertEquals(false, context.getVariable("cors.isPreflight"));
        Assert.assertEquals(false, context.getVariable("cors.isCors"));
        Assert.assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));
    }

    @Test
    public void testDefaultAcceptAllPreflight() throws Exception {
        final String acceptedOrigin = "acceptedOrigin";

        Message request = createRequest(HttpMethod.OPTIONS.toString());
        configureRequestHeaders(request, acceptedOrigin, "GET", "param,x-param,x-requested-with");

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.NONE, result);
        Assert.assertEquals(true, context.getVariable("cors.isPreflight"));
        Assert.assertEquals(true, context.getVariable("cors.isCors"));
        Assert.assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals(acceptedOrigin, response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals("true", response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals("GET", response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));

        String[] allowedHeaderValues =
                response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP);

        Assert.assertEquals(3, allowedHeaderValues.length);
        Assert.assertEquals("param", allowedHeaderValues[0]);
        Assert.assertEquals("x-param", allowedHeaderValues[1]);
        Assert.assertEquals("x-requested-with", allowedHeaderValues[2]);
    }

    @Test
    public void testDefaultAcceptAllRequest() throws Exception {
        final String acceptedOrigin = "acceptedOrigin";

        Message request = createRequest(HttpMethod.GET.toString());
        configureRequestHeaders(request, acceptedOrigin, null, null);

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.NONE, result);
        Assert.assertEquals(false, context.getVariable("cors.isPreflight"));
        Assert.assertEquals(true, context.getVariable("cors.isCors"));
        Assert.assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals(acceptedOrigin, response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals("true", response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void testAcceptedOriginPreflight() throws Exception {
        final String acceptedOrigin = "acceptedOrigin";

        Message request = createRequest(HttpMethod.OPTIONS.toString());
        configureRequestHeaders(request, acceptedOrigin, "GET", "param,x-param,x-requested-with");

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();
        assertion.setAcceptedOrigins(CollectionUtils.list(acceptedOrigin));

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.NONE, result);
        Assert.assertEquals(true, context.getVariable("cors.isPreflight"));
        Assert.assertEquals(true, context.getVariable("cors.isCors"));
        Assert.assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals(acceptedOrigin, response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals("true", response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals("GET", response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));

        String[] allowedHeaderValues =
                response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP);

        Assert.assertEquals(3, allowedHeaderValues.length);
        Assert.assertEquals("param", allowedHeaderValues[0]);
        Assert.assertEquals("x-param", allowedHeaderValues[1]);
        Assert.assertEquals("x-requested-with", allowedHeaderValues[2]);
    }

    private void configureRequestHeaders(Message message, String origin, String method, String headers) {
        if (null != origin)
            message.getHeadersKnob().setHeader("Origin", origin, HeadersKnob.HEADER_TYPE_HTTP);
        
        if (null != method)
            message.getHeadersKnob().setHeader("Access-Control-Request-Method", method, HeadersKnob.HEADER_TYPE_HTTP);
        
        if (null != headers)
            message.getHeadersKnob().setHeader("Access-Control-Request-Headers", headers, HeadersKnob.HEADER_TYPE_HTTP);
    }

    private ServerCORSAssertion createServer(CORSAssertion assertion) {
        ServerCORSAssertion serverAssertion = new ServerCORSAssertion(assertion);

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                        .put("auditFactory", testAudit.factory())
                        .unmodifiableMap()
        );

        return serverAssertion;
    }

    private Message createRequest(String method) {
        MockHttpServletRequest hRequest = new MockHttpServletRequest();
        hRequest.setMethod(method);

        Message request = new Message();
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));

        return request;
    }

    private Message createResponse() {
        MockHttpServletResponse hResponse = new MockHttpServletResponse();

        Message response = new Message();
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hResponse));

        return response;
    }
}
