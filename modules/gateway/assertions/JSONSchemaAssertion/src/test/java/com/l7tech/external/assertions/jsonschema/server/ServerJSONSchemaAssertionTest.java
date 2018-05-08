package com.l7tech.external.assertions.jsonschema.server;

import com.l7tech.common.http.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.jsonschema.JSONSchemaAssertion;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.json.JsonSchemaVersion;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.l7tech.common.mime.ContentTypeHeader.APPLICATION_JSON;
import static com.l7tech.external.assertions.jsonschema.JSONSchemaAssertion.JSON_SCHEMA_FAILURE_VARIABLE;
import static com.l7tech.external.assertions.jsonschema.JSONSchemaAssertion.PARAM_JSON_SCHEMA_VERSION_STRICT;
import static com.l7tech.external.assertions.jsonschema.server.ServerJSONSchemaAssertionTest.*;
import static com.l7tech.json.ConnectionBlockingUrlResolver.MESSAGE_NO_REMOTE_REFERENCES;
import static com.l7tech.json.JsonSchemaVersion.DRAFT_V2;
import static com.l7tech.json.JsonSchemaVersion.DRAFT_V4;
import static com.l7tech.policy.assertion.TargetMessageType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Test the ServerJSONSchemaAssertion.
 */
@PrepareForTest(ServerConfig.class)
@RunWith(PowerMockRunner.class)
public class ServerJSONSchemaAssertionTest {

    @Mock
    private ServerConfig mockServerConfig;
    static final String BEAN_NAME_STASH_MANAGER_FACTORY = "stashManagerFactory";

    static final String BEAN_NAME_SERVERCONFIG = "serverConfig";
    static final String BEAN_NAME_HTTP_CLIENT_FACTORY = "httpClientFactory";
    private static final String VARIABLE_NAME_TARGETVAR = "targetvar";

    private static final String VARIABLE_NAME_SCHEMAHOST = "schemahost";
    private static final String VARIABLE_NAME_STRING_VARIABLE = "STRING_VARIABLE";
    private static final String VARIABLE_NAME_JSON_SCHEMA = "JSON_SCHEMA";
    private static final String VARIABLE_NAME_JSON_DATA = "JSON_DATA";
    private static final String PROPERTY_NAME_JSON_SCHEMA_CACHE = "SCHEMA_OBJ_CACHE";

    private static final String HOSTNAME_IMAURL_COM = "imaurl.com";

    private static final String URL_IMAURL_COM = "http://" + HOSTNAME_IMAURL_COM;
    private static final String URL_RESOURCE_NOMATCH = "http://nomatch.*";
    private static final String URL_CONTEXT_VAR_SCHEMAHOST = "http://${schemahost}";
    private static final String URL_WITH_404 = "http://imaurlwith404.com";
    private static final String URL_WITH_INVALID_SCHEMA = "http://imaurlwithinvalidschema.com";
    private static final String URL_RESOURCE_HTTP_STAR = "http://.*";
    private static final String URL_WONTMATCH = "http://wontmatch.*";
    private static final String LINK_HEADER_TEMPLATE_SCHEMA = "<http://irishman:8080/templateschema>;rel=\"describedby\"";

    private static final ContentTypeHeader CONTENT_TYPE_JSON_PROFILE_TESTURL = ContentTypeHeader.create("application/json;profile=http://testurl.com");
    private static final String VARIABLE_NAME_JSONSCHEMA_FAILURE = "jsonschema.failure";
    private static final String VALID_JSON_SCHEMA_V2 = "{\n" +
            "   \"type\":\"object\",\n" +
            "   \"properties\": {\n" +
            "\t\t\"reportType\": {\"type\":\"string\"},\n" +
            "\t\t\"entityType\": {\"type\":\"string\"},\n" +
            "\t\t\"isIgnoringPagination\": {\"type\":\"boolean\"},\n" +
            "\t\t\"numberOfPages\": {\"type\":\"integer\", \"divisibleBy\":7},\n" +
            "\t\t\"entities\": {\n" +
            "\t\t\t\"type\":\"array\",\n" +
            "\t\t\t\"items\": {\n" +
            "\t\t\t\t\"type\":\"object\",\n" +
            "\t\t\t\t\"properties\":{\"clusterId\":{\"type\":\"string\"}},\n" +
            "\t\t\t\t\"additionalProperties\":false\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t\"summaryChart\": {\"type\":\"boolean\"},\n" +
            "\t\t\"summaryReport\": {\"type\":\"boolean\"},\n" +
            "\t\t\"reportName\": {\"type\":\"string\"}\n" +
            "\t},\n" +
            "\t\"additionalProperties\":false\n" +
            "}";

    private static final String VALID_JSON_SCHEMA_V4 = "{\n" +
            "   \"type\":\"object\",\n" +
            "   \"properties\": {\n" +
            "\t\t\"reportType\": {\"type\":\"string\"},\n" +
            "\t\t\"entityType\": {\"type\":\"string\"},\n" +
            "\t\t\"isIgnoringPagination\": {\"type\":\"boolean\"},\n" +
            "\t\t\"numberOfPages\": {\"type\":\"integer\", \"multipleOf\":7},\n" +
            "\t\t\"entities\": {\n" +
            "\t\t\t\"type\":\"array\",\n" +
            "\t\t\t\"items\": {\n" +
            "\t\t\t\t\"type\":\"object\",\n" +
            "\t\t\t\t\"properties\":{\"clusterId\":{\"type\":\"string\"}},\n" +
            "\t\t\t\t\"additionalProperties\":false\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t\"summaryChart\": {\"type\":\"boolean\"},\n" +
            "\t\t\"summaryReport\": {\"type\":\"boolean\"},\n" +
            "\t\t\"reportName\": {\"type\":\"string\"}\n" +
            "\t},\n" +
            "\t\"additionalProperties\":false\n" +
            "}";

    private static final String VALID_JSON_SCHEMA_V2_UNDEFINED_SCHEMA =
            "{\n" +
                    "\"type\":\"integer\", " +
                    "\"divisibleBy\":2" +
                    "}";

    private static final String VALID_JSON_SCHEMA_V2_VALID_V2_SCHEMA =
            "{\n" +
                    "    \"$schema\": \"http://json-schema.org/draft-02/schema#\", " +
                    "    \"type\":\"integer\", " +
                    "    \"divisibleBy\":2" +
                    "}";

    private static final String VALID_JSON_SCHEMA_V2_UNKNOWN_SCHEMA =
            "{\n" +
                    "    \"$schema\": \"hattap:a//jsaon-ascheama.aorag/adraaft-0a2/sacheamaa#\", " +
                    "    \"type\":\"integer\", " +
                    "    \"divisibleBy\":2" +
                    "}";

    private static final String VALID_JSON_SCHEMA_V4_UNDEFINED_SCHEMA =
            "{\n" +
                    "    \"oneOf\": [" +
                    "        {\"type\":\"number\"},\n" +
                    "        {\"type\":\"integer\"}\n" +
                    "    ]\n" +
                    "}";

