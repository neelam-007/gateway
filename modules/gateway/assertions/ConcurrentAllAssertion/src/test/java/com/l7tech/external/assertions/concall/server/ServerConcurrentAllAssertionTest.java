package com.l7tech.external.assertions.concall.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.concall.ConcurrentAllAssertion;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.audit.AuditContextFactoryStub;
import com.l7tech.server.audit.AuditDetailProcessingAuditListener;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.folder.FolderCacheStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.*;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.MockInjector;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.IOUtils;
import com.l7tech.util.MockConfig;
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
    static ConcurrentDetailCollectingAuditContextStub auditContext;
    static PolicyCache policyCache;


    @BeforeClass
    public static void registerAssertions() throws Exception {
        assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        assertionRegistry.registerAssertion(ConcurrentAllAssertion.class);
        assertionRegistry.registerAssertion(DelayedCopyVariableAssertion.class);
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(),new MockInjector());
        auditContext = new ConcurrentDetailCollectingAuditContextStub();
        auditContext.logDetails = true;
        AuditContextFactoryStub.setCurrent(auditContext);
        policyCache = new PolicyCacheImpl(null, new ServerPolicyFactory(new TestLicenseManager(),new MockInjector()), new FolderCacheStub()) {
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
            put("serverConfig", new MockConfig( new Properties() ));
            put("policyFactory", serverPolicyFactory);
            put("policyCache", policyCache);
            put("auditFactory", LoggingAudit.factory());
        }});
        applicationContext = new GenericApplicationContext(beanFactory);
        serverPolicyFactory.setApplicationContext(applicationContext);
        applicationContext.addApplicationListener(new AuditDetailProcessingAuditListener());
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
        context.setVariable("source1", new String[] { "var content 1-1", "var content 1-2" });
        context.setVariable("source2", "var content 2");
        context.setVariable("source3", new Short[] { 3, 4, 5 });
        context.setVariable("source4", "var content 4");

        // Run all requests concurrently
        long before = System.currentTimeMillis();
        AssertionStatus status = sass.checkRequest(context);
        long after = System.currentTimeMillis();
        assertEquals(AssertionStatus.NONE, status);

        assertTrue("Should have taken less than 4 seconds if it ran concurrently", after - before < 4000L);

        assertTrue(Arrays.equals(new String[] { "var content 1-1", "var content 1-2" }, (Object[])context.getVariable("dest1")));
        assertEquals("var content 2", context.getVariable("dest2"));
        assertTrue(Arrays.equals(new Short[] { 3, 4, 5 }, (Object[])context.getVariable("dest3")));
        assertEquals("var content 4", context.getVariable("dest4"));

        try {
            context.getVariable("destNever");
            fail("Expected exception not thrown: variable destNever should not exist as the server assertion should not have been reached");
        } catch (NoSuchVariableException e) {
            // Ok
        }
    }

    @Test
    public void testMultiValuedVariableNotStringNotSafeToCopy() throws Exception {
        ConcurrentAllAssertion ass = new ConcurrentAllAssertion(Arrays.asList(
            new DelayedCopyVariableAssertion(0L, "source1", "dest1")
        ));
        ServerAssertion sass = serverPolicyFactory.compilePolicy(ass, false);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("source1", new Date[] {new Date(), new Date() });
        AssertionStatus result = sass.checkRequest(context);
        assertTrue("Should fail to copy arrays containing non-primitive non-Strings which it doesn't know how to copy", !AssertionStatus.NONE.equals(result));
        // TODO split this into two more test cases, a success and a failure, when the code is changed to support copying arrays of any Cloneable type
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
        assertEquals(3L, (long)details.size());
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

        assertEquals(String.valueOf(Calendar.getInstance(TimeZone.getTimeZone("GMT")).get(Calendar.YEAR)), context.getVariable("date").toString().substring(0, 4));
    }

    /**
     * Ensure that variables requiring copied context information are correct
     */
    @Test
    public void testServicePolicyAndAssertionVariables() throws Exception {
        final ConcurrentAllAssertion ass = new ConcurrentAllAssertion(Arrays.asList(
            new SetVariableAssertion("concurrent.service.name", "${service.name}"),
            new SetVariableAssertion("concurrent.service.oid", "${service.oid}"),
            new SetVariableAssertion("concurrent.service.policy.guid", "${service.policy.guid}"),
            new SetVariableAssertion("concurrent.service.policy.version", "${service.policy.version}"),
            new SetVariableAssertion("concurrent.policy.name", "${policy.name}"),
            new SetVariableAssertion("concurrent.policy.oid", "${policy.oid}"),
            new SetVariableAssertion("concurrent.policy.guid", "${policy.guid}"),
            new SetVariableAssertion("concurrent.policy.version", "${policy.version}"),
            new SetVariableAssertion("concurrent.assertion.number", "${assertion.number||}"),
            new SetVariableAssertion("concurrent.assertion.numberstr", "${assertion.numberstr}")
        ));
        final ServerAssertion sass = serverPolicyFactory.compilePolicy(ass, false);

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        // Set context info for service
        final PublishedService ps = new PublishedService();
        ps.setGoid(new Goid(0,123456L));
        ps.setName( "testServiceNameContextVariable" );
        ps.getPolicy().setGuid( "8ca3ff80-eaf5-11e0-9572-0800200c9a66" );
        context.setService( ps );
        context.setServicePolicyMetadata( new PolicyMetadataStub() {
            @Override
            public PolicyHeader getPolicyHeader() {
                return new PolicyHeader( ps.getPolicy(), 123L );
            }
        } );
        // Set context info for current policy
        final Policy policy = new Policy( PolicyType.INCLUDE_FRAGMENT, "testPolicyNameContextVariable", null, false );
        policy.setGoid( new Goid(0,1234567L) );
        policy.setGuid( "8ca3ff80-eaf5-11e0-9572-0800200c9a67" );
        context.setCurrentPolicyMetadata( new PolicyMetadataStub() {
            @Override
            public PolicyHeader getPolicyHeader() {
                return new PolicyHeader( policy, 1234321L );
            }
        } );
        // Set context info for assertion number
        context.pushAssertionOrdinal( 1 );
        context.pushAssertionOrdinal( 2 );
        context.pushAssertionOrdinal( 3 );

        // Run all requests concurrently
        final AssertionStatus status = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        assertEquals("concurrent.service.name", "testServiceNameContextVariable", context.getVariable("concurrent.service.name").toString());
        assertEquals("concurrent.service.oid", new Goid(0,123456L).toHexString(), context.getVariable("concurrent.service.oid").toString());
        assertEquals("concurrent.service.policy.guid", "8ca3ff80-eaf5-11e0-9572-0800200c9a66", context.getVariable("concurrent.service.policy.guid").toString());
        assertEquals("concurrent.service.policy.version", "123", context.getVariable("concurrent.service.policy.version").toString());
        assertEquals("concurrent.policy.name", "testPolicyNameContextVariable", context.getVariable("concurrent.policy.name").toString());
        assertEquals("concurrent.policy.oid", new Goid(0,1234567L).toHexString(), context.getVariable("concurrent.policy.oid").toString());
        assertEquals("concurrent.policy.guid", "8ca3ff80-eaf5-11e0-9572-0800200c9a67", context.getVariable("concurrent.policy.guid").toString());
        assertEquals("concurrent.policy.version", "1234321", context.getVariable("concurrent.policy.version").toString());
        assertEquals("concurrent.assertion.number", "1|2|3|10", context.getVariable("concurrent.assertion.number").toString());
        assertEquals("concurrent.assertion.numberstr", "1.2.3.11", context.getVariable("concurrent.assertion.numberstr").toString());
    }
}
