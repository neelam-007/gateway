package com.l7tech.external.assertions.js.server;

import static com.l7tech.external.assertions.js.features.JavaScriptAssertionConstants.DEFAULT_EXECUTION_TIMEOUT;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.js.JavaScriptAssertion;
import com.l7tech.external.assertions.js.RuntimeScriptException;
import com.l7tech.external.assertions.js.features.bindings.JavaScriptLogger;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.TimeSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Test the ServerJavaScriptAssertion.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerJavaScriptAssertionTest {
    private static final Logger LOGGER = Logger.getLogger( ServerJavaScriptAssertion.class.getName() );
    private static final String TEST_SERVICE_NAME = "JavaScriptTest";

    @Autowired
    private ApplicationContext applicationContext;
    private TestAudit testAudit;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        testAudit = new TestAudit();
    }

    private JavaScriptAssertion createAssertion(final String code) {
        return createAssertion("", code);
    }

    private JavaScriptAssertion createAssertion(final String name, final String code) {
        final JavaScriptAssertion assertion = new JavaScriptAssertion();
        assertion.setName(name);
        assertion.setScript(code);
        return assertion;
    }

    private JavaScriptAssertion createAssertion(final String code, final boolean strictMode, final String timeout) {
        final JavaScriptAssertion assertion = new JavaScriptAssertion();

        assertion.setScript(code);
        assertion.setStrictModeEnabled(strictMode);
        assertion.setExecutionTimeout(timeout);

        return assertion;
    }

    private ServerJavaScriptAssertion createServerAssertion(final JavaScriptAssertion assertion) {
        return ApplicationContexts.inject(new ServerJavaScriptAssertion(assertion, applicationContext), CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );
    }

    private PolicyEnforcementContext createServerContext() {
        return createServerContext(new Message(), new Message());
    }

    private PolicyEnforcementContext createServerContext(final Message request, final Message response) {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private PolicyEnforcementContext createSpiedServerContext() {
        final PolicyEnforcementContext spyContext = spy(createServerContext());
        final PublishedService spyService = spy(new PublishedService());

        doReturn(TEST_SERVICE_NAME).when(spyService).getName();
        doReturn(spyService).when(spyContext).getService();

        return spyContext;
    }

    @Test
    public void testScriptExecution() throws Exception {
        final String code = "context.setVariable( 'js.blah', 'foo' ); return true;";
        final PolicyEnforcementContext pec = createServerContext();
        final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest( pec );

        // Ensure true maps to success
        assertEquals( "Script return value of true is supposed to map to AssertionStatus.NONE",
                AssertionStatus.NONE, status );

        // Ensure that script executed
        assertEquals( "Script is supposed execute",
                "foo", pec.getVariable( "js.blah" ) );
    }

    @Test
    public void testScriptReturningFalse() throws Exception {
        final String code = "context.setVariable( 'js.blah', 'foo' ); return false;";
        final PolicyEnforcementContext pec = createServerContext();
        final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest( pec );

        // Ensure true maps to success
        assertEquals( "Script return value of false is supposed to map to AssertionStatus.FALSIFIED",
                AssertionStatus.FALSIFIED, status );

        // Ensure that script executed and set its variable anyway
        assertEquals( "Script is still supposed to execute",
                "foo", pec.getVariable( "js.blah" ) );
    }

    @Test(expected = RuntimeScriptException.class)
    public void testScriptWithSyntaxError() {
        final String code = "bla &$^@* foo 23 a7sdfapsid9; \n\n\n context.setVariable( 'blah', 'foo' ); return true;";
        final PolicyEnforcementContext pec = createServerContext();
        createServerAssertion(createAssertion(code));
    }

    @Test
    public void testReflectionDisallowed() throws Exception {
        final String code = "context.setVariable( 'blah', context.getClass().getClassLoader().toString() ); return true;";
        final PolicyEnforcementContext pec = createServerContext();
        final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest( pec );

        assertEquals( "Reflection attempt should produce FAILED",
                AssertionStatus.FAILED, status );

        // Remainder of script should NOT have executed due to reflection error
        assertTrue( "Invalid script does not execute",
                pec.getVariableMap( new String[] { "blah" }, testAudit ).isEmpty() );
    }

    @Test
    public void testScriptRestriction_UnsafeDefinitions() {
        final String[][] unsafeDefinitions = new String[][] {
                {"load", "load('https://code.jquery.com/jquery-3.3.1.min.js'); return true;"},
                {"loadWithNewGlobal", "loadWithNewGlobal('https://code.jquery.com/jquery-3.3.1.min.js'); return true;"},
                {"eval", "return eval('1 + 2') == 3;"},
                {"print", "print('Hello World!!'); return true;"},
                {"exit", "exit(); return true;"},
                {"quit", "quit(); return true;"},
                {"print", "print('Hello World!!'); return true;"},
                {"readFully", "readFully('Hello World!!'); return true;"},
                {"Java", "var uuid = Java.type('java.util.UUID'); return true;"}
        };

        for (final String[] unsafeDef : unsafeDefinitions) {
            String unsafeDefinitionString = "Unsafe definition (" + unsafeDef[0] + ")";
            LOGGER.info("Checking over " + unsafeDefinitionString);

            final PolicyEnforcementContext pec = createServerContext();
            final AssertionStatus status = createServerAssertion(createAssertion(unsafeDef[1])).checkRequest( pec );

            assertEquals(unsafeDefinitionString + " should not be allowed and must FAIL.",
                    AssertionStatus.FAILED, status );
            assertTrue("Expecting audit for " + unsafeDefinitionString,
                    testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
            assertTrue("Looking for " + unsafeDefinitionString + " error text",
                    testAudit.isAuditPresentContaining(String.format("\"%s\" is not defined", unsafeDef[0])));
        }
    }

    @Test
    public void testScriptForInsensitiveToDirectContextVariableReferences() throws Exception {
        final String code = "var foo = '${foo}'; context.setVariable( 'bar', foo ); return true;";
        final PolicyEnforcementContext pec = createServerContext();
        pec.setVariable("foo", "hello world!");
        pec.setVariable("bar", "hi!");

        final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);
        assertEquals(AssertionStatus.NONE, status);
        assertNotEquals("hello world!", pec.getVariable("bar"));
        assertEquals("${foo}", pec.getVariable("bar"));
    }

    @Test
    public void testScriptUsingUndeclaredVariablesWithStrictModeEnabled() throws Exception {
        final String code = "foo = 'hello world'; context.setVariable( 'foo', foo ); return true;";
        final PolicyEnforcementContext pec = createServerContext();
        final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

        assertEquals( "Script execution should FAIL",
                AssertionStatus.FAILED, status );
        assertTrue("Expecting audit for variable reference error",
                testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
        assertTrue("Expecting audit for variable reference error",
                testAudit.isAuditPresentContaining("ReferenceError:"));
    }

    @Test
    public void testScriptUsingUndeclaredVariablesWithStrictModeDisabled() throws Exception {
        final String code = "foo = 'hello world'; context.setVariable( 'foo', foo ); return true;";
        final PolicyEnforcementContext pec = createServerContext();
        final AssertionStatus status = createServerAssertion(createAssertion(code, false, "1500")).checkRequest(pec);

        assertEquals( "Script execution should PASS",
                AssertionStatus.NONE, status );
        assertEquals("hello world", pec.getVariable("foo"));
    }

    @Test
    public void testScriptUsingDateVariable() throws Exception {
        final String code = "var someDate = context.getVariable('in.someDate');" +
                "context.setVariable( 'out.someDate', someDate );" +
                "context.setVariable( 'out.currentDate', new Date() );" +
                " return true;";
        final PolicyEnforcementContext pec = createServerContext();
        final Date someDate = new Date(System.currentTimeMillis() + 123456);
        pec.setVariable("in.someDate", someDate);
        final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

        assertEquals( "Script execution should PASS",
                AssertionStatus.NONE, status );
        assertEquals(someDate, pec.getVariable("out.someDate"));
        assertTrue(pec.getVariable("out.currentDate") instanceof  Date);
    }

    @Test
    public void testScriptContextForInterferenceFree() throws Exception {
        final String code1 = "foo = 'hello world'; context.setVariable( 'foo', foo ); return true;";
        final ServerJavaScriptAssertion serverAssertion1 = createServerAssertion(createAssertion(code1, false, "1500"));
        final PolicyEnforcementContext pec1 = createServerContext();
        final AssertionStatus status1 = serverAssertion1.checkRequest( pec1 );

        assertEquals( "Script execution should PASS",
                AssertionStatus.NONE, status1 );
        assertEquals("hello world", pec1.getVariable("foo"));

        final String code2 = "context.setVariable( 'foo', foo ); return true;";
        final ServerJavaScriptAssertion serverAssertion2 = createServerAssertion(createAssertion(code2, false, "1500"));
        final PolicyEnforcementContext pec2 = createServerContext();
        final AssertionStatus status2 = serverAssertion2.checkRequest( pec2 );

        assertEquals( "Script execution should FAIL",
                AssertionStatus.FAILED, status2 );
        assertTrue("Expecting audit for variable reference error",
                testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
        assertTrue("Expecting audit for variable reference error",
                testAudit.isAuditPresentContaining("ReferenceError:"));
    }

    @Test
    public void testScriptDefaultExecutionTimeout() throws Exception {
        final String code = "while(true); return true;";
        final int scriptExecutionTimeout = DEFAULT_EXECUTION_TIMEOUT;
        final int testExecutionTimeout = scriptExecutionTimeout + scriptExecutionTimeout / 2;
        final Pair<AssertionStatus, Long> testStatus = testScriptForExecutionTimeout(createAssertion(code), testExecutionTimeout);

        assertEquals( "Script execution should FAIL",
                AssertionStatus.FAILED, testStatus.left );
        assertTrue("Expecting audit for timeout",
                testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
        assertTrue("Expecting audit for timeout",
                testAudit.isAuditPresentContaining("TimeoutException"));
    }

    @Test
    public void testScriptCustomExecutionTimeout() throws Exception {
        final String code = "while(true); return true;";
        final int scriptExecutionTimeout = DEFAULT_EXECUTION_TIMEOUT * 2 + DEFAULT_EXECUTION_TIMEOUT / 2;
        final int testExecutionTimeout = scriptExecutionTimeout + scriptExecutionTimeout / 2;
        final Pair<AssertionStatus, Long> testStatus = testScriptForExecutionTimeout(createAssertion(code, true, String.valueOf(scriptExecutionTimeout)), testExecutionTimeout);

        assertEquals( "Script execution should FAIL",
                AssertionStatus.FAILED,  testStatus.left);
        assertTrue("Expecting audit for timeout",
                testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
        assertTrue("Expecting audit for timeout",
                testAudit.isAuditPresentContaining("TimeoutException"));
        assertTrue("Expected execution timeout should be greater than the default: " + DEFAULT_EXECUTION_TIMEOUT,
                testStatus.right >= scriptExecutionTimeout);
    }

    private Pair<AssertionStatus, Long> testScriptForExecutionTimeout(final JavaScriptAssertion assertion, final int testExecutionTime) throws Exception {
        // Run the test in separate thread to break the infinite loops.
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Future<Pair<AssertionStatus, Long>> future = executorService.submit(() -> {
            final String code = "while(true); return true;";
            final PolicyEnforcementContext pec = createServerContext();
            final ServerJavaScriptAssertion serverAssertion = createServerAssertion(assertion);
            final TimeSource timeSource = new TimeSource();
            final long startTime = timeSource.currentTimeMillis();
            final AssertionStatus status = serverAssertion.checkRequest(pec);
            final long endTime = timeSource.currentTimeMillis();
            LOGGER.log(Level.INFO, "Script Execution time: " + (endTime - startTime));
            return new Pair<>(status, endTime - startTime);
        });

        try {
            return future.get(testExecutionTime, TimeUnit.MILLISECONDS);
        } finally {
            executorService.shutdown();
        }
    }

    @Test
    public void testScriptLogging() {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "var foo = 'hello world'; logger.log(LOG_LEVEL.WARNING, foo); return true;";
            final JavaScriptAssertion assertion = createAssertion(code);
            final PolicyEnforcementContext pec = createSpiedServerContext();
            final AssertionStatus status = createServerAssertion(assertion).checkRequest(pec);

            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
            final String systemScriptName = TEST_SERVICE_NAME + "_" + assertion.getOrdinal();
            assertTrue(logCollector.indexOf("WARNING:" + systemScriptName + ": hello world") != -1);
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testScriptNameInLogger() {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "logger.log(LOG_LEVEL.WARNING, 'Hello World!'); return true;";
            // Tests user defined script name is being logged in SSG logs
            final String name = "JS_ScriptManager1";
            final AssertionStatus status = createServerAssertion(createAssertion(name, code)).checkRequest(createSpiedServerContext());

            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
            assertTrue(logCollector.indexOf("WARNING:" + name + ": Hello World!") != -1);
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testPrimitiveJSDatatypeRead() {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "var intNum = context.getVariable('intNum') + 2;\n" +
                    "var today = context.getVariable('today');\n" +
                    "var tomorrow = new Date();\n" +
                    "tomorrow.setDate(today.getDate() + 1);\n" +
                    "var name = context.getVariable('name');\n" +
                    "var log = \"int: \" + intNum + \", tomorrow: \" + tomorrow + \", name: \" + name;\n" +
                    "logger.log(LOG_LEVEL.WARNING, log);\n" +
                    "context.setVariable(\"js.log\", log);\n" +
                    "return true;";

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            PolicyEnforcementContext pec = createServerContext();
            pec.setVariable("intNum", 5);
            pec.setVariable("today", calendar.getTime());
            pec.setVariable("name", "John");

            calendar.add(Calendar.DAY_OF_MONTH, 1);

            final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);
            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
            assertTrue(logCollector.indexOf("int: 7") != -1);
            assertTrue(logCollector.indexOf("name: John") != -1);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E MMM dd yyyy");
            final String tomorrow = simpleDateFormat.format(calendar.getTime());
            assertTrue(logCollector.indexOf("tomorrow: " + tomorrow) != -1);

        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testPrimitiveDataTypesWriteAndBoundaryConditions() throws NoSuchVariableException, IOException, InvalidJsonException {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "var intMax = 2147483647; /* Max Int value in java */" +
                    "context.setVariable('js.intMax', intMax);" +
                    "var longNum = 2147483900; /* Number greater than integer range */" +
                    "context.setVariable('js.longNum', longNum);" +
                    "var longMax = 9223372036854775807; /* Number greater than integer range */" +
                    "context.setVariable('js.longMax', longMax);" +
                    "context.setVariable('js.maxDate', new Date(8640000000000000)); /* Max Date in ECMAScript */" +
                    "var object = new Object();" +
                    "object.prop = 'its value';" +
                    "context.setVariable('js.object', object);" +
                    "var numberMax = Number.MAX_VALUE;" +
                    "context.setVariable('js.numberMax', numberMax);" +
                    "var numberMin = Number.MIN_VALUE;" +
                    "context.setVariable('js.numberMin', numberMin);" +
                    "var numberNegativeMin = -Number.MAX_VALUE;" +
                    "context.setVariable('js.numberNegativeMin', numberNegativeMin);" +
                    "return true;";

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            PolicyEnforcementContext pec = createServerContext();

            final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
            assertEquals(2147483900L, (long) pec.getVariable("js.longNum"));
            assertEquals(Long.MAX_VALUE, (long) pec.getVariable("js.longMax"));
            assertEquals(Integer.MAX_VALUE, (int) pec.getVariable("js.intMax"));
            assertEquals(Double.MAX_VALUE, (double) pec.getVariable("js.numberMax"), 0);
            assertEquals(Double.MIN_VALUE, (double) pec.getVariable("js.numberMin"), 0);
            assertEquals(-Double.MAX_VALUE, (double) pec.getVariable("js.numberNegativeMin"), 0);
            assertEquals(new Date(8640000000000000L), pec.getVariable("js.maxDate"));

            final Message object = (Message) pec.getVariable("js.object");
            final String objectContent = object.getJsonKnob().getJsonData().getJsonData();

            assertEquals("{\"prop\":\"its value\"}", objectContent);
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testMessageTypeContextVariable() {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "var message = context.getVariable('bookstore');" +
                    "var content = message.getContent();" +
                    "var contentType = message.getContentType();" +
                    "logger.log(LOG_LEVEL.WARNING, 'Content: ' + content);" +
                    "logger.log(LOG_LEVEL.WARNING, 'ContentType: ' + contentType);" +
                    "return true;";

            PolicyEnforcementContext pec = createServerContext();

            final String xmlContent = "<bookstore><book><title>Everyday Italian</title></book></bookstore>";
            Message message = new Message();
            message.initialize(XmlUtil.stringAsDocument(xmlContent));

            pec.setVariable("bookstore", message);

            final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
            assertTrue(logCollector.indexOf("Content: " + xmlContent) != -1);
            assertTrue(logCollector.indexOf("ContentType: text/xml") != -1);
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testRequestMessageReadWrite() throws IOException, NoSuchPartException {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "var view = context.getVariable('request');" +
                    "var content = view.getContent();" +
                    "var contentType = view.getContentType();" +
                    "if(contentType.indexOf('application/json') != -1) {" +
                    "content.message = 'Hello ' + content.name;" +
                    "}" +
                    "view.removeHeader('Authorization');" +
                    "view.addHeader('Authorization', 'Basic N2xheWVy');" +
                    "logger.log(LOG_LEVEL.WARNING, 'Content: ' + JSON.stringify(content));" +
                    "logger.log(LOG_LEVEL.WARNING, 'ContentType: ' + contentType);" +
                    "logger.log(LOG_LEVEL.WARNING, 'HttpMethod: ' + view.getMethod());" +
                    "logger.log(LOG_LEVEL.WARNING, 'HttpVersion: ' + view.getHttpVersion());" +
                    "logger.log(LOG_LEVEL.WARNING, 'URL: ' + view.getUrl());" +
                    "logger.log(LOG_LEVEL.WARNING, 'HasHeaderContentType: ' + view.hasHeader('Content-Type'));" +
                    "view.setContent(content, contentType);" +
                    "return true;";

            Message request = createRequestMessageWithHttpServletRequestKnob();
            Message response = new Message();
            PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request,
                    response);

            final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

            final String modifiedRequestJson = "{\"name\":\"John\",\"message\":\"Hello John\"}";

            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);

            final String updatedRequestMessage = new String(IOUtils.slurpStream(request.getMimeKnob()
                    .getEntireMessageBodyAsInputStream()), request.getMimeKnob().getFirstPart().getContentType().getEncoding());

            assertEquals(modifiedRequestJson, updatedRequestMessage);
            assertTrue(logCollector.indexOf("Content: " + modifiedRequestJson) != -1);
            assertTrue(logCollector.indexOf("ContentType: application/json") != -1);
            assertTrue(logCollector.indexOf("HttpMethod: POST") != -1);
            assertTrue(logCollector.indexOf("HttpVersion: 1.1") != -1);
            assertTrue(logCollector.indexOf("URL: http://localhost:80/js?name=John") != -1);
            assertTrue(logCollector.indexOf("HasHeaderContentType: true") != -1);
            assertFalse(request.getHeadersKnob().containsHeader("RandomAuth", HeadersKnob.HEADER_TYPE_HTTP));
            assertTrue(request.getHeadersKnob().containsHeader("Authorization", HeadersKnob.HEADER_TYPE_HTTP));
            assertEquals("Basic N2xheWVy", request.getHeadersKnob().getHeaderValues("Authorization", HeadersKnob
                    .HEADER_TYPE_HTTP)[0]);
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testRequestMessageHeaders() throws IOException {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "var view = context.getVariable('request');" +
                    "var headers = view.getHeaders();" +
                    "logger.log(LOG_LEVEL.WARNING, 'ContentTypeHeader: ' + headers['Content-Type']);" +
                    "logger.log(LOG_LEVEL.WARNING, 'AuthorizationHeader: ' + headers.Authorization);" +
                    "headers = {\"header1\":\"header1Value\",\"header2\":[\"header2Value1\",\"header2Value2\"]};" +
                    "view.setHeaders(headers);" +
                    "return true;";

            Message request = createRequestMessageWithHttpServletRequestKnob();
            Message response = new Message();
            PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request,
                    response);

            final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);
            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
            assertTrue(logCollector.indexOf("ContentTypeHeader: application/json") != -1);
            assertTrue(logCollector.indexOf("AuthorizationHeader: Basic") != -1);
            assertEquals(3, request.getHeadersKnob().getHeaders().size());
            assertTrue(request.getHeadersKnob().containsHeader("header1", HeadersKnob.HEADER_TYPE_HTTP));
            assertTrue(request.getHeadersKnob().containsHeader("header2", HeadersKnob.HEADER_TYPE_HTTP));
            assertEquals(2, request.getHeadersKnob().getHeaderValues("header2", HeadersKnob.HEADER_TYPE_HTTP).length);
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testResponseMessageReadWrite() throws IOException, NoSuchPartException {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "var view = context.getVariable('request');" +
                    "var headers = view.getHeaders();" +
                    "var authHeader = headers.Authorization;" +
                    "if(authHeader.indexOf('Basic N2xheWVy') == -1) {" +
                    "var res = context.getVariable('response');" +
                    "logger.log(LOG_LEVEL.WARNING, 'BeforeStatusCode: ' + res.getStatusCode());" +
                    "res.setContent('Invalid Authorization header.', 'text/plain', 500);" +
                    "logger.log(LOG_LEVEL.WARNING, 'HasContentType: ' + res.hasHeader('Content-Type'));" +
                    "var resHeaders = {};" +
                    "resHeaders['Content-Type'] = 'text/plain';" +
                    "res.setHeaders(resHeaders);" +
                    "}" +
                    "return true;";

            Message request = createRequestMessageWithHttpServletRequestKnob();
            Message response = createResponseMessageWithHttpServletResponseKnob();
            PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request,
                    response);

            final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

            final String updatedResponseMessage = new String(IOUtils.slurpStream(response.getMimeKnob()
                    .getEntireMessageBodyAsInputStream()), response.getMimeKnob().getFirstPart().getContentType()
                    .getEncoding());

            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
            assertEquals( 500, response.getHttpResponseKnob().getStatus());
            assertEquals("Invalid Authorization header.", updatedResponseMessage);
            assertTrue(logCollector.indexOf("HasContentType: false") != -1);
            assertEquals("text/plain", response.getHeadersKnob().getHeaderValues("Content-Type")[0]);
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testRequestHttpViewReadWrite() throws IOException, NoSuchPartException {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "var view = context.getVariable('request:http');" +
                    "var content = view.getContent();" +
                    "logger.log(LOG_LEVEL.WARNING, 'Content: ' + JSON.stringify(content));" +
                    "logger.log(LOG_LEVEL.WARNING, 'ContentType: ' + view.contentType);" +
                    "logger.log(LOG_LEVEL.WARNING, 'HttpMethod: ' + view.method);" +
                    "logger.log(LOG_LEVEL.WARNING, 'HttpVersion: ' + view.httpVersion);" +
                    "logger.log(LOG_LEVEL.WARNING, 'URL: ' + view.url);" +
                    "logger.log(LOG_LEVEL.WARNING, 'ParameterName: ' + view.parameters.name);" +
                    "logger.log(LOG_LEVEL.WARNING, 'MultiParam: ' + view.parameters.multiparam);" +
                    "logger.log(LOG_LEVEL.WARNING, 'MultiParam[0]: ' + view.parameters.multiparam[0]);" +
                    "content = 'Content and content type changed.';" +
                    "view.setContent(content, 'text/plain');" +
                    "logger.log(LOG_LEVEL.WARNING, 'ChangedContentType: ' + view.contentType);" +
                    "var headers = view.headers;" +
                    "logger.log(LOG_LEVEL.WARNING, 'AuthorizationHeader: ' + headers.Authorization);" +
                    "headers = {};" +
                    "view.headers = headers;" +
                    "view.end();" +
                    "return true;";

            Message request = createRequestMessageWithHttpServletRequestKnob();
            Message response = createResponseMessageWithHttpServletResponseKnob();
            PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request,
                    response);

            final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

            final String updatedRequestMessage = new String(IOUtils.slurpStream(request.getMimeKnob()
                    .getEntireMessageBodyAsInputStream()), request.getMimeKnob().getFirstPart().getContentType().getEncoding());

            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
            assertTrue(logCollector.indexOf("Content: {\"name\":\"John\"}") != -1);
            assertTrue(logCollector.indexOf("ContentType: application/json") != -1);
            assertTrue(logCollector.indexOf("HttpMethod: POST") != -1);
            assertTrue(logCollector.indexOf("HttpVersion: 1.1") != -1);
            assertTrue(logCollector.indexOf("URL: http://localhost:80/js?name=John") != -1);
            assertTrue(logCollector.indexOf("ParameterName: John") != -1);
            assertTrue(logCollector.indexOf("MultiParam: multiParamVal1,multiParamVal2") != -1);
            assertTrue(logCollector.indexOf("MultiParam[0]: multiParamVal1") != -1);
            assertTrue(logCollector.indexOf("ChangedContentType: text/plain") != -1);
            assertTrue(logCollector.indexOf("AuthorizationHeader: Basic") != -1);
            assertEquals("Content and content type changed.", updatedRequestMessage);
            assertTrue(request.getHeadersKnob().getHeaders().isEmpty());
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testHttpResponseViewReadWrite() throws IOException, NoSuchPartException {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "var view = context.getVariable('request');" +
                    "var headers = view.getHeaders();" +
                    "var authHeader = headers.Authorization;" +
                    "logger.log(LOG_LEVEL.WARNING, 'AuthorizationHeader: ' + headers.Authorization);" +
                    "if(authHeader.indexOf('Basic N2xheWVy') == -1) {" +
                    "var res = context.getVariable('response:http');" +
                    "logger.log(LOG_LEVEL.WARNING, 'BeforeStatusCode: ' + res.statusCode);" +
                    "logger.log(LOG_LEVEL.WARNING, 'BeforeStatusCode: ' + res.statusCode);" +
                    "res.setContent('Invalid Authorization header.', 'text/plain', 500);" +
                    "var resHeaders = {};" +
                    "resHeaders['Content-Type'] = 'text/plain';" +
                    "res.headers = resHeaders;" +
                    "res.end();" +
                    "}" +
                    "return true;";

            Message request = createRequestMessageWithHttpServletRequestKnob();
            Message response = createResponseMessageWithHttpServletResponseKnob();
            PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request,
                    response);

            final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

            final String updatedResponseMessage = new String(IOUtils.slurpStream(response.getMimeKnob()
                    .getEntireMessageBodyAsInputStream()), response.getMimeKnob().getFirstPart().getContentType()
                    .getEncoding());

            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
            assertEquals( 500, response.getHttpResponseKnob().getStatus());
            assertEquals("Invalid Authorization header.", updatedResponseMessage);
            assertTrue(logCollector.indexOf("BeforeStatusCode: 200") != -1);
            assertTrue(logCollector.indexOf("AuthorizationHeader: Basic") != -1);
            assertEquals("text/plain", response.getHeadersKnob().getHeaderValues("Content-Type")[0]);
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testRequestResponseHeadersViewReadWrite() throws IOException {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "var reqHeaders = context.getVariable('request:headers');" +
                    "var resHeaders = context.getVariable('response:headers');" +
                    "if(reqHeaders.headers.Authorization == 'Basic') {" +
                    "resHeaders.headers.Authorization = 'Basic N2xheWVy';" +
                    "delete reqHeaders.headers.Authorization;" +
                    "}" +
                    "reqHeaders.end(); resHeaders.end();" +
                    "return true;";

            Message request = createRequestMessageWithHttpServletRequestKnob();
            Message response = createResponseMessageWithHttpServletResponseKnob();
            PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request,
                    response);

            final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
            assertEquals("Basic N2xheWVy", response.getHeadersKnob().getHeaderValues("Authorization")[0]);
            assertFalse(request.getHeadersKnob().containsHeader("Authorization", HeadersKnob.HEADER_TYPE_HTTP));
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testRequestUrlView() throws IOException {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "var url = context.getVariable('request:url');" +
                    "logger.log(LOG_LEVEL.WARNING, JSON.stringify(url.query));" +
                    "logger.log(LOG_LEVEL.WARNING, 'Protocol: ' + url.protocol);" +
                    "logger.log(LOG_LEVEL.WARNING, 'Host: ' + url.host);" +
                    "logger.log(LOG_LEVEL.WARNING, 'Port: ' + url.port);" +
                    "logger.log(LOG_LEVEL.WARNING, 'Path: ' + url.path);" +
                    "logger.log(LOG_LEVEL.WARNING, 'QueryParamName: ' + url.query.name);" +
                    "logger.log(LOG_LEVEL.WARNING, 'QueryMultiParam: ' + url.query.multiparam);" +
                    "logger.log(LOG_LEVEL.WARNING, 'QueryMultiParam1stValue: ' + url.query.multiparam[0]);" +
                    "logger.log(LOG_LEVEL.WARNING, 'QueryString: ' + url.queryString);" +
                    "return true;";

            Message request = createRequestMessageWithHttpServletRequestKnob();
            Message response = createResponseMessageWithHttpServletResponseKnob();
            PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request,
                    response);

            final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
            assertTrue(logCollector.indexOf("Protocol: http") != -1);
            assertTrue(logCollector.indexOf("Host: localhost") != -1);
            assertTrue(logCollector.indexOf("Port: 80") != -1);
            assertTrue(logCollector.indexOf("Path: /js") != -1);
            assertTrue(logCollector.indexOf("QueryParamName: John") != -1);
            assertTrue(logCollector.indexOf("QueryMultiParam: multiParamVal1,multiParamVal2") != -1);
            assertTrue(logCollector.indexOf("QueryMultiParam1stValue: multiParamVal1") != -1);
            assertTrue(logCollector.indexOf("QueryString: name=John&multiparam=multiParamVal1&multiparam=multiParamVal2") != -1);
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testCopyRequestHeaders() throws IOException {
        final StringBuilder logCollector = new StringBuilder();
        final Handler logHandler = createLogHandler(logCollector);
        Logger.getLogger(JavaScriptLogger.class.getName()).addHandler(logHandler);

        try {
            final String code = "// Log the expected headers from the request\n" +
                    "var req = context.getVariable(\"request\");\n" +
                    "var reqHeaders = req.getHeaders();\n" +
                    "logger.log(LOG_LEVEL.WARNING, 'ContentTypeHeader: ' + reqHeaders['Content-Type']);\n" +
                    "logger.log(LOG_LEVEL.WARNING, 'AuthorizationHeader: ' + reqHeaders.Authorization);\n" +
                    "\n" +
                    "// Copy the request headers to response\n" +
                    "var resp = context.getVariable(\"response\");\n" +
                    "resp.setHeaders(req.getHeaders());\n" +
                    "return true;";
            final Message request = createRequestMessageWithHttpServletRequestKnob();
            final Message response = new Message();
            final PolicyEnforcementContext pec = createServerContext(request, response);

            assertEquals(0, response.getHeadersKnob().getHeaders().size());
            final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

            assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
            assertTrue(logCollector.indexOf("ContentTypeHeader: application/json") != -1);
            assertTrue(logCollector.indexOf("AuthorizationHeader: Basic") != -1);
            assertEquals(2, request.getHeadersKnob().getHeaders().size());

            assertEquals(2, response.getHeadersKnob().getHeaders().size());
            assertArrayEquals(new String[] {"Authorization", "Content-Type"}, response.getHeadersKnob().getHeaderNames());
            assertArrayEquals(new String[] {"Basic"}, response.getHeadersKnob().getHeaderValues("Authorization"));
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
    }

    @Test
    public void testModifyRequestResponseHeaders() throws IOException {
        final String code = "// Modify request headers using add/set methods\n" +
                "var req = context.getVariable(\"request\");\n" +
                "req.setHeader(\"header1\", \"value1\");\n" +
                "req.setHeader(\"header2\", \"value2.1\");\n" +
                "\n" +
                "// Copy request headers to response\n" +
                "// Modify request headers using add/set methods\n" +
                "var resp = context.getVariable(\"response\");\n" +
                "resp.setHeaders(req.getHeaders());\n" +
                "resp.setHeader(\"header1\", \"new-value1\");\n" +
                "resp.addHeader(\"header2\", \"value2.2\");\n" +
                "resp.removeHeader(\"Authorization\");\n" +
                "\n" +
                "return true;";
        final Message request = createRequestMessageWithHttpServletRequestKnob();
        final Message response = new Message();
        final PolicyEnforcementContext pec = createServerContext(request, response);

        assertEquals(0, response.getHeadersKnob().getHeaders().size());
        final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

        assertEquals("Script execution should PASS", AssertionStatus.NONE, status);
        assertEquals(4, request.getHeadersKnob().getHeaders().size());
        assertArrayEquals(new String[] {"Authorization", "Content-Type", "header1", "header2"}, request.getHeadersKnob().getHeaderNames());

        assertArrayEquals(new String[] {"Content-Type", "header1", "header2"}, response.getHeadersKnob().getHeaderNames());
        assertArrayEquals(new String[] {"new-value1"}, response.getHeadersKnob().getHeaderValues("header1"));
        assertArrayEquals(new String[] {"value2.1", "value2.2"}, response.getHeadersKnob().getHeaderValues("header2"));
    }

    private Message createRequestMessageWithHttpServletRequestKnob() throws IOException {
        Message request = new Message();
        request.initialize(ContentTypeHeader.APPLICATION_JSON, "{\"name\":\"John\"}".getBytes());
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setRequestURI("/js");
        hrequest.setQueryString("name=John&multiparam=multiParamVal1&multiparam=multiParamVal2");
        hrequest.addParameter("a", new String[] {"a", "b"});
        hrequest.setMethod("POST");
        hrequest.setProtocol("HTTP/1.1");
        hrequest.addHeader("Content-Type", ContentTypeHeader.TEXT_DEFAULT.getFullValue());
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        request.getHeadersKnob().addHeader("Content-Type", ContentTypeHeader.APPLICATION_JSON.getFullValue(), HeadersKnob.HEADER_TYPE_HTTP);
        request.getHeadersKnob().addHeader("Authorization", "Basic", HeadersKnob.HEADER_TYPE_HTTP);
        return request;
    }

    private Message createResponseMessageWithHttpServletResponseKnob() throws IOException {
        Message response = new Message();
        response.initialize(ContentTypeHeader.TEXT_DEFAULT, "Ok".getBytes());
        MockHttpServletResponse hresponse = new MockHttpServletResponse();
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));
        response.getHttpResponseKnob().setStatus(200);
        return response;
    }

    private Handler createLogHandler(final StringBuilder collector) {
        return new Handler() {
            @Override
            public void publish(LogRecord record) {
                collector.append(record.getLevel() + ":" + record.getMessage());
                collector.append(System.lineSeparator());
            }

            @Override
            public void flush() {
                // does nothing
            }

            @Override
            public void close() throws SecurityException {
                // does nothing
            }
        };
    }

    private String getScriptFromJavaScriptFile(final String file) throws IOException {
        final StringBuilder codeBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(file)))) {
            codeBuilder.append(reader.readLine());
            codeBuilder.append(System.lineSeparator());
        }

        return codeBuilder.toString();
    }

}
