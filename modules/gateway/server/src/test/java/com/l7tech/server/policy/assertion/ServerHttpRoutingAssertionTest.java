/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.HttpHeaders;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
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

import java.io.InputStream;

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

        TestingHttpClientFactory testingHttpClientFactory = (TestingHttpClientFactory) appContext.getBean("httpRoutingHttpClientFactory", TestingHttpClientFactory.class);

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

}
