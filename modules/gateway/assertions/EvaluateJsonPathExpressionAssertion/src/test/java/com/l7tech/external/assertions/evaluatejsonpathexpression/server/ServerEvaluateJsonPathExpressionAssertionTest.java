package com.l7tech.external.assertions.evaluatejsonpathexpression.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.evaluatejsonpathexpression.EvaluateJsonPathExpressionAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugId;
import net.minidev.json.JSONArray;
import net.minidev.json.parser.JSONParser;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test the EvaluateJsonPathExpressionAssertion.
 */
public class ServerEvaluateJsonPathExpressionAssertionTest {

    private static ApplicationContext applicationContext = ApplicationContexts.getTestApplicationContext();
    private static ServerConfig serverConfig = applicationContext.getBean("serverConfig", ServerConfigStub.class);

    private static final String TEST_JSON = "{ \"store\": {\n" +
            "    \"book\": [ \n" +
            "      { \"category\": \"reference\",\n" +
            "        \"author\": \"Nigel Rees\",\n" +
            "        \"title\": \"Sayings of the Century\",\n" +
            "        \"price\": 8.95\n" +
            "      },\n" +
            "      { \"category\": \"fiction\",\n" +
            "        \"author\": \"Evelyn Waugh\",\n" +
            "        \"title\": \"Sword of Honour\",\n" +
            "        \"price\": 12.99,\n" +
            "        \"isbn\": \"0-553-21311-3\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"bicycle\": {\n" +
            "      \"color\": \"red\",\n" +
            "      \"price\": 19.95\n" +
            "    },\n" +
            "    \"members\": []\n" +
            "  }\n" +
            "}";

    private static final String TEST_JSON_FOR_ESCAPED_SLASHES = "{ \"store\": {\n" +
            "    \"book\": [ \n" +
            "      { \"category\": \"reference\",\n" +
            "        \"author\": \"Nigel /test/ Rees\",\n" +
            "        \"title\": \"Sayings of the Century\",\n" +
            "        \"price\": 8.95\n" +
            "      },\n" +
            "      { \"category\": \"fiction\",\n" +
            "        \"author\": \"Evelyn /test/ Waugh\",\n" +
            "        \"title\": \"Sword of Honour\",\n" +
            "        \"price\": 12.99,\n" +
            "        \"isbn\": \"0-553-21311-3\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"bicycle\": {\n" +
            "      \"color\": \"red\",\n" +
            "      \"price\": 19.95\n" +
            "    },\n" +
            "    \"members\": []\n" +
            "  }\n" +
            "}";

    private EvaluateJsonPathExpressionAssertion assertion;
    private PolicyEnforcementContext pec;
    private ServerEvaluateJsonPathExpressionAssertion serverAssertion;
    private Message request;
    private Message response;

    private JSONParser jsonParser;

    @Before
    public void setUp() {
        try {
            serverConfig.putProperty(EvaluateJsonPathExpressionAssertion.PARAM_JSON_EVALJSONPATH_WITHCOMPRESSION, "false");
            assertion = new EvaluateJsonPathExpressionAssertion();
            assertion.setTarget(TargetMessageType.REQUEST);
            request = new Message();
            request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(TEST_JSON.getBytes()));
            jsonParser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
            response = new Message();
            response.initialize(XmlUtil.stringAsDocument("<response />"));
            pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
            serverAssertion = new ServerEvaluateJsonPathExpressionAssertion(assertion, applicationContext);
        } catch (Exception e) {
            Assert.fail("Error initializing test");
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        serverConfig.putProperty(EvaluateJsonPathExpressionAssertion.PARAM_JSON_EVALJSONPATH_WITHCOMPRESSION, "false");
    }

    @Test
    public void testInvalidContentHeader() {
        try {
            assertion.setTarget(TargetMessageType.REQUEST);
            final Message xmlMessage = pec.getOrCreateTargetMessage(new MessageTargetableSupport(TargetMessageType.REQUEST), false);
            xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(TEST_JSON.getBytes()));
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, status);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEmptyExpression() {
        try {
            assertion.setExpression("");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, status);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testInvalidEvaluator() {
        try {
            assertion.setExpression("$.store.book[1].author");
            assertion.setTarget(TargetMessageType.REQUEST);
            assertion.setEvaluator("layer7");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, status);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testJsonPathSingleStringResult() {
        testJsonPathSingleStringResult("Evelyn Waugh");
    }

    @Test
    public void testJsonPathSingleStringResultWithoutCompression() throws IOException {
        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        testJsonPathSingleStringResult("Evelyn /test/ Waugh");
    }

    @Test
    public void testJsonPathSingleStringResultWithCompression() throws IOException {
        serverConfig.putProperty(EvaluateJsonPathExpressionAssertion.PARAM_JSON_EVALJSONPATH_WITHCOMPRESSION, "true");
        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        testJsonPathSingleStringResult("Evelyn /test/ Waugh");
    }

    private void testJsonPathSingleStringResult(String expectedResult) {
        try {
            assertion.setEvaluator("JsonPath");
            assertion.setExpression("$.store.book[1].author");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable("jsonPath.found"));
            Assert.assertEquals(1, pec.getVariable("jsonPath.count"));
            try {
                Assert.assertEquals(expectedResult, pec.getVariable("jsonPath.result"));
            } catch (NoSuchVariableException e) {
                Assert.fail("Should have NOT failed with NoSuchVariableException!");
            }
        } catch (Exception e) {
            Assert.fail("Test JsonPath failed: " + e.getMessage());
        }
    }

    @Test
    public void testJsonPathSingleObjectResult() {
        final String expected = "{\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"category\":\"fiction\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}";
        testJsonPathSingleObjectResult(expected, "Evelyn Waugh");
    }

    @Test
    public void testJsonPathSingleObjectResultWithoutCompression() throws IOException {
        final String expected = "{\"author\":\"Evelyn /test/ Waugh\",\"title\":\"Sword of Honour\",\"category\":\"fiction\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}";

        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        testJsonPathSingleObjectResult(expected, "Evelyn \\/test\\/ Waugh");
    }

    @Test
    public void testJsonPathSingleObjectResultWithCompression() throws IOException {
        final String expected = "{\"author\":\"Evelyn /test/ Waugh\",\"title\":\"Sword of Honour\",\"category\":\"fiction\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}";

        serverConfig.putProperty(EvaluateJsonPathExpressionAssertion.PARAM_JSON_EVALJSONPATH_WITHCOMPRESSION, "true");
        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        testJsonPathSingleObjectResult(expected, "Evelyn /test/ Waugh");
    }

    public void testJsonPathSingleObjectResult(String expectedJsonString, String propertyValue) {
        try {
            assertion.setEvaluator("JsonPath");
            assertion.setExpression("$.store.book[1]");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable("jsonPath.found"));
            Assert.assertEquals(1, pec.getVariable("jsonPath.count"));
            Object expectedJson = jsonParser.parse(expectedJsonString);
            try {
                String actualJsonString = pec.getVariable("jsonPath.result").toString();
                Object actualJson = jsonParser.parse(actualJsonString);
                assertEquals(expectedJson, actualJson);
                assertTrue(actualJsonString.contains(propertyValue));
            } catch (NoSuchVariableException e) {
                Assert.fail("Should have NOT failed with NoSuchVariableException!");
            }
        } catch (Exception e) {
            Assert.fail("Test JsonPath failed: " + e.getMessage());
        }
    }

    @Test
    public void testJsonPathMultipleResultsObject() {
        String[] expected = new String[]{
                "{\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"category\":\"reference\",\"price\":8.95}",
                "{\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"category\":\"fiction\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}"
        };
        testJsonPathMultipleResultsObject(expected, new String[] { "Nigel Rees", "Evelyn Waugh"});
    }

