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
    public void testDefaultAcceptAllPreflight() throws Exception {
        final String acceptedOrigin = "acceptedOrigin";

        Message request = createRequest(HttpMethod.OPTIONS.toString());
        request.getHeadersKnob().setHeader("origin",acceptedOrigin, HeadersKnob.HEADER_TYPE_HTTP);
        request.getHeadersKnob().setHeader("Access-Control-Request-Method","GET", HeadersKnob.HEADER_TYPE_HTTP);
        request.getHeadersKnob().setHeader("Access-Control-Request-Headers","param,x-param,x-requested-with", HeadersKnob.HEADER_TYPE_HTTP);

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.NONE, result);
        Assert.assertEquals(true, context.getVariable("cors.isPreflight"));
        Assert.assertEquals(true, context.getVariable("cors.isCors"));
        Assert.assertEquals(true, context.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals(acceptedOrigin, context.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-credentials", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals("true", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-methods", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals("GET", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-methods", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-headers", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals("param,x-param,x-requested-with", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-headers", HeadersKnob.HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void testDefaultAcceptAllRequest() throws Exception {
        final String acceptedOrigin = "acceptedOrigin";

        Message request = createRequest(HttpMethod.OPTIONS.toString());
        request.getHeadersKnob().setHeader("origin",acceptedOrigin, HeadersKnob.HEADER_TYPE_HTTP);

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.NONE, result);
        Assert.assertEquals(false, context.getVariable("cors.isPreflight"));
        Assert.assertEquals(true, context.getVariable("cors.isCors"));
        Assert.assertEquals(true, context.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals(acceptedOrigin, context.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-credentials", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals("true", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void testAcceptedOriginPreflight() throws Exception {
        final String acceptedOrigin = "acceptedOrigin";

        Message request = createRequest(HttpMethod.OPTIONS.toString());
        request.getHeadersKnob().setHeader("origin",acceptedOrigin, HeadersKnob.HEADER_TYPE_HTTP);
        request.getHeadersKnob().setHeader("Access-Control-Request-Method","GET", HeadersKnob.HEADER_TYPE_HTTP);
        request.getHeadersKnob().setHeader("Access-Control-Request-Headers","param,x-param,x-requested-with", HeadersKnob.HEADER_TYPE_HTTP);

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();
        assertion.setAcceptedOrigins(CollectionUtils.list(acceptedOrigin));

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        Assert.assertEquals(AssertionStatus.NONE, result);
        Assert.assertEquals(true, context.getVariable("cors.isPreflight"));
        Assert.assertEquals(true, context.getVariable("cors.isCors"));
        Assert.assertEquals(true, context.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals(acceptedOrigin, context.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-credentials", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals("true", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-methods", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals("GET", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-methods", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals(true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-headers", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals("param,x-param,x-requested-with", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-headers", HeadersKnob.HEADER_TYPE_HTTP)[0]);
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
