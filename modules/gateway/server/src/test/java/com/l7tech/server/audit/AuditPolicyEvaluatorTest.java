package com.l7tech.server.audit;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.InvalidPolicyException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.event.PolicyCacheEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.folder.FolderCacheStub;
import com.l7tech.server.jdbc.JdbcConnectionManagerStub;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.*;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.MockInjector;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class AuditPolicyEvaluatorTest {
    private static final Logger logger = Logger.getLogger(AuditPolicyEvaluatorTest.class.getName());

    static final String TEST_POLICY_GUID = "testpolicyguid";

    int sinkInvocationCount;
    AssertionStatus sinkResult;

    @Before
    public void beforeEachTest() {
        sinkInvocationCount = 0;
        sinkResult = AssertionStatus.NONE;
    }

    @Test
    public void testSinkPolicyInvocation() throws Exception {
        AuditPolicyEvaluator ape = sink();

        assertEquals(0, sinkInvocationCount);
        ape.outputRecordToPolicyAuditSink(makeSystemAuditRecord(1), null);
        assertEquals(1, sinkInvocationCount);
    }

    @Test
    public void testSaveStartupEvents() throws Exception {
        AuditPolicyEvaluator ape = sink(false);

        ape.outputRecordToPolicyAuditSink(makeSystemAuditRecord(1), null);
        assertEquals("The sink should not be opened until the first Starting event", 0, sinkInvocationCount);

        ape.outputRecordToPolicyAuditSink(makeSystemAuditRecord(2), null);
        assertEquals("The sink should not be opened until the first Starting event", 0, sinkInvocationCount);

        fireStartedEvent(ape);
        assertEquals("The first Starting event should flush all buffered records", 2, sinkInvocationCount);

        fireStartedEvent(ape);
        assertEquals("A duplicate Starting event should not flush any more buffered records", 2, sinkInvocationCount);
    }

    @Test
    public void testNoSinkPolicyConfigred() {
        serverConfig.putProperty( ServerConfigParams.PARAM_AUDIT_SINK_POLICY_GUID, "");
        AssertionStatus result = sink().outputRecordToPolicyAuditSink(makeSystemAuditRecord(1), null);
        assertNull(result);
        assertEquals(0, sinkInvocationCount);
    }

    @Test
    public void testInvalidSinkPolicyGuidConfigured() {
        serverConfig.putProperty( ServerConfigParams.PARAM_AUDIT_SINK_POLICY_GUID, "qergqer3qergqerg");
        AssertionStatus result = sink().outputRecordToPolicyAuditSink(makeSystemAuditRecord(1), null);
        assertEquals(AssertionStatus.SERVER_ERROR, result);
        assertEquals(0, sinkInvocationCount);
    }


    private AuditPolicyEvaluator sink() {
        return sink(true);
    }

    AuditPolicyEvaluator sink(boolean open) {
        AuditPolicyEvaluator ape = new AuditPolicyEvaluator(serverConfig, policyCache,policyManager,jdbcConnectionPoolManager);
        if (open)
            fireStartedEvent(ape);
        return ape;
    }

    private void fireStartedEvent(AuditPolicyEvaluator ape) {
        ape.onApplicationEvent(new PolicyCacheEvent.Started(this));
    }

    private AuditRecord makeSystemAuditRecord(int idx) {
        AuditRecord auditRecord = new SystemAuditRecord(Level.INFO, "node1", Component.GW_TRUST_STORE, "sysrec#" + idx, false, -1, null, null, "Checking", "192.168.1.42");
        return auditRecord;
    }

    void log(AuditDetailMessage message, String[] params, Exception ex) {
        LogRecord record = new LogRecord(message.getLevel(), message.getMessage());
        record.setParameters(params);
        record.setThrown(ex);
        record.setSourceClassName("");
        record.setSourceMethodName("");
        logger.log(record);
    }

    // Mocks for AuditPolicyEvaluator's collaborators

    /** A ServerConfig that has a sink policy GUID configured. */
    ServerConfigStub serverConfig = new ServerConfigStub() {{
        putProperty( ServerConfigParams.PARAM_AUDIT_SINK_POLICY_GUID, TEST_POLICY_GUID);
    }};

    /** A fake no-op test policy that uses the appropriate GUID. */
    Policy testPolicy = new Policy(PolicyType.INTERNAL, "Sink policy", WspWriter.getPolicyXml(new TrueAssertion()), false) {{
        setGoid(new Goid(0,33333));
        setGuid(TEST_POLICY_GUID);
    }};

    /** A stub policy manager that publishes our fake no-op test policy. */
    PolicyManagerStub policyManager = new PolicyManagerStub(new Policy[] { testPolicy });

    /** A PolicyCache that always returns the fake policy, with a ServerAssertion instrumented to count its invocations. */
    PolicyCache policyCache = new PolicyCacheImpl(null, new ServerPolicyFactory(new TestLicenseManager(),new MockInjector()), new FolderCacheStub()) {
        {
            setPolicyManager(policyManager);
            setApplicationEventPublisher(new EventChannel());
            onApplicationEvent(new Started(this, Component.GATEWAY, "Test"));
        }

        @Override
        protected ServerAssertion buildServerPolicy(Policy policy) throws ServerPolicyInstantiationException, ServerPolicyException, InvalidPolicyException {
            return new AbstractServerAssertion<TrueAssertion>(new TrueAssertion()) {
                @Override
                public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
                    sinkInvocationCount++;
                    return sinkResult;
                }
            };
        }

        @Override
        protected void logAndAudit(AuditDetailMessage message, String[] params, Exception ex) {
            log(message, params, ex);
        }

        @Override
        protected void logAndAudit(AuditDetailMessage message, String... params) {
            log(message, params, null);
        }
    };

    JdbcConnectionPoolManager jdbcConnectionPoolManager = new JdbcConnectionPoolManager(new JdbcConnectionManagerStub());

}