    @Test
    public void testJsonPathMultipleResultsObjectWithoutCompression() throws IOException {
        String[] expected = new String[]{
                "{\"author\":\"Nigel /test/ Rees\",\"title\":\"Sayings of the Century\",\"category\":\"reference\",\"price\":8.95}",
                "{\"author\":\"Evelyn /test/ Waugh\",\"title\":\"Sword of Honour\",\"category\":\"fiction\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}"
        };

        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        testJsonPathMultipleResultsObject(expected, new String[] { "Nigel \\/test\\/ Rees", "Evelyn \\/test\\/ Waugh"});
    }

    @Test
    public void testJsonPathMultipleResultsObjectWithCompression() throws IOException {
        String[] expected = new String[]{
                "{\"author\":\"Nigel /test/ Rees\",\"title\":\"Sayings of the Century\",\"category\":\"reference\",\"price\":8.95}",
                "{\"author\":\"Evelyn /test/ Waugh\",\"title\":\"Sword of Honour\",\"category\":\"fiction\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}"
        };

        serverConfig.putProperty(EvaluateJsonPathExpressionAssertion.PARAM_JSON_EVALJSONPATH_WITHCOMPRESSION, "true");
        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        testJsonPathMultipleResultsObject(expected, new String[] { "Nigel /test/ Rees", "Evelyn /test/ Waugh"});
    }