    private static final String VALID_JSON_SCHEMA_V4_VALID_V4_SCHEMA =
            "{\n" +
                    "    \"$schema\": \"http://json-schema.org/draft-04/schema#\", " +
                    "    \"oneOf\": [" +
                    "        {\"type\":\"number\"},\n" +
                    "        {\"type\":\"integer\"}\n" +
                    "    ]\n" +
                    "}";

    private static final String VALID_JSON_SCHEMA_V4_UNKNOWN_SCHEMA =
            "{\n" +
                    "    \"$schema\": \"htsdftp://json-schesdfsdfma.org/draft-01234/schemsdfa#\", " +
                    "    \"oneOf\": [" +
                    "        {\"type\":\"number\"},\n" +
                    "        {\"type\":\"integer\"}\n" +
                    "    ]\n" +
                    "}";

    private static final String VALID_JSON_DATA_TO_V2_BUT_NOT_V4 =
            "100";

    private static final String VALID_JSON_DATA = "{\"reportType\":\"performance\",\n" +
            "   \"entityType\" : \"publishedService\",\n" +
            "   \"isIgnoringPagination\" : true, \n" +
            "   \"numberOfPages\" : 21, \n" +
            "   \"entities\" : [{\"clusterId\":\"cluster1\"},{\"clusterId\":\"cluster2\"}],\n" +
            "   \"summaryChart\" : true,\n" +
            "   \"summaryReport\" : false,\n" +
            "   \"reportName\" : \"My Report\"\n" +
            "}";

    private static final String INVALID_JSON_SCHEMA = "{\n" +
            "   \"type\":\"object\",\n" +
            "   \"properties\": {\n" +
            "\t\t\"reportType\": {\"type\":\"string\"},\n" +
            "\t\t\"entityType\": {\"type\":\"string\"},\n" +
            "\t\t\"isIgnoringPagination\": {\"type\":\"boolean\"},\n" +
            "\t\t\"entities\": {\n" +
            "\t\t\t\"type\":\"array\",\n" +
            "\t\t\t\"items\": {\n" +
            "\t\t\t\t\"type\":\"object\",\n" +
            "\t\t\t\t\"properties\":{\"clusterId\":{\"type\":\"string\"}},\n" +
            "\t\t\t\t\"additionalProperties\":false\n" +
            "\t\t\t}\n" +
            "\t\t},\n" +
            "\t\t\"summaryChart\": {\"type\":\"boolean\"},\n" +
            "\t\t\"summaryReport\": {\"type\":\"boolean\"},\n" +
            "\t\t\"reportName\": {\"type\":\"string\"}\n" +
            "\t},\n" +
            "\t\"additionalProperties\":false\n" +
            "";

    private static final String INVALID_JSON_DATA = "{\"reportType\":\"performance\",\n" +
            "   \"entityType1\" : \"publishedService\",\n" +
            "   \"isIgnoringPagination\" : true, \n" +
            "   \"numberOfPages\" : 3, \n" +
            "   \"entities\" : [{\"clusterId\":\"cluster1\"},{\"clusterId\":\"cluster2\"}],\n" +
            "   \"summaryChart2\" : true,\n" +
            "   \"summaryReport\" : false,\n" +
            "   \"reportName\" : \"My Report\"\n" +
            "}";

    private static final String INVALID_JSON_DATA_BAD_NUMBER_OF_PAGES = "{\"reportType\":\"performance\",\n" +
            "   \"entityType\" : \"publishedService\",\n" +
            "   \"isIgnoringPagination\" : true, \n" +
            "   \"numberOfPages\" : 11, \n" +
            "   \"entities\" : [{\"clusterId\":\"cluster1\"},{\"clusterId\":\"cluster2\"}],\n" +
            "   \"summaryChart\" : true,\n" +
            "   \"summaryReport\" : false,\n" +
            "   \"reportName\" : \"My Report\"\n" +
            "}";

    private static final String JSON_SCHEMA_WITH_REMOTE_REFERENCES = "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"residential_address\": { \"$ref\": \"http://apim-httpd-tls.l7tech.com/json/jsonSchemaV4/json_schema_address_v4.txt\" },\n" +
            "    \"office_address\": { \"$ref\": \"http://apim-httpd-tls.l7tech.com/json/jsonSchemaV4/json_schema_address_v4.txt\" }\n" +
            "  }\n" +
            "}";

    private static final String JSON_SCHEMA_WITH_REMOTE_REFERENCES_2 = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
            "  \"type\": \"object\",\n" +
            "\n" +
            "  \"properties\": {\n" +
            "    \"billing_address\": { \"$ref\": \"http://localhost:8000/address.json#/definitions/address\" },\n" +
            "    \"shipping_address\": {\n" +
            "      \"allOf\": [\n" +
            "        { \"$ref\": \"http://localhost:8000/address.json#/definitions/address\" },\n" +
            "        { \"properties\":\n" +
            "          { \"type\": { \"enum\": [ \"residential\", \"business\" ] } },\n" +
            "          \"required\": [\"type\"]\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";
    private static final String JSON_DATA_SHIPPING_ADDRESS_VALID = "{\n" +
            "  \"shipping_address\": {\n" +
            "    \"street_address\": \"1600 Pennsylvania Avenue NW\",\n" +
            "    \"city\": \"Washington\",\n" +
            "    \"state\": \"DC\",\n" +
            "    \"type\": \"business\"\n" +
            "  }\n" +
            "}";
    private static final String JSON_DATA_SHIPPING_ADDRESS_INVALID = "{\n" +
            "  \"shipping_address\": {\n" +
            "    \"street_address\": \"1600 Pennsylvania Avenue NW\",\n" +
            "    \"city\": \"Washington\",\n" +
            "    \"state\": \"DC\"\n" +
            "  }\n" +
            "}";
    private static final String JSON_SCHEMA_ADDRESSES = "{\n" +
            "  \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
            "\n" +
            "  \"definitions\": {\n" +
            "    \"address\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"properties\": {\n" +
            "        \"street_address\": { \"type\": \"string\" },\n" +
            "        \"city\":           { \"type\": \"string\" },\n" +
            "        \"state\":          { \"type\": \"string\" }\n" +
            "      },\n" +
            "      \"required\": [\"street_address\", \"city\", \"state\"]\n" +
            "    }\n" +
            "  },\n" +
            "\n" +
            "  \"type\": \"object\",\n" +
            "\n" +
            "  \"properties\": {\n" +
            "    \"billing_address\": { \"$ref\": \"#/definitions/address\" },\n" +
            "    \"shipping_address\": {\n" +
            "      \"allOf\": [\n" +
            "        { \"$ref\": \"#/definitions/address\" },\n" +
            "        { \"properties\":\n" +
            "          { \"type\": { \"enum\": [ \"residential\", \"business\" ] } },\n" +
            "          \"required\": [\"type\"]\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Before
    public void clearStaticFields() throws NoSuchFieldException, IllegalAccessException {
        final Field field = ServerJSONSchemaAssertion.class.getDeclaredField(PROPERTY_NAME_JSON_SCHEMA_CACHE);
        field.setAccessible(true);
        ((ConcurrentMap) field.get(null)).clear();
    }

