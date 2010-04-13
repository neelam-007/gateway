package com.l7tech.external.assertions.concall.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.concall.ConcurrentAllAssertion;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.InvalidPolicyException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.audit.AuditDetailProcessingAuditListener;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.*;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Test the ConcurrentAllAssertion.
 */
public class ServerConcurrentAllAssertionTest {
    private static final Logger log = Logger.getLogger(ServerConcurrentAllAssertionTest.class.getName());

    static AssertionRegistry assertionRegistry;
    static ServerPolicyFactory serverPolicyFactory;
    static DefaultListableBeanFactory beanFactory;
    static GenericApplicationContext applicationContext;
    static ConcurrentDetailCollectingAuditContext auditContext;
    static PolicyCache policyCache;


    @BeforeClass
    public static void registerAssertions() throws Exception {
        assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        assertionRegistry.registerAssertion(ConcurrentAllAssertion.class);
        assertionRegistry.registerAssertion(DelayedCopyVariableAssertion.class);
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager());
        auditContext = new ConcurrentDetailCollectingAuditContext();
        auditContext.logDetails = true;
        policyCache = new PolicyCacheImpl(null, new ServerPolicyFactory(new TestLicenseManager())) {
            {
                PolicyManagerStub policyManager = new PolicyManagerStub( new Policy[0] );
                setPolicyManager(policyManager);
                setApplicationEventPublisher(new EventChannel());
                onApplicationEvent(new Started(this, Component.GATEWAY, "Test"));
            }

            @Override
            protected ServerAssertion buildServerPolicy(Policy policy) throws ServerPolicyInstantiationException, ServerPolicyException, InvalidPolicyException {
                throw new ServerPolicyInstantiationException("stub");
            }

            @Override
            protected void logAndAudit(AuditDetailMessage message, String[] params, Exception ex) {
                log(message, params, ex);
            }

            @Override
            protected void logAndAudit(AuditDetailMessage message, String... params) {
                log(message, params, null);
            }

            void log(AuditDetailMessage message, String[] params, Exception ex) {
                LogRecord record = new LogRecord(message.getLevel(), message.getMessage());
                record.setParameters(params);
                record.setThrown(ex);
                record.setSourceClassName("");
                record.setSourceMethodName("");
                log.log(record);
            }
        };
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("serverConfig", ServerConfig.getInstance());
            put("policyFactory", serverPolicyFactory);
            put("auditContext", auditContext);
            put("policyCache", policyCache);
        }});
        applicationContext = new GenericApplicationContext(beanFactory);
        serverPolicyFactory.setApplicationContext(applicationContext);
        applicationContext.addApplicationListener(new AuditDetailProcessingAuditListener(auditContext));
        applicationContext.refresh();


    }

    @Before
    public void before() {
        auditContext.clearAll();
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
                new CommentAssertion("blah"),
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

    static Message makeMessage(ContentTypeHeader contentType, String body) throws IOException {
        return new Message(new ByteArrayStashManager(), contentType, new ByteArrayInputStream(body.getBytes()));
    }

    static String toString(Message message) throws IOException, NoSuchPartException {
        return new String(IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream()));
    }

    @Test
    public void testMessageVariables() throws Exception {
        ConcurrentAllAssertion ass = new ConcurrentAllAssertion(Arrays.asList(
                new AllAssertion(Arrays.asList(
                        new DelayedCopyVariableAssertion(2000L, "source1", "dest1")
                )),
                new AllAssertion(Arrays.asList(
                        new DelayedCopyVariableAssertion(2000L, "source2", "dest2")
                ))
        ));
        ServerAssertion sass = serverPolicyFactory.compilePolicy(ass, false);

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("source1", makeMessage(ContentTypeHeader.TEXT_DEFAULT, "var content 1"));
        context.setVariable("source2", makeMessage(ContentTypeHeader.XML_DEFAULT, "<foo>var content 2</foo>"));

        AssertionStatus status = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        assertEquals("var content 1", toString((Message)context.getVariable("dest1")));
        assertEquals("<foo>var content 2</foo>", toString((Message)context.getVariable("dest2")));
    }

    @Test
    public void testAuditDetailMessages() throws Exception {
        ConcurrentAllAssertion ass = new ConcurrentAllAssertion(Arrays.asList(
                new AllAssertion(Arrays.asList(
                        new AuditDetailAssertion("detail1")
                )),
                new AllAssertion(Arrays.asList(
                        new AuditDetailAssertion("detail2"),
                        new AuditDetailAssertion("detail3")
                ))
        ));
        ServerAssertion sass = serverPolicyFactory.compilePolicy(ass, false);

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("source1", makeMessage(ContentTypeHeader.TEXT_DEFAULT, "var content 1"));

        AssertionStatus status = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        // Ensure all audit details from concurrent subpolicies got collected.  Note that the order they get saved in is undefined.
        List<AuditDetail> details = auditContext.getCurrentThreadDetails();
        assertEquals(3, details.size());
        Set<String> msgs = new HashSet<String>();
        msgs.add(details.get(0).getParams()[0]);
        msgs.add(details.get(1).getParams()[0]);
        msgs.add(details.get(2).getParams()[0]);
        assertTrue(msgs.contains("detail1"));
        assertTrue(msgs.contains("detail2"));
        assertTrue(msgs.contains("detail3"));
    }

    @Test
    public void testRejectedSubmission() throws Exception {
        ServerConcurrentAllAssertion.resetAssertionExecutor(1, 1, 1);
        try {
            ConcurrentAllAssertion ass = new ConcurrentAllAssertion(Arrays.asList(
                    new AllAssertion(Arrays.asList(
                            new DelayedCopyVariableAssertion(2000L, "source1", "dest1")
                    )),
                    new AllAssertion(Arrays.asList(
                            new DelayedCopyVariableAssertion(2000L, "source2", "dest2")
                    )),
                    new AllAssertion(Arrays.asList(
                            new DelayedCopyVariableAssertion(2000L, "source3", "dest3")
                    )),
                    new AllAssertion(Arrays.asList(
                            new DelayedCopyVariableAssertion(2000L, "source4", "dest4")
                    ))
            ));
            ServerAssertion sass = serverPolicyFactory.compilePolicy(ass, false);

            PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
            context.setVariable("source1", makeMessage(ContentTypeHeader.TEXT_DEFAULT, "var content 1"));
            context.setVariable("source2", makeMessage(ContentTypeHeader.XML_DEFAULT, "<foo>var content 2</foo>"));
            context.setVariable("source3", makeMessage(ContentTypeHeader.TEXT_DEFAULT, "var content 3"));
            context.setVariable("source4", makeMessage(ContentTypeHeader.XML_DEFAULT, "<foo>var content 4</foo>"));

            long before = System.currentTimeMillis();
            AssertionStatus status = sass.checkRequest(context);
            long after = System.currentTimeMillis();
            assertEquals(AssertionStatus.NONE, status);

            assertTrue("Should not have achieved full concurrency with only 1 worker thread", after - before > 4000L);

            assertEquals("var content 1", toString((Message)context.getVariable("dest1")));
            assertEquals("<foo>var content 2</foo>", toString((Message)context.getVariable("dest2")));
            assertEquals("var content 3", toString((Message)context.getVariable("dest3")));
            assertEquals("<foo>var content 4</foo>", toString((Message)context.getVariable("dest4")));
        } finally {
            ServerConcurrentAllAssertion.resetAssertionExecutor(64, 32, 64);
        }
    }

    @Test
    @BugNumber(8669)
    public void testReadOnlyBuiltinVariable() throws Exception {
        ConcurrentAllAssertion ass = new ConcurrentAllAssertion(Arrays.asList(
            new SetVariableAssertion("date", "${gateway.time}")
        ));
        ServerAssertion sass = serverPolicyFactory.compilePolicy(ass, false);

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        // Run all requests concurrently
        AssertionStatus status = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        assertEquals(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)), context.getVariable("date").toString().substring(0, 4));
    }
}
