package com.l7tech.external.assertions.uuidgenerator.server;

import com.l7tech.external.assertions.uuidgenerator.UUIDGeneratorAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * User: alee
 */
public class ServerUUIDGeneratorAssertionTest {
    private static final String TARGET_VARIABLE = "myUUID";
    private UUIDGeneratorAssertion assertion;
    private ServerUUIDGeneratorAssertion serverAssertion;
    private PolicyEnforcementContext policyContext;

    @Before
    public void setup() throws Exception {
        assertion = new UUIDGeneratorAssertion();
        serverAssertion = new ServerUUIDGeneratorAssertion(assertion);
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @Test
    public void checkRequestHappyPath() throws Exception {
        assertion.setAmount(UUIDGeneratorAssertion.DEFAULT_AMOUNT);
        assertion.setTargetVariable(TARGET_VARIABLE);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String[] contextVariable = (String[]) policyContext.getVariable(TARGET_VARIABLE);
        assertEquals(1, contextVariable.length);
        assertTrue(allUnique(contextVariable));
    }

    @Test
    public void checkRequestMultipleAmount() throws Exception {
        assertion.setAmount("2");
        assertion.setTargetVariable(TARGET_VARIABLE);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String[] contextVariable = (String[]) policyContext.getVariable(TARGET_VARIABLE);
        assertEquals(2, contextVariable.length);
        assertTrue(allUnique(contextVariable));
    }

    @Test
    public void checkRequestAmountLessThanMin() throws Exception {
        assertion.setAmount("0");
        assertion.setTargetVariable(TARGET_VARIABLE);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void checkRequestAmountInvalid() throws Exception {
        assertion.setAmount("invalid");
        assertion.setTargetVariable(TARGET_VARIABLE);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void checkRequestAmountNull() throws Exception {
        assertion.setAmount(null);
        assertion.setTargetVariable(TARGET_VARIABLE);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void checkRequestAmountAsContextVariable() throws Exception {
        policyContext.setVariable("amount", 2);
        assertion.setAmount("${amount}");
        assertion.setTargetVariable(TARGET_VARIABLE);
        serverAssertion = new ServerUUIDGeneratorAssertion(assertion);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String[] contextVariable = (String[]) policyContext.getVariable(TARGET_VARIABLE);
        assertEquals(2, contextVariable.length);
        assertTrue(allUnique(contextVariable));
    }

    @Test
    public void checkRequestTargetVariableNull() throws Exception {
        assertion.setAmount(UUIDGeneratorAssertion.DEFAULT_AMOUNT);
        assertion.setTargetVariable(null);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void checkRequestTargetVariableEmpty() throws Exception {
        assertion.setAmount(UUIDGeneratorAssertion.DEFAULT_AMOUNT);
        assertion.setTargetVariable("");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
    }

    private boolean allUnique(final String[] items) {
        boolean allUnique = true;
        final List<String> itemList = Arrays.asList(items);
        final Set<String> itemSet = new HashSet<String>(itemList);
        if (itemList.size() != itemSet.size()) {
            allUnique = false;
        }
        return allUnique;
    }
}