    @Test
    public void v2RequestWithValidV2InstanceDocumentShouldValidate() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withTarget(REQUEST).withStaticResource(VALID_JSON_SCHEMA_V2)
                .build();
        testRequest_ValidInstanceDocument(assertion);
    }

    @Test
    public void v4RequestWithValidV4InstanceDocumentShouldValidate() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withTarget(REQUEST).withStaticResource(VALID_JSON_SCHEMA_V4)
                .build();
        testRequest_ValidInstanceDocument(assertion);
    }

    private void testRequest_ValidInstanceDocument(JSONSchemaAssertion assertion) throws PolicyAssertionException, IOException {
        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(VALID_JSON_DATA)
                        .build())
                .build();

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertNoValidationFailure(pec);
    }

    @Test
    public void v2RequestUsingOneOfKeyAndDataMatchingAllSchemasShouldValidate() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withTarget(REQUEST).withStaticResource(VALID_JSON_SCHEMA_V4_UNDEFINED_SCHEMA)
                .build();

        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(VALID_JSON_DATA_TO_V2_BUT_NOT_V4)
                        .build())
                .build();

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertNoValidationFailure(pec);
    }

    @Test
    public void v4RequestUsingOneOfKeyAndDataMatchingAllSchemasShouldFail() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withTarget(REQUEST).withStaticResource(VALID_JSON_SCHEMA_V4_UNDEFINED_SCHEMA)
                .build();

        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(VALID_JSON_DATA_TO_V2_BUT_NOT_V4)
                        .build())
                .build();

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);
    }

    /*
     * Should FAIL because non-singleton multivalue cannot be converted to Message, even though first value is valid
     */
    @Test
    @BugNumber(12148)
    public void v2NonSingletonArrayTargetWithFirstValueValidShouldFail() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{VALID_JSON_DATA, "blarg", "blah"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void v4NonSingletonArrayTargetWithFirstValueValidShouldFail() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{VALID_JSON_DATA, "blarg", "blah"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    /*
     * Should FAIL because non-singleton multivalue cannot be converted to Message, even though all values are valid
     */
    @Test
    @BugNumber(12148)
    public void v2NonSingletonArrayTargetWithAllValuesValidShouldFail() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{VALID_JSON_DATA, VALID_JSON_DATA})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void v4NonSingletonArrayTargetWithAllValuesValidShouldFail() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{VALID_JSON_DATA, VALID_JSON_DATA})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    /*
     * Should PASS because singleton-CharSequence multivalue CAN be converted to a Message
     */
    @Test
    @BugNumber(12148)
    public void v2SingletonCharSequeneceTargetShouldValidate() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new Object[]{asCharSeq(VALID_JSON_DATA)})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void v4SingletonCharSequeneceTargetShouldValidate() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new Object[]{asCharSeq(VALID_JSON_DATA)})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    /*
     * hould FAIL because empty multivalue cannot be converted to Message
     */
    @Test
    @BugNumber(12148)
    public void v2EmptyArrayTargetShouldFail() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void v4EmptyArrayTargetShouldFail() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    /*
     * Should FAIL because non-singleton multivalue cannot be converted to Message, even though first value is valid
     */
    @Test
    @BugNumber(12148)
    public void v2NonSingletonListTargetWithFirstValueValidShouldFail() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, Arrays.asList(VALID_JSON_DATA, "blarg", "blah"))
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    /*
     * Should FAIL because non-singleton multivalue cannot be converted to Message, even though all values are valid
     */
    @Test
    @BugNumber(12148)
    public void v2NonSingletonListTargetWithAllValuesValidShouldFail() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, Arrays.asList(VALID_JSON_DATA, VALID_JSON_DATA))
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    /*
     * Should PASS because singleton-CharSequence multivalue CAN be converted to a Message
     */
    @Test
    @BugNumber(12148)
    public void v2SingletonListCharSequenceTargetShouldValidate() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, Collections.singletonList(asCharSeq(VALID_JSON_DATA)))
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    /*
     * Should FAIL because empty multivalue cannot be converted to Message
     */
    @Test
    @BugNumber(12148)
    public void v2EmptyListTargetShouldFail() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, Collections.emptyList())
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    /*
     * Should PASS because nested-singleton-CharSequence multivalue CAN be converted to a Message
     */
    @Test
    @BugNumber(12148)
    public void v2ListOfArrayOfListTargetWithSingleValueShouldValidate() throws Exception {
        final List<CharSequence> list = Collections.singletonList(asCharSeq(VALID_JSON_DATA));
        final Object[] arrayOfList = {list};
        final List<Object> listOfArrayOfList = Arrays.asList(arrayOfList);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, listOfArrayOfList)
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    /*
     * Should FAIL because value of some random class CANNOT be converted to Message (even if it happens to have overridden toString() to produce valid data)
     */
    @Test
    @BugNumber(12148)
    public void v2UnknownClassTargetShouldFail() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new Object() {
                    @Override
                    public String toString() {
                        return VALID_JSON_DATA;
                    }
                })
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    private void assertNoValidationFailure(PolicyEnforcementContext pec) {
        NoSuchVariableException expected = null;
        Object[] errors = null;
        try {
            errors = (Object[]) pec.getVariable(VARIABLE_NAME_JSONSCHEMA_FAILURE);
        } catch (NoSuchVariableException e) {
            expected = e;
        }
        assertNotNull("Unexpected errors: " + Arrays.toString(errors), expected);
    }

    // Wrap any CharSequence type in a wrapper that prevents downcasting it below CharSequence itself,
    // to verify that all using code will work with any old CharSequence (not just String or whatever).
    private static CharSequence asCharSeq(final CharSequence in) {
        return new CharSequence() {
            @Override
            public int length() {
                return in.length();
            }

            @Override
            public char charAt(int index) {
                return in.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return in.subSequence(start, end);
            }

            @NotNull
            @Override
            public String toString() {
                return in.toString();
            }
        };
    }

    @Test
    public void v2RequestStaticResourceWithMessageShouldValidate() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withStaticResource("${" + VARIABLE_NAME_JSON_SCHEMA + "}").withTarget(REQUEST)
                .build();
        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(VALID_JSON_DATA)
                        .build())
                .build();
        pec.setVariable(VARIABLE_NAME_JSON_SCHEMA, VALID_JSON_SCHEMA_V2);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void v2ResponseWithValidInstanceDocumentShouldValidate() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withStaticResource(VALID_JSON_SCHEMA_V2).withTarget(RESPONSE)
                .build();
        final ApplicationContext context = new AppContextBuilder().build();
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = new PECBuilder()
                .withResponse(new ResponseBuilder(context, APPLICATION_JSON)
                        .withBody(VALID_JSON_DATA)
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void v2StringMessageVariableWithValidInstanceDocumentShouldValidate() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2)
                .withStaticResource(VALID_JSON_SCHEMA_V2).withTarget(OTHER)
                .withOtherTargetMessageVariable(VARIABLE_NAME_STRING_VARIABLE)
                .build();
        final ApplicationContext context = new AppContextBuilder().build();
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON).build())
                .withVariable(VARIABLE_NAME_STRING_VARIABLE, VALID_JSON_DATA)
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void v4NonSingletonArrayTargetWithEmptyStringShouldFail() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{""})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4_VALID_V4_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void v4NonSingletonArrayTargetWithNullShouldFail() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, null)
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4_VALID_V4_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void v2ConfigAndSchemaUndefinedShouldValidateAsV2() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"100.1"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2_UNDEFINED_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void v2ConfigAndSchemaV2ShouldValidateAsV2() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"3"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2_VALID_V2_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void v2ConfigAndSchemaV4ShouldValidateAsV2() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"3"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4_VALID_V4_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void v2ConfigAndSchemaUnknownShouldValidateAsV2() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"3"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2_UNKNOWN_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void v4ConfigAndSchemaUndefinedShouldValidateAsV4() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"100"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4_VALID_V4_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void v4ConfigAndSchemaV2ShouldValidateAsV4() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"101"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2_VALID_V2_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void v4ConfigAndSchemaV4ShouldValidateAsV4() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"100"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4_VALID_V4_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void v4ConfigAndSchemaUnknownShouldValidateAsV4() throws Exception {
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"100"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4_UNKNOWN_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder().build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void strictV4ConfigAndSchemaUndefinedShouldValidateAsV4() throws Exception {
        PowerMockito.mockStatic(ServerConfig.class);
        when(ServerConfig.getInstance()).thenReturn(mockServerConfig);
        when(mockServerConfig.getBooleanProperty(PARAM_JSON_SCHEMA_VERSION_STRICT, false)).thenReturn(true);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"100"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4_UNDEFINED_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder()
                        .withServerConfig(mockServerConfig)
                        .build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void strictV4ConfigAndSchemaV2ShouldFail() throws Exception {
        PowerMockito.mockStatic(ServerConfig.class);
        when(ServerConfig.getInstance()).thenReturn(mockServerConfig);
        when(mockServerConfig.getBooleanProperty(PARAM_JSON_SCHEMA_VERSION_STRICT, false)).thenReturn(true);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"100"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2_VALID_V2_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder()
                        .withServerConfig(mockServerConfig)
                        .build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void strictV4ConfigAndSchemaV4ShouldValidateAsV4() throws Exception {
        PowerMockito.mockStatic(ServerConfig.class);
        when(ServerConfig.getInstance()).thenReturn(mockServerConfig);
        when(mockServerConfig.getBooleanProperty(PARAM_JSON_SCHEMA_VERSION_STRICT, false)).thenReturn(true);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"101"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4_VALID_V4_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder()
                        .withServerConfig(mockServerConfig)
                        .build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void strictV4ConfigAndSchemaUnknownShouldFail() throws Exception {
        PowerMockito.mockStatic(ServerConfig.class);
        when(ServerConfig.getInstance()).thenReturn(mockServerConfig);
        when(mockServerConfig.getBooleanProperty(PARAM_JSON_SCHEMA_VERSION_STRICT, false)).thenReturn(true);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"101.1"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V4)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4_UNKNOWN_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder()
                        .withServerConfig(mockServerConfig)
                        .build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void strictV2ConfigAndSchemaUndefinedShouldValidateAsV2() throws Exception {
        PowerMockito.mockStatic(ServerConfig.class);
        when(ServerConfig.getInstance()).thenReturn(mockServerConfig);
        when(mockServerConfig.getBooleanProperty(PARAM_JSON_SCHEMA_VERSION_STRICT, false)).thenReturn(true);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"101"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2_UNDEFINED_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder()
                        .withServerConfig(mockServerConfig)
                        .build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void strictV2ConfigAndSchemaV2ShouldValidateAsV2() throws Exception {
        PowerMockito.mockStatic(ServerConfig.class);
        when(ServerConfig.getInstance()).thenReturn(mockServerConfig);
        when(mockServerConfig.getBooleanProperty(PARAM_JSON_SCHEMA_VERSION_STRICT, false)).thenReturn(true);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"101"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2_VALID_V2_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder()
                        .withServerConfig(mockServerConfig)
                        .build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void strictV2ConfigAndSchemaV4ShouldFail() throws Exception {
        PowerMockito.mockStatic(ServerConfig.class);
        when(ServerConfig.getInstance()).thenReturn(mockServerConfig);
        when(mockServerConfig.getBooleanProperty(PARAM_JSON_SCHEMA_VERSION_STRICT, false)).thenReturn(true);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"101"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V4_VALID_V4_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder()
                        .withServerConfig(mockServerConfig)
                        .build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void strictV2ConfigAndSchemaUnknownShouldFail() throws Exception {
        PowerMockito.mockStatic(ServerConfig.class);
        when(ServerConfig.getInstance()).thenReturn(mockServerConfig);
        when(mockServerConfig.getBooleanProperty(PARAM_JSON_SCHEMA_VERSION_STRICT, false)).thenReturn(true);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new Message()).withResponse(new Message())
                .withVariable(VARIABLE_NAME_TARGETVAR, new String[]{"100"})
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(
                new JSONSchemaAssertionBuilder()
                        .withVersion(DRAFT_V2)
                        .withTarget(OTHER)
                        .withStaticResource(VALID_JSON_SCHEMA_V2_UNKNOWN_SCHEMA)
                        .withOtherTargetMessageVariable(VARIABLE_NAME_TARGETVAR)
                        .build(),
                new AppContextBuilder()
                        .withServerConfig(mockServerConfig)
                        .build());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void monitorUrlChangeSchemaVersion() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withUrlResource(URL_IMAURL_COM).withTarget(REQUEST)
                .build();
        ApplicationContext context = new AppContextBuilder().withJsonSchema(null, VALID_JSON_SCHEMA_V4_VALID_V4_SCHEMA).build();
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody("100")
                        .build())
                .build();
        AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);

        assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withUrlResource(URL_IMAURL_COM).withTarget(REQUEST)
                .build();
        context = new AppContextBuilder().withJsonSchema(null, VALID_JSON_SCHEMA_V4_VALID_V4_SCHEMA).build();
        serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody("100")
                        .build())
                .build();
        assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);

    }


    @Test
    public void urlRetrieveFromLinkHeaderChangeSchemaVersion() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2)
                .withMessageUrlResource(URL_RESOURCE_HTTP_STAR) // this matches the url set up in getContext
                .withTarget(REQUEST)
                .build();

        final ApplicationContext context = new AppContextBuilder().withJsonSchema(null,
                VALID_JSON_SCHEMA_V2_VALID_V2_SCHEMA).build();
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new ResponseBuilder(context, APPLICATION_JSON)
                        .withLinkHeader(LINK_HEADER_TEMPLATE_SCHEMA)
                        .withBody("3")
                        .build())
                .build();
        AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);

        assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4)
                .withMessageUrlResource(URL_RESOURCE_HTTP_STAR) // this matches the url set up in getContext
                .withTarget(REQUEST)
                .build();

        serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        pec = new PECBuilder()
                .withRequest(new ResponseBuilder(context, APPLICATION_JSON)
                        .withLinkHeader(LINK_HEADER_TEMPLATE_SCHEMA)
                        .withBody("3")
                        .build())
                .build();
        assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void urlFromLinkHeaderV4ConfigUndefinedSchemaVersion() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4)
                .withMessageUrlResource(URL_RESOURCE_HTTP_STAR) // this matches the url set up in getContext
                .withTarget(REQUEST)
                .build();

        final ApplicationContext context = new AppContextBuilder()
                .withJsonSchema(null, VALID_JSON_SCHEMA_V4_UNDEFINED_SCHEMA).build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new ResponseBuilder(context, APPLICATION_JSON)
                        .withLinkHeader(LINK_HEADER_TEMPLATE_SCHEMA)
                        .withBody("3")
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);
    }

    @Test
    public void urlFromLinkHeaderV4ConfigUnknownSchemaVersion() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4)
                .withMessageUrlResource(URL_RESOURCE_HTTP_STAR) // this matches the url set up in getContext
                .withTarget(REQUEST)
                .build();

        final ApplicationContext context = new AppContextBuilder()
                .withJsonSchema(null, VALID_JSON_SCHEMA_V4_UNKNOWN_SCHEMA).build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new ResponseBuilder(context, APPLICATION_JSON)
                        .withLinkHeader(LINK_HEADER_TEMPLATE_SCHEMA)
                        .withBody("3")
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);
    }

    @Test
    public void testJsonMessageVariable_ValidInstanceDocument() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withStaticResource(VALID_JSON_SCHEMA_V2).withTarget(OTHER)
                .withOtherTargetMessageVariable(VARIABLE_NAME_STRING_VARIABLE)
                .build();
        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final Message message = new Message(
                new StashManagerBuilder(context).build(),
                APPLICATION_JSON,
                new ByteArrayInputStream(VALID_JSON_DATA.getBytes()));
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON).build())
                .withVariable(VARIABLE_NAME_STRING_VARIABLE, message)
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testRequest_InvalidInstanceDocument() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withStaticResource(VALID_JSON_SCHEMA_V2).withTarget(REQUEST)
                .build();
        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(INVALID_JSON_DATA)
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);
    }

    @Test
    public void testResponse_InvalidInstanceDocument() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withStaticResource(VALID_JSON_SCHEMA_V2).withTarget(RESPONSE)
                .build();
        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withResponse(new ResponseBuilder(context, APPLICATION_JSON)
                        .withBody(INVALID_JSON_DATA)
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.BAD_RESPONSE, assertionStatus);
    }

    @Test
    public void testStringMessageVariable_InvalidInstanceDocument() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withStaticResource(VALID_JSON_SCHEMA_V2).withTarget(OTHER)
                .withOtherTargetMessageVariable(VARIABLE_NAME_STRING_VARIABLE)
                .build();
        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON).build())
                .withVariable(VARIABLE_NAME_STRING_VARIABLE, INVALID_JSON_DATA)
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void testJsonMessageVariable_InvalidInstanceDocument() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withStaticResource(VALID_JSON_SCHEMA_V2).withTarget(OTHER)
                .withOtherTargetMessageVariable(VARIABLE_NAME_STRING_VARIABLE)
                .build();
        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);

        final Message message = new Message(
                new StashManagerBuilder(context).build(),
                APPLICATION_JSON,
                new ByteArrayInputStream(INVALID_JSON_DATA.getBytes()));

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON).build())
                .withVariable(VARIABLE_NAME_STRING_VARIABLE, message)
                .build();

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    // -  MONITOR URL RESOURCE

    @Test
    public void testMonitorUrl() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withUrlResource(URL_IMAURL_COM).withTarget(REQUEST)
                .build();
        final ApplicationContext context = new AppContextBuilder().withJsonSchema(null, VALID_JSON_SCHEMA_V2).build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(VALID_JSON_DATA)
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testMonitorUrlWithContextVariables() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withUrlResource(URL_CONTEXT_VAR_SCHEMAHOST).withTarget(REQUEST)
                .build();
        final ApplicationContext context = new AppContextBuilder()
                .withJsonSchema(URL_IMAURL_COM, VALID_JSON_SCHEMA_V2)
                .build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(VALID_JSON_DATA)
                        .build())
                .withVariable(VARIABLE_NAME_SCHEMAHOST, HOSTNAME_IMAURL_COM)
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void v2ValidContextVariableBackedJsonDataShouldValidate() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withTarget(OTHER).withOtherTargetMessageVariable("jsonData").withVersion(DRAFT_V2)
                .withStaticResource(VALID_JSON_SCHEMA_V2)
                .build();
        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withVariable("jsonData", INVALID_JSON_DATA)
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        final Object[] validationErrors = (Object[]) pec.getVariable(VARIABLE_NAME_JSONSCHEMA_FAILURE);
        assertEquals(5, validationErrors.length);
    }

    @Test
    public void v4ValidContextVariableBackedJsonDataShouldValidate() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withTarget(OTHER).withOtherTargetMessageVariable("jsonData").withVersion(DRAFT_V4)
                .withStaticResource(VALID_JSON_SCHEMA_V4)
                .build();
        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withVariable("jsonData", INVALID_JSON_DATA)
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.FAILED, assertionStatus);
        final Object[] validationErrors = (Object[]) pec.getVariable(VARIABLE_NAME_JSONSCHEMA_FAILURE);
        assertEquals(3, validationErrors.length);
    }

    @Test
    public void testMonitorUrl_InvalidInstanceDocument() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withUrlResource(URL_IMAURL_COM).withTarget(REQUEST)
                .build();
        final ApplicationContext context = new AppContextBuilder().withJsonSchema(null, VALID_JSON_SCHEMA_V2).build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(INVALID_JSON_DATA)
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);
    }

    /**
     * Logs should contain the correct audit code: 9134. This is an expected error when a url returns status 404 (or 500 etc)
     *
     * @throws Exception
     */
    @Test
    public void testMonitorUrl_InvalidUrl() throws Exception {
        final ApplicationContext context = new AppContextBuilder().withJsonSchema(null, "").build();
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withUrlResource(URL_WITH_404).withTarget(REQUEST)
                .build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON).build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.SERVER_ERROR, assertionStatus);
    }

    /**
     * Logs should contain the correct audit code: 9134. This is an expected error when a url provides invalid json data
     *
     * @throws Exception
     */
    @Test
    public void testMonitorUrl_InvalidRuntimeSchema() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withUrlResource(URL_WITH_INVALID_SCHEMA).withTarget(REQUEST)
                .build();
        final ApplicationContext context = new AppContextBuilder().withJsonSchema(null, INVALID_JSON_SCHEMA).build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(VALID_JSON_DATA)
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.SERVER_ERROR, assertionStatus);
    }

    // - RETRIEVE URL FROM HEADER

    @Test
    public void testUrlRetrieveFromMimeParameter() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withMessageUrlResource(URL_RESOURCE_HTTP_STAR).withTarget(REQUEST)
                .build();
        final ApplicationContext context = new AppContextBuilder().withJsonSchema(null, VALID_JSON_SCHEMA_V2).build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, CONTENT_TYPE_JSON_PROFILE_TESTURL)
                        .withBody(VALID_JSON_DATA)
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testUrlRetrieveFromMimeParameter_NoWhiteListMatch() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withMessageUrlResource(URL_WONTMATCH).withTarget(REQUEST)
                .build();

        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, CONTENT_TYPE_JSON_PROFILE_TESTURL)
                        .withBody(VALID_JSON_DATA)
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.SERVER_ERROR, assertionStatus);
    }

    @Test
    public void testUrlRetrieveFromLinkHeader() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withMessageUrlResource(URL_RESOURCE_HTTP_STAR) // this matches the url set up in getContext
                .withTarget(REQUEST)
                .build();

        final ApplicationContext context = new AppContextBuilder().withJsonSchema(null, VALID_JSON_SCHEMA_V2).build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new ResponseBuilder(context, APPLICATION_JSON)
                        .withLinkHeader(LINK_HEADER_TEMPLATE_SCHEMA)
                        .withBody(VALID_JSON_DATA)
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testUrlRetrieveFromLinkHeader_Response() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withMessageUrlResource(URL_RESOURCE_HTTP_STAR)
                .withTarget(RESPONSE)
                .build();

        final ApplicationContext context = new AppContextBuilder().withJsonSchema(null, VALID_JSON_SCHEMA_V2).build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withResponse(new ResponseBuilder(context, APPLICATION_JSON)
                        .withLinkHeader(LINK_HEADER_TEMPLATE_SCHEMA)
                        .withBody(VALID_JSON_DATA)
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testUrlRetrieveFromLinkHeader_NoWhiteListMatch() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withMessageUrlResource(URL_RESOURCE_NOMATCH).withTarget(REQUEST)
                .build();

        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(VALID_JSON_DATA)
                        .withLinkHeader(LINK_HEADER_TEMPLATE_SCHEMA)
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.SERVER_ERROR, assertionStatus);
    }

    @Test
    public void testUrlRetrieveFromLinkHeader_NoURLFound() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withMessageUrlResource(URL_RESOURCE_HTTP_STAR).withTarget(REQUEST)
                .build();

        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(VALID_JSON_DATA)
                        .build())
                .build();
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.SERVER_ERROR, assertionStatus);
    }

    @Test
    public void testUrlRetrieveFromLinkHeader_NoURLFoundAllowed() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withTarget(REQUEST)
                .withResourceInfo(new MessageUrlResourceInfoBuilder()
                        .withUrl(URL_RESOURCE_HTTP_STAR).withAllowMessagesWithoutUrl(true)
                        .build())
                .build();

        final ApplicationContext context = new AppContextBuilder().build();
        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(VALID_JSON_DATA)
                        .build())
                .build();

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testSimpleV4Validation() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withTarget(REQUEST)
                .withStaticResource(VALID_JSON_SCHEMA_V4)
                .build();

        final ApplicationContext context = new AppContextBuilder().build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(INVALID_JSON_DATA_BAD_NUMBER_OF_PAGES)
                        .build())
                .build();

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);

        @SuppressWarnings("unchecked")
        final Object[] errors = (Object[]) pec.getVariable(VARIABLE_NAME_JSONSCHEMA_FAILURE);
        assertEquals("Unexpected number of errors", 1, errors.length);

        @SuppressWarnings("unchecked")
        final String error = (String) errors[0];
        assertEquals("Unexpected error message", "$.numberOfPages: must be multiple of 7.0", error);
    }

    @Test
    public void testPatternSupportedInV4() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withTarget(REQUEST)
                .withStaticResource(
                        "{\n" +
                        "   \"type\": \"string\",\n" +
                        "   \"pattern\": \"^(\\\\([0-9]{3}\\\\))?[0-9]{3}-[0-9]{4}$\"\n" +
                        "}")
                .build();

        final ApplicationContext context = new AppContextBuilder().build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);

        // Valid input
        {
            final PolicyEnforcementContext pec = new PECBuilder()
                    .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                            .withBody("\"555-1212\"")
                            .build())
                    .build();

            final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
            assertEquals(AssertionStatus.NONE, assertionStatus);
        }
        // Invalid input
        {
            final PolicyEnforcementContext pec = new PECBuilder()
                    .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                            .withBody("\"(800)FLOWERS\"")
                            .build())
                    .build();

            final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
            assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);
        }
    }

    @Test
    public void testDateTimeSupportedInV4() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withTarget(REQUEST)
                .withStaticResource(
                        "{\n" +
                        "   \"type\": \"string\",\n" +
                        "   \"format\": \"date-time\"\n" +
                        "}")
                .build();

        final ApplicationContext context = new AppContextBuilder().build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);

        // Valid input
        {
            final PolicyEnforcementContext pec = new PECBuilder()
                    .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                            .withBody("\"2002-10-02T10:00:00-05:00\"")
                            .build())
                    .build();

            final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
            assertEquals(AssertionStatus.NONE, assertionStatus);
        }
        // Invalid input
        {
            final PolicyEnforcementContext pec = new PECBuilder()
                    .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                            .withBody("\"2019\"")
                            .build())
                    .build();

            final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
            assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);
        }
    }

    @Test
    public void testDefAndInternalRefInV4() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withTarget(REQUEST)
                .withStaticResource(JSON_SCHEMA_ADDRESSES)
                .build();

        final ApplicationContext context = new AppContextBuilder().build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);

        // Valid input
        {
            final PolicyEnforcementContext pec = new PECBuilder()
                    .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                            .withBody(JSON_DATA_SHIPPING_ADDRESS_VALID)
                            .build())
                    .build();

            final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
            assertEquals(AssertionStatus.NONE, assertionStatus);
        }
        // Invalid input
        {
            final PolicyEnforcementContext pec = new PECBuilder()
                    .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                            .withBody(JSON_DATA_SHIPPING_ADDRESS_INVALID)
                            .build())
                    .build();

            final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
            assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);
        }
    }

    @Test
    public void testRemoteReferenceNotSupportedV4StaticResource() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withTarget(REQUEST)
                .withStaticResource(JSON_SCHEMA_WITH_REMOTE_REFERENCES_2)
                .build();

        final ApplicationContext context = new AppContextBuilder().build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(JSON_DATA_SHIPPING_ADDRESS_VALID)
                        .build())
                .build();

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void testRemoteReferenceNotSupportedV4StaticResourceVariableReference() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withTarget(REQUEST)
                .withStaticResource("${" + VARIABLE_NAME_JSON_SCHEMA + "}")
                .build();

        final ApplicationContext context = new AppContextBuilder().build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(JSON_DATA_SHIPPING_ADDRESS_VALID)
                        .build())
                .withVariable(VARIABLE_NAME_JSON_SCHEMA, JSON_SCHEMA_WITH_REMOTE_REFERENCES_2)
                .build();

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void testRemoteReferenceNotSupportedV4MonitorUrl() throws PolicyAssertionException, IOException, NoSuchVariableException {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withTarget(REQUEST)
                .withUrlResource(URL_IMAURL_COM)
                .build();

        final ApplicationContext context = new AppContextBuilder()
                .withJsonSchema(null, JSON_SCHEMA_WITH_REMOTE_REFERENCES)
                .build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);

        // in live testing, this error was occurring on the second attempt at retrieving the schema was not producing the right error message
        final PolicyEnforcementContext firstPec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(
                                "\"abc\"")
                        .build())
                .build();
        final AssertionStatus firstAssertionStatus = serverAssertion.checkRequest(firstPec);
        assertEquals(AssertionStatus.SERVER_ERROR, firstAssertionStatus);
        assertTrue(((String) firstPec.getVariable(JSON_SCHEMA_FAILURE_VARIABLE)).contains(MESSAGE_NO_REMOTE_REFERENCES));

        final PolicyEnforcementContext secondPec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(
                                "\"abc\"")
                        .build())
                .build();
        final AssertionStatus secondAssertionStatus = serverAssertion.checkRequest(secondPec);
        assertEquals(AssertionStatus.SERVER_ERROR, secondAssertionStatus);
        assertTrue(((String) secondPec.getVariable(JSON_SCHEMA_FAILURE_VARIABLE)).contains(MESSAGE_NO_REMOTE_REFERENCES));
    }

    @Test
    public void schemaFromContextVariableOfTypeMessageShouldThrowException() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withTarget(REQUEST)
                .withStaticResource("${" + VARIABLE_NAME_JSON_SCHEMA + "}")
                .build();

        final ApplicationContext context = new AppContextBuilder().build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(JSON_DATA_SHIPPING_ADDRESS_VALID)
                        .build())
                .withVariable(VARIABLE_NAME_JSON_SCHEMA, new Message(
                        new ByteArrayStashManager(),
                        ContentTypeHeader.APPLICATION_JSON,
                        new ByteArrayInputStream(JSON_SCHEMA_ADDRESSES.getBytes())))
                .build();

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.SERVER_ERROR, assertionStatus);
        final String failureMessage = (String) pec.getVariable(VARIABLE_NAME_JSONSCHEMA_FAILURE);
        assertTrue(failureMessage.contains(String.format(ServerJSONSchemaAssertion.MESSAGE_NOT_A_STRING, VARIABLE_NAME_JSON_SCHEMA)));
    }

    @Test
    public void jsonDataFromContextVariableOfTypeMessageShouldValidate() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withTarget(OTHER)
                .withOtherTargetMessageVariable(VARIABLE_NAME_JSON_DATA)
                .withStaticResource(JSON_SCHEMA_ADDRESSES)
                .build();

        final ApplicationContext context = new AppContextBuilder().build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withVariable(VARIABLE_NAME_JSON_DATA, new Message(
                        new ByteArrayStashManager(),
                        ContentTypeHeader.APPLICATION_JSON,
                        new ByteArrayInputStream(JSON_DATA_SHIPPING_ADDRESS_VALID.getBytes())))
                .build();

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    /**
     * Test to preserve backwards compatibility with v2 accepting trailing tokens as valid JSON
     * @throws Exception
     */
    @Test
    public void testExtraTrailingTokensV2() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V2).withTarget(REQUEST)
                .withStaticResource(
                        "{\n" +
                                "\"type\": \"string\"\n" +
                                "}giberish")
                .build();

        final ApplicationContext context = new AppContextBuilder().build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);

        // Any valid JSON should pass
        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(
                                "\"abc\"")
                        .build())
                .build();

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    /**
     * Test trailing tokens in V4 is marked as invalid json
     * @throws Exception
     */
    @Test
    public void testExtraTrailingTokensV4() throws Exception {
        final JSONSchemaAssertion assertion = new JSONSchemaAssertionBuilder()
                .withVersion(DRAFT_V4).withTarget(REQUEST)
                .withStaticResource(
                        "{\n" +
                                "\"type\": \"string\"\n" +
                                "}giberish")
                .build();

        final ApplicationContext context = new AppContextBuilder().build();

        final ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);

        final PolicyEnforcementContext pec = new PECBuilder()
                .withRequest(new RequestBuilder(context, APPLICATION_JSON)
                        .withBody(
                                "\"abc\"")
                        .build())
                .build();

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void testLinkHeaderRegex() throws Exception {
        Pattern p = Pattern.compile(ServerJSONSchemaAssertion.LINK_HEADER_PATTERN);

        final String[] patterns = {LINK_HEADER_TEMPLATE_SCHEMA,
                "<http://json.com/my-hyper-schema> ;    rel=\"describedby\"",
                "<hTtpS://123.au>; rel=\"describedby\"",
                "<https://123.au>; rel=\"describedby\""};

        final String[] urls = {"http://irishman:8080/templateschema",
                "http://json.com/my-hyper-schema",
                "hTtpS://123.au",
                "https://123.au"
        };

        for (int i = 0, patternsLength = patterns.length; i < patternsLength; i++) {
            String pattern = patterns[i];
            final Matcher m = p.matcher(pattern);
            Assert.assertTrue("Regex did not match", m.matches());
            assertEquals("Invalid group 1 value found", urls[i], m.group(1));
        }

        Matcher m = p.matcher("<http://irishman:8080/templateschema>;rel1=\"describedby\"");
        Assert.assertFalse("Regex should not match", m.matches());

        m = p.matcher("<http://irishman:8080/templateschema>;rel=describedby");
        Assert.assertFalse("Regex should not match", m.matches());

        m = p.matcher("<http://irishman:8080/templateschema>;rel=\"describedby1\"");
        Assert.assertFalse("Regex should not match", m.matches());

        m = p.matcher("<abc>;rel=\"describedby\"");
        Assert.assertTrue("Regex should match", m.matches());
    }

    @Test
    public void testNew() throws Exception {
        Pattern pattern = Pattern.compile("<(.*)>\\s*;\\s*rel=\"describedby\"");
        String blah = "<http://irishman:8080/templateschema> ; rel=\"describedby\"";
        final Matcher matcher = pattern.matcher(blah);
        String url = "";
        if (matcher.matches() && matcher.groupCount() > 0) {
            url = matcher.group(1);
        }
        System.out.print(url.toCharArray());

        Assert.assertTrue("Bleh", url.equalsIgnoreCase("http://irishman:8080/templateschema"));
    }

}

