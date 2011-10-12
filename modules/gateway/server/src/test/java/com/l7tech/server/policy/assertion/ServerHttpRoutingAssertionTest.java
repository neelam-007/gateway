/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.HttpPassthroughRule;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ServerHttpRoutingAssertionTest {

    /**
     * Test that its possible to overwrite a Message variable with the contents of a response, when the message variable
     * has previously held the contents of a routing assertion response
     */
    @Test
    @BugNumber(8396)
    public void testOverwriteResponseVariable() throws Exception{
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
    public void testPassthroughAuthHeader() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/testPassthroughAuthHeader");

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

        final Object[] requestRecordedExtraHeaders = { null };
        final PasswordAuthentication[] requestRecordedPasswordAuthentication = { null };

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
}
