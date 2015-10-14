package com.l7tech.external.assertions.cors.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.cors.CORSAssertion;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HttpRequestKnobStub;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test the CORSAssertion.
 *
 */
public class ServerCORSAssertionTest {


    private ServerCORSAssertion buildServerAssertion(final CORSAssertion ass) {
        return new ServerCORSAssertion(
                ass,
                new StashManagerFactory() {
                    @Override
                    public StashManager createStashManager() {
                        return new ByteArrayStashManager();
                    }
                }
        );
    }

    @Test
    @Ignore
    public void testDefaultAcceptAllPreflight() throws Exception {
        final String acceptedOrigin = "acceptedOrigin";

        Message request = new Message();
//        request.attachHttpRequestKnob(new HttpRequestKnobStub(
//                null,
//                null,
//                HttpMethod.OPTIONS));
        request.getHeadersKnob().setHeader("origin",acceptedOrigin, HeadersKnob.HEADER_TYPE_HTTP);
        request.getHeadersKnob().setHeader("Access-Control-Request-Method","GET", HeadersKnob.HEADER_TYPE_HTTP);
        request.getHeadersKnob().setHeader("Access-Control-Request-Headers","param,x-param,x-requested-with", HeadersKnob.HEADER_TYPE_HTTP);

        Message response = new Message();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();

        ServerCORSAssertion serverAssertion = buildServerAssertion(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        Assert.assertEquals( AssertionStatus.NONE , result);
        Assert.assertEquals( true, context.getVariable("cors.isPreflight"));
        Assert.assertEquals( true, context.getVariable("cors.isCors"));
        Assert.assertEquals( true, context.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals( acceptedOrigin, context.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals( true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-credentials", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals( "true", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals( true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-methods", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals( "GET", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-methods", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals( true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-headers", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals( "param,x-param,x-requested-with", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-headers", HeadersKnob.HEADER_TYPE_HTTP)[0]);
    }

    @Test
    @Ignore
    public void testDefaultAcceptAllRequest() throws Exception {
        final String acceptedOrigin = "acceptedOrigin";

        Message request = new Message();
//        request.attachHttpRequestKnob(new HttpRequestKnobStub(
//                null,
//                null,
//                HttpMethod.OPTIONS));
        request.getHeadersKnob().setHeader("origin",acceptedOrigin, HeadersKnob.HEADER_TYPE_HTTP);

        Message response = new Message();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();

        ServerCORSAssertion serverAssertion = buildServerAssertion(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        Assert.assertEquals( AssertionStatus.NONE , result);
        Assert.assertEquals( false, context.getVariable("cors.isPreflight"));
        Assert.assertEquals( true, context.getVariable("cors.isCors"));
        Assert.assertEquals( true, context.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals( acceptedOrigin, context.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals( true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-credentials", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals( "true", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
    }

    @Test
    @Ignore
    public void testAcceptedOriginPreflight() throws Exception {
        final String acceptedOrigin = "acceptedOrigin";

        Message request = new Message();
//        request.attachHttpRequestKnob(new HttpRequestKnobStub(
//                null,
//                null,
//                HttpMethod.OPTIONS));
        request.getHeadersKnob().setHeader("origin",acceptedOrigin, HeadersKnob.HEADER_TYPE_HTTP);
        request.getHeadersKnob().setHeader("Access-Control-Request-Method","GET", HeadersKnob.HEADER_TYPE_HTTP);
        request.getHeadersKnob().setHeader("Access-Control-Request-Headers","param,x-param,x-requested-with", HeadersKnob.HEADER_TYPE_HTTP);

        Message response = new Message();

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        CORSAssertion assertion = new CORSAssertion();
        assertion.setAcceptedOrigins(CollectionUtils.list(acceptedOrigin));

        ServerCORSAssertion serverAssertion = buildServerAssertion(assertion);

        AssertionStatus result = serverAssertion.checkRequest(context);

        Assert.assertEquals( AssertionStatus.NONE , result);
        Assert.assertEquals( true, context.getVariable("cors.isPreflight"));
        Assert.assertEquals( true, context.getVariable("cors.isCors"));
        Assert.assertEquals( true, context.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals( acceptedOrigin, context.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals( true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-credentials", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals( "true", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals( true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-methods", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals( "GET", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-methods", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        Assert.assertEquals( true, context.getResponse().getHeadersKnob().containsHeader("access-control-allow-headers", HeadersKnob.HEADER_TYPE_HTTP));
        Assert.assertEquals( "param,x-param,x-requested-with", context.getResponse().getHeadersKnob().getHeaderValues("access-control-allow-headers", HeadersKnob.HEADER_TYPE_HTTP)[0]);
    }



}
