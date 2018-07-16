package com.l7tech.external.assertions.throughputquota.server;

import com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest;
import com.ca.apim.gateway.extension.sharedstate.counter.SharedCounterStore;
import com.l7tech.external.assertions.throughputquota.ThroughputQuotaAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.security.token.OpaqueSecurityToken;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.extension.provider.sharedstate.counter.LocalCounterProvider;
import com.l7tech.server.extension.registry.sharedstate.SharedCounterProviderRegistry;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.MockConfig;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.l7tech.util.CollectionUtils.MapBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ServerThroughputQuotaAssertionTest {
    private static final int DEFAULT_QUOTA = 5;
    private ThroughputQuotaAssertion assertion;
    private ApplicationContext applicationContext;
    private PolicyEnforcementContext context;
    private ServerThroughputQuotaAssertion serverAssertion;
    private TestAudit testAudit;
    private SharedCounterStore counterStore;
    private Properties counterOperationConfig;
    private String resolvedCounterName;

    @Before
    public void setup() {
        assertion = new ThroughputQuotaAssertion();
        assertion.setQuota(DEFAULT_QUOTA);
        assertion.setTimeUnit(CounterFieldOfInterest.SEC.ordinal());
        assertion.setCounterName("quotaCounter");
        applicationContext = ApplicationContexts.getTestApplicationContext();
        testAudit = new TestAudit();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.getDefaultAuthenticationContext().addAuthenticationResult(new AuthenticationResult(new InternalUser("testUser"), new OpaqueSecurityToken()));
        resolvedCounterName = ExpandVariables.process(assertion.getCounterName(), context.getVariableMap(assertion.getVariablesUsed(), testAudit), testAudit);
        counterStore = applicationContext.getBean("sharedCounterProviderRegistry", SharedCounterProviderRegistry.class).getExtension(LocalCounterProvider.KEY)
                .getCounterStore(ServerThroughputQuotaAssertion.COUNTER_STORE_NAME);
        counterOperationConfig = new Properties();
    }

    @After
    public void tearDown() {
        counterStore.reset(resolvedCounterName);
    }

    @Test
    @BugId("SSG-6851")
    public void testCompatibilityHalfAsyncThoughputQuotaModularAssertion() throws Exception {
        final String policyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <L7p:HalfAsyncThroughputQuota>\n" +
                "            <L7p:CounterName stringValue=\"36994f1d-${request.authenticateduser.id}-${request.authenticateduser.providerid}-blahblah\"/>\n" +
                "            <L7p:CounterStrategy intValue=\"1\"/>\n" +
                "            <L7p:Global booleanValue=\"true\"/>\n" +
                "            <L7p:Quota stringValue=\"553\"/>\n" +
                "            <L7p:Synchronous booleanValue=\"false\"/>\n" +
                "            <L7p:VariablePrefix stringValue=\"asdf\"/>\n" +
                "        </L7p:HalfAsyncThroughputQuota>\n" +
                "</wsp:Policy>";

        AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
        WspReader wspReader = new WspReader(tmf);
        tmf.registerAssertion(ThroughputQuotaAssertion.class);

        final ThroughputQuotaAssertion assertion = (ThroughputQuotaAssertion) wspReader.parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);
        assertTrue("Expected throughput quota 553, got '" + assertion.getQuota(), assertion.getQuota().equals("553"));
        assertTrue("Expected high perf mode; sync=" + assertion.isSynchronous(), !assertion.isSynchronous());
        assertEquals("var prefix", assertion.getVariablePrefix(), "asdf");
        assertEquals("custom counter name", assertion.getCounterName(), "36994f1d-${request.authenticateduser.id}-${request.authenticateduser.providerid}-blahblah");
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
        tmf.registerAssertion(ThroughputQuotaAssertion.class);

        final ThroughputQuotaAssertion assertion = (ThroughputQuotaAssertion) wspReader.parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);
        assertTrue("Expected throughput quota 202, got '" + assertion.getQuota(), assertion.getQuota().equals("202"));
    }

    @Test
    @BugNumber(10495)
    public void testLogOnlyIncrementOnSuccessLimitMet() throws Exception {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.INCREMENT_ON_SUCCESS);
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        //limit has been met
        setCounterTo(DEFAULT_QUOTA);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        //counter value should not have incremented
        assertEquals(String.valueOf(DEFAULT_QUOTA), context.getVariable("counter.value"));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.THROUGHPUT_QUOTA_ALREADY_MET));
    }

    @Test
    @BugNumber(10495)
    public void testLogOnlyIncrementOnSuccessLimitExceeded() throws Exception {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.INCREMENT_ON_SUCCESS);
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        //limit has been exceeded
        setCounterTo(DEFAULT_QUOTA + 1);
        context.getIncrementedCounters().add(resolvedCounterName);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        //counter value should not have incremented
        assertEquals(String.valueOf(DEFAULT_QUOTA + 1), context.getVariable("counter.value"));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED));
    }

    @Test
    @BugNumber(10495)
    public void testLogOnlyIncrementAlwaysLimitExceeded() throws Exception {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.ALWAYS_INCREMENT);
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        //limit has been exceeded
        setCounterTo(DEFAULT_QUOTA + 1);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED));
    }

    @Test
    @BugNumber(13590)
    public void testInvalidMaximumQuotaConfiguration() throws Exception {
        // assertion should fail when incorrect configured
        testMaximumQuotaConfiguration(true, null);
    }

    /**
     * Max quota value is ignored by default.
     */
    @Test
    @BugNumber(13590)
    public void testNoDefaultUpperLimitOnQuotaConfiguration() throws Exception {
        testMaximumQuotaConfiguration(false, null);
    }

    @Test
    @BugNumber(13590)
    public void testConfigurableMaxQuotaValue() throws Exception {
        testMaximumQuotaConfiguration(true, 100L);
    }

    private void testMaximumQuotaConfiguration(boolean expectFail, @Nullable Long maxAllowableQuotaValue) throws Exception {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.ALWAYS_INCREMENT);
        // This should be greater than the maximum allowed quota for each test case. Only in some tests is the max quota enforced.
        final String maxQuotaValue = String.valueOf(Integer.MAX_VALUE + 1L);
        context.setVariable("max_quota", maxQuotaValue);
        assertion.setQuota("${max_quota}");
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        Map<String, String> props = new HashMap<>();
        props.put(ServerConfigParams.PARAM_THROUGHPUTQUOTA_ENFORCE_MAX_QUOTA, String.valueOf(expectFail));
        if (maxAllowableQuotaValue != null) {
            props.put(ServerConfigParams.PARAM_THROUGHPUTQUOTA_MAX_THROUGHPUT_QUOTA, String.valueOf(maxAllowableQuotaValue));
        }
        configureServerAssertion(serverAssertion, props);
        setCounterTo(DEFAULT_QUOTA);

        boolean failed = false;
        try {
            serverAssertion.checkRequest(context);

            final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
            assertEquals(AssertionStatus.NONE, assertionStatus);
            assertFalse(testAudit.isAuditPresent(AssertionMessages.THROUGHPUT_QUOTA_INVALID_MAX_QUOTA));

        } catch (AssertionStatusException e) {
            failed = true;
            for (String s : testAudit) {
                System.out.println(s);
            }
            assertTrue(testAudit.isAuditPresent(AssertionMessages.THROUGHPUT_QUOTA_INVALID_MAX_QUOTA));
            assertTrue(testAudit.isAuditPresentContaining(
                    MessageFormat.format(AssertionMessages.THROUGHPUT_QUOTA_INVALID_MAX_QUOTA.getMessage(),
                            maxQuotaValue, String.valueOf((maxAllowableQuotaValue == null) ? Integer.MAX_VALUE : maxAllowableQuotaValue))));
        }

        if (expectFail && !failed) {
            fail("Assertion should throw due to invalid max quota");
        }
    }

    @Test
    public void testReset() throws IOException, PolicyAssertionException, NoSuchVariableException {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.RESET);
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        //limit has been met
        setCounterTo(DEFAULT_QUOTA);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(0, counterStore.get(resolvedCounterName, counterOperationConfig, getAssertionTimeUnitAsFieldOfInterest()), DEFAULT_QUOTA);
        assertEquals("0", String.valueOf(context.getVariable("counter.value")));
    }

    @Test
    public void testIncrementByAlwaysIncrement() throws IOException, PolicyAssertionException, NoSuchVariableException {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.ALWAYS_INCREMENT);
        assertion.setByValue("3");
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        setCounterTo(1);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals("4", context.getVariable(assertion.valueVariable()));
    }

    @Test
    public void testIncrementByExceededLimitAlwaysIncrement() throws IOException, PolicyAssertionException, NoSuchVariableException {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.ALWAYS_INCREMENT);
        assertion.setByValue("3");
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        setCounterTo(DEFAULT_QUOTA);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals("8", context.getVariable(assertion.valueVariable()));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.THROUGHPUT_QUOTA_EXCEEDED));
    }

    @Test
    public void testIncrementByInvalidIncrementValueAlwaysIncrement() throws IOException, PolicyAssertionException, NoSuchVariableException {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.ALWAYS_INCREMENT);
        assertion.setByValue("xxx");
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        setCounterTo(DEFAULT_QUOTA);

        try {
            final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
            fail();
        } catch (AssertionStatusException ex) {
            assertNotNull(ex);
        }
    }

    @Test
    public void testIncrementByOnSuccess() throws IOException, PolicyAssertionException, NoSuchVariableException {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.INCREMENT_ON_SUCCESS);
        assertion.setByValue("3");
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        setCounterTo(1);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals("4", context.getVariable(assertion.valueVariable()));
    }

    @Test
    public void testIncrementByExceededLimitOnSuccess() throws IOException, PolicyAssertionException, NoSuchVariableException {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.INCREMENT_ON_SUCCESS);
        assertion.setByValue("3");
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        setCounterTo(DEFAULT_QUOTA);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals("5", context.getVariable(assertion.valueVariable()));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.THROUGHPUT_QUOTA_ALREADY_MET));
    }

    @Test
    public void testIncrementByInvalidIncrementValueOnSuccess() throws IOException, PolicyAssertionException, NoSuchVariableException {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.INCREMENT_ON_SUCCESS);
        assertion.setByValue("xxx");
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        setCounterTo(DEFAULT_QUOTA);

        try {
            final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
            fail();
        } catch (AssertionStatusException ex) {
            assertNotNull(ex);
        }
    }

    @Test
    public void testIncrementByDecrement() throws IOException, PolicyAssertionException, NoSuchVariableException {
        final int INIT_COUNTER_VALUE = 0;

        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.DECREMENT);
        assertion.setByValue("1");
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        setCounterTo(INIT_COUNTER_VALUE);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(String.valueOf(INIT_COUNTER_VALUE), context.getVariable(assertion.valueVariable()));
    }

    @Test
    public void testIncrementByExceededLimitDecrement() throws IOException, PolicyAssertionException, NoSuchVariableException {
        final int EXCEEDED_COUNTER_VALUE = DEFAULT_QUOTA + 2;

        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.DECREMENT);
        assertion.setByValue("1");
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        setCounterTo(EXCEEDED_COUNTER_VALUE);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertEquals(String.valueOf(EXCEEDED_COUNTER_VALUE), context.getVariable(assertion.valueVariable()));
    }

    @Test
    public void testIncrementByInvalidIncrementValueDecrement() throws IOException, PolicyAssertionException, NoSuchVariableException {
        assertion.setLogOnly(true);
        assertion.setCounterStrategy(ThroughputQuotaAssertion.DECREMENT);
        assertion.setByValue("xxx");
        serverAssertion = new ServerThroughputQuotaAssertion(assertion, applicationContext);
        configureServerAssertion(serverAssertion, null);
        setCounterTo(DEFAULT_QUOTA);

        try {
            final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
            fail();
        } catch (AssertionStatusException ex) {
            assertNotNull(ex);
        }
    }

    private void configureServerAssertion(ServerThroughputQuotaAssertion serverAssertion, @Nullable Map<String, String> props) {
        ApplicationContexts.inject(serverAssertion,
                MapBuilder.<String, Object>builder()
                        .put("auditFactory", testAudit.factory())
                        .put("serverConfig", new MockConfig((props == null) ? MapBuilder.<String, String>builder().map() : props))
                        .map());
    }

    private void setCounterTo(int desiredCount) throws NoSuchVariableException {
        context.setVariable(assertion.valueVariable(), desiredCount);
        counterStore.update(resolvedCounterName, counterOperationConfig, getAssertionTimeUnitAsFieldOfInterest(), System.currentTimeMillis(), desiredCount);
        assertEquals(desiredCount, counterStore.get(resolvedCounterName, counterOperationConfig, getAssertionTimeUnitAsFieldOfInterest()));
        assertEquals(desiredCount, context.getVariable(assertion.valueVariable()));
    }

    private CounterFieldOfInterest getAssertionTimeUnitAsFieldOfInterest() {
        return CounterFieldOfInterest.values()[assertion.getTimeUnit()];
    }

}