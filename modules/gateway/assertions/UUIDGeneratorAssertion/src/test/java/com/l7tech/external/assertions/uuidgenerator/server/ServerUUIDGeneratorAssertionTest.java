package com.l7tech.external.assertions.uuidgenerator.server;

import com.l7tech.external.assertions.uuidgenerator.UUIDGeneratorAssertion;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.MockConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
        assertion.setAmount(String.valueOf(UUIDGeneratorAssertion.MINIMUM_AMOUNT));
        assertion.setTargetVariable(TARGET_VARIABLE);
        serverAssertion = new ServerUUIDGeneratorAssertion(assertion);
        inject( serverAssertion );
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    @Test(expected= PolicyAssertionException.class)
    public void constructorNullTargetVariable() throws Exception{
        assertion.setTargetVariable(null);
        new ServerUUIDGeneratorAssertion(assertion);
    }

    @Test(expected= PolicyAssertionException.class)
    public void constructorEmptyTargetVariable() throws Exception{
        assertion.setTargetVariable("");
        new ServerUUIDGeneratorAssertion(assertion);
    }

    @Test(expected= PolicyAssertionException.class)
    public void constructorNullAmount() throws Exception{
        assertion.setAmount(null);
        new ServerUUIDGeneratorAssertion(assertion);
    }

    @Test(expected= PolicyAssertionException.class)
    public void constructorEmptyAmount() throws Exception{
        assertion.setAmount("");
        new ServerUUIDGeneratorAssertion(assertion);
    }

    @Test
    public void checkRequestHappyPath() throws Exception {
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String[] contextVariable = (String[]) policyContext.getVariable(TARGET_VARIABLE);
        assertEquals(1, contextVariable.length);
        assertTrue(allUnique(contextVariable));
    }

    @Test
    public void checkRequestMultipleAmount() throws Exception {
        assertion.setAmount("2");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String[] contextVariable = (String[]) policyContext.getVariable(TARGET_VARIABLE);
        assertEquals(2, contextVariable.length);
        assertTrue(allUnique(contextVariable));
    }

    @Test
    public void checkRequestAmountLessThanMin() throws Exception {
        assertion.setAmount(String.valueOf(UUIDGeneratorAssertion.MINIMUM_AMOUNT - 1));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        checkContextVariableDoesNotExist();
    }

    @Test
    public void checkRequestAmountOverMax() throws Exception {
        assertion.setAmount(String.valueOf(UUIDGeneratorAssertion.MAXIMUM_AMOUNT + 1));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        checkContextVariableDoesNotExist();
    }

    @Test
    public void checkRequestAmountInvalid() throws Exception {
        assertion.setAmount("invalid");
        assertion.setTargetVariable(TARGET_VARIABLE);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        checkContextVariableDoesNotExist();
    }

    @Test
    public void checkRequestAmountAsContextVariable() throws Exception {
        policyContext.setVariable("amount", 2);
        assertion.setAmount("${amount}");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String[] contextVariable = (String[]) policyContext.getVariable(TARGET_VARIABLE);
        assertEquals(2, contextVariable.length);
        assertTrue(allUnique(contextVariable));
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

    private void checkContextVariableDoesNotExist() throws NoSuchVariableException{
        try{
            policyContext.getVariable(TARGET_VARIABLE);
            fail("Expected NoSuchVariableException");
        }catch(final NoSuchVariableException e){
            //pass
        }
    }

    private void inject( final ServerUUIDGeneratorAssertion serverAssertion ) {
        ApplicationContexts.inject( serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put( "serverConfig", new MockConfig( Collections.<String, String>emptyMap() ) )
                .put( "auditFactory", new TestAudit().factory() )
                .unmodifiableMap()
        );
    }    
}
