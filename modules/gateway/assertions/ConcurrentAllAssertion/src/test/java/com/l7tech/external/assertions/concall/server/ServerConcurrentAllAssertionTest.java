package com.l7tech.external.assertions.concall.server;

import com.l7tech.external.assertions.concall.ConcurrentAllAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import org.junit.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Test the ConcurrentAllAssertion.
 */
public class ServerConcurrentAllAssertionTest {
    private static final Logger log = Logger.getLogger(ServerConcurrentAllAssertionTest.class.getName());

    static AssertionRegistry assertionRegistry;
    static ServerPolicyFactory serverPolicyFactory;
    static DefaultListableBeanFactory beanFactory;
    static GenericApplicationContext applicationContext;


    @BeforeClass
    public static void registerAssertions() throws Exception {
        assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        assertionRegistry.registerAssertion(ConcurrentAllAssertion.class);
        assertionRegistry.registerAssertion(DelayedCopyVariableAssertion.class);
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager());
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("serverConfig", ServerConfig.getInstance());
            put("policyFactory", serverPolicyFactory);
        }});
        applicationContext = new GenericApplicationContext(beanFactory);
        serverPolicyFactory.setApplicationContext(applicationContext);
        applicationContext.refresh();
    }

    @Test
    public void testConcurrentExecution() throws Exception {
        ConcurrentAllAssertion ass = new ConcurrentAllAssertion(Arrays.asList(
                new AllAssertion(Arrays.asList(
                        new DelayedCopyVariableAssertion(2000L, "source1", "dest1")
                )),
                new AllAssertion(Arrays.asList(
                        new DelayedCopyVariableAssertion(2000L, "source2", "dest2")
                )),
                new AllAssertion(Arrays.asList(
                        new DelayedCopyVariableAssertion(2000L, "source3", "dest3"),
                        new TrueAssertion()
                )),
                new OneOrMoreAssertion(Arrays.asList(
                        new FalseAssertion(),
                        new DelayedCopyVariableAssertion(2000L, "source4", "dest4"),
                        new DelayedCopyVariableAssertion(2000L, "sourceNever", "destNever") // Not reached -- OneOrMore should short-circuit
                ))
        ));
        ServerAssertion sass = serverPolicyFactory.compilePolicy(ass, false);

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("source1", "var content 1");
        context.setVariable("source2", "var content 2");
        context.setVariable("source3", "var content 3");
        context.setVariable("source4", "var content 4");

        // Run all requests concurrently
        long before = System.currentTimeMillis();
        AssertionStatus status = sass.checkRequest(context);
        long after = System.currentTimeMillis();
        assertEquals(AssertionStatus.NONE, status);

        assertTrue("Should have taken less than 4 seconds if it ran concurrently", after - before < 4000L);

        assertEquals("var content 1", context.getVariable("dest1"));
        assertEquals("var content 2", context.getVariable("dest2"));
        assertEquals("var content 3", context.getVariable("dest3"));
        assertEquals("var content 4", context.getVariable("dest4"));

        try {
            context.getVariable("destNever");
            fail("Expected exception not thrown: variable destNever should not exist as the server assertion should not have been reached");
        } catch (NoSuchVariableException e) {
            // Ok
        }
    }

    @Test
    public void testConcurrentExecutionWithFailure() throws Exception {
        ConcurrentAllAssertion ass = new ConcurrentAllAssertion(Arrays.asList(
                new AllAssertion(Arrays.asList(
                        new DelayedCopyVariableAssertion(2000L, "source1", "dest1")
                )),
                new AllAssertion(Arrays.asList(
                        new DelayedCopyVariableAssertion(2000L, "source2", "dest2")
                )),
                new AllAssertion(Arrays.asList(
                        new DelayedCopyVariableAssertion(2000L, "source3", "dest3"),
                        new FalseAssertion() // Will cause outermost all to fail
                )),
                new OneOrMoreAssertion(Arrays.asList(
                        new FalseAssertion(),
                        new DelayedCopyVariableAssertion(2000L, "source4", "dest4"),
                        new DelayedCopyVariableAssertion(2000L, "sourceNever", "destNever") // Not reached -- OneOrMore should short-circuit
                ))
        ));
        ServerAssertion sass = serverPolicyFactory.compilePolicy(ass, false);

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("source1", "var content 1");
        context.setVariable("source2", "var content 2");
        context.setVariable("source3", "var content 3");
        context.setVariable("source4", "var content 4");

        // Run all requests concurrently
        long before = System.currentTimeMillis();
        AssertionStatus status = sass.checkRequest(context);
        long after = System.currentTimeMillis();
        assertEquals(AssertionStatus.FALSIFIED, status);

        assertTrue("Should have taken less than 4 seconds if it ran concurrently", after - before < 4000L);

        assertEquals("var content 1", context.getVariable("dest1"));
        assertEquals("var content 2", context.getVariable("dest2"));
        assertEquals("var content 3", context.getVariable("dest3"));
        assertEquals("var content 4", context.getVariable("dest4"));

        try {
            context.getVariable("destNever");
            fail("Expected exception not thrown: variable destNever should not exist as the server assertion should not have been reached");
        } catch (NoSuchVariableException e) {
            // Ok
        }
    }
}
