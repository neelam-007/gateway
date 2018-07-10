package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by drace01 on 3/24/2016.
 */
class PolicyEnforcementContextBuilder {
    private final PolicyEnforcementContext policyEnforcementContext;
    private final Map<String, Object> variables;

    PolicyEnforcementContextBuilder() {
        policyEnforcementContext = mock(PolicyEnforcementContext.class);
        variables = new HashMap<>();

        Answer<Void> setVariableAnswer = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                String key = (String) invocationOnMock.getArguments()[0];
                Object value = invocationOnMock.getArguments()[1];
                variables.put(key, value);
                return null;
            }
        };
        doAnswer(setVariableAnswer).when(policyEnforcementContext).setVariable(anyString(), any());

        Answer<Object> getVariableAnswer = new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String key = (String) invocationOnMock.getArguments()[0];
                return variables.get(key);
            }
        };
        try {
            when(policyEnforcementContext.getVariable(anyString())).then(getVariableAnswer);
            when(policyEnforcementContext.getAllVariables()).thenReturn(variables);
        } catch (NoSuchVariableException e) {
            throw new RuntimeException("Unexpected Exception: " + e.getMessage());
        }
    }

    PolicyEnforcementContext build() {
        return policyEnforcementContext;
    }
}
