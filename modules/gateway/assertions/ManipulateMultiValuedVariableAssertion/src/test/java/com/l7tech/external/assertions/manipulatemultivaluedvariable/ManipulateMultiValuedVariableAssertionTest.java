package com.l7tech.external.assertions.manipulatemultivaluedvariable;

import static org.junit.Assert.*;

import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.policy.variable.VariableMetadata;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Test the ManipulateMultiValuedVariableAssertion.
 */
public class ManipulateMultiValuedVariableAssertionTest {

    private static final Logger log = Logger.getLogger(ManipulateMultiValuedVariableAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new ManipulateMultiValuedVariableAssertion());
    }

    @Test
    public void testVariablesSet() throws Exception {
        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setTargetVariableName("myMultiVar");

        final VariableMetadata[] variablesSet = ass.getVariablesSet();
        assertNotNull(variablesSet);
        assertEquals(1, variablesSet.length);
        final VariableMetadata metadata = variablesSet[0];
        assertEquals("myMultiVar", metadata.getName());

    }

    @Test
    public void testVariablesUsed() throws Exception {
        ManipulateMultiValuedVariableAssertion ass = new ManipulateMultiValuedVariableAssertion();
        ass.setTargetVariableName("myMultiVar");
        ass.setSourceVariableName("myVariable");

        final List<String> varsUsed = Arrays.asList(ass.getVariablesUsed());
        assertNotNull(varsUsed);
        assertEquals(1, varsUsed.size());
        assertEquals("myVariable", varsUsed.get(0));
    }
}