class JSONSchemaAssertionBuilder {

    private final JSONSchemaAssertion assertion;

    JSONSchemaAssertionBuilder() {
        assertion = new JSONSchemaAssertion();
    }

    JSONSchemaAssertionBuilder withVersion(JsonSchemaVersion version) {
        assertion.setJsonSchemaVersion(version);
        return this;
    }

    JSONSchemaAssertionBuilder withStaticResource(String jsonSchema) {
        assertion.setResourceInfo(new StaticResourceInfo(jsonSchema));
        return this;
    }

    JSONSchemaAssertionBuilder withTarget(TargetMessageType target) {
        assertion.setTarget(target);
        return this;
    }

    JSONSchemaAssertionBuilder withOtherTargetMessageVariable(String variable) {
        assertion.setOtherTargetMessageVariable(variable);
        return this;
    }

    JSONSchemaAssertionBuilder withUrlResource(String url) {
        assertion.setResourceInfo(new SingleUrlResourceInfo(url));
        return this;
    }

    JSONSchemaAssertionBuilder withMessageUrlResource(String url) {
        assertion.setResourceInfo(new MessageUrlResourceInfo(new String[]{url}));
        return this;
    }

    JSONSchemaAssertionBuilder withResourceInfo(AssertionResourceInfo resourceInfo) {
        assertion.setResourceInfo(resourceInfo);
        return this;
    }

