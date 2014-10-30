package com.l7tech.external.assertions.retrieveservicewsdl;

import static org.junit.Assert.*;

import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.policy.variable.VariableMetadata;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the RetrieveServiceWsdlAssertion.
 */
public class RetrieveServiceWsdlAssertionTest {

    @Test
    public void testGetVariablesUsedDefault() {
        String[] variablesUsed = new RetrieveServiceWsdlAssertion().getVariablesUsed();
        assertEquals(0, variablesUsed.length);
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
