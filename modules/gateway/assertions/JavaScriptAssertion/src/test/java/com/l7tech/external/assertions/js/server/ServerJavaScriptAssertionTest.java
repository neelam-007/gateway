package com.l7tech.external.assertions.js.server;

import static org.junit.Assert.*;

import com.l7tech.external.assertions.js.JavaScriptAssertion;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the JavaScriptAssertion.
 */
public class ServerJavaScriptAssertionTest {
    private static final Logger logger = Logger.getLogger( ServerJavaScriptAssertion.class.getName() );
    private Audit audit = new LoggingAudit( logger );

    @BeforeClass
    public static void init() {
        ServerJavaScriptAssertion.allowNoSecurityManager = true;
    }

    @AfterClass
    public static void cleanUp() {
        ServerJavaScriptAssertion.allowNoSecurityManager = false;
    }

    @Test
    public void testScriptExecution() throws Exception {
        JavaScriptAssertion jas = new JavaScriptAssertion();
        jas.encodeScript( "context.setVariable( \"blah\", \"foo\" ); true; " );

        ServerJavaScriptAssertion sass = new ServerJavaScriptAssertion( jas, null );

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
        AssertionStatus got = sass.checkRequest( pec );

        // Ensure true maps to success
        assertEquals( "Script return value of true is supposed to map to AssertionStatus.NONE", AssertionStatus.NONE, got );

        // Ensure that script executed
        assertEquals( "Script is supposed execute",  "foo", pec.getVariable( "blah" ) );
    }

    @Test
    public void testScriptReturningFalse() throws Exception {
        JavaScriptAssertion jas = new JavaScriptAssertion();
        jas.encodeScript( "context.setVariable( \"blah\", \"foo\" ); false; " );

        ServerJavaScriptAssertion sass = new ServerJavaScriptAssertion( jas, null );

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
        AssertionStatus got = sass.checkRequest( pec );

        // Ensure true maps to success
        assertEquals( "Script return value of false is supposed to map to AssertionStatus.FALSIFIED", AssertionStatus.FALSIFIED, got );

        // Ensure that script executed and set its variable anyway
        assertEquals( "Script is still supposed to execute",  "foo", pec.getVariable( "blah" ) );
        assertEquals( "Script is still supposed to execute", 1, pec.getVariableMap( new String[] { "blah" }, audit ).size() );

    }

    @Test
    public void testScriptWithSyntaxError() throws Exception {
        JavaScriptAssertion jas = new JavaScriptAssertion();
        jas.encodeScript( " bla &$^@* foo 23 a7sdfapsid9; \n\n\n context.setVariable( \"blah\", \"foo\" ); true; " );

        ServerJavaScriptAssertion sass = new ServerJavaScriptAssertion( jas, null );

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
        AssertionStatus got = sass.checkRequest( pec );

        assertEquals( "Syntax error should produce FAILED", AssertionStatus.FAILED, got );

        // Remainder of script should NOT have executed due to syntax error
        assertTrue( "Invalid script does not execute", pec.getVariableMap( new String[] { "blah" }, audit ).isEmpty() );
    }

    @Test
    public void testReflectionDisallowed() throws Exception {
        JavaScriptAssertion jas = new JavaScriptAssertion();
        jas.encodeScript( " context.setVariable( \"blah\", context.getClass().getClassLoader().toString(); true; " );

        ServerJavaScriptAssertion sass = new ServerJavaScriptAssertion( jas, null );

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
        AssertionStatus got = sass.checkRequest( pec );

        assertEquals( "Reflection attempt should produce FAILED", AssertionStatus.FAILED, got );

        // Remainder of script should NOT have executed due to reflection error
        assertTrue( "Invalid script does not execute", pec.getVariableMap( new String[] { "blah" }, audit ).isEmpty() );
    }



}
