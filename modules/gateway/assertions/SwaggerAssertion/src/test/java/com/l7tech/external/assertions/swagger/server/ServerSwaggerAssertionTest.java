package com.l7tech.external.assertions.swagger.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.swagger.SwaggerAssertion;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.SwaggerParser;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static com.l7tech.external.assertions.swagger.server.ServerSwaggerAssertion.PathDefinition;
import static com.l7tech.external.assertions.swagger.server.ServerSwaggerAssertion.PathResolver;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test the SwaggerAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerSwaggerAssertionTest {

    public static final String INVALID_SWAGGER_JSON = "{\"name\":\"value\"}";

    @Mock
    PolicyEnforcementContext mockContext;
    @Mock
    HttpRequestKnob mockRequestKnob;
    Message requestMsg;
    Message responseMsg;

    SwaggerAssertion assertion;
    ServerSwaggerAssertion fixture;

    private static Swagger testModel;
    private static PathResolver testModelPathResolver;

    @BeforeClass
    public static void init() throws Exception {
        SwaggerParser parser = new SwaggerParser();
        List<AuthorizationValue> authorizationValues = new ArrayList<>();
        authorizationValues.add(new AuthorizationValue());

        String swaggerDoc = new String(IOUtils.slurpStream(ServerSwaggerAssertionTest.class
                .getResourceAsStream("petstore_swagger.json")), Charsets.UTF8);
        testModel = parser.parse(swaggerDoc, authorizationValues);
        testModelPathResolver = new PathResolver(testModel);
    }

    @Before
    public void setUp() throws Exception {
        //Setup Context
        requestMsg = new Message();
        requestMsg.attachHttpRequestKnob(mockRequestKnob);
        responseMsg = new Message();
        responseMsg.attachHttpResponseKnob(new HttpResponseKnob() {
            @Override
            public void addChallenge(String value) {

            }

            @Override
            public void setStatus(int code) {

            }

            @Override
            public int getStatus() {
                return 0;
            }
        });


        PublishedService service = new PublishedService();
        service.setRoutingUri("/svr/*");
        service.setSoap(false);
        when(mockContext.getService()).thenReturn(service);

        assertion = new SwaggerAssertion();
    }

    @Test
    public void testInvalidSwagger() throws Exception {
        assertion.setSwaggerDoc("swaggerDoc");
        Map<String,Object> varMap = new HashMap<>();
        varMap.put("swaggerdoc", INVALID_SWAGGER_JSON);
        when(mockContext.getVariableMap(eq(assertion.getVariablesUsed()), any(Audit.class))).thenReturn(varMap);
        when(mockContext.getVariable("swaggerDoc")).thenReturn(INVALID_SWAGGER_JSON);
        fixture = new ServerSwaggerAssertion(assertion);
        assertEquals(AssertionStatus.FAILED, fixture.checkRequest(mockContext));

    }

    @Test
    public void testSwaggerValidRequest_checkBaseUriAndHostContextVariables() throws  Exception {
        String swaggerDoc = new String(IOUtils.slurpStream(ServerSwaggerAssertionTest.class
                .getResourceAsStream("petstore_swagger.json")), Charsets.UTF8);

        assertion.setSwaggerDoc("swaggerDoc");
        Map<String,Object> varMap = new HashMap<>();
        varMap.put("swaggerdoc", swaggerDoc);
        when(mockRequestKnob.getRequestUri()).thenReturn("/svr/pet/findByStatus");
        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockContext.getVariableMap(eq(assertion.getVariablesUsed()), any(Audit.class))).thenReturn(varMap);
        when(mockContext.getVariable("swaggerDoc")).thenReturn(swaggerDoc);
        when(mockContext.getRequest()).thenReturn(requestMsg);
        fixture = new ServerSwaggerAssertion(assertion);
        assertEquals(AssertionStatus.NONE, fixture.checkRequest(mockContext));
        verify(mockContext, times(1)).setVariable("sw.apiUri", "/pet/findByStatus");
        verify(mockContext, times(1)).setVariable("sw.host","petstore.swagger.io");
        verify(mockContext, times(1)).setVariable("sw.baseUri", "/v2");
    }

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

    @Test
    public void test() {
        String requestUri = "/store/order";

        Path path = testModel.getPath(requestUri);

        assertNull(path.getGet());
        assertNotNull(path.getPost());
    }

    @Test
    public void testValidateWithPathAndMethodEnabled_validPathAndMethod_() throws  Exception {
        String requestUri = "/store/order";

        assertion.setValidatePath(true);
        assertion.setValidateMethod(true);

        fixture = new ServerSwaggerAssertion(assertion);

        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.POST);

        assertTrue(fixture.validate(testModel, testModelPathResolver, mockRequestKnob, requestUri));



    }
}
