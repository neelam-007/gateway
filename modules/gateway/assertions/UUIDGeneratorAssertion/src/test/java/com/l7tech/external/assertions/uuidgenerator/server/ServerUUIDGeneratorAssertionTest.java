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

import java.util.*;

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
        assertion.setQuantity(String.valueOf(UUIDGeneratorAssertion.MINIMUM_QUANTITY));
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
    public void constructorNullQuantity() throws Exception{
        assertion.setQuantity(null);
        new ServerUUIDGeneratorAssertion(assertion);
    }

    @Test(expected= PolicyAssertionException.class)
    public void constructorEmptyQuantity() throws Exception{
        assertion.setQuantity("");
        new ServerUUIDGeneratorAssertion(assertion);
    }

    @Test
    public void checkRequestHappyPath() throws Exception {
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String contextVariable = (String) policyContext.getVariable(TARGET_VARIABLE);
        assertTrue(isValidUUID(contextVariable));
    }

    @Test
    public void checkRequestMultipleQuantity() throws Exception {
        assertion.setQuantity("2");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String[] contextVariable = (String[]) policyContext.getVariable(TARGET_VARIABLE);
        assertEquals(2, contextVariable.length);
        assertTrue(allUnique(contextVariable));
        assertTrue(allValid(contextVariable));
    }

    @Test
    public void checkRequestQuantityLessThanMin() throws Exception {
        assertion.setQuantity(String.valueOf(UUIDGeneratorAssertion.MINIMUM_QUANTITY - 1));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        checkContextVariableDoesNotExist();
    }

    @Test
    public void checkRequestQuantityOverMax() throws Exception {
        assertion.setQuantity(String.valueOf(UUIDGeneratorAssertion.MAXIMUM_QUANTITY + 1));

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        checkContextVariableDoesNotExist();
    }

    @Test
    public void checkRequestQuantityEqualToOverriddenMax() throws Exception{
        assertion.setMaximumQuantity(5);
        assertion.setQuantity("5");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String[] contextVariable = (String[]) policyContext.getVariable(TARGET_VARIABLE);
        assertEquals(5, contextVariable.length);
        assertTrue(allUnique(contextVariable));
        assertTrue(allValid(contextVariable));
    }

    @Test
    public void checkRequestQuantityUnderOverriddenMax() throws Exception{
        assertion.setMaximumQuantity(5);
        assertion.setQuantity("4");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.NONE, assertionStatus);
        final String[] contextVariable = (String[]) policyContext.getVariable(TARGET_VARIABLE);
        assertEquals(4, contextVariable.length);
        assertTrue(allUnique(contextVariable));
        assertTrue(allValid(contextVariable));
    }

    @Test
    public void checkRequestQuantityOverOverriddenMax() throws Exception{
        assertion.setMaximumQuantity(5);
        assertion.setQuantity("6");

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        checkContextVariableDoesNotExist();

    }

    @Test
    public void checkRequestQuantityInvalid() throws Exception {
        assertion.setQuantity("invalid");
        assertion.setTargetVariable(TARGET_VARIABLE);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(policyContext);

        assertEquals(AssertionStatus.FAILED, assertionStatus);
        checkContextVariableDoesNotExist();
    }

    @Test
    public void checkRequestQuantityAsContextVariable() throws Exception {
        policyContext.setVariable("quantity", 2);
        assertion.setQuantity("${quantity}");

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

    private boolean allValid(final String[] items){
        boolean allValid = true;
        for (final String item : items){
            if (!isValidUUID(item)){
                allValid = false;
                break;
            }
        }
        return allValid;
    }

    private boolean isValidUUID(final String toCheck){
        try{
            UUID.fromString(toCheck);
        }catch (final IllegalArgumentException e){
            return false;
        }
        return true;
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
