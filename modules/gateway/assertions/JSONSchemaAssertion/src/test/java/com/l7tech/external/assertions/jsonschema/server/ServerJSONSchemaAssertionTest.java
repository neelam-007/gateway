package com.l7tech.external.assertions.jsonschema.server;

import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.jsonschema.JSONSchemaAssertion;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.util.Charsets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test the JSONSchemaAssertion.
 */
public class ServerJSONSchemaAssertionTest {

    private static final Logger log = Logger.getLogger(ServerJSONSchemaAssertionTest.class.getName());
    private ApplicationContext context;
    private StashManager stashManager;

    @Before
    public void setUp() throws Exception{
        final ApplicationContext delegateContext = ApplicationContexts.getTestApplicationContext();

        final TestingHttpClientFactory testFactory = new TestingHttpClientFactory();
        final byte[] bytes = jsonSchema.getBytes();
        MockGenericHttpClient mockClient = new MockGenericHttpClient(200, new GenericHttpHeaders(new HttpHeader[]{}),
                ContentTypeHeader.APPLICATION_JSON, (long) bytes.length, bytes);
        testFactory.setMockHttpClient(mockClient);

        //todo find the better way of doing this custom application context behaviour in other test cases
        context = new AbstractApplicationContext() {
            @Override
            public Object getBean(String name) throws BeansException {
                if(name.equals("httpClientFactory")){
                    System.out.println("Returned mock client");
                    return testFactory;
                }

                return delegateContext.getBean(name);
            }

            @Override
            public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
                return delegateContext.getBean(name, requiredType);
            }

            @Override
            public <T> T getBean(Class<T> requiredType) throws BeansException {
                return delegateContext.getBean(requiredType);
            }

            @Override
            protected void refreshBeanFactory() throws BeansException, IllegalStateException {}

            @Override
            protected void closeBeanFactory() {}

            @Override
            public ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException {
                return null;
            }

            @Override
            public boolean containsBean(String name) {
                return delegateContext.containsBean(name);
            }

            @Override
            public void publishEvent(ApplicationEvent event) {
                //do nothing
            }
        };

        //not needed for any tests, just doing to remove the warning messages and to allow for testing of the
        //cluster properties if needed
        ServerConfig serverConfig = (ServerConfig) delegateContext.getBean("serverConfig");
        final AssertionMetadata assertionMetadata = new JSONSchemaAssertion().meta();
        final Map<String, String[]> props = assertionMetadata.get(AssertionMetadata.CLUSTER_PROPERTIES);
        List<String[]> toAdd = new ArrayList<String[]>();

