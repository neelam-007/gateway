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
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import net.minidev.json.JSONArray;
import net.minidev.json.parser.JSONParser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static junit.framework.Assert.assertEquals;

/**
 * Test the EvaluateJsonPathExpressionAssertion.
 */
public class ServerEvaluateJsonPathExpressionAssertionTest {

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
            "    }\n" +
            "  }\n" +
            "}";

    private EvaluateJsonPathExpressionAssertion assertion;

    private PolicyEnforcementContext pec;

    private ServerEvaluateJsonPathExpressionAssertion serverAssertion;

    private JSONParser jsonParser;

    @Before
    public void setUp() {
        try {
            assertion = new EvaluateJsonPathExpressionAssertion();
            assertion.setTarget(TargetMessageType.REQUEST);
            final Message request = new Message();
            request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(TEST_JSON.getBytes()));
            jsonParser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
            Message response = new Message();
            response.initialize(XmlUtil.stringAsDocument("<response />"));
            pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
            serverAssertion = new ServerEvaluateJsonPathExpressionAssertion(assertion);
        } catch (Exception e) {
            Assert.fail("Error initializing test");
        }
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
        try {
            assertion.setEvaluator("JsonPath");
            assertion.setExpression("$.store.book[1].author");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable("jsonPath.found"));
            Assert.assertEquals(1, pec.getVariable("jsonPath.count"));
            try {
                Assert.assertEquals("Evelyn Waugh", pec.getVariable("jsonPath.result"));
            } catch (NoSuchVariableException e) {
                Assert.fail("Should have NOT failed with NoSuchVariableException!");
            }
        } catch (Exception e) {
            Assert.fail("Test JsonPath failed: " + e.getMessage());
        }
    }

    @Test
    public void testJsonPathSingleObjectResult() {
        try {
            assertion.setEvaluator("JsonPath");
            assertion.setExpression("$.store.book[1]");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable("jsonPath.found"));
            Assert.assertEquals(1, pec.getVariable("jsonPath.count"));
            final String expected = "{\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"category\":\"fiction\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}";
            Object expectedJson = jsonParser.parse(expected);
            try {
                Object actualJson = jsonParser.parse(pec.getVariable("jsonPath.result").toString());
                assertEquals(expectedJson, actualJson);
            } catch (NoSuchVariableException e) {
                Assert.fail("Should have NOT failed with NoSuchVariableException!");
            }
        } catch (Exception e) {
            Assert.fail("Test JsonPath failed: " + e.getMessage());
        }
    }

    @Test
    public void testJsonPathMultipleResultsObject() {
        try {
            assertion.setEvaluator("JsonPath");
            assertion.setExpression("$.store.book[*]");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable("jsonPath.found"));
            Assert.assertEquals(2, pec.getVariable("jsonPath.count"));

            String[] expected = new String[]{
                    "{\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"category\":\"reference\",\"price\":8.95}",
                    "{\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"category\":\"fiction\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}"
            };
            JSONArray expectedArray = new JSONArray();
            expectedArray.add(jsonParser.parse(expected[0]));
            expectedArray.add(jsonParser.parse(expected[1]));
            try {
                assertEquals(expectedArray.get(0), jsonParser.parse(pec.getVariable("jsonPath.result").toString()));
                Object results = pec.getVariable("jsonPath.results");
                JSONArray actualArray = new JSONArray();
                for(Object s : (Object[])results) {
                    actualArray.add(jsonParser.parse(s.toString()));
                }
                assertEquals(expectedArray, actualArray);
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
            try {
                pec.getVariable("jsonPath.result");
                pec.getVariable("jsonPath.results");
                Assert.fail("Should have failed with NoSuchVariableException!");
            } catch (NoSuchVariableException e) {

            }
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

            try {
                //check results - these vars shouldn't exist
                pec.getVariable("jsonPath.found");
                pec.getVariable("jsonPath.count");
                pec.getVariable("jsonPath.result");
                pec.getVariable("jsonPath.results");
                Assert.fail("Should have failed with NoSuchVariableException!");
            } catch (NoSuchVariableException e) {

            }
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
            ServerEvaluateJsonPathExpressionAssertion serverAssertion = new ServerEvaluateJsonPathExpressionAssertion(assertion);

            assertion.setEvaluator("JsonPath");
            assertion.setExpression("$.foo");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, status);
            try {
                //check results - these vars shouldn't exist
                pec.getVariable("jsonPath.found");
                pec.getVariable("jsonPath.count");
                pec.getVariable("jsonPath.result");
                pec.getVariable("jsonPath.results");
                Assert.fail("Should have failed with NoSuchVariableException!");
            } catch (NoSuchVariableException e) {

            }
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
            try {
                pec.getVariable("jsonPath.found");
                pec.getVariable("jsonPath.count");
                pec.getVariable("jsonPath.result");
                pec.getVariable("jsonPath.results");
                Assert.fail("Should have failed with NoSuchVariableException!");
            } catch (NoSuchVariableException e) {

            }
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
}