    JSONSchemaAssertion build() {
        return assertion;
    }
}

class MessageUrlResourceInfoBuilder {

    private final MessageUrlResourceInfo resourceInfo;

    MessageUrlResourceInfoBuilder() {
        resourceInfo = new MessageUrlResourceInfo();
    }

    MessageUrlResourceInfoBuilder withUrl(String url) {
        resourceInfo.setUrlRegexes(new String[]{url});
        return this;
    }

    MessageUrlResourceInfoBuilder withAllowMessagesWithoutUrl(boolean allow) {
        resourceInfo.setAllowMessagesWithoutUrl(allow);
        return this;
    }

    MessageUrlResourceInfo build() {
        return resourceInfo;
    }
}

class PECBuilder {

    private Message request;
    private Message response;
    private Map<String, Object> variables;

    PECBuilder() {
        variables = new HashMap<>();
    }

    PECBuilder withRequest(Message request) {
        this.request = request;
        return this;
    }

    PECBuilder withResponse(Message response) {
        this.response = response;
        return this;
    }

    PECBuilder withVariable(String name, Object value) {
        variables.put(name, value);
        return this;
    }

    PolicyEnforcementContext build() {
        PolicyEnforcementContext result = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            result.setVariable(entry.getKey(), entry.getValue());
        }
        return result;
    }
}