    public void testJsonPathMultipleResultsObject(String[] expected, String[] propertyValues) {
        try {
            assertion.setEvaluator("JsonPath");
            assertion.setExpression("$.store.book[*]");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable("jsonPath.found"));
            Assert.assertEquals(expected.length, pec.getVariable("jsonPath.count"));

            JSONArray expectedArray = new JSONArray();
            for (String item : expected) {
                expectedArray.add(jsonParser.parse(item));
            }

            try {
                String actualJsonString = pec.getVariable("jsonPath.result").toString();
                assertEquals(expectedArray.get(0), jsonParser.parse(actualJsonString));
                assertTrue(actualJsonString.contains(propertyValues[0]));

                assertTrue(pec.getVariable("jsonPath.results") instanceof Object[]);
                Object[] results = (Object[]) pec.getVariable("jsonPath.results");

                JSONArray actualArray = new JSONArray();
                for(Object s : results) {
                    actualArray.add(jsonParser.parse(s.toString()));
                }

                assertEquals(expectedArray, actualArray);

                for (int i = 0; i < results.length; i ++) {
                    assertTrue(results[i].toString().contains(propertyValues[i]));
                }
            } catch (NoSuchVariableException e) {
                Assert.fail("Should have NOT failed with NoSuchVariableException!");
            }

        } catch (Exception e) {
            Assert.fail("Test JsonPath failed: " + e.getMessage());
        }
    }

    @Test
    public void testJsonPathNonExistentObject() {
        try {
            assertion.setEvaluator("JsonPath");
            assertion.setExpression("$.book2");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FALSIFIED, status);

            //check results
            Assert.assertEquals(false, pec.getVariable("jsonPath.found"));
            Assert.assertEquals(0, pec.getVariable("jsonPath.count"));

            doTestForNonExistingContextVar("jsonPath.result");
            doTestForNonExistingContextVar("jsonPath.results");
        } catch (Exception e) {
            Assert.fail("Test JsonPath failed: " + e.getMessage());
        }
    }

    @Test
    public void testJsonPathIncompletePath() {
        try {
            assertion.setEvaluator("JsonPath");
            assertion.setExpression("$.book[2");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, status);
            //check results - these vars shouldn't exist
            doTestForNonExistingContextVar("jsonPath.found");
            doTestForNonExistingContextVar("jsonPath.count");
            doTestForNonExistingContextVar("jsonPath.result");
            doTestForNonExistingContextVar("jsonPath.results");
        } catch (Exception e) {
            Assert.fail("Test JsonPath failed: " + e.getMessage());
        }
    }

    @Test
    public void testInvalidJson(){
        try {
            EvaluateJsonPathExpressionAssertion assertion = new EvaluateJsonPathExpressionAssertion();
            assertion.setTarget(TargetMessageType.REQUEST);
            final Message request = new Message();
            request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream("<foo>bar</foo>".getBytes()));

            Message response = new Message();
            response.initialize(XmlUtil.stringAsDocument("<response />"));
            PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
            ServerEvaluateJsonPathExpressionAssertion serverAssertion = new ServerEvaluateJsonPathExpressionAssertion(assertion,
                    applicationContext);

            assertion.setEvaluator("JsonPath");
            assertion.setExpression("$.foo");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, status);

            //check results - these vars shouldn't exist
            doTestForNonExistingContextVar("jsonPath.found");
            doTestForNonExistingContextVar("jsonPath.count");
            doTestForNonExistingContextVar("jsonPath.result");
            doTestForNonExistingContextVar("jsonPath.results");
        } catch (Exception e) {
            Assert.fail("Error initializing test");
        }
    }

    @Test
    public void testJsonPathSingleObjectResultFromContextVariable() {
        try {
            assertion.setEvaluator("JsonPath");
            assertion.setExpression("${myExpression}");
            pec.setVariable("myExpression", "$.store.book[1]");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);
            //check results
            Assert.assertEquals(true, pec.getVariable("jsonPath.found"));
            Assert.assertEquals(1, pec.getVariable("jsonPath.count"));
            final String expected = "{\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"category\":\"fiction\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}";
            Object expectedJson = jsonParser.parse(expected);
            try {
                Assert.assertEquals(expectedJson, jsonParser.parse(pec.getVariable("jsonPath.result").toString()));
            } catch (NoSuchVariableException e) {
                Assert.fail("Should have NOT failed with NoSuchVariableException!");
            }
        } catch (Exception e) {
            Assert.fail("Test JsonPath failed: " + e.getMessage());
        }
    }

    @Test
    public void testJsonPathSingleObjectResultFromInvalidContextVariable() {
        try {
            assertion.setEvaluator("JsonPath");
            assertion.setExpression("${myExpression[1}");
            pec.setVariable("myExpression", "$.store.book[1]");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, status);

            //check results
            doTestForNonExistingContextVar("jsonPath.found");
            doTestForNonExistingContextVar("jsonPath.count");
            doTestForNonExistingContextVar("jsonPath.result");
            doTestForNonExistingContextVar("jsonPath.results");
        } catch (VariableNameSyntaxException e) {

        } catch (Exception e) {
            Assert.fail("Test JsonPath failed: " + e.getMessage());
        }
    }

    @Test
    public void testJsonPathSingleNumericResult() {
        try {
            assertion.setEvaluator("JsonPath");
            assertion.setExpression("$.store.book[1].price");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable("jsonPath.found"));
            Assert.assertEquals(1, pec.getVariable("jsonPath.count"));
            try {
                Assert.assertEquals("12.99", pec.getVariable("jsonPath.result"));
            } catch (NoSuchVariableException e) {
                Assert.fail("Should have NOT failed with NoSuchVariableException!");
            }
        } catch (Exception e) {
            Assert.fail("Test JsonPath failed: " + e.getMessage());
        }
    }

    @BugId("DE246126")
    @Test
    public void testEmptyArray() {
        try {
            assertion.setEvaluator("JsonPath");
            assertion.setExpression("$.store.members");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable("jsonPath.found"));
            Assert.assertEquals(0, pec.getVariable("jsonPath.count"));

            doTestForNonExistingContextVar("jsonPath.result");

            final Object objects = pec.getVariable("jsonPath.results");
            Assert.assertThat(objects, Matchers.instanceOf(String[].class));
            Assert.assertThat((String[]) objects, Matchers.emptyArray());
        } catch (Exception e) {
            Assert.fail("Test JsonPath failed: " + e.getMessage());
        }
    }

    private void doTestForNonExistingContextVar(final String varName) {
        Assert.assertNotNull(varName);
        Assert.assertThat(varName, Matchers.not(Matchers.isEmptyOrNullString()));

        try {
            pec.getVariable("varName");
            Assert.fail("\"" + varName + "\" should have failed with NoSuchVariableException!");
        } catch (NoSuchVariableException e) {
            // ok
        }
    }
}
