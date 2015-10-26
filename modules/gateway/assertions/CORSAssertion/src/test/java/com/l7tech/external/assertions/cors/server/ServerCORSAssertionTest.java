package com.l7tech.external.assertions.cors.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.cors.CORSAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

/**
 * Test the CORSAssertion.
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
    public void testCorsRequired_NoOriginHeader_FailsWithCorsVariablesFalseAndWarningAudited() throws Exception {
        Message request = createRequest(HttpMethod.DELETE.toString());

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();
        assertion.setRequireCors(true); // CORS required

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.BAD_REQUEST, result);
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_WARNING,
                "Request must be a valid CORS request."));
        assertEquals(false, context.getVariable("cors.isPreflight"));
        assertEquals(false, context.getVariable("cors.isCors"));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
    }

    @Test
    public void testCorsNotRequired_NoOriginHeader_SucceedsWithCorsVariablesFalseAndNoAudits() throws Exception {
        Message request = createRequest(HttpMethod.DELETE.toString());

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();
        assertion.setRequireCors(false);    // CORS not required

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, result);
        assertEquals(0, testAudit.getAuditCount());
        assertEquals(false, context.getVariable("cors.isPreflight"));
        assertEquals(false, context.getVariable("cors.isCors"));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
    }

    @Test
    public void testAcceptAllPreflightWithSupportCredentialsEnabled_AllowCredentialsHeaderAdded() throws Exception {
        final String acceptedOrigin = "acceptedOrigin";

        Message request = createRequest(HttpMethod.OPTIONS.toString());
        configureRequestHeaders(request, acceptedOrigin, "GET", "param,x-param,x-requested-with");

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, result);
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_FINE,
                "Origin allowed: " + acceptedOrigin));
        assertEquals(true, context.getVariable("cors.isPreflight"));
        assertEquals(true, context.getVariable("cors.isCors"));
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(acceptedOrigin, response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals("true", response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals("GET", response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));

        String[] allowedHeaderValues =
                response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP);

        assertEquals(3, allowedHeaderValues.length);
        assertEquals("param", allowedHeaderValues[0]);
        assertEquals("x-param", allowedHeaderValues[1]);
        assertEquals("x-requested-with", allowedHeaderValues[2]);
    }

    @Test
    public void testAcceptAllPreflightWithSupportCredentialsDisabled_AllowOriginsWildcardAndAllowCredentialsHeaderNotAdded() throws Exception {
        final String acceptedOrigin = "acceptedOrigin";

        Message request = createRequest(HttpMethod.OPTIONS.toString());
        configureRequestHeaders(request, acceptedOrigin, "GET", "param,x-param,x-requested-with");

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();
        assertion.setSupportsCredentials(false);

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, result);
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_FINE,
                "Origin allowed: " + acceptedOrigin));
        assertEquals(true, context.getVariable("cors.isPreflight"));
        assertEquals(true, context.getVariable("cors.isCors"));
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals("*", response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals("GET", response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));

        String[] allowedHeaderValues =
                response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP);

        assertEquals(3, allowedHeaderValues.length);
        assertEquals("param", allowedHeaderValues[0]);
        assertEquals("x-param", allowedHeaderValues[1]);
        assertEquals("x-requested-with", allowedHeaderValues[2]);
    }

    @Test
    public void testDefaultAcceptAllRequestWithExposedHeaders() throws Exception {
        final String acceptedOrigin = "acceptedOrigin";
        final String exposedHeader1 = "x-ca-something";
        final String exposedHeader2 = "x-ca-other";

        Message request = createRequest(HttpMethod.GET.toString());
        configureRequestHeaders(request, acceptedOrigin, null, null);

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();
        assertion.setExposedHeaders(Arrays.asList(exposedHeader1, exposedHeader2));

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, result);
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_FINE,
                "Origin allowed: " + acceptedOrigin));
        assertEquals(false, context.getVariable("cors.isPreflight"));
        assertEquals(true, context.getVariable("cors.isCors"));
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(acceptedOrigin, response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals("true", response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));

        String[] exposedHeaderValues =
                response.getHeadersKnob().getHeaderValues("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP);

        assertEquals(2, exposedHeaderValues.length);
        assertEquals(exposedHeader1, exposedHeaderValues[0]);
        assertEquals(exposedHeader2, exposedHeaderValues[1]);
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

        assertEquals(AssertionStatus.NONE, result);
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_FINE,
                "Origin allowed: " + acceptedOrigin));
        assertEquals(true, context.getVariable("cors.isPreflight"));
        assertEquals(true, context.getVariable("cors.isCors"));
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(acceptedOrigin, response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals("true", response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals("GET", response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(true, response.getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));

        String[] allowedHeaderValues =
                response.getHeadersKnob().getHeaderValues("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP);

        assertEquals(3, allowedHeaderValues.length);
        assertEquals("param", allowedHeaderValues[0]);
        assertEquals("x-param", allowedHeaderValues[1]);
        assertEquals("x-requested-with", allowedHeaderValues[2]);
    }

    @Test
    public void testOriginNotAccepted_AssertionFalsifiedAndWarningAudited() throws Exception {
        final String origin = "invalid.com";

        Message request = createRequest(HttpMethod.GET.toString());
        configureRequestHeaders(request, origin, null, null);

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();
        assertion.setAcceptedOrigins(Arrays.asList("allowed.com"));

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FALSIFIED, result);
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_WARNING,
                "Origin not allowed: " + origin));
        assertEquals(false, context.getVariable("cors.isPreflight"));
        assertEquals(true, context.getVariable("cors.isCors"));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
    }

    @Test
    public void testPreflight_WithUnacceptedRequestedHeader_AssertionFalsifiedAndWarningAudited() throws Exception {
        final String origin = "allowed.com";
        final String method = "DELETE";
        final String invalidHeader = "x-not-allowed";

        Message request = createRequest(HttpMethod.OPTIONS.toString());
        configureRequestHeaders(request, origin, method, invalidHeader);

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();
        assertion.setAcceptedOrigins(Arrays.asList(origin));
        assertion.setAcceptedMethods(Arrays.asList(method));
        assertion.setAcceptedHeaders(Arrays.asList("x-allowed"));

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FALSIFIED, result);
        assertEquals(2, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_FINE,
                "Origin allowed: " + origin));
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_WARNING,
                invalidHeader + " is not an accepted header."));
        assertEquals(true, context.getVariable("cors.isPreflight"));
        assertEquals(true, context.getVariable("cors.isCors"));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
    }

    @Test
    public void testPreflight_WithUnacceptedRequestedMethod_AssertionFalsifiedAndWarningAudited() throws Exception {
        final String origin = "allowed.com";
        final String unacceptedMethod = "DELETE";
        final String header = "x-allowed";

        Message request = createRequest(HttpMethod.OPTIONS.toString());
        configureRequestHeaders(request, origin, unacceptedMethod, header);

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();
        assertion.setAcceptedOrigins(Arrays.asList(origin));
        assertion.setAcceptedMethods(Arrays.asList("PUT"));
        assertion.setAcceptedHeaders(Arrays.asList(header));

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FALSIFIED, result);
        assertEquals(2, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_FINE,
                "Origin allowed: " + origin));
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_WARNING,
                unacceptedMethod + " is not an accepted method."));
        assertEquals(true, context.getVariable("cors.isPreflight"));
        assertEquals(true, context.getVariable("cors.isCors"));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
    }

    @Test
    public void testPreflight_WithInvalidRequestedMethod_AssertionFailsAndWarningAudited() throws Exception {
        final String origin = "allowed.com";
        final String invalidMethod = "NOTAMETHOD";
        final String header = "x-allowed";

        Message request = createRequest(HttpMethod.OPTIONS.toString());
        configureRequestHeaders(request, origin, invalidMethod, header);

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();
        assertion.setAcceptedOrigins(Arrays.asList(origin));
        assertion.setAcceptedMethods(Arrays.asList("PUT"));
        assertion.setAcceptedHeaders(Arrays.asList(header));

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.BAD_REQUEST, result);
        assertEquals(2, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_FINE,
                "Origin allowed: " + origin));
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_WARNING,
                "The value of the " + ServerCORSAssertion.REQUEST_METHOD_HEADER +
                        " header is not a recognized HTTP method: " + invalidMethod));
        assertEquals(true, context.getVariable("cors.isPreflight"));
        assertEquals(true, context.getVariable("cors.isCors"));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
    }

    @Test
    public void testPreflight_WithMultipleRequestedMethodHeaders_AssertionFailsAndWarningAudited() throws Exception {
        final String origin = "allowed.com";
        final String method = "PUT";
        final String header = "x-allowed";

        Message request = createRequest(HttpMethod.OPTIONS.toString());
        configureRequestHeaders(request, origin, method, header);

        // add an extra invalid method header
        request.getHeadersKnob().addHeader("Access-Control-Request-Method", "DELETE", HeadersKnob.HEADER_TYPE_HTTP);

        Message response = createResponse();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();
        assertion.setAcceptedOrigins(Arrays.asList(origin));
        assertion.setAcceptedMethods(Arrays.asList(method));
        assertion.setAcceptedHeaders(Arrays.asList(header));

        ServerCORSAssertion serverAssertion = createServer(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.BAD_REQUEST, result);
        assertEquals(2, testAudit.getAuditCount());
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_FINE,
                "Origin allowed: " + origin));
        assertTrue(testAudit.isAuditPresentWithParameters(AssertionMessages.USERDETAIL_WARNING,
                "The request must contain exactly one " + ServerCORSAssertion.REQUEST_METHOD_HEADER + " header."));
        assertEquals(true, context.getVariable("cors.isPreflight"));
        assertEquals(true, context.getVariable("cors.isCors"));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, response.getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
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
