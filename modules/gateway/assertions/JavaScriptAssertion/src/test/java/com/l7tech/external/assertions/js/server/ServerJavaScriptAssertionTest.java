package com.l7tech.external.assertions.js.server;

import static com.l7tech.external.assertions.js.features.JavaScriptAssertionConstants.DEFAULT_EXECUTION_TIMEOUT;
import static org.junit.Assert.*;

import com.l7tech.external.assertions.js.JavaScriptAssertion;
import com.l7tech.external.assertions.js.features.JavaScriptLogger;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.TimeSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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

    @Autowired
    private ApplicationContext applicationContext;
    private TestAudit testAudit;
    private ServerConfig serverConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        testAudit = new TestAudit();
        serverConfig = applicationContext.getBean("serverConfig", ServerConfig.class);
    }

    private JavaScriptAssertion createAssertion(final String code) {
        final JavaScriptAssertion assertion = new JavaScriptAssertion();
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
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
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

    @Test
    public void testScriptWithSyntaxError() {
        final String code = "bla &$^@* foo 23 a7sdfapsid9; \n\n\n context.setVariable( 'blah', 'foo' ); return true;";
        final PolicyEnforcementContext pec = createServerContext();
        final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest( pec );

        assertEquals("Syntax error should produce FAILED",
                AssertionStatus.FAILED, status );
        assertTrue("Exception must be logged/audited",
                testAudit.isAuditPresent(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO));
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
            final String code = "var foo = 'hello world'; logger.log('WARNING', foo); return true;";
            final PolicyEnforcementContext pec = createServerContext();
            final AssertionStatus status = createServerAssertion(createAssertion(code)).checkRequest(pec);

            assertEquals("Script execution should PASS",
                    AssertionStatus.NONE, status);
            assertTrue(logCollector.indexOf("WARNING:hello world") != -1);
        } finally {
            Logger.getLogger(JavaScriptLogger.class.getName()).removeHandler(logHandler);
        }
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

}
