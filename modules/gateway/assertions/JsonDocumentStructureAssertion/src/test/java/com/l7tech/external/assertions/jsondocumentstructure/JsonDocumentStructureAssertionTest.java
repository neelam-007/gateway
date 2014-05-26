package com.l7tech.external.assertions.jsondocumentstructure;

import static org.junit.Assert.*;

import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.policy.variable.VariableMetadata;
import org.junit.Test;

/**
 * Tests for JsonDocumentStructureAssertion
 */
public class JsonDocumentStructureAssertionTest {

    @Test
    public void testGetVariablesUsedDefault() {
        String[] variablesUsed = new JsonDocumentStructureAssertion().getVariablesUsed();
        assertEquals(0, variablesUsed.length);
    }

    @Test
    public void testGetVariablesSetDefault() {
        VariableMetadata[] variablesSet = new JsonDocumentStructureAssertion().getVariablesSet();
        assertEquals(0, variablesSet.length);
    }

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new JsonDocumentStructureAssertion());
    }

    @Test
    public void testAssertionMetadataConsistency() throws Exception {
        AllAssertionsTest.testAssertionMetadataConsistency(new JsonDocumentStructureAssertion());
    }
}
