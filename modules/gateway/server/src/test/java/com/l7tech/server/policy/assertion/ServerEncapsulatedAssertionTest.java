package com.l7tech.server.policy.assertion;

import com.l7tech.message.Message;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionArgumentDescriptor;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionResultDescriptor;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.policy.variable.DataType;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServerEncapsulatedAssertionTest {
    private static final long CONFIG_ID = 1L;
    private static final long POLICY_ID = 2L;
    private Policy policy;
    private EncapsulatedAssertionConfig config;
    private Set<EncapsulatedAssertionArgumentDescriptor> inParams;
    private Set<EncapsulatedAssertionResultDescriptor> outParams;
    private EncapsulatedAssertion assertion;
    private ServerEncapsulatedAssertion serverAssertion;
    private PolicyEnforcementContext context;
    @Mock
    private EncapsulatedAssertionConfigManager configManager;
    @Mock
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
        config.setPolicy(policy);
        config.setArgumentDescriptors(inParams);
        config.setResultDescriptors(outParams);
        assertion = new EncapsulatedAssertion();
        assertion.setEncapsulatedAssertionConfigId(String.valueOf(CONFIG_ID));

        when(configManager.findByPrimaryKey(CONFIG_ID)).thenReturn(config);
        when(policyCache.getServerPolicy(POLICY_ID)).thenReturn(handle);

        serverAssertion = new ServerEncapsulatedAssertion(assertion);
        serverAssertion.setEncapsulatedAssertionConfigManager(configManager);
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

        mockHandle(Collections.singletonMap(in, inVal), Collections.singletonMap(out, outVal), AssertionStatus.NONE);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
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

        mockHandle(Collections.singletonMap(in, inVal), Collections.singletonMap(out, outVal), AssertionStatus.NONE);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        // ensure outputs from child context are set on parent context
        assertEquals(outVal, context.getVariable(out));
    }

    /**
     * Mocks the ServerPolicyHandle (execution of the policy).
     *
     * @param expectedInputs context variables that are expected on the child context (key=name,value=variable value).
     * @param outputs        context variables that should be set on the child context (key=name,value=variable value).
     * @param toReturn       AssertionStatus to return.
     */
    private void mockHandle(final Map<String, String> expectedInputs, final Map<String, String> outputs, final AssertionStatus toReturn) throws Exception {
        when(handle.checkRequest(any(PolicyEnforcementContext.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final Object[] args = invocationOnMock.getArguments();
                final PolicyEnforcementContext childContext = (PolicyEnforcementContext) args[0];
                // ensure input args are set in child context
                for (final EncapsulatedAssertionArgumentDescriptor inParam : inParams) {
                    final String argumentName = inParam.getArgumentName();
                    assertEquals(expectedInputs.get(argumentName), childContext.getVariable(argumentName));
                }
                // mock execution of policy here
                for (final EncapsulatedAssertionResultDescriptor outParam : outParams) {
                    final String resultName = outParam.getResultName();
                    childContext.setVariable(resultName, outputs.get(resultName));
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
        input.setArgumentType(type.getName());
        input.setGuiPrompt(guiPrompt);
        return input;
    }

    private EncapsulatedAssertionResultDescriptor outputParam(final String name, final DataType type) {
        final EncapsulatedAssertionResultDescriptor output = new EncapsulatedAssertionResultDescriptor();
        output.setResultName(name);
        output.setResultType(type.getName());
        return output;
    }
}
