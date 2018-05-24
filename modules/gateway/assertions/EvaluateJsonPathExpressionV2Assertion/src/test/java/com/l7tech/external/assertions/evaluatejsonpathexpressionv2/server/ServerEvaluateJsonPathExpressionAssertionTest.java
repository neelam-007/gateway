package com.l7tech.external.assertions.evaluatejsonpathexpressionv2.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.evaluatejsonpathexpressionv2.EvaluateJsonPathExpressionV2Assertion;
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

import static com.l7tech.external.assertions.evaluatejsonpathexpressionv2.server.ServerEvaluateJsonPathExpressionV2Assertion.BEAN_NAME_SERVERCONFIG;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test the EvaluateJsonPathExpressionAssertion.
 */
public class ServerEvaluateJsonPathExpressionAssertionTest {

    private static final ApplicationContext APPLICATION_CONTEXT = ApplicationContexts.getTestApplicationContext();
    private static final ServerConfig SERVER_CONFIG = APPLICATION_CONTEXT.getBean(BEAN_NAME_SERVERCONFIG,
            ServerConfigStub.class);

    private static final String EVALUATOR_JSONPATH = "JsonPath";
    private static final String EVALUATOR_JSONPATH_COMPRESSION = "JsonPathWithCompression";
    private static final String EVALUATOR_SYTEMDEFAULT = "SystemDefault";
    private static final String EVALUATOR_INVALID = "layer7";
    private static final String BOOK_NIGEL = "{\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\"," +
            "\"category\":\"reference\",\"price\":8.95}";
    private static final String BOOK_NIGEL_BACKSLASHES = "{\"author\":\"Nigel /test/ Rees\",\"title\":" +
            "\"Sayings of the Century\",\"category\":\"reference\",\"price\":8.95}";
    private static final String BOOK_EVELYN = "{\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\"," +
            "\"category\":\"fiction\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}";
    private static final String BOOK_EVELYN_BACKSLASHES = "{\"author\":\"Evelyn /test/ Waugh\",\"title\":" +
            "\"Sword of Honour\",\"category\":\"fiction\",\"price\":12.99,\"isbn\":\"0-553-21311-3\"}";
    private static final String AUTHOR_NIGEL = "Nigel Rees";
    private static final String AUTHOR_NIGEL_BACKSLASHES = "Nigel /test/ Rees";
    private static final String AUTHOR_EVELYN = "Evelyn Waugh";
    private static final String AUTHOR_EVELYN_BACKSLASHES = "Evelyn /test/ Waugh";
    private static final String VAR_NAME_JSONPATH_FOUND = "jsonPath.found";
    private static final String VAR_NAME_JSONPATH_COUNT = "jsonPath.count";
    private static final String VAR_NAME_JSONPATH_RESULT = "jsonPath.result";
    private static final String VAR_NAME_JSONPATH_RESULTS = "jsonPath.results";
    private static final String VAR_NAME_MYEXPRESSION = "myExpression";
    private static final String JSONPATH_EXPRESSION_FIRSTBOOKPRICE = "$.store.book[1].price";
    private static final String JSONPATH_EXPRESSION_FIRSTBOOK = "$.store.book[1]";
    private static final String JSONPATH_EXPRESSION_STORE_MEMBERS = "$..store.members";
    private static final String JSONPATH_EXPRESSION_STORE_BOOK_ALL = "$.store.book[*]";
    private static final String JSONPATH_EXPRESSION_FIRSTBOOKAUTHOR = "$.store.book[1].author";
    private static final String JSONPATH_EXPRESISON_FOO = "$.foo";
    private static final String JSONPATH_EXPRESSION_WITH_INVALID_INDEX = "${myExpression[1}";
    private static final String JSON_STRING_INVALID = "<foo>bar</foo>";
    private static final String JSON_STRING_TRAILING_GARBAGE = "{\"foo\":\"bar\"}blahblahblah";
    private static final String MESSAGE_TEST_FAILED = "Test JsonPath failed: ";
    private static final String MESSAGE_NOSUCHVARIABLEEXCEPTION_NOT_EXPECTED =
            "Should have NOT failed with NoSuchVariableException!";
    private static final String MESSAGE_NOSUCHVARIABLEEXCEPTION_EXPECTED
            = "\" should have failed with NoSuchVariableException!";

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
            "    \"members\": [],\n" +
            "    \"users\": {}\n" +
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
    private static final String TEST_JSON_WITH_NULL_ATTRIBUTE = "{\n" +
            "  \"book\": {\n" +
            "    \"category\": \"fiction\",\n" +
            "    \"author\": \"J. R. R. Tolkien\",\n" +
            "    \"title\": \"The Lord of the Rings\",\n" +
            "    \"isbn\": \"0-395-19395-8\",\n" +
            "    \"price\": null\n" +
            "  }\n" +
            "}";

    private EvaluateJsonPathExpressionV2Assertion assertion;
    private PolicyEnforcementContext pec;
    private ServerEvaluateJsonPathExpressionV2Assertion serverAssertion;
    private Message request;
    private JSONParser jsonParser;

    @Before
    public void setUp() {
        try {
            SERVER_CONFIG.putProperty(EvaluateJsonPathExpressionV2Assertion.PARAM_JSON_SYSTEM_DEFAULT_EVALUATOR,
                    EVALUATOR_JSONPATH);
            assertion = new EvaluateJsonPathExpressionV2Assertion();
            assertion.setTarget(TargetMessageType.REQUEST);
            request = new Message();
            request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON,
                    new ByteArrayInputStream(TEST_JSON.getBytes()));
            jsonParser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
            final Message response = new Message();
            response.initialize(XmlUtil.stringAsDocument("<response />"));
            pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
            serverAssertion = new ServerEvaluateJsonPathExpressionV2Assertion(assertion, APPLICATION_CONTEXT);
        } catch (Exception e) {
            Assert.fail("Error initializing test");
        }
    }

    @AfterClass
    public static void tearDownAfterClass() {
        SERVER_CONFIG.putProperty(EvaluateJsonPathExpressionV2Assertion.PARAM_JSON_SYSTEM_DEFAULT_EVALUATOR,
                EVALUATOR_JSONPATH);
    }

    @Test
    public void testInvalidContentHeader() {
        try {
            assertion.setTarget(TargetMessageType.REQUEST);
            final Message xmlMessage = pec.getOrCreateTargetMessage(
                    new MessageTargetableSupport(TargetMessageType.REQUEST), false);
            xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT,
                    new ByteArrayInputStream(TEST_JSON.getBytes()));
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
            assertion.setExpression(JSONPATH_EXPRESSION_FIRSTBOOKAUTHOR);
            assertion.setTarget(TargetMessageType.REQUEST);
            assertion.setEvaluator(EVALUATOR_INVALID);
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, status);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testJsonPathSingleStringResult() {
        testJsonPathSingleStringResult(EVALUATOR_JSONPATH, AUTHOR_EVELYN);
    }

    @Test
    public void testJsonPathSingleStringResultWithoutCompression() throws IOException {
        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        testJsonPathSingleStringResult(EVALUATOR_JSONPATH, AUTHOR_EVELYN_BACKSLASHES);
    }

    @Test
    public void testJsonPathSingleStringResultWithCompression() throws IOException {
        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        testJsonPathSingleStringResult(EVALUATOR_JSONPATH_COMPRESSION, AUTHOR_EVELYN_BACKSLASHES);
    }

    @Test
    public void findFirstBookAndExpectResult() {
        findFirstBookAndExpectResult(EVALUATOR_JSONPATH, BOOK_EVELYN, AUTHOR_EVELYN);
    }

    @Test
    public void testJsonPathSingleObjectResultWithoutCompression() throws IOException {
        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        findFirstBookAndExpectResult(EVALUATOR_JSONPATH, BOOK_EVELYN_BACKSLASHES, "Evelyn \\/test\\/ Waugh");
    }

    @Test
    public void testJsonPathSingleObjectResultWithCompression() throws IOException {
        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        findFirstBookAndExpectResult(EVALUATOR_JSONPATH_COMPRESSION, BOOK_EVELYN_BACKSLASHES,
                AUTHOR_EVELYN_BACKSLASHES);
    }

    @Test
    public void testJsonPathSingleObjectResultWithCompressionUsingCWP() throws IOException {
        SERVER_CONFIG.putProperty(EvaluateJsonPathExpressionV2Assertion.PARAM_JSON_SYSTEM_DEFAULT_EVALUATOR,
                EVALUATOR_JSONPATH_COMPRESSION);
        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        findFirstBookAndExpectResult(EVALUATOR_SYTEMDEFAULT, BOOK_EVELYN_BACKSLASHES, AUTHOR_EVELYN_BACKSLASHES);
    }

    @Test
    public void testJsonPathMultipleResultsObject() {
        final String[] expected = new String[]{BOOK_NIGEL, BOOK_EVELYN};
        testJsonPathMultipleResultsObject(EVALUATOR_JSONPATH, expected, new String[] {AUTHOR_NIGEL, AUTHOR_EVELYN});
    }

    @Test
    public void testJsonPathMultipleResultsObjectWithoutCompression() throws IOException {
        final String[] expected = new String[]{BOOK_NIGEL_BACKSLASHES, BOOK_EVELYN_BACKSLASHES};
        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        testJsonPathMultipleResultsObject(EVALUATOR_JSONPATH, expected,
                new String[] { "Nigel \\/test\\/ Rees", "Evelyn \\/test\\/ Waugh"});
    }

    @Test
    public void testJsonPathMultipleResultsObjectWithCompression() throws IOException {
        final String[] expected = new String[]{BOOK_NIGEL_BACKSLASHES, BOOK_EVELYN_BACKSLASHES};
        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        testJsonPathMultipleResultsObject(EVALUATOR_JSONPATH_COMPRESSION, expected,
                new String[] {AUTHOR_NIGEL_BACKSLASHES, AUTHOR_EVELYN_BACKSLASHES});
    }

    @Test
    public void testJsonPathMultipleResultsObjectWithCompressionUsingCWP() throws IOException {
        final String[] expected = new String[]{BOOK_NIGEL_BACKSLASHES, BOOK_EVELYN_BACKSLASHES};
        SERVER_CONFIG.putProperty(EvaluateJsonPathExpressionV2Assertion.PARAM_JSON_SYSTEM_DEFAULT_EVALUATOR,
                EVALUATOR_JSONPATH_COMPRESSION);
        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(TEST_JSON_FOR_ESCAPED_SLASHES.getBytes()));
        testJsonPathMultipleResultsObject(EVALUATOR_SYTEMDEFAULT, expected,
                new String[] {AUTHOR_NIGEL_BACKSLASHES, AUTHOR_EVELYN_BACKSLASHES});
    }

    @Test
    public void testJsonPathNonExistentObject() {
        try {
            assertion.setEvaluator(EVALUATOR_JSONPATH);
            assertion.setExpression("$.book2");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FALSIFIED, status);

            //check results
            Assert.assertEquals(false, pec.getVariable(VAR_NAME_JSONPATH_FOUND));
            Assert.assertEquals(0, pec.getVariable(VAR_NAME_JSONPATH_COUNT));

            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_RESULT);
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_RESULTS);

        } catch (Exception e) {
            Assert.fail(MESSAGE_TEST_FAILED + e.getMessage());
        }
    }

 	@Test
    public void testFindKeyForEmptyJsonObject() {
        try {
            assertion.setExpression("$..users");
            assertion.setTarget(TargetMessageType.REQUEST);
            assertion.setEvaluator(EVALUATOR_JSONPATH);
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            Assert.assertEquals(true, pec.getVariable(VAR_NAME_JSONPATH_FOUND));
            Assert.assertEquals(1, pec.getVariable(VAR_NAME_JSONPATH_COUNT));

            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_RESULT);

            final Object objects = pec.getVariable(VAR_NAME_JSONPATH_RESULTS);
            Assert.assertThat(objects, Matchers.instanceOf(String[].class));
            Assert.assertThat((String[]) objects, Matchers.hasItemInArray("{}"));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEvaluateNonKeyWithRecursivedescentExpr() {
        try {
            assertion.setExpression("$..categories");
            assertion.setTarget(TargetMessageType.REQUEST);
            assertion.setEvaluator(EVALUATOR_JSONPATH);
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FALSIFIED, status);

            //check results
            Assert.assertEquals(false, pec.getVariable(VAR_NAME_JSONPATH_FOUND));
            Assert.assertEquals(0, pec.getVariable(VAR_NAME_JSONPATH_COUNT));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testJsonPathIncompletePath() {
        try {
            assertion.setEvaluator(EVALUATOR_JSONPATH);
            assertion.setExpression("$.book[2");
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, status);
            //check results - these vars shouldn't exist
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_FOUND);
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_COUNT);
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_RESULT);
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_RESULTS);

        } catch (Exception e) {
            Assert.fail(MESSAGE_TEST_FAILED + e.getMessage());
        }
    }

    @Test
    public void testInvalidJson(){
        try {
            EvaluateJsonPathExpressionV2Assertion assertion = new EvaluateJsonPathExpressionV2Assertion();
            assertion.setTarget(TargetMessageType.REQUEST);
            final Message request = new Message();
            request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON,
                    new ByteArrayInputStream(JSON_STRING_INVALID.getBytes()));

            final Message response = new Message();
            response.initialize(XmlUtil.stringAsDocument("<response />"));
            final PolicyEnforcementContext pec = PolicyEnforcementContextFactory
                    .createPolicyEnforcementContext(request, response);
            final ServerEvaluateJsonPathExpressionV2Assertion serverAssertion =
                    new ServerEvaluateJsonPathExpressionV2Assertion(assertion, APPLICATION_CONTEXT);

            assertion.setEvaluator(EVALUATOR_JSONPATH);
            assertion.setExpression(JSONPATH_EXPRESISON_FOO);
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, status);

            //check results - these vars shouldn't exist
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_FOUND);
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_COUNT);
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_RESULT);
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_RESULTS);
        } catch (Exception e) {
            Assert.fail("Error initializing test");
        }
    }

    @Test
    public void testJsonPathSingleObjectResultFromContextVariable() {
        try {
            assertion.setEvaluator(EVALUATOR_JSONPATH);
            assertion.setExpression("${myExpression}");
            pec.setVariable(VAR_NAME_MYEXPRESSION, JSONPATH_EXPRESSION_FIRSTBOOK);
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);
            //check results
            Assert.assertEquals(true, pec.getVariable(VAR_NAME_JSONPATH_FOUND));
            Assert.assertEquals(1, pec.getVariable(VAR_NAME_JSONPATH_COUNT));
            final String expected = BOOK_EVELYN;
            final Object expectedJson = jsonParser.parse(expected);
            try {
                Assert.assertEquals(expectedJson, jsonParser.parse(pec.getVariable(VAR_NAME_JSONPATH_RESULT).toString()));
            } catch (NoSuchVariableException e) {
                Assert.fail(MESSAGE_NOSUCHVARIABLEEXCEPTION_NOT_EXPECTED);
            }

        } catch (Exception e) {
            Assert.fail(MESSAGE_TEST_FAILED + e.getMessage());
        }
    }

    @Test
    public void testJsonPathSingleObjectResultFromInvalidContextVariable() {
        try {
            assertion.setEvaluator(EVALUATOR_JSONPATH);
            assertion.setExpression(JSONPATH_EXPRESSION_WITH_INVALID_INDEX);
            pec.setVariable(VAR_NAME_MYEXPRESSION, JSONPATH_EXPRESSION_FIRSTBOOK);
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, status);

            //check results
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_FOUND);
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_COUNT);
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_RESULT);
            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_RESULTS);

        } catch (VariableNameSyntaxException ignored) {

        } catch (Exception e) {
            Assert.fail(MESSAGE_TEST_FAILED + e.getMessage());
        }
    }

    @Test
    public void testJsonPathSingleNumericResult() {
        try {
            assertion.setEvaluator(EVALUATOR_JSONPATH);
            assertion.setExpression(JSONPATH_EXPRESSION_FIRSTBOOKPRICE);
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable(VAR_NAME_JSONPATH_FOUND));
            Assert.assertEquals(1, pec.getVariable(VAR_NAME_JSONPATH_COUNT));
            try {
                Assert.assertEquals("12.99", pec.getVariable(VAR_NAME_JSONPATH_RESULT));
            } catch (NoSuchVariableException e) {
                Assert.fail(MESSAGE_NOSUCHVARIABLEEXCEPTION_NOT_EXPECTED);
            }

        } catch (Exception e) {
            Assert.fail(MESSAGE_TEST_FAILED + e.getMessage());
        }
    }

    @BugId("DE246126")
    @Test
    public void testEmptyArray() {
        try {
            assertion.setEvaluator(EVALUATOR_JSONPATH);
            assertion.setExpression(JSONPATH_EXPRESSION_STORE_MEMBERS);
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable(VAR_NAME_JSONPATH_FOUND));
            Assert.assertEquals(1, pec.getVariable(VAR_NAME_JSONPATH_COUNT));

            doTestForNonExistingContextVar(VAR_NAME_JSONPATH_RESULT);

            final Object objects = pec.getVariable(VAR_NAME_JSONPATH_RESULTS);
            Assert.assertThat(objects, Matchers.instanceOf(String[].class));
            Assert.assertThat((String[]) objects, Matchers.arrayContaining("[]"));

        } catch (Exception e) {
            Assert.fail(MESSAGE_TEST_FAILED + e.getMessage());
        }
    }

    @BugId("DE315889")
    @Test
    public void shouldRejectJsonWithTrailingGarbage() {
        try {
            final EvaluateJsonPathExpressionV2Assertion assertion = new EvaluateJsonPathExpressionV2Assertion();
            assertion.setTarget(TargetMessageType.REQUEST);
            assertion.setEvaluator(EVALUATOR_JSONPATH);
            assertion.setExpression(JSONPATH_EXPRESISON_FOO);

            final Message request = new Message();
            request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON,
                    new ByteArrayInputStream(JSON_STRING_TRAILING_GARBAGE.getBytes()));

            final PolicyEnforcementContext pec = PolicyEnforcementContextFactory
                    .createPolicyEnforcementContext(request, new Message());
            final ServerEvaluateJsonPathExpressionV2Assertion serverAssertion =
                    new ServerEvaluateJsonPathExpressionV2Assertion(assertion, APPLICATION_CONTEXT);

            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.FAILED, status);

        } catch (Exception e) {
            Assert.fail(MESSAGE_TEST_FAILED + e.getMessage());
        }
    }

    @BugId("DE347516")
    @Test
    public void testEvaluateJsonPathExpressionV2_NULL_Result() throws Exception {
        assertion.setEvaluator(EVALUATOR_JSONPATH);
        assertion.setExpression("$.book.price");

        request.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON,
                new ByteArrayInputStream(TEST_JSON_WITH_NULL_ATTRIBUTE.getBytes()));

        final AssertionStatus status = serverAssertion.checkRequest(pec);
        Assert.assertEquals(AssertionStatus.NONE, status);
        Assert.assertEquals(true, pec.getVariable(VAR_NAME_JSONPATH_FOUND));
        Assert.assertEquals(1, pec.getVariable(VAR_NAME_JSONPATH_COUNT));

        /* jsonPath.result throws NoSuchVariableException as it is NOT set if the value is NULL.
        Check for jsonPath.results instead. */
        Assert.assertEquals(null, ((String[]) pec.getVariable(VAR_NAME_JSONPATH_RESULTS))[0]);
    }

    private void testJsonPathSingleStringResult(String evaluator, String expectedResult) {
        try {
            assertion.setEvaluator(evaluator);
            assertion.setExpression(JSONPATH_EXPRESSION_FIRSTBOOKAUTHOR);
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable(VAR_NAME_JSONPATH_FOUND));
            Assert.assertEquals(1, pec.getVariable(VAR_NAME_JSONPATH_COUNT));
            try {
                Assert.assertEquals(expectedResult, pec.getVariable(VAR_NAME_JSONPATH_RESULT));
            } catch (NoSuchVariableException e) {
                Assert.fail(MESSAGE_NOSUCHVARIABLEEXCEPTION_NOT_EXPECTED);
            }

        } catch (Exception e) {
            Assert.fail(MESSAGE_TEST_FAILED + e.getMessage());
        }
    }

    private void testJsonPathMultipleResultsObject(String evaluator, String[] expected, String[] propertyValues) {
        try {
            assertion.setEvaluator(evaluator);
            assertion.setExpression(JSONPATH_EXPRESSION_STORE_BOOK_ALL);
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable(VAR_NAME_JSONPATH_FOUND));
            Assert.assertEquals(expected.length, pec.getVariable(VAR_NAME_JSONPATH_COUNT));

            final JSONArray expectedArray = new JSONArray();
            for (String item : expected) {
                expectedArray.add(jsonParser.parse(item));
            }

            try {
                final String actualJsonString = pec.getVariable(VAR_NAME_JSONPATH_RESULT).toString();
                assertEquals(expectedArray.get(0), jsonParser.parse(actualJsonString));
                assertTrue(actualJsonString.contains(propertyValues[0]));

                assertTrue(pec.getVariable(VAR_NAME_JSONPATH_RESULTS) instanceof Object[]);
                final Object[] results = (Object[]) pec.getVariable(VAR_NAME_JSONPATH_RESULTS);

                final JSONArray actualArray = new JSONArray();
                for(Object s : results) {
                    actualArray.add(jsonParser.parse(s.toString()));
                }

                assertEquals(expectedArray, actualArray);

                for (int i = 0; i < results.length; i ++) {
                    assertTrue(results[i].toString().contains(propertyValues[i]));
                }

            } catch (NoSuchVariableException e) {
                Assert.fail(MESSAGE_NOSUCHVARIABLEEXCEPTION_NOT_EXPECTED);
            }

        } catch (Exception e) {
            Assert.fail(MESSAGE_TEST_FAILED + e.getMessage());
        }
    }

    private void findFirstBookAndExpectResult(final String evaluator, final String expectedJsonString,
                                              final String propertyValue) {
        try {
            assertion.setEvaluator(evaluator);
            assertion.setExpression(JSONPATH_EXPRESSION_FIRSTBOOK);
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            Assert.assertEquals(AssertionStatus.NONE, status);

            //check results
            Assert.assertEquals(true, pec.getVariable(VAR_NAME_JSONPATH_FOUND));
            Assert.assertEquals(1, pec.getVariable(VAR_NAME_JSONPATH_COUNT));
            final Object expectedJson = jsonParser.parse(expectedJsonString);
            try {
                final String actualJsonString = pec.getVariable(VAR_NAME_JSONPATH_RESULT).toString();
                final Object actualJson = jsonParser.parse(actualJsonString);
                assertEquals(expectedJson, actualJson);
                assertTrue(actualJsonString.contains(propertyValue));

            } catch (NoSuchVariableException e) {
                Assert.fail(MESSAGE_NOSUCHVARIABLEEXCEPTION_NOT_EXPECTED);
            }

        } catch (Exception e) {
            Assert.fail(MESSAGE_TEST_FAILED + e.getMessage());
        }
    }

    private void doTestForNonExistingContextVar(final String varName) {
        Assert.assertNotNull(varName);
        Assert.assertThat(varName, Matchers.not(Matchers.isEmptyOrNullString()));

        try {
            pec.getVariable("varName");
            Assert.fail("\"" + varName + MESSAGE_NOSUCHVARIABLEEXCEPTION_EXPECTED);
        } catch (NoSuchVariableException e) {
            // ok
        }
    }
}
