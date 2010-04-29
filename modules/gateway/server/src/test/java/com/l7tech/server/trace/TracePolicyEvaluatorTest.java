package com.l7tech.server.trace;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.*;
import com.l7tech.util.Charsets;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.*;

/**
 *
 */
public class TracePolicyEvaluatorTest {
    static final int TEST_SERVICE_OID = 47847;
    static final int TEST_POLICY_OID = 4848;
    static final int TRACE_POLICY_OID = 5454;
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
        Policy policyToTrace = new Policy(PolicyType.SHARED_SERVICE, "policy to trace", WspWriter.getPolicyXml(assertionToTrace), false);
        policyToTrace.setOid(TEST_POLICY_OID);
        policyToTrace.setGuid("guid" + TEST_POLICY_OID);
        testService = new PublishedService();
        testService.setOid(TEST_SERVICE_OID);

        AllAssertion traceAssertion = new AllAssertion();
        traceAssertion.addChild(new SetVariableAssertion("origVar", "${trace.var.orig.one}"));
        traceAssertion.addChild(new SetVariableAssertion("t.final", "${t.final}~${trace.final}"));
        traceAssertion.addChild(new SetVariableAssertion("t.service.oid", "${trace.service.oid}"));
        traceAssertion.addChild(new SetVariableAssertion("t.policy.oid", "${trace.policy.oid}"));
        traceAssertion.addChild(new SetVariableAssertion("t.status", "${t.status}~${trace.status}"));
        traceAssertion.addChild(new SetVariableAssertion("t.status.message", "${t.status.message}~${trace.status.message}"));
        traceAssertion.addChild(new SetVariableAssertion("t.request.mainpart", "${trace.request.mainpart}"));
        traceAssertion.addChild(new SetVariableAssertion("t.response.mainpart", "${trace.response.mainpart}"));
        traceAssertion.addChild(new SetVariableAssertion("t.assertion.ordinal", "${t.assertion.ordinal}~${trace.assertion.ordinal}"));
        traceAssertion.addChild(new SetVariableAssertion("t.assertion.path", "${t.assertion.path}~${trace.assertion.path|.}"));
        traceAssertion.addChild(new SetVariableAssertion("t.assertion.pathStr", "${t.assertion.pathStr}~${trace.assertion.pathStr}"));
        traceAssertion.addChild(new SetVariableAssertion("t.assertion.shortName", "${t.assertion.shortName}~${trace.assertion.shortName}"));
        traceAssertion.addChild(new SetVariableAssertion("t.assertion.xml", "${t.assertion.xml}~${trace.assertion.xml}"));
        Policy tracePolicy = new Policy(PolicyType.INTERNAL, "[Internal Debug Trace Policy]", WspWriter.getPolicyXml(traceAssertion), false);
        tracePolicy.setOid(TRACE_POLICY_OID);
        tracePolicy.setGuid("guid" + TRACE_POLICY_OID);

        PolicyManager policyManager = new PolicyManagerStub(policyToTrace, tracePolicy);
        final ServerPolicyFactory spf = new ServerPolicyFactory(new TestLicenseManager());
        spf.setApplicationContext(applicationContext);
        policyCache = new PolicyCacheImpl(null, spf);
        policyCache.setPolicyManager(policyManager);
        policyCache.setApplicationContext(applicationContext);
        policyCache.onApplicationEvent(new Started(new Object(), Component.GATEWAY, "127.0.0.1"));
    }


    @Test
    public void testTrace() throws Exception {
        PolicyEnforcementContext tracedContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        tracedContext.setService(testService);
        tracedContext.getRequest().initialize(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream("Howdy there!".getBytes(Charsets.UTF8)));
        tracedContext.getResponse().initialize(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream("Howdy yourself!".getBytes(Charsets.UTF8)));

        ServerPolicyHandle testHandle = policyCache.getServerPolicy(TEST_POLICY_OID);
        TracePolicyEvaluator evaluator = TracePolicyEvaluator.createAndAttachToContext(tracedContext, policyCache.getServerPolicy(TRACE_POLICY_OID));
        testHandle.checkRequest(tracedContext);

        final TracePolicyEnforcementContext traceContext = evaluator.getTraceContext();
        assertEquals("testorigvar", traceContext.getVariable("origVar"));
        assertEquals(String.valueOf(TEST_POLICY_OID), traceContext.getVariable("t.policy.oid"));
        assertEquals(String.valueOf(TEST_SERVICE_OID), traceContext.getVariable("t.service.oid"));
        assertEquals("Howdy there!", traceContext.getVariable("t.request.mainpart"));
        assertEquals("Howdy yourself!", traceContext.getVariable("t.response.mainpart"));

        // per-assertion stats are accumulated using set("$f", "${f},${newstuff}") append idiom
        assertEquals("~false~false~false~true", traceContext.getVariable("t.final"));
        assertEquals("~2~3~4~1", traceContext.getVariable("t.assertion.ordinal"));
        assertEquals("~2~3~4~1", traceContext.getVariable("t.assertion.pathStr"));
        assertEquals("~2~3~4~1", traceContext.getVariable("t.assertion.path"));
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

        ServerPolicyHandle testHandle = policyCache.getServerPolicy(TEST_POLICY_OID);
        TracePolicyEvaluator evaluator = TracePolicyEvaluator.createAndAttachToContext(tracedContext, policyCache.getServerPolicy(TRACE_POLICY_OID));
        testHandle.checkRequest(tracedContext);

        final TracePolicyEnforcementContext traceContext = evaluator.getTraceContext();

        // Request and response were left uninitialized for this test
        assertEquals("", traceContext.getVariable("t.request.mainpart"));
        assertEquals("", traceContext.getVariable("t.response.mainpart"));

        // per-assertion stats are accumulated using set("$f", "${f},${newstuff}") append idiom
        assertEquals("~2~3~4~1", traceContext.getVariable("t.assertion.ordinal"));
        assertEquals("~3.6.12.2~3.6.12.3~3.6.12.4~3.6.12.1", traceContext.getVariable("t.assertion.pathStr"));
        assertEquals("~3.6.12.2~3.6.12.3~3.6.12.4~3.6.12.1", traceContext.getVariable("t.assertion.path"));

        tracedContext.close();
        testHandle.close();
    }
}
