package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
import com.l7tech.server.message.HasOutputVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.test.BugId;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.event.ContextStartedEvent;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerEncapsulatedAssertionTest {
    private static final Goid CONFIG_GOID = new Goid(0L,1L);
    private static final String CONFIG_GUID = UUID.randomUUID().toString();
    private static final Goid POLICY_ID = new Goid(0,2L);
    private static final String ENCAPSULATED_ASSERTION_NAME = "testEncapsulatedAssertion";
    private Policy policy;
    private EncapsulatedAssertionConfig config;
    private Set<EncapsulatedAssertionArgumentDescriptor> inParams;
    private Set<EncapsulatedAssertionResultDescriptor> outParams;
    private EncapsulatedAssertion assertion;
    private ServerEncapsulatedAssertion serverAssertion;
    private PolicyEnforcementContext context;
    private TestAudit testAudit;
    @Mock
    private EncapsulatedAssertionConfigManager configManager;
    private ApplicationEventProxy applicationEventProxy;
    @Mock
    private PolicyCache policyCache;
    @Mock
    private ServerPolicyHandle handle;

    @Before
    public void setup() throws Exception {
        testAudit = new TestAudit();
        inParams = new HashSet<EncapsulatedAssertionArgumentDescriptor>();
        outParams = new HashSet<EncapsulatedAssertionResultDescriptor>();
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "testPolicy", "xml", false);
        policy.setGoid(POLICY_ID);
        config = new EncapsulatedAssertionConfig();
        config.setName(ENCAPSULATED_ASSERTION_NAME);
        config.setGoid(CONFIG_GOID);
        config.setGuid(CONFIG_GUID);
        config.setPolicy(policy);
        config.setArgumentDescriptors(inParams);
        config.setResultDescriptors(outParams);
        assertion = new EncapsulatedAssertion();
        assertion.setEncapsulatedAssertionConfigGuid(CONFIG_GUID);

        when(configManager.findByGuid(CONFIG_GUID)).thenReturn(config);
        when(policyCache.getServerPolicy(POLICY_ID)).thenReturn(handle);

        applicationEventProxy = new ApplicationEventProxy();
        setupServerAssertion();
        serverAssertion.afterPropertiesSet();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    /**
     * Input param configured to be passed from parent to child context.
     */
    @Test
    public void checkRequestInputParamFromParentContext() throws Exception {
        final String in = "in";
        final String inVal = "testInput";
        final String out = "out";
        final String outVal = "testOutput";
        context.setVariable(in, inVal);
        inParams.add(inputParam(in, DataType.STRING));
        outParams.add(outputParam(out, DataType.STRING));

        mockHandle(Collections.singletonMap(in, (Object) inVal), Collections.singletonMap(out, outVal), AssertionStatus.NONE, null);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        // ensure outputs from child context are set on parent context
        assertEquals(outVal, context.getVariable(out));
    }

    /**
     * Input param configured to be retrieved from the EncapsulatedAssertion properties.
     */
    @Test
    public void checkRequestInputParamFromAssertion() throws Exception {
        final String in = "in";
        final String inVal = "testInput";
        final String out = "out";
        final String outVal = "testOutput";
        assertion.putParameter(in, inVal);
        inParams.add(inputParam(in, DataType.STRING, true));
        outParams.add(outputParam(out, DataType.STRING));

        mockHandle(Collections.singletonMap(in, (Object) inVal), Collections.singletonMap(out, outVal), AssertionStatus.NONE, null);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        // ensure outputs from child context are set on parent context
        assertEquals(outVal, context.getVariable(out));
    }

    @Test
    public void entityInvalidationEventReloadsConfig() throws Exception {
        final EncapsulatedAssertionConfig beforeUpdate = serverAssertion.getConfigOrErrorRef().get().right();
        assertEquals(ENCAPSULATED_ASSERTION_NAME, beforeUpdate.getName());

        // change the name
        final EncapsulatedAssertionConfig updatedConfig = config.getCopy();
        updatedConfig.setName("updatedEncapsulatedAssertion");
        when(configManager.findByGuid(CONFIG_GUID)).thenReturn(updatedConfig);

        applicationEventProxy.publishEvent(new GoidEntityInvalidationEvent("source", EncapsulatedAssertionConfig.class,
                new Goid[]{CONFIG_GOID}, new char[]{GoidEntityInvalidationEvent.UPDATE}));

        assertEquals("updatedEncapsulatedAssertion", serverAssertion.getConfigOrErrorRef().get().right().getName());
        // once during init and another for entity invalidation event
        verify(configManager, times(2)).findByGuid(anyString());
    }

    @Test
    public void entityInvalidationEventReloadsConfigWithError() throws Exception {
        // config it references does not yet exist
        assertion.setEncapsulatedAssertionConfigGuid("doesNotExistYet");
        setupServerAssertion();
        serverAssertion.afterPropertiesSet();
        assertTrue(serverAssertion.getConfigOrErrorRef().get().isLeft());

        // simulate import of config
        when(configManager.findByGuid("doesNotExistYet")).thenReturn(config);
        applicationEventProxy.publishEvent(new GoidEntityInvalidationEvent("source", EncapsulatedAssertionConfig.class,
                new Goid[]{CONFIG_GOID}, new char[]{GoidEntityInvalidationEvent.UPDATE}));

        // config should now be loaded
        assertEquals(ENCAPSULATED_ASSERTION_NAME, serverAssertion.getConfigOrErrorRef().get().right().getName());
    }

    @Test
    public void applicationEventNotEntityInvalidationEvent() throws Exception {
        final EncapsulatedAssertionConfig beforeUpdate = serverAssertion.getConfigOrErrorRef().get().right();
        assertEquals(ENCAPSULATED_ASSERTION_NAME, beforeUpdate.getName());

        applicationEventProxy.publishEvent(new ContextStartedEvent(ApplicationContexts.getTestApplicationContext()));

        assertEquals(ENCAPSULATED_ASSERTION_NAME, serverAssertion.getConfigOrErrorRef().get().right().getName());
        // once during init but should not be called for entity invalidation event
        verify(configManager, times(1)).findByGuid(anyString());
    }

    /**
     * Entity invalidation event entity id does not match the assertion's EncapsulatedAssertionConfig id.
     */
    @Test
    public void entityInvalidationEventConfigIdNoMatch() throws Exception {
        final EncapsulatedAssertionConfig beforeEvent = serverAssertion.getConfigOrErrorRef().get().right();
        assertEquals(ENCAPSULATED_ASSERTION_NAME, beforeEvent.getName());

        applicationEventProxy.publishEvent(new GoidEntityInvalidationEvent("source", EncapsulatedAssertionConfig.class,
                new Goid[]{new Goid(1,1)}, new char[]{GoidEntityInvalidationEvent.UPDATE}));

        assertEquals(ENCAPSULATED_ASSERTION_NAME, serverAssertion.getConfigOrErrorRef().get().right().getName());
        // once during init but should not be called for entity invalidation event
        verify(configManager, times(1)).findByGuid(anyString());
    }

    @Test
    public void close() {
        assertNotNull(serverAssertion.getUpdateListener());
        serverAssertion.close();
        assertNull(serverAssertion.getUpdateListener());
    }

    @Test
    public void checkRequestPolicyThrowsAssertionStatusException() throws Exception {
        when(handle.checkRequest(any(PolicyEnforcementContext.class))).thenThrow(new AssertionStatusException(AssertionStatus.SERVER_ERROR, "mocking exception"));
        assertEquals(AssertionStatus.SERVER_ERROR, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkRequestMissingConfigGuid() throws Exception {
        assertion.setEncapsulatedAssertionConfigGuid(null);
        serverAssertion.afterPropertiesSet();

        assertEquals(AssertionStatus.SERVER_ERROR, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkRequestInvalidConfigGuid() throws Exception {
        assertion.setEncapsulatedAssertionConfigGuid("notANumber");
        serverAssertion.afterPropertiesSet();

        assertEquals(AssertionStatus.SERVER_ERROR, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkRequestCannotFindConfig() throws Exception {
        final String guid = UUID.randomUUID().toString();
        assertion.setEncapsulatedAssertionConfigGuid(guid);
        when(configManager.findByGuid(guid)).thenReturn(null);
        serverAssertion.afterPropertiesSet();

        assertEquals(AssertionStatus.SERVER_ERROR, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkRequestErrorFindingConfig() throws Exception {
        final String guid = UUID.randomUUID().toString();
        assertion.setEncapsulatedAssertionConfigGuid(guid);
        when(configManager.findByGuid(guid)).thenThrow(new FindException("mocking exception"));
        serverAssertion.afterPropertiesSet();

        assertEquals(AssertionStatus.SERVER_ERROR, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkRequestOutputParamNotFound() throws Exception {
        final String out = "out";
        outParams.add(outputParam(out, DataType.STRING));

        mockHandle(Collections.<String, Object>emptyMap(), Collections.<String, String>emptyMap(), AssertionStatus.NONE, null);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        checkContextVariableNotSet(out);
    }

    @Test
    public void checkRequestMessageInputParamFromAssertion() throws Exception {
        /*
            If the encapsulated assertion has an argument:
                - "in" of type Message, gui=true

            And the parent policy looks like:
                - set variable parentMess as Message
                - encass  in=parentMess

            Then the encapsulated fragment will see a context variable:
                - "in" of type Message, that is an alias to "parentMess" in the parent PEC
        */

        final String in = "in";
        final String inVal = "inMessage";
        assertion.putParameter(in, inVal);
        final Message msg = new Message();
        context.setVariable(inVal, msg);
        inParams.add(inputParam(in, DataType.MESSAGE, true));

        mockHandle(Collections.singletonMap(in, (Object) msg), Collections.<String, String>emptyMap(), AssertionStatus.NONE, null);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
    }

    @Test
    @BugId("SSM-4246")
    public void checkOutputsDeclaredViaHasOutputVariables() throws Exception {
        outParams.add(outputParam("out", DataType.STRING));
        outParams.add(outputParam("out2", DataType.STRING));

        final Set<String> declaredOutputs = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        mockHandle(Collections.<String, Object>emptyMap(), Collections.<String, String>emptyMap(), AssertionStatus.NONE, new Functions.UnaryVoid<PolicyEnforcementContext>() {
            @Override
            public void call(PolicyEnforcementContext policyEnforcementContext) {
                declaredOutputs.addAll(((HasOutputVariables) policyEnforcementContext).getOutputVariableNames());
            }
        });
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertTrue("Output params shall be declared as HasOutVariables of the child PEC while the backing policy is being executed",
                declaredOutputs.containsAll(Arrays.asList("out", "out2")));
    }

    @Test
    @BugId("SSM-4246")
    public void checkNoOutputsDeclaredViaHasOutputVariablesIfConfigHasNoOutputs() throws Exception {
        outParams.clear();

        final Set<String> declaredOutputs = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        mockHandle(Collections.<String, Object>emptyMap(), Collections.<String, String>emptyMap(), AssertionStatus.NONE, new Functions.UnaryVoid<PolicyEnforcementContext>() {
            @Override
            public void call(PolicyEnforcementContext policyEnforcementContext) {
                declaredOutputs.addAll(((HasOutputVariables) policyEnforcementContext).getOutputVariableNames());
            }
        });
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        assertTrue("If the config has no outputs, then HasOutVariables of the child PEC shall be completely empty",
                declaredOutputs.isEmpty());
    }

    @Test
    @BugId("SSM-4223")
    public void checkRequestInvalidBackingPolicy() throws Exception {
        policy.setGoid(new Goid(0,5555L));
        when(policyCache.getServerPolicy(new Goid(0,5555L))).thenReturn(null);
        assertEquals(AssertionStatus.SERVER_ERROR, serverAssertion.checkRequest(context));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.ENCASS_INVALID_BACKING_POLICY));
    }

    private void checkContextVariableNotSet(final String varName) {
        try {
            context.getVariable(varName);
            fail("expected NoSuchVariableException");
        } catch (final NoSuchVariableException e) {
            // expected
            assertEquals(varName, e.getVariable());
        }
    }

    /**
     * Mocks the ServerPolicyHandle (execution of the policy).
     *
     * @param expectedInputs context variables that are expected on the child context (key=name,value=variable value).
     * @param outputs        context variables that should be set on the child context (key=name,value=variable value).
     * @param toReturn       AssertionStatus to return.
     * @param callback       callback to invoke when checkRequest() is called on the backing fragment, or null.
     */
    private void mockHandle(final Map<String, Object> expectedInputs, final Map<String, String> outputs, final AssertionStatus toReturn, @Nullable final Functions.UnaryVoid<PolicyEnforcementContext> callback) throws Exception {
        when(handle.checkRequest(any(PolicyEnforcementContext.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final Object[] args = invocationOnMock.getArguments();
                final PolicyEnforcementContext childContext = (PolicyEnforcementContext) args[0];
                // ensure input args are set in child context
                for (final Map.Entry<String, Object> entry : expectedInputs.entrySet()) {
                    assertEquals(entry.getValue(), childContext.getVariable(entry.getKey()));
                }
                // mock execution of policy here
                for (final Map.Entry<String, String> entry : outputs.entrySet()) {
                    childContext.setVariable(entry.getKey(), entry.getValue());
                }
                if (callback != null) {
                    callback.call(childContext);
                }
                return toReturn;
            }
        });
    }

    private EncapsulatedAssertionArgumentDescriptor inputParam(final String name, final DataType type) {
        return inputParam(name, type, false);
    }

    private EncapsulatedAssertionArgumentDescriptor inputParam(final String name, final DataType type, final boolean guiPrompt) {
        final EncapsulatedAssertionArgumentDescriptor input = new EncapsulatedAssertionArgumentDescriptor();
        input.setArgumentName(name);
        input.setArgumentType(type.getShortName());
        input.setGuiPrompt(guiPrompt);
        return input;
    }

    private EncapsulatedAssertionResultDescriptor outputParam(final String name, final DataType type) {
        final EncapsulatedAssertionResultDescriptor output = new EncapsulatedAssertionResultDescriptor();
        output.setResultName(name);
        output.setResultType(type.getShortName());
        return output;
    }

    private void setupServerAssertion() {
        serverAssertion = new ServerEncapsulatedAssertion(assertion);
        ApplicationContexts.inject(serverAssertion,
                CollectionUtils.MapBuilder.<String, Object>builder()
                        .put("auditFactory", testAudit.factory())
                        .put("encapsulatedAssertionConfigManager", configManager)
                        .put("applicationEventProxy", applicationEventProxy)
                        .put("policyCache", policyCache).map());
    }
}
