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
    public void testValidateWithPathAndMethodEnabled_ValidPathAndMethod_SucceedsNoAudits() throws  Exception {
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
    public void testValidateWithPathAndMethodEnabled_ValidPathAndMethod_Falsified11402Audited() throws  Exception {
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
