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
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.boot.GatewayPermissiveLoggingSecurityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test the CORSAssertion with cors.useMultiValuedHeaders option.
 */
@RunWith(Parameterized.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerCORSAssertionUseMultiValuedHeadersTest {

    private CORSAssertion assertion;
    private Executor executor;
    private AssertionStatus result;

    private static final SecurityManager ORIGINAL_SECURITY_MANAGER = System.getSecurityManager();
    private static ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
    private static ServerConfig serverConfig = applicationContext.getBean("serverConfig", ServerConfigStub.class);

    @Before
    public void setUp() {
        assertion = new CORSAssertion();
        executor = Executor.newInstance();
        result = AssertionStatus.UNDEFINED;
    }

    @After
    public void tearDown() {
        serverConfig.putProperty(ServerConfigParams.PARAM_CORS_USE_MULTI_VALUED_HEADERS, "false");
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        System.setSecurityManager(new GatewayPermissiveLoggingSecurityManager());
    }

    @AfterClass
    public static void tearDownAfterClass() {
        serverConfig.putProperty(ServerConfigParams.PARAM_CORS_USE_MULTI_VALUED_HEADERS, "false");
        System.setSecurityManager(ORIGINAL_SECURITY_MANAGER);
    }

    @Parameterized.Parameters
    public static String[] getUseMultiValuedHeadersInputs() {
        return new String[] { "false", "true" };
    }

    @Parameterized.Parameter
    public String useMultiValuedHeaders;

    @Test
    public void testDefaultAcceptAllRequestWithExposedHeaders() throws Exception {
        final String acceptedOrigin = "test.origin.com";

        serverConfig.putProperty(ServerConfigParams.PARAM_CORS_USE_MULTI_VALUED_HEADERS, useMultiValuedHeaders);

        assertion.setExposedHeaders(Arrays.asList("x-ca-something", "x-ca-other"));
        executor.setRequest(HttpMethod.GET, acceptedOrigin)
                .setAssertion(assertion);
        result = executor.run();

        assertEquals(AssertionStatus.NONE, result);
        assertEquals(1, executor.getAudit().getAuditCount());
        assertTrue(executor.getAudit().isAuditPresentWithParameters(AssertionMessages.USERDETAIL_FINE,
                "Origin allowed: " + acceptedOrigin));

        Validator.VALID_NON_PREFLIGHT_CORS_REQUEST_VALIDATOR.assertExecutor(executor);
        assertEquals(acceptedOrigin, executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals("true", executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);

        assertTrue(executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));

        if ("true".equals(useMultiValuedHeaders)) {
            assertArrayEquals(new String[] {"x-ca-something,x-ca-other"},
                    executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        } else {
            assertArrayEquals(new String[] {"x-ca-something", "x-ca-other"},
                    executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        }

        assertEquals(false, executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Max-Age", HeadersKnob.HEADER_TYPE_HTTP));
    }

    @Test
    public void testDefaultAcceptAllRequestWithOneInExposedHeaders() throws Exception {
        final String acceptedOrigin = "test.origin.com";

        serverConfig.putProperty(ServerConfigParams.PARAM_CORS_USE_MULTI_VALUED_HEADERS, useMultiValuedHeaders);

        assertion.setExposedHeaders(Arrays.asList("x-ca-something"));   // One in the list
        executor.setRequest(HttpMethod.GET, acceptedOrigin)
                .setAssertion(assertion);
        result = executor.run();

        assertEquals(AssertionStatus.NONE, result);
        assertEquals(1, executor.getAudit().getAuditCount());
        assertTrue(executor.getAudit().isAuditPresentWithParameters(AssertionMessages.USERDETAIL_FINE,
                "Origin allowed: " + acceptedOrigin));

        Validator.VALID_NON_PREFLIGHT_CORS_REQUEST_VALIDATOR.assertExecutor(executor);
        assertEquals(acceptedOrigin, executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals("true", executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);

        assertTrue(executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        assertArrayEquals(new String[] {"x-ca-something"},
                executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(false, executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Max-Age", HeadersKnob.HEADER_TYPE_HTTP));
    }

    @Test
    public void testDefaultAcceptAllRequestWithNoneInExposedHeaders() throws Exception {
        final String acceptedOrigin = "test.origin.com";

        serverConfig.putProperty(ServerConfigParams.PARAM_CORS_USE_MULTI_VALUED_HEADERS, useMultiValuedHeaders);

        assertion.setExposedHeaders(Arrays.asList());   // None in the list
        executor.setRequest(HttpMethod.GET, acceptedOrigin)
                .setAssertion(assertion);
        result = executor.run();

        assertEquals(AssertionStatus.NONE, result);
        assertEquals(1, executor.getAudit().getAuditCount());
        assertTrue(executor.getAudit().isAuditPresentWithParameters(AssertionMessages.USERDETAIL_FINE,
                "Origin allowed: " + acceptedOrigin));

        Validator.VALID_NON_PREFLIGHT_CORS_REQUEST_VALIDATOR.assertExecutor(executor);
        assertFalse(executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        assertEquals(acceptedOrigin, executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals("true", executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertFalse(executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Max-Age", HeadersKnob.HEADER_TYPE_HTTP));
    }

    @Test
    public void testAcceptedOriginPreflight() throws Exception {
        final String acceptedOrigin = "test.origin.com";

        serverConfig.putProperty(ServerConfigParams.PARAM_CORS_USE_MULTI_VALUED_HEADERS, useMultiValuedHeaders);

        assertion.setAcceptedOrigins(CollectionUtils.list(acceptedOrigin));
        executor.setRequest(HttpMethod.OPTIONS, acceptedOrigin, "GET", "param,x-param,x-requested-with")
                .setAssertion(assertion);
        result = executor.run();

        assertEquals(AssertionStatus.NONE, result);
        assertEquals(1, executor.getAudit().getAuditCount());
        assertTrue(executor.getAudit().isAuditPresentWithParameters(AssertionMessages.USERDETAIL_FINE,
                "Origin allowed: " + acceptedOrigin));

        Validator.VALID_PREFLIGHT_CORS_REQUEST_VALIDATOR.assertExecutor(executor);
        assertEquals(acceptedOrigin, executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals("true", executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
        assertEquals("GET", executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP)[0]);

        assertTrue(executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));

        if ("true".equals(useMultiValuedHeaders)) {
            assertArrayEquals(new String[]{"param,x-param,x-requested-with"},
                    executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        } else {
            assertArrayEquals(new String[]{"param", "x-param", "x-requested-with"},
                    executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));
        }

        assertEquals(false, executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Max-Age", HeadersKnob.HEADER_TYPE_HTTP));
    }

    /**
     * To execute the tests for CORS requests.
     */
    public static class Executor {

        private TestAudit audit;
        private Message request;
        private Message response;
        private PolicyEnforcementContext policyContext;
        private CORSAssertion assertion;
        private ServerCORSAssertion serverAssertion;

        public static Executor newInstance() {
            return new Executor();
        }

        public TestAudit getAudit() {
            return audit;
        }

        public Message getRequest() {
            return request;
        }

        public Message getResponse() {
            return response;
        }

        public PolicyEnforcementContext getPolicyContext() {
            return policyContext;
        }

        public CORSAssertion getAssertion() {
            return assertion;
        }

        public ServerCORSAssertion getServerAssertion() {
            return serverAssertion;
        }

        public Executor setRequest(HttpMethod method, String origin) {
            return setRequest(method, origin, null, null);
        }

        public Executor setRequest(HttpMethod method, String origin, String corsMethod, String corsHeaders) {
            this.request = createCorsRequest(method, origin, corsMethod, corsHeaders);
            return this;
        }

        public Executor setAssertion(CORSAssertion assertion) {
            this.assertion = assertion;
            return this;
        }

        /**
         * executes the test by creating the server and invoking the checkRequest over it.
         * @return
         * @throws Exception
         */
        public AssertionStatus run() throws Exception {
            audit = new TestAudit();
            response = createResponse();
            policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
            serverAssertion = createServer(assertion, audit);
            return serverAssertion.checkRequest(policyContext);
        }

        public static ServerCORSAssertion createServer(CORSAssertion assertion, TestAudit audit) {
            ServerCORSAssertion serverAssertion = new ServerCORSAssertion(assertion, ApplicationContexts.getTestApplicationContext());

            ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                    .put("auditFactory", audit.factory())
                    .unmodifiableMap()
            );

            return serverAssertion;
        }

        public static Message createCorsRequest(HttpMethod method, String origin, String corsMethod, String corsHeaders) {
            Message request = createRequest(method);
            HeadersKnob headers = request.getHeadersKnob();

            if (null != origin) {
                headers.setHeader("Origin", origin, HeadersKnob.HEADER_TYPE_HTTP);
            }

            if (null != corsMethod) {
                headers.setHeader("Access-Control-Request-Method", corsMethod, HeadersKnob.HEADER_TYPE_HTTP);
            }

            if (null != corsHeaders) {
                headers.setHeader("Access-Control-Request-Headers", corsHeaders, HeadersKnob.HEADER_TYPE_HTTP);
            }

            return request;
        }

        public static Message createRequest(HttpMethod method) {
            MockHttpServletRequest hRequest = new MockHttpServletRequest();
            hRequest.setMethod(method.toString());
            hRequest.setProtocol("http");
            hRequest.setServerPort(8080);
            hRequest.setServerName("www.example.com");

            Message request = new Message();
            request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));

            return request;
        }

        public static Message createResponse() {
            MockHttpServletResponse hResponse = new MockHttpServletResponse();

            Message response = new Message();
            response.attachHttpResponseKnob(new HttpServletResponseKnob(hResponse));

            return response;
        }
    }

    /**
     * Provides specialized CORS Request Validators.
     */
    public static abstract class Validator {

        /**
         * Asserts the executor for validity
         * @param executor
         * @throws Exception
         */
        public abstract void assertExecutor(Executor executor) throws Exception;

        public static final Validator VALID_NON_PREFLIGHT_CORS_REQUEST_VALIDATOR = new Validator() {
            @Override
            public void assertExecutor(Executor executor) throws Exception {
                assertEquals(false, executor.getPolicyContext().getVariable("cors.isPreflight"));
                assertEquals(true, executor.getPolicyContext().getVariable("cors.isCors"));

                assertTrue(executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
                assertTrue(executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
            }
        };

        public static final Validator VALID_PREFLIGHT_CORS_REQUEST_VALIDATOR = new Validator() {
            @Override
            public void assertExecutor(Executor executor) throws Exception {
                assertEquals(true, executor.getPolicyContext().getVariable("cors.isPreflight"));
                assertEquals(true, executor.getPolicyContext().getVariable("cors.isCors"));

                assertTrue(executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Origin", HeadersKnob.HEADER_TYPE_HTTP));
                assertTrue(executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP));
                assertTrue(executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Methods", HeadersKnob.HEADER_TYPE_HTTP));
                assertTrue(executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Allow-Headers", HeadersKnob.HEADER_TYPE_HTTP));
                assertFalse(executor.getResponse().getHeadersKnob().containsHeader("Access-Control-Expose-Headers", HeadersKnob.HEADER_TYPE_HTTP));

                assertEquals("true", executor.getResponse().getHeadersKnob().getHeaderValues("Access-Control-Allow-Credentials", HeadersKnob.HEADER_TYPE_HTTP)[0]);
            }
        };
    }

}
