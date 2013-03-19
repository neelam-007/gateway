package com.l7tech.server.policy.assertion;

import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.ExportVariablesAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.ResourceUtils;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for ServerExportVariablesAssertion.
 */
public class ServerExportVariablesAssertionTest {
    ExportVariablesAssertion ass = new ExportVariablesAssertion();
    PolicyEnforcementContext grandParent;
    PolicyEnforcementContext parent;
    PolicyEnforcementContext child;

    @After
    public void cleanup() {
        ResourceUtils.closeQuietly(child);
        ResourceUtils.closeQuietly(parent);
        ResourceUtils.closeQuietly(grandParent);
    }

    @Test
    public void testVariablesUsedEmpty() {
        assertTrue(ass.getVariablesUsed().length == 0);
    }

    @Test
    public void testVariablesUsedSome() {
        ass.setExportedVars(new String[] { "foo", "bar" });
        String[] used = ass.getVariablesUsed();
        assertNotNull(used);
        assertEquals(2, used.length);
        assertEquals("foo", used[0]);
        assertEquals("bar", used[1]);
    }

    @Test
    public void testExportToParent() throws Exception {
        grandParent = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        parent = PolicyEnforcementContextFactory.createPolicyEnforcementContext(grandParent);
        child = PolicyEnforcementContextFactory.createPolicyEnforcementContext(parent);
        child.setVariable("blah", "blah1");

        // Pre
        assertEquals("blah1", child.getVariable("blah"));
        assertNoSuchVariable(parent, "blah");
        assertNoSuchVariable(grandParent, "blah");

        // Test
        ass.setExportedVars(new String[] { "blah" });
        ServerAssertion sass = new ServerExportVariablesAssertion(ass);
        AssertionStatus result = sass.checkRequest(child);
        assertEquals(AssertionStatus.NONE, result);

        // Post
        assertEquals("blah1", child.getVariable("blah"));

        // Value is visible via shared prefix in all descendants of the root context
        assertEquals("blah1", child.getVariable("request.shared.blah"));
        assertEquals("blah1", parent.getVariable("request.shared.blah"));
        assertEquals("blah1", grandParent.getVariable("request.shared.blah"));

        // Value not available without prefix in intermediate context or root context
        assertNoSuchVariable(parent, "blah");
        assertNoSuchVariable(grandParent, "blah");
    }

    private static void assertNoSuchVariable(PolicyEnforcementContext context, String var) {
        try {
            context.getVariable(var);
            fail("Expected NoSuchVariableException for variable " + var);
        } catch (NoSuchVariableException e) {
            // Ok
        }
    }
}