abstract class MessageBuilder {

    private final StashManager stashManager;
    private final ContentTypeHeader contentType;
    final Message message;
    final ServletContext servletContext;

    MessageBuilder(StashManager stashManager, ContentTypeHeader contentType) {
        this.stashManager = stashManager;
        this.contentType = contentType;
        message = new Message(XmlUtil.createEmptyDocument());
        servletContext = new MockServletContext();
    }

    MessageBuilder withLinkHeader(String linkHeader) {
        message.getHeadersKnob().addHeader("Link", linkHeader, HeadersKnob.HEADER_TYPE_HTTP);
        return this;
    }

    MessageBuilder withBody(String body) {
        try {
            message.initialize(stashManager, contentType, new ByteArrayInputStream(body.getBytes(Charsets.UTF8)));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return this;
    }

    abstract Message build();
}

class RequestBuilder extends MessageBuilder {

    RequestBuilder(ApplicationContext context, ContentTypeHeader contentType) {
        super(new StashManagerBuilder(context).build(), contentType);
    }

    Message build() {
        message.attachHttpRequestKnob(new HttpServletRequestKnob(new MockHttpServletRequest(servletContext)));
        return message;
    }
}

class ResponseBuilder extends MessageBuilder {

    ResponseBuilder(ApplicationContext context, ContentTypeHeader contentType) {
        super(new StashManagerBuilder(context).build(), contentType);
    }

