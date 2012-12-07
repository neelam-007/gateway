package com.l7tech.server.policy.assertion.sla;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;

import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.sla.CounterManagerStub;
import com.l7tech.test.BugNumber;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.text.MessageFormat;
import java.util.Collections;

/**
 * @author jbufu
 */
public class ServerThroughputQuotaTest {
    private static final long DEFAULT_QUOTA = 5L;
    private ThroughputQuota assertion;
    private ApplicationContext applicationContext;
    private PolicyEnforcementContext context;
    private CounterManagerStub counterManager;
    private ServerThroughputQuota serverAssertion;
    private TestAudit testAudit;

    @Before
    public void setup(){
        assertion = new ThroughputQuota();
        assertion.setQuota(DEFAULT_QUOTA);
        assertion.setTimeUnit(ThroughputQuota.PER_SECOND);
        assertion.setCounterName("quotaCounter");
        applicationContext = ApplicationContexts.getTestApplicationContext();
        counterManager = (CounterManagerStub)applicationContext.getBean("counterManager");
        testAudit = new TestAudit();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new InternalUser("testUser"), new OpaqueSecurityToken()));
    }


    @Test
    public void testCompatibilityBug5043Format() throws Exception {
        final String policyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "            <L7p:ThroughputQuota>\n" +
            "                <L7p:CounterName stringValue=\"quota1\"/>\n" +
            "                <L7p:Quota longValue=\"202\"/>\n" +
            "            </L7p:ThroughputQuota>\n" +
            "    </wsp:Policy>";

        AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
        WspReader wspReader = new WspReader(tmf);
        tmf.registerAssertion(ThroughputQuota.class);

        final ThroughputQuota assertion = (ThroughputQuota) wspReader.parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);
        assertTrue("Expected throughput quota 202, got '" + assertion.getQuota(), assertion.getQuota().equals("202"));
    }

    @Test
    @BugNumber(10495)
    public void testLogOnlyIncrementOnSuccessLimitMet() throws Exception{
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuota.INCREMENT_ON_SUCCESS);
        serverAssertion = new ServerThroughputQuota(assertion, applicationContext);
        ApplicationContexts.inject( serverAssertion, Collections.singletonMap( "auditFactory", testAudit.factory() ) );
        //limit has been met
        counterManager.setThrowException(true);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertTrue( testAudit.isAuditPresent( AssertionMessages.THROUGHPUT_QUOTA_ALREADY_MET ) );
    }

    @Test
    @BugNumber(10495)
    public void testLogOnlyIncrementOnSuccessLimitExceeded() throws Exception{
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuota.INCREMENT_ON_SUCCESS);
        serverAssertion = new ServerThroughputQuota(assertion, applicationContext);
        ApplicationContexts.inject( serverAssertion, Collections.singletonMap( "auditFactory", testAudit.factory() ) );
        //limit has been exceeded
        counterManager.setCounterValue(DEFAULT_QUOTA + 1);
        context.getIncrementedCounters().add(serverAssertion.getCounterName(context));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        //counter value should not have incremented
        assertEquals(String.valueOf(DEFAULT_QUOTA + 1), context.getVariable("counter.value"));
        assertTrue( testAudit.isAuditPresent( AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED ) );
    }

    @Test
    @BugNumber(10495)
    public void testLogOnlyIncrementAlwaysLimitExceeded() throws Exception{
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuota.ALWAYS_INCREMENT);
        serverAssertion = new ServerThroughputQuota(assertion, applicationContext);
        ApplicationContexts.inject( serverAssertion, Collections.singletonMap( "auditFactory", testAudit.factory() ) );
        //limit has been exceeded
        counterManager.setCounterValue(DEFAULT_QUOTA + 1);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertTrue( testAudit.isAuditPresent( AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED ) );
    }

    @Test
    @BugNumber(13590)
    public void testInvalidMaximumQuota() throws Exception {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuota.ALWAYS_INCREMENT);
        final String maxQuotaValue = String.valueOf(Integer.MAX_VALUE + 1L);
        context.setVariable("max_quota", maxQuotaValue);
        assertion.setQuota("${max_quota}");
        serverAssertion = new ServerThroughputQuota(assertion, applicationContext);
        ApplicationContexts.inject( serverAssertion, Collections.singletonMap( "auditFactory", testAudit.factory() ) );
        counterManager.setCounterValue(DEFAULT_QUOTA);

        try {
            serverAssertion.checkRequest(context);
            fail("Assertion should throw due to invalid max quota");
        } catch (AssertionStatusException e) {
            for (String s : testAudit) {
                System.out.println(s);
            }
            assertTrue( testAudit.isAuditPresent( AssertionMessages.THROUGHPUT_QUOTA_INVALID_MAX_QUOTA ) );
            assertTrue( testAudit.isAuditPresentContaining(
                    MessageFormat.format(AssertionMessages.THROUGHPUT_QUOTA_INVALID_MAX_QUOTA.getMessage(),
                            maxQuotaValue, String.valueOf(Integer.MAX_VALUE))));
        }
    }

    /**
     * Max quota value is ignored when the system is configured to do so.
     */
    @Test
    @BugNumber(13590)
    public void testInvalidMaximumQuotaIgnored() throws Exception {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuota.ALWAYS_INCREMENT);
        final String maxQuotaValue = String.valueOf(Integer.MAX_VALUE + 1L);
        context.setVariable("max_quota", maxQuotaValue);
        assertion.setQuota("${max_quota}");
        serverAssertion = new ServerThroughputQuota(assertion, applicationContext);
        ApplicationContexts.inject( serverAssertion, Collections.singletonMap( "auditFactory", testAudit.factory() ) );
        counterManager.setCounterValue(DEFAULT_QUOTA);

        try {
            System.setProperty("com.l7tech.server.policy.assertion.sla.enforce_max_quota", "false");
            final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
            assertEquals(AssertionStatus.NONE, assertionStatus);

            assertFalse( testAudit.isAuditPresent( AssertionMessages.THROUGHPUT_QUOTA_INVALID_MAX_QUOTA ) );
        } finally {
            System.clearProperty("com.l7tech.server.policy.assertion.sla.enforce_max_quota");
        }
    }

}
