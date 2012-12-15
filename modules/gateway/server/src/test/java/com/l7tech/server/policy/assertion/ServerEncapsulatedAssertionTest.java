package com.l7tech.server.policy.assertion;

import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
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
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.util.ApplicationEventProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.event.ContextStartedEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerEncapsulatedAssertionTest {
    private static final long CONFIG_ID = 1L;
    private static final long POLICY_ID = 2L;
    private static final String ENCAPSULATED_ASSERTION_NAME = "testEncapsulatedAssertion";
    private Policy policy;
    private EncapsulatedAssertionConfig config;
    private Set<EncapsulatedAssertionArgumentDescriptor> inParams;
    private Set<EncapsulatedAssertionResultDescriptor> outParams;
    private EncapsulatedAssertion assertion;
    private ServerEncapsulatedAssertion serverAssertion;
    private PolicyEnforcementContext context;
    @Mock
    private EncapsulatedAssertionConfigManager configManager;
    private ApplicationEventProxy applicationEventProxy;
    @Mock
    private PolicyCache policyCache;
    @Mock
    private ServerPolicyHandle handle;

    @Before
    public void setup() throws Exception {
        inParams = new HashSet<EncapsulatedAssertionArgumentDescriptor>();
        outParams = new HashSet<EncapsulatedAssertionResultDescriptor>();
        policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "testPolicy", "xml", false);
        policy.setOid(POLICY_ID);
        config = new EncapsulatedAssertionConfig();
        config.setName(ENCAPSULATED_ASSERTION_NAME);
        config.setOid(CONFIG_ID);
        config.setPolicy(policy);
        config.setArgumentDescriptors(inParams);
        config.setResultDescriptors(outParams);
        assertion = new EncapsulatedAssertion();
        assertion.setEncapsulatedAssertionConfigId(String.valueOf(CONFIG_ID));

        when(configManager.findByPrimaryKey(CONFIG_ID)).thenReturn(config);
        when(policyCache.getServerPolicy(POLICY_ID)).thenReturn(handle);

        serverAssertion = new ServerEncapsulatedAssertion(assertion);
        serverAssertion.setEncapsulatedAssertionConfigManager(configManager);
        applicationEventProxy = new ApplicationEventProxy();
        serverAssertion.setApplicationEventProxy(applicationEventProxy);
        serverAssertion.setPolicyCache(policyCache);
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

        mockHandle(Collections.singletonMap(in, (Object) inVal), Collections.singletonMap(out, outVal), AssertionStatus.NONE);

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

        mockHandle(Collections.singletonMap(in, (Object) inVal), Collections.singletonMap(out, outVal), AssertionStatus.NONE);

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
        when(configManager.findByPrimaryKey(CONFIG_ID)).thenReturn(updatedConfig);

        applicationEventProxy.publishEvent(new EntityInvalidationEvent("source", EncapsulatedAssertionConfig.class,
                new long[]{CONFIG_ID}, new char[]{EntityInvalidationEvent.UPDATE}));

        assertEquals("updatedEncapsulatedAssertion", serverAssertion.getConfigOrErrorRef().get().right().getName());
        // once during init and another for entity invalidation event
        verify(configManager, times(2)).findByPrimaryKey(anyLong());
    }

    @Test
    public void applicationEventNotEntityInvalidationEvent() throws Exception {
        final EncapsulatedAssertionConfig beforeUpdate = serverAssertion.getConfigOrErrorRef().get().right();
        assertEquals(ENCAPSULATED_ASSERTION_NAME, beforeUpdate.getName());

        applicationEventProxy.publishEvent(new ContextStartedEvent(ApplicationContexts.getTestApplicationContext()));

        assertEquals(ENCAPSULATED_ASSERTION_NAME, serverAssertion.getConfigOrErrorRef().get().right().getName());
        // once during init but should not be called for entity invalidation event
        verify(configManager, times(1)).findByPrimaryKey(anyLong());
    }

    /**
     * Entity invalidation event entity id does not match the assertion's EncapsulatedAssertionConfig id.
     */
    @Test
    public void entityInvalidationEventConfigIdNoMatch() throws Exception {
        final EncapsulatedAssertionConfig beforeEvent = serverAssertion.getConfigOrErrorRef().get().right();
        assertEquals(ENCAPSULATED_ASSERTION_NAME, beforeEvent.getName());

        applicationEventProxy.publishEvent(new EntityInvalidationEvent("source", EncapsulatedAssertionConfig.class,
                new long[]{CONFIG_ID + 1}, new char[]{EntityInvalidationEvent.UPDATE}));

        assertEquals(ENCAPSULATED_ASSERTION_NAME, serverAssertion.getConfigOrErrorRef().get().right().getName());
        // once during init but should not be called for entity invalidation event
        verify(configManager, times(1)).findByPrimaryKey(anyLong());
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
    public void checkRequestMissingConfigId() throws Exception {
        assertion.setEncapsulatedAssertionConfigId(null);
        serverAssertion.afterPropertiesSet();

        assertEquals(AssertionStatus.SERVER_ERROR, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkRequestInvalidConfigId() throws Exception {
        assertion.setEncapsulatedAssertionConfigId("notANumber");
        serverAssertion.afterPropertiesSet();

        assertEquals(AssertionStatus.SERVER_ERROR, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkRequestCannotFindConfig() throws Exception {
        assertion.setEncapsulatedAssertionConfigId(String.valueOf(CONFIG_ID + 1));
        when(configManager.findByPrimaryKey(CONFIG_ID + 1)).thenReturn(null);
        serverAssertion.afterPropertiesSet();

        assertEquals(AssertionStatus.SERVER_ERROR, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkRequestErrorFindingConfig() throws Exception {
        assertion.setEncapsulatedAssertionConfigId(String.valueOf(CONFIG_ID + 1));
        when(configManager.findByPrimaryKey(CONFIG_ID + 1)).thenThrow(new FindException("mocking exception"));
        serverAssertion.afterPropertiesSet();

        assertEquals(AssertionStatus.SERVER_ERROR, serverAssertion.checkRequest(context));
    }

    @Test
    public void checkRequestOutputParamNotFound() throws Exception {
        final String out = "out";
        outParams.add(outputParam(out, DataType.STRING));

        mockHandle(Collections.<String, Object>emptyMap(), Collections.<String, String>emptyMap(), AssertionStatus.NONE);

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

        mockHandle(Collections.singletonMap(in, (Object) msg), Collections.<String, String>emptyMap(), AssertionStatus.NONE);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
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
     */
    private void mockHandle(final Map<String, Object> expectedInputs, final Map<String, String> outputs, final AssertionStatus toReturn) throws Exception {
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
}