    ResponseBuilder withLinkHeader(String linkHeader) {
        message.getHeadersKnob().addHeader("Link", linkHeader, HeadersKnob.HEADER_TYPE_HTTP);
        return this;
    }

    Message build() {
        message.attachHttpResponseKnob(new HttpServletResponseKnob(new MockHttpServletResponse()));
        return message;
    }
}

class StashManagerBuilder {

    private final StashManager stashManager;

    StashManagerBuilder(ApplicationContext context) {
        StashManagerFactory factory = (StashManagerFactory) context.getBean(BEAN_NAME_STASH_MANAGER_FACTORY);
        stashManager = factory.createStashManager();
    }

    StashManager build() {
        return stashManager;
    }
}

class AppContextBuilder {

    private TestingHttpClientFactory httpClientFactory;
    private ServerConfig serverConf;

    AppContextBuilder() {
        httpClientFactory = new TestingHttpClientFactory();
        serverConf = ServerConfig.getInstance();
    }

    AppContextBuilder withJsonSchema(String expectedUrl, String jsonSchema) {
        final byte[] bytes = jsonSchema.getBytes(Charsets.UTF8);

        final MockGenericHttpClient mockClient = new MockGenericHttpClient(200,
                new GenericHttpHeaders(new HttpHeader[]{}), APPLICATION_JSON, (long) bytes.length, bytes) {

            @Override
            public GenericHttpRequest createRequest(final HttpMethod method, final GenericHttpRequestParams params)
                    throws GenericHttpException {
                if (expectedUrl != null) {
                    assertEquals("JSON schema url", expectedUrl, params.getTargetUrl().toString());
                }
                return super.createRequest(method, params);
            }
        };
        httpClientFactory.setMockHttpClient(mockClient);

        return this;
    }

    AppContextBuilder withServerConfig(ServerConfig serverConf){
        this.serverConf = serverConf;
        return this;
    }

    ApplicationContext build() {
        final DefaultListableBeanFactory beanFactory = new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put(BEAN_NAME_SERVERCONFIG, serverConf);
            put(BEAN_NAME_HTTP_CLIENT_FACTORY, httpClientFactory);
            put(BEAN_NAME_STASH_MANAGER_FACTORY, TestStashManagerFactory.getInstance());
        }});

        GenericApplicationContext context = new GenericApplicationContext(beanFactory);
        context.refresh();

        //not needed for any tests, just doing to remove the warning messages and to allow for testing of the
        //cluster properties if needed
        final ServerConfig serverConfig = (ServerConfig) context.getBean(BEAN_NAME_SERVERCONFIG);
        final AssertionMetadata assertionMetadata = new JSONSchemaAssertion().meta();
        final Map<String, String[]> props = assertionMetadata.get(AssertionMetadata.CLUSTER_PROPERTIES);
        final List<String[]> toAdd = new ArrayList<>();

        for (Map.Entry<String, String[]> entry : props.entrySet()) {
            final String clusterPropertyName = entry.getKey();
            final String[] tuple = entry.getValue();
            // Dynamically register this new cluster property
            final String desc = tuple[0];
            final String dflt = tuple[1];
            final String serverConfigName = ClusterProperty.asServerConfigPropertyName(clusterPropertyName);

            toAdd.add(new String[]{serverConfigName, clusterPropertyName, desc, dflt});
        }
        if (!toAdd.isEmpty()) {
            serverConfig.registerServerConfigProperties(toAdd.toArray(new String[toAdd.size()][]));
        }

        return context;
    }
}
