package com.l7tech.server.trace;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.folder.FolderCacheStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.*;
import com.l7tech.server.util.MockInjector;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TracePolicyEvaluatorTest {
    static final String TEST_SERVICE_NAME = "_Foo_TestServiceName___";
    static final String TEST_POLICY_NAME = "_Puu_TestPolicyName___";
    static final Goid TEST_SERVICE_GOID = new Goid(0,47847);
    static final Goid TEST_POLICY_GOID = new Goid(0,4848);
    static final Goid TRACE_POLICY_GOID = new Goid(0,5454);
    static PublishedService testService;
    static ApplicationContext applicationContext;
    static PolicyCacheImpl policyCache;


    @BeforeClass
    public static void buildTestBeans() throws Exception {
        applicationContext = ApplicationContexts.getTestApplicationContext();

        AssertionRegistry assreg = new AssertionRegistry();
        assreg.setApplicationContext(null);
        assreg.afterPropertiesSet();

        AllAssertion assertionToTrace = new AllAssertion();
        assertionToTrace.addChild(new SetVariableAssertion("orig.one", "testorigvar"));
        assertionToTrace.addChild(new TrueAssertion());
        assertionToTrace.addChild(new FalseAssertion());
        Policy policyToTrace = new Policy(PolicyType.SHARED_SERVICE, TEST_POLICY_NAME, WspWriter.getPolicyXml(assertionToTrace), false);
        policyToTrace.setGoid(TEST_POLICY_GOID);
        policyToTrace.setGuid("guid" + TEST_POLICY_GOID);
        testService = new PublishedService();
        testService.setGoid(TEST_SERVICE_GOID);
        testService.setName(TEST_SERVICE_NAME);

        AllAssertion traceAssertion = new AllAssertion();
        traceAssertion.addChild(new SetVariableAssertion("origVar", "${trace.var.orig.one}"));
        traceAssertion.addChild(new SetVariableAssertion("messvarMainpart", "${trace.var.messvar.mainpart}"));
        traceAssertion.addChild(new SetVariableAssertion("trace.out", "${trace.out}\nTRACE:${trace.assertion.numberstr}:${trace.status.message}"));
        traceAssertion.addChild(new SetVariableAssertion("t.out", "${t.out}~${trace.out}"));
        traceAssertion.addChild(new SetVariableAssertion("t.final", "${t.final}~${trace.final}"));
        traceAssertion.addChild(new SetVariableAssertion("t.service.oid", "${trace.service.oid}"));
        traceAssertion.addChild(new SetVariableAssertion("t.service.name", "${trace.service.name}"));
        traceAssertion.addChild(new SetVariableAssertion("t.policy.guid", "${trace.policy.guid}"));
        traceAssertion.addChild(new SetVariableAssertion("t.policy.oid", "${trace.policy.oid}"));
        traceAssertion.addChild(new SetVariableAssertion("t.policy.name", "${trace.policy.name}"));
        traceAssertion.addChild(new SetVariableAssertion("t.policy.version", "${trace.policy.version}"));
        traceAssertion.addChild(new SetVariableAssertion("t.status", "${t.status}~${trace.status}"));
        traceAssertion.addChild(new SetVariableAssertion("t.status.message", "${t.status.message}~${trace.status.message}"));
        traceAssertion.addChild(new SetVariableAssertion("t.request.mainpart", "${trace.request.mainpart}"));
        traceAssertion.addChild(new SetVariableAssertion("t.response.mainpart", "${trace.response.mainpart}"));
        traceAssertion.addChild(new SetVariableAssertion("t.assertion.ordinal", "${t.assertion.ordinal}~${trace.assertion.ordinal}"));
        traceAssertion.addChild(new SetVariableAssertion("t.assertion.number", "${t.assertion.number}~${trace.assertion.number|.}"));
        traceAssertion.addChild(new SetVariableAssertion("t.assertion.numberStr", "${t.assertion.numberstr}~${trace.assertion.numberSTr}"));
        traceAssertion.addChild(new SetVariableAssertion("t.assertion.shortName", "${t.assertion.shortName}~${trace.assertion.shortName}"));
        traceAssertion.addChild(new SetVariableAssertion("t.assertion.xml", "${t.assertion.xml}~${trace.assertion.xml}"));
        Policy tracePolicy = new Policy(PolicyType.INTERNAL, "[Internal Debug Trace Policy]", WspWriter.getPolicyXml(traceAssertion), false);
        tracePolicy.setGoid(TRACE_POLICY_GOID);
        tracePolicy.setGuid("guid" + TRACE_POLICY_GOID);

        PolicyManager policyManager = new PolicyManagerStub(policyToTrace, tracePolicy);
        final ServerPolicyFactory spf = new ServerPolicyFactory(new TestLicenseManager(),new MockInjector());
        spf.setApplicationContext(applicationContext);
        policyCache = new PolicyCacheImpl(null, spf, new FolderCacheStub());
        policyCache.setPolicyManager(policyManager);
        policyCache.setPolicyVersionManager( new PolicyVersionManagerStub(){
            @Override
            public PolicyVersion findActiveVersionForPolicy( final Goid policyGoid ) throws FindException {
                final PolicyVersion policyVersion = new PolicyVersion();
                policyVersion.setActive( true );
                policyVersion.setOrdinal( 7575L );
                policyVersion.setPolicyGoid( policyGoid );
                return policyVersion;
            }
        } );
        policyCache.setApplicationContext(applicationContext);
        policyCache.onApplicationEvent(new Started(new Object(), Component.GATEWAY, "127.0.0.1"));
    }


    @Test
    public void testTrace() throws Exception {
        PolicyEnforcementContext tracedContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        tracedContext.setService(testService);
        tracedContext.getRequest().initialize(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream("Howdy there!".getBytes(Charsets.UTF8)));
        tracedContext.getResponse().initialize(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream("Howdy yourself!".getBytes(Charsets.UTF8)));

        ServerPolicyHandle testHandle = policyCache.getServerPolicy(TEST_POLICY_GOID);
        TracePolicyEvaluator evaluator = TracePolicyEvaluator.createAndAttachToContext(tracedContext, policyCache.getServerPolicy(TRACE_POLICY_GOID));
        testHandle.checkRequest(tracedContext);

        final TracePolicyEnforcementContext traceContext = evaluator.getTraceContext();
        assertEquals("testorigvar", traceContext.getVariable("origVar"));
        assertEquals(String.valueOf(TEST_POLICY_GOID), traceContext.getVariable("t.policy.oid"));
        assertEquals(String.valueOf(TEST_SERVICE_GOID), traceContext.getVariable("t.service.oid"));
        assertEquals("guid" + TEST_POLICY_GOID, traceContext.getVariable("t.policy.guid"));
        assertEquals(TEST_POLICY_NAME, traceContext.getVariable("t.policy.name"));
        assertEquals("7575", traceContext.getVariable("t.policy.version"));
        assertEquals(TEST_SERVICE_NAME, traceContext.getVariable("t.service.name"));
        assertEquals("Howdy there!", traceContext.getVariable("t.request.mainpart"));
        assertEquals("Howdy yourself!", traceContext.getVariable("t.response.mainpart"));

        // per-assertion stats are accumulated using set("$f", "${f},${newstuff}") append idiom
        assertEquals("~\n" +
                "TRACE:2:No Error~\n" +
                "TRACE:2:No Error\n" +
                "TRACE:3:No Error~\n" +
                "TRACE:2:No Error\n" +
                "TRACE:3:No Error\n" +
                "TRACE:4:Assertion Falsified~\n" +
                "TRACE:2:No Error\n" +
                "TRACE:3:No Error\n" +
                "TRACE:4:Assertion Falsified\n" +
                "TRACE:1:Assertion Falsified",
                traceContext.getVariable("t.out"));
        assertEquals("~false~false~false~true", traceContext.getVariable("t.final"));
        assertEquals("~2~3~4~1", traceContext.getVariable("t.assertion.ordinal"));
        assertEquals("~2~3~4~1", traceContext.getVariable("t.assertion.numberStr"));
        assertEquals("~2~3~4~1", traceContext.getVariable("t.assertion.number"));
        assertEquals("~Set Context Variable~Continue Processing~Stop Processing~All assertions must evaluate to true", traceContext.getVariable("t.assertion.shortName"));
        assertEquals("~0~0~600~600", traceContext.getVariable("t.status"));
        assertEquals("~No Error~No Error~Assertion Falsified~Assertion Falsified", traceContext.getVariable("t.status.message"));

        String[] assertionXmls = traceContext.getVariable("t.assertion.xml").toString().split("~");
        assertEquals("", assertionXmls[0]);
        assertTrue(assertionXmls[1].contains("<L7p:SetVariable>") && !assertionXmls[1].contains("<wsp:All "));
        assertTrue(assertionXmls[2].contains("<L7p:TrueAssertion/>") && !assertionXmls[2].contains("<wsp:All "));
        assertTrue(assertionXmls[3].contains("<L7p:FalseAssertion/>") && !assertionXmls[3].contains("<wsp:All "));
        assertTrue(assertionXmls[4].contains("<wsp:All "));

        tracedContext.close();
        testHandle.close();
    }

    @Test
    public void testOrdinalPath() throws Exception {
        PolicyEnforcementContext tracedContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        tracedContext.pushAssertionOrdinal(3);
        tracedContext.pushAssertionOrdinal(6);
        tracedContext.pushAssertionOrdinal(12);

        ServerPolicyHandle testHandle = policyCache.getServerPolicy(TEST_POLICY_GOID);
        TracePolicyEvaluator evaluator = TracePolicyEvaluator.createAndAttachToContext(tracedContext, policyCache.getServerPolicy(TRACE_POLICY_GOID));
        testHandle.checkRequest(tracedContext);

        final TracePolicyEnforcementContext traceContext = evaluator.getTraceContext();

        // Request and response were left uninitialized for this test
        assertEquals("", traceContext.getVariable("t.request.mainpart"));
        assertEquals("", traceContext.getVariable("t.response.mainpart"));

        // per-assertion stats are accumulated using set("$f", "${f},${newstuff}") append idiom
        assertEquals("~2~3~4~1", traceContext.getVariable("t.assertion.ordinal"));
        assertEquals("~3.6.12.2~3.6.12.3~3.6.12.4~3.6.12.1", traceContext.getVariable("t.assertion.numberStr"));
        assertEquals("~3.6.12.2~3.6.12.3~3.6.12.4~3.6.12.1", traceContext.getVariable("t.assertion.number"));

        tracedContext.close();
        testHandle.close();
    }

    @Test
    @BugNumber(8757)
    public void testMessageVariableMainpart() throws Exception {
        PolicyEnforcementContext tracedContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        tracedContext.setVariable("messvar", new Message(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream("test message var body".getBytes(Charsets.UTF8))));

        ServerPolicyHandle testHandle = policyCache.getServerPolicy(TEST_POLICY_GOID);
        TracePolicyEvaluator evaluator = TracePolicyEvaluator.createAndAttachToContext(tracedContext, policyCache.getServerPolicy(TRACE_POLICY_GOID));
        testHandle.checkRequest(tracedContext);

        final TracePolicyEnforcementContext traceContext = evaluator.getTraceContext();

        // Request and response were left uninitialized for this test
        assertEquals("test message var body", traceContext.getVariable("messvarMainpart"));

        tracedContext.close();
        testHandle.close();
    }
}
