package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.HttpHeaders;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerHttpRoutingAssertion;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.test.BugNumber;
import org.junit.*;
import org.springframework.context.ApplicationContext;
import static org.junit.Assert.*;

/**
 *
 */
public class HttpRoutingAssertionMultipleContentLengthHeadersTest {
    private static TestingHttpClientFactory testingHttpClientFactory;

    @BeforeClass
    public static void setUp() throws Exception {
        ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
        testingHttpClientFactory = applicationContext.getBean("httpRoutingHttpClientFactory2", TestingHttpClientFactory.class);
    }

    @Test
    @BugNumber(7353)
    public void testResponseWithMultipleContentLengthHeaders() throws Exception {
        HttpRoutingAssertion hra = new HttpRoutingAssertion();
        hra.setProtectedServiceUrl("http://localhost:17380/multipleContentLengthHeaders");

        ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        Message request = new Message(XmlUtil.stringAsDocument("<foo/>"));
        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());

        HttpHeaders headers = new GenericHttpHeaders(new HttpHeader[]{
                new GenericHttpHeader("Content-Length", "6, 6"),
                new GenericHttpHeader("Content-Length", "6"),
                new GenericHttpHeader("Content-Type", "text/xml"),
        });
        testingHttpClientFactory.setMockHttpClient(
                new MockGenericHttpClient(200, headers, ContentTypeHeader.XML_DEFAULT, 6L, ("<bar/>".getBytes())));
        new ServerHttpRoutingAssertion(hra, appContext).checkRequest(pec);
        assertTrue(pec.getResponse().isXml());
        assertEquals(6, pec.getResponse().getMimeKnob().getContentLength());
    }
}
