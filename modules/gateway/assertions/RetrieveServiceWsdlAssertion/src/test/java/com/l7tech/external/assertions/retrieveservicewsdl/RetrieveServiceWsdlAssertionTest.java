package com.l7tech.external.assertions.retrieveservicewsdl;

import static org.junit.Assert.*;

import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.policy.variable.VariableMetadata;
import org.junit.Test;

/**
 * Test the RetrieveServiceWsdlAssertion.
 */
public class RetrieveServiceWsdlAssertionTest {

    @Test
    public void testGetVariablesUsedDefault() {
        String[] variablesUsed = new RetrieveServiceWsdlAssertion().getVariablesUsed();
        assertEquals(2, variablesUsed.length);
    }

    @Test
    public void testGetVariablesUsed_WithServiceIdButNotHostnameVariables() {
        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setHost("localhost");
        assertion.setServiceId("${foo}");

        String[] variablesUsed = assertion.getVariablesUsed();
        assertEquals(1, variablesUsed.length);
        assertEquals("foo", variablesUsed[0]);
    }

    @Test
    public void testGetVariablesSetDefault() {
        VariableMetadata[] variablesSet = new RetrieveServiceWsdlAssertion().getVariablesSet();
        assertEquals(0, variablesSet.length);
    }

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new RetrieveServiceWsdlAssertion());
    }

}