        for (Map.Entry<String, String[]> entry : props.entrySet()) {

            String clusterPropertyName = entry.getKey();
            String[] tuple = entry.getValue();
            // Dynamically register this new cluster property
            String desc = tuple[0];
            String dflt = tuple[1];
            String serverConfigName = ClusterProperty.asServerConfigPropertyName(clusterPropertyName);

            toAdd.add(new String[] { serverConfigName, clusterPropertyName, desc, dflt });
        }
        if (!toAdd.isEmpty()) serverConfig.registerServerConfigProperties(toAdd.toArray(new String[toAdd.size()][]));

//        context = delegateContext;
        StashManagerFactory factory = (StashManagerFactory) context.getBean("stashManagerFactory");
        stashManager = factory.createStashManager();
    }

    // -  STATIC RESOURCE

    @Test
    public void testRequest_ValidInstanceDocument() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new StaticResourceInfo(jsonSchema));
        assertion.setTarget(TargetMessageType.REQUEST);
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(jsonInstance, true, null, context, ContentTypeHeader.APPLICATION_JSON, null);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testRequest_StaticResourceWithMessage() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        final String schemaVar = "JSON_SCHEMA";
        assertion.setResourceInfo(new StaticResourceInfo("${" + schemaVar + "}"));
        assertion.setTarget(TargetMessageType.REQUEST);
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(jsonInstance, true, null, context, ContentTypeHeader.APPLICATION_JSON, null);
        pec.setVariable(schemaVar, jsonSchema);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testResponse_ValidInstanceDocument() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new StaticResourceInfo(jsonSchema));
        assertion.setTarget(TargetMessageType.RESPONSE);
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(jsonInstance, false, null, context, ContentTypeHeader.APPLICATION_JSON, null);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testStringMessageVariable_ValidInstanceDocument() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new StaticResourceInfo(jsonSchema));
        assertion.setTarget(TargetMessageType.OTHER);
        final String varName = "STRING_VARIABLE";
        assertion.setOtherTargetMessageVariable(varName);
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(null, true, varName, context, ContentTypeHeader.APPLICATION_JSON, null);
        pec.setVariable(varName, jsonInstance);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testJsonMessageVariable_ValidInstanceDocument() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new StaticResourceInfo(jsonSchema));
        assertion.setTarget(TargetMessageType.OTHER);
        final String varName = "STRING_VARIABLE";
        assertion.setOtherTargetMessageVariable(varName);
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(null, true, varName, context, ContentTypeHeader.APPLICATION_JSON, null);
        final Message message = new Message(stashManager, ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(jsonInstance.getBytes()));
        pec.setVariable(varName, message);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testRequest_InValidInstanceDocument() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new StaticResourceInfo(jsonSchema));
        assertion.setTarget(TargetMessageType.REQUEST);
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(invalidJsonInstance, true, null, context, ContentTypeHeader.APPLICATION_JSON, null);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);
    }

    @Test
    public void testResponse_InValidInstanceDocument() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new StaticResourceInfo(jsonSchema));
        assertion.setTarget(TargetMessageType.RESPONSE);
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(invalidJsonInstance, false, null, context, ContentTypeHeader.APPLICATION_JSON, null);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.BAD_RESPONSE, assertionStatus);
    }

    @Test
    public void testStringMessageVariable_InValidInstanceDocument() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new StaticResourceInfo(jsonSchema));
        assertion.setTarget(TargetMessageType.OTHER);
        final String varName = "STRING_VARIABLE";
        assertion.setOtherTargetMessageVariable(varName);
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(null, true, varName, context, ContentTypeHeader.APPLICATION_JSON, null);
        pec.setVariable(varName, invalidJsonInstance);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void testJsonMessageVariable_InValidInstanceDocument() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new StaticResourceInfo(jsonSchema));
        assertion.setTarget(TargetMessageType.OTHER);
        final String varName = "STRING_VARIABLE";
        assertion.setOtherTargetMessageVariable(varName);
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(null, true, varName, context, ContentTypeHeader.APPLICATION_JSON, null);
        final Message message = new Message(stashManager, ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(invalidJsonInstance.getBytes()));
        pec.setVariable(varName, message);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    // -  MONITOR URL RESOURCE

    @Test
    public void testMonitorUrl() throws Exception{
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new SingleUrlResourceInfo("http://imaurl.com"));
        assertion.setTarget(TargetMessageType.REQUEST);
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(jsonInstance, true, null, context, ContentTypeHeader.APPLICATION_JSON, null);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testMonitorUrl_InValidInstanceDocument() throws Exception {
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new SingleUrlResourceInfo("http://imaurl.com"));
        assertion.setTarget(TargetMessageType.REQUEST);
        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(invalidJsonInstance, true, null, context, ContentTypeHeader.APPLICATION_JSON, null);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.BAD_REQUEST, assertionStatus);
    }

    // - RETRIEVE URL FROM HEADER

    @Test
    public void testUrlRetrieveFromMimeParameter() throws Exception{
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new MessageUrlResourceInfo(new String[]{"http://.*"}));
        assertion.setTarget(TargetMessageType.REQUEST);

        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(jsonInstance, true, null, context, ContentTypeHeader.create("application/json;profile=http://testurl.com"), null);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testUrlRetrieveFromMimeParameter_NoWhiteListMatch() throws Exception{
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new MessageUrlResourceInfo(new String[]{"http://wontmatch.*"}));
        assertion.setTarget(TargetMessageType.REQUEST);

        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(jsonInstance, true, null, context, ContentTypeHeader.create("application/json;profile=http://testurl.com"), null);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.SERVER_ERROR, assertionStatus);
    }

    @Test
    public void testUrlRetrieveFromLinkHeader() throws Exception{
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new MessageUrlResourceInfo(new String[]{"http://.*"})); //this matches the url set up in getContext
        assertion.setTarget(TargetMessageType.REQUEST);

        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        String linkHeader = "<http://irishman:8080/templateschema>;rel=\"describedby\"";
        PolicyEnforcementContext pec = getContext(jsonInstance, true, null, context, ContentTypeHeader.create("application/json"), linkHeader);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testUrlRetrieveFromLinkHeader_Response() throws Exception{
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new MessageUrlResourceInfo(new String[]{"http://.*"})); //this matches the url set up in getContext
        assertion.setTarget(TargetMessageType.RESPONSE);

        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        String linkHeader = "<http://irishman:8080/templateschema>;rel=\"describedby\"";
        PolicyEnforcementContext pec = getContext(jsonInstance, false, null, context, ContentTypeHeader.create("application/json"), linkHeader);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testUrlRetrieveFromLinkHeader_NoWhiteListMatch() throws Exception{
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new MessageUrlResourceInfo(new String[]{"http://nomatch.*"})); //this matches the url set up in getContext
        assertion.setTarget(TargetMessageType.REQUEST);

        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        String linkHeader = "<http://irishman:8080/templateschema>;rel=\"describedby\"";
        PolicyEnforcementContext pec = getContext(jsonInstance, true, null, context, ContentTypeHeader.create("application/json"), linkHeader);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.SERVER_ERROR, assertionStatus);
    }

    @Test
    public void testUrlRetrieveFromLinkHeader_NoURLFound() throws Exception{
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        assertion.setResourceInfo(new MessageUrlResourceInfo(new String[]{"http://.*"})); //this matches the url set up in getContext
        assertion.setTarget(TargetMessageType.REQUEST);

        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(jsonInstance, true, null, context, ContentTypeHeader.create("application/json"), null);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.SERVER_ERROR, assertionStatus);
    }

    @Test
    public void testUrlRetrieveFromLinkHeader_NoURLFoundAllowed() throws Exception{
        JSONSchemaAssertion assertion = new JSONSchemaAssertion();
        final MessageUrlResourceInfo resourceInfo = new MessageUrlResourceInfo(new String[]{"http://.*"});
        resourceInfo.setAllowMessagesWithoutUrl(true);
        assertion.setResourceInfo(resourceInfo); //this matches the url set up in getContext
        assertion.setTarget(TargetMessageType.REQUEST);

        ServerJSONSchemaAssertion serverAssertion = new ServerJSONSchemaAssertion(assertion, context);
        PolicyEnforcementContext pec = getContext(jsonInstance, true, null, context, ContentTypeHeader.create("application/json"), null);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, assertionStatus);
    }

    @Test
    public void testLinkHeaderRegex() throws Exception{
        Pattern p = Pattern.compile(ServerJSONSchemaAssertion.linkHeaderPattern);

        final String[] patterns = {"<http://irishman:8080/templateschema>;rel=\"describedby\"",
        "<http://json.com/my-hyper-schema> ;    rel=\"describedby\"",
        "<http://123.au>; rel=\"describedby\""};

        final String[] urls = {"http://irishman:8080/templateschema",
                "http://json.com/my-hyper-schema",
                "http://123.au"
        };

        for (int i = 0, patternsLength = patterns.length; i < patternsLength; i++) {
            String pattern = patterns[i];
            final Matcher m = p.matcher(pattern);
            Assert.assertTrue("Regex did not match", m.matches());
            Assert.assertEquals("Invalid group 1 value found", urls[i], m.group(1));
        }

        Matcher m = p.matcher("<http://irishman:8080/templateschema>;rel1=\"describedby\"");
        Assert.assertFalse("Regex should not match", m.matches());

        m = p.matcher("<http://irishman:8080/templateschema>;rel=describedby");
        Assert.assertFalse("Regex should not match", m.matches());

        m = p.matcher("<http://irishman:8080/templateschema>;rel=\"describedby1\"");
        Assert.assertFalse("Regex should not match", m.matches());

        m = p.matcher("<http://12.a>;rel=\"describedby\"");
        Assert.assertFalse("Regex should not match", m.matches());

    }

    /**
     * contextVariableName takes precedence over useRequest. When not null the xacmlRequestXml is placed in a variable
     */
    private PolicyEnforcementContext getContext(String requestData,
                                                boolean useRequest,
                                                String contextVariableName,
                                                ApplicationContext context,
                                                ContentTypeHeader contentType,
                                                String addLinkHeader) throws IOException {

        Message request = new Message();
        Message response = new Message();

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        if(addLinkHeader != null && useRequest){
            hrequest.addHeader("Link", addLinkHeader);
        }

        PolicyEnforcementContext policyEnforcementContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        if (contextVariableName != null && !contextVariableName.isEmpty()) {
        } else {
            if (useRequest) {
                request.initialize(stashManager, contentType, new ByteArrayInputStream(requestData.getBytes(Charsets.UTF8)));
            } else {
                response.initialize(stashManager, contentType, new ByteArrayInputStream(requestData.getBytes(Charsets.UTF8)));
            }
        }
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));
        if(addLinkHeader  != null && !useRequest){
            response.getHttpResponseKnob().addHeader("Link", addLinkHeader);
        }
        

        return policyEnforcementContext;
    }

    private static String jsonSchema = "{\n" +
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
            "}";

    private static String jsonInstance = "{\"reportType\":\"performance\",\n" +
            "   \"entityType\" : \"publishedService\",\n" +
            "   \"isIgnoringPagination\" : true, \n" +
            "   \"entities\" : [{\"clusterId\":\"cluster1\"},{\"clusterId\":\"cluster2\"}],\n" +
            "   \"summaryChart\" : true,\n" +
            "   \"summaryReport\" : false,\n" +
            "   \"reportName\" : \"My Report\"\n" +
            "}";

    private static String invalidJsonInstance = "{\"reportType\":\"performance\",\n" +
            "   \"entityType1\" : \"publishedService\",\n" +
            "   \"isIgnoringPagination\" : true, \n" +
            "   \"entities\" : [{\"clusterId\":\"cluster1\"},{\"clusterId\":\"cluster2\"}],\n" +
            "   \"summaryChart2\" : true,\n" +
            "   \"summaryReport\" : false,\n" +
            "   \"reportName\" : \"My Report\"\n" +
            "}";

}
