package com.l7tech.external.assertions.swagger.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.swagger.SwaggerAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.boot.GatewayPermissiveLoggingSecurityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.IOUtils;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.SwaggerParser;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import static com.l7tech.external.assertions.swagger.server.ServerSwaggerAssertion.PathDefinition;
import static com.l7tech.external.assertions.swagger.server.ServerSwaggerAssertion.PathResolver;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Test the SwaggerAssertion.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerSwaggerAssertionTest {

    public static final String INVALID_SWAGGER_JSON = "{\"name\":\"value\"}";
    public static final String INVALID_SWAGGER_NONJSON = "<name>value</name>";
    public static final String OAUTH2_AUTH_TOKEN = " bearer J1qK1c18UUGJFAzz9xnH56584l4";
    public static final String OAUTH2_PARAM_TOKEN = "J1qK1c18UUGJFAzz9xnH56584l4";
    public static final String APIKEY_TOKEN = "Jlkhbejhven789hhetbJMHeur";
    public static final String BASIC_TOKEN = "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==";

    SwaggerAssertion assertion;
    ServerSwaggerAssertion fixture;

    @Inject
    private StashManagerFactory stashManagerFactory;

    private StashManager stashManager;
    private TestAudit testAudit;
    private SecurityManager originalSecurityManager;

    private static String testDocument;
    private static Swagger testModel;
    private static PathResolver testModelPathResolver;

    @BeforeClass
    public static void init() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        List<AuthorizationValue> authorizationValues = new ArrayList<>();
        authorizationValues.add(new AuthorizationValue());

        testDocument = new String(IOUtils.slurpStream(ServerSwaggerAssertionTest.class
                .getResourceAsStream("petstore_swagger.json")), Charsets.UTF8);
        testModel = parser.parse(testDocument, authorizationValues);
        testModelPathResolver = new PathResolver(testModel);
    }

    @Before
    public void setUp() {
        assertion = new SwaggerAssertion();
        assertion.setRequireSecurityCredentials(false);

        testAudit = new TestAudit();
        stashManager = stashManagerFactory.createStashManager();

        originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new GatewayPermissiveLoggingSecurityManager());
    }

    @After
    public void tearDown() throws Exception {
        System.setSecurityManager(originalSecurityManager);
    }

    @Test
    public void testInvalidSwagger() throws Exception {
        assertion.setSwaggerDoc("swaggerDoc");
        fixture = createServer(assertion);

        PolicyEnforcementContext pec = createPolicyEnforcementContext(
                createHttpRequestMessage("/svr/pet/findByStatus", "GET")
        );

        pec.setVariable("swaggerDoc", INVALID_SWAGGER_JSON);

        assertEquals(AssertionStatus.FAILED, fixture.checkRequest(pec));
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(AssertionMessages.SWAGGER_INVALID_DOCUMENT.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.SWAGGER_INVALID_DOCUMENT));
    }

    @Test
    public void testInvalidSwaggerNonJson() throws Exception {
        assertion.setSwaggerDoc("swaggerDoc");
        fixture = createServer(assertion);

        PolicyEnforcementContext pec = createPolicyEnforcementContext(
                createHttpRequestMessage("/svr/pet/findByStatus", "GET")
        );

        pec.setVariable("swaggerDoc", INVALID_SWAGGER_NONJSON);

        assertEquals(AssertionStatus.FAILED, fixture.checkRequest(pec));
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(AssertionMessages.SWAGGER_INVALID_DOCUMENT.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.SWAGGER_INVALID_DOCUMENT));
    }

    @Test
    public void testSwaggerValidRequest_checkBaseUriAndHostContextVariables() throws Exception {
        assertion.setSwaggerDoc("swaggerDoc");
        fixture = createServer(assertion);

        PolicyEnforcementContext pec = createPolicyEnforcementContext(
                createHttpRequestMessage("/svr/pet/findByStatus", "GET")
        );

        pec.setVariable("swaggerDoc", testDocument);

        assertEquals(AssertionStatus.NONE, fixture.checkRequest(pec));
        assertEquals("petstore.swagger.io", pec.getVariable(SwaggerAssertion.DEFAULT_PREFIX + SwaggerAssertion.SWAGGER_HOST));
        assertEquals("/v2", pec.getVariable(SwaggerAssertion.DEFAULT_PREFIX + SwaggerAssertion.SWAGGER_BASE_URI));
        assertEquals("/pet/findByStatus", pec.getVariable(SwaggerAssertion.DEFAULT_PREFIX + SwaggerAssertion.SWAGGER_API_URI));
    }

    @Test
    public void testSwaggerValidRequest_noSecurityDefinitionsRequired() throws Exception {
        String requestUri = "/pet/findByTags";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(false); // scheme validation disabled
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(true);  // scheme is https - not supported

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
     public void testSwaggerValidRequest_checkServiceBaseContextVariables() throws Exception {
        assertion.setSwaggerDoc("swaggerDoc");
        assertion.setServiceBase("${abc}");
        fixture = createServer(assertion);

        PolicyEnforcementContext pec = createPolicyEnforcementContext(
                createHttpRequestMessage("/svr/pet/findByStatus", "GET")
        );

        pec.setVariable("swaggerDoc", testDocument);
        pec.setVariable("abc", "/svr");
        assertEquals(AssertionStatus.NONE, fixture.checkRequest(pec));
        assertEquals("/pet/findByStatus", pec.getVariable(SwaggerAssertion.DEFAULT_PREFIX + SwaggerAssertion.SWAGGER_API_URI));
    }

    @Test
    public void testSwaggerValidRequest_checkServiceBase() throws Exception {
        assertion.setSwaggerDoc("swaggerDoc");
        assertion.setServiceBase("/svr");
        fixture = createServer(assertion);

        PolicyEnforcementContext pec = createPolicyEnforcementContext(
                createHttpRequestMessage("/svr/pet/findByStatus", "GET")
        );

        pec.setVariable("swaggerDoc", testDocument);

        assertEquals(AssertionStatus.NONE, fixture.checkRequest(pec));
        assertEquals("/pet/findByStatus", pec.getVariable(SwaggerAssertion.DEFAULT_PREFIX + SwaggerAssertion.SWAGGER_API_URI));
    }

    @Test
    public void testSwaggerValidRequest_invalidServiceBasePattern() throws Exception {
        assertion.setSwaggerDoc("swaggerDoc");
        assertion.setServiceBase("/svr/pet/");
        fixture = createServer(assertion);
        PolicyEnforcementContext pec = createPolicyEnforcementContext(
                createHttpRequestMessage("/svr/pet/findByStatus", "GET")
        );

        pec.setVariable("swaggerDoc", testDocument);

        assertEquals(AssertionStatus.FALSIFIED, fixture.checkRequest(pec));
    }

    @Test
    public void testSwaggerValidRequest_checkEmptyServiceBaseContextVariable() throws Exception {
        assertion.setSwaggerDoc("swaggerDoc");
        assertion.setServiceBase("${empty}");
        assertion.setValidatePath(false);

        fixture = createServer(assertion);

        PolicyEnforcementContext pec = createPolicyEnforcementContext(
                createHttpRequestMessage("/svr/pet/findByStatus", "GET")
        );

        pec.setVariable("swaggerDoc", testDocument);
        pec.setVariable("empty","");
        assertEquals(AssertionStatus.NONE, fixture.checkRequest(pec));
        assertEquals("/svr/pet/findByStatus", pec.getVariable(SwaggerAssertion.DEFAULT_PREFIX + SwaggerAssertion.SWAGGER_API_URI));
    }

    @Test
    public void testValidateWithPathAndMethodEnabled_ValidPathAndMethod_SucceedsNoAudits() throws Exception {
        String requestUri = "/store/order";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.POST);

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testValidateWithPathAndMethodEnabled_ValidPathAndMethod_Falsified11402Audited() throws Exception {
        String requestUri = "/store/order";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);   // get not defined

        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(AssertionMessages.SWAGGER_INVALID_METHOD.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.SWAGGER_INVALID_METHOD));
    }

    @Test
    public void testValidateWithPathAndMethodEnabled_InvalidHeadMethod_Falsified11402Audited() throws Exception {
        String requestUri = "/store/order";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.HEAD);   // head not defined

        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(AssertionMessages.SWAGGER_INVALID_METHOD.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.SWAGGER_INVALID_METHOD));
    }

    @Test
    public void testValidateWithPathAndMethodEnabled_ValidHeadMethod_SucceedsNoAudits() throws Exception {
        String requestUri = "/store/order/{orderId}";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.HEAD);   // head defined

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testValidateWithPathAndMethod_GetSwaggerDoc_Falsified11401Audited() throws Exception {
        String requestUri = "/swagger.json";    // swagger doc is not defined as a path item

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);

        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(AssertionMessages.SWAGGER_INVALID_PATH.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.SWAGGER_INVALID_PATH));
    }

    @Test
    public void testValidateWithPathMethodScheme_ValidRequest_SucceedsNoAudits() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(false);

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testValidateWithPathMethodScheme_InvalidRequestScheme_Falsified11403Audited() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(true);  // scheme is https - not supported

        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(1, testAudit.getAuditCount());
        assertTrue(AssertionMessages.SWAGGER_INVALID_SCHEME.getMessage(),
                testAudit.isAuditPresent(AssertionMessages.SWAGGER_INVALID_SCHEME));
    }

    @Test
    public void testValidateWithPathMethodEnabledSchemeDisabled_InvalidRequestScheme_SucceedsNoAudits() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(false); // scheme validation disabled

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(true);  // scheme is https - not supported

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testValidateWithPathMethodSchemeSecurity_BasicAuthentication() throws Exception {
        String requestUri = "/user/test123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getHeaderValues("authorization")).thenReturn(new String[]{OAUTH2_AUTH_TOKEN, BASIC_TOKEN});

        fixture = createServer(assertion);

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testValidateWithPathMethodSchemeSecurity_MissingBasicAuthentication() throws Exception {
        String requestUri = "/user/test123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getHeaderValues("authorization")).thenReturn(new String[]{APIKEY_TOKEN});

        fixture = createServer(assertion);

        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertTrue("Missing audit: " + AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED.getMessage(), testAudit.isAuditPresent(AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED));

    }

    @Test
    public void testValidateWithPathMethodSchemeSecurity_ApiKeyPresent() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getHeaderValues("api_key")).thenReturn(new String[]{APIKEY_TOKEN});

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testValidateWithPathMethodSchemeSecurity_WrongApiKey() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getHeaderValues("authorization")).thenReturn(new String[]{APIKEY_TOKEN});

        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertTrue("Missing audit:" + AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED.getMessage(), testAudit.isAuditPresent(AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED));
    }

    @Test
    public void testValidateWithPathMethodSchemeSecurity_ApiKeyParamPresent() throws Exception {
        String requestUri = "/petParams/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getParameter("api_key")).thenReturn("blah");

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testValidateWithPathMethodSchemeSecurity_NoTokenRequired() throws Exception {
        String requestUri = "/store/order/111111";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(false);

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testValidateWithPathMethodSchemeSecurity_OauthTokenPresent() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.POST);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getHeaderValues("authorization")).thenReturn(new String[]{OAUTH2_AUTH_TOKEN});
        when(mockRequestKnob.getParameterValues("access_token")).thenReturn(new String[]{});


        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testValidateWithPathMethodSchemeSecurity_OauthParamPresent() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.POST);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getParameterValues("access_token")).thenReturn(new String[]{OAUTH2_PARAM_TOKEN});

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testValidateWithPathMethodSchemeSecurity_OauthParamThrowsIOException() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.POST);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getHeaderValues("authorization")).thenReturn(new String[]{OAUTH2_AUTH_TOKEN});
        doThrow(IOException.class).when(mockRequestKnob).getParameterValues("access_token");

        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertTrue("Missing audit:" + AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED.getMessage(), testAudit.isAuditPresent(AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED));
    }

    @Test
    public void testValidateWithPathMethodSchemeSecurity_twoOauth2parameters() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.POST);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getParameterValues("access_token")).thenReturn(new String[]{"fakeoauth2token", OAUTH2_PARAM_TOKEN});


        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertTrue("Missing audit:" + AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED.getMessage(), testAudit.isAuditPresent(AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED));
    }

    @Test
    public void testValidateWithPathMethodSchemeSecurityMultipleOauthTokenPresentFails() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.POST);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getHeaderValues("authorization")).thenReturn(new String[]{OAUTH2_AUTH_TOKEN});
        when(mockRequestKnob.getParameterValues("access_token")).thenReturn(new String[]{OAUTH2_PARAM_TOKEN});

        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertTrue("Missing audit:" + AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED.getMessage(), testAudit.isAuditPresent(AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED));
    }

    @Test
    public void testValidateWithPathMethodSchemeSecurity_MissingRequestToken() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.POST);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getParameterValues("access_token")).thenReturn(new String[]{});

        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertTrue("Missing audit:" + AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED.getMessage(), testAudit.isAuditPresent(AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED));
    }

    @Test
    public void testValidateWithPathMethodSchemeSecurity_WrongRequestToken() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.POST);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getHeaderValues("authorization")).thenReturn(new String[]{BASIC_TOKEN});
        when(mockRequestKnob.getParameterValues("access_token")).thenReturn(new String[]{});

        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertTrue("Missing audit:" + AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED.getMessage(), testAudit.isAuditPresent(AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED));
    }

    @Test
    public void testValidateWithPathMethod_WrongAccessTokenType() throws Exception {
        String requestUri = "/pet/123";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getHeaderValues("authorization")).thenReturn(new String[]{OAUTH2_AUTH_TOKEN});
        when(mockRequestKnob.getParameterValues("access_token")).thenReturn(new String[]{OAUTH2_PARAM_TOKEN});

        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertTrue("Missing audit: " + AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED.getMessage(), testAudit.isAuditPresent(AssertionMessages.SWAGGER_CREDENTIALS_CHECK_FAILED));
    }

    @Test
    public void testValidateSecurityWithMultipleRequirementsOR () throws Exception {
        String requestUri = "/store/inventoryOR";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        // when(mockRequestKnob.getHeaderValues("authorization")).thenReturn(new String[]{APIKEY_TOKEN});
        when(mockRequestKnob.getHeaderValues("authorization")).thenReturn(new String[]{BASIC_TOKEN});

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testValidateSecurityWithMultipleRequirementsAND () throws Exception {
        String requestUri = "/store/inventoryAND";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getParameter("api_key")).thenReturn("blah");
        when(mockRequestKnob.getHeaderValues("authorization")).thenReturn(new String[]{BASIC_TOKEN});

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testValidateSecurityWithMissingSecurityDefinition () throws Exception {
        String requestUri = "/store/inventoryMissingSecurityDefinition";

        HttpRequestKnob mockRequestKnob = Mockito.mock(HttpRequestKnob.class);

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);
        assertion.setValidateScheme(true);
        assertion.setRequireSecurityCredentials(true);

        fixture = createServer(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockRequestKnob.isSecure()).thenReturn(false);
        when(mockRequestKnob.getParameter("api_key")).thenReturn("blah");
        when(mockRequestKnob.getHeaderValues("authorization")).thenReturn(new String[]{BASIC_TOKEN});

        assertFalse(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));
        assertEquals(1, testAudit.getAuditCount());
    }
    // PATH DEFINITION RESOLUTION HELPER CLASS TESTS

    @Test
    public void testPathDefinitionResolver_ValidRequestUriForPathWithNoVariables_PathFound() {
        String requestUri = "/store/order";
        PathDefinition path = testModelPathResolver.getPathForRequestUri(requestUri); // matches path template "/pet/{id}"

        assertNotNull(path);
        assertEquals(requestUri, path.path);
    }

    @Test
    public void testPathDefinitionResolver_ValidRequestUriForPathWithVariables_PathFound() {
        String requestUri = "/pet/1234/uploadImage";
        PathDefinition path = testModelPathResolver.getPathForRequestUri(requestUri); // matches path template "/pet/{id}"

        assertNotNull(path);
        assertEquals("/pet/{petId}/uploadImage", path.path);
        assertEquals(0, testAudit.getAuditCount());
    }

    @Test
    public void testPathDefinitionResolver_UndefinedRequestUri_PathNotFound() {
        PathDefinition path = testModelPathResolver.getPathForRequestUri("/pets"); // erroneous trailing 's'

        assertNull(path);
    }

    @Test
    public void testPathDefinitionResolver_ZeroLengthRequestUri_PathNotFound() {
        PathDefinition path = testModelPathResolver.getPathForRequestUri(""); // zero-length - shouldn't match anything

        assertNull(path);
    }

    @Test
    public void testPathDefinitionResolver_NullRequestUri_PathNotFound() {
        PathDefinition path = testModelPathResolver.getPathForRequestUri(null); // null - shouldn't match anything

        assertNull(path);
    }


    // HELPER METHODS

    private ServerSwaggerAssertion createServer(SwaggerAssertion assertion) {
        ServerSwaggerAssertion serverAssertion = new ServerSwaggerAssertion(assertion);

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                        .put("auditFactory", testAudit.factory())
                        .unmodifiableMap()
        );

        return serverAssertion;
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(Message request) {
        return createPolicyEnforcementContext(request, new Message());
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(Message request, Message response) {
        PublishedService service = new PublishedService();
        service.setRoutingUri("/svr/*");
        service.setSoap(false);

        PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        context.setService(service);

        return context;
    }

    private Message createHttpRequestMessage(String requestUri, String method) throws IOException {
        MockHttpServletRequest hRequest = new MockHttpServletRequest();

        hRequest.setMethod(method);
        hRequest.setRequestURI(requestUri);

        Message request = new Message(stashManager,
                ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(new byte[0]));

        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));

        return request;
    }
}
