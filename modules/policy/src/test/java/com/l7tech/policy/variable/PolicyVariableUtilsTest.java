package com.l7tech.policy.variable;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ForEachLoopAssertion;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class PolicyVariableUtilsTest {
    private Assertion root;
    private Assertion before;
    private Assertion foreach;
    private Assertion iterating;
    private Assertion middle;
    private Assertion afterAll;

    @Before
    public void setUp() {
        root = new AllAssertion(Arrays.asList(
            new SetVariableAssertion("neverused", "neverusedval"),
            new SetVariableAssertion("foo", "fooval"),
            new AllAssertion(Arrays.asList(
                before = new SetVariableAssertion("blah", "blahval ${foo}")
            )),
            foreach = new ForEachLoopAssertion("blah", "i", Arrays.asList(
                iterating = new SetVariableAssertion("accum", "${accum}${i.current}")
            )),
            middle = new SetVariableAssertion("asdf", "asdfval ${blah}"),
            afterAll = new AllAssertion(Arrays.asList(
                new SetVariableAssertion("qwer", "qwerval ${neverset}")
            ))
        ));
    }

    @Test
    public void testGetVariablesSetByPredecessors() throws Exception {
        Map<String, VariableMetadata> varsSet = PolicyVariableUtils.getVariablesSetByPredecessors(middle);
        assertTrue(varsSet.containsKey("blah"));
        assertTrue(varsSet.containsKey("accum"));
        assertTrue(varsSet.containsKey("i.current"));
        assertFalse(varsSet.containsKey("asdf"));
        assertFalse(varsSet.containsKey("qwer"));
        assertFalse(varsSet.containsKey("neverset"));
    }

    @Test
    public void testGetVariablesSetByPredecessors_foreach() throws Exception {
        Map<String, VariableMetadata> varsSet = PolicyVariableUtils.getVariablesSetByPredecessors(iterating);
        assertTrue(varsSet.containsKey("blah"));
        assertFalse(varsSet.containsKey("accum"));
        assertTrue(varsSet.containsKey("i.current"));
        assertFalse(varsSet.containsKey("asdf"));
        assertFalse(varsSet.containsKey("qwer"));
        assertFalse(varsSet.containsKey("neverset"));
    }

    @Test
    public void testGetVariablesSetByPredecessorsAndSelf() throws Exception {
        Map<String, VariableMetadata> varsSet = PolicyVariableUtils.getVariablesSetByPredecessorsAndSelf(middle);
        assertTrue(varsSet.containsKey("blah"));
        assertTrue(varsSet.containsKey("asdf"));
        assertFalse(varsSet.containsKey("qwer"));
        assertFalse(varsSet.containsKey("neverset"));
    }

    @Test
    public void testGetVariablesSetByPredecessorsAndSelf_foreach() throws Exception {
        Map<String, VariableMetadata> varsSet = PolicyVariableUtils.getVariablesSetByPredecessorsAndSelf(foreach);
        assertTrue(varsSet.containsKey("blah"));
        assertFalse(varsSet.containsKey("accum"));
        assertTrue(varsSet.containsKey("i.current"));
        assertFalse(varsSet.containsKey("asdf"));
        assertFalse(varsSet.containsKey("qwer"));
        assertFalse(varsSet.containsKey("neverset"));
    }

    @Test
    public void testGetVariablesSetByDescendantsAndSelf() throws Exception {
        Map<String, VariableMetadata> varsSet = PolicyVariableUtils.getVariablesSetByDescendantsAndSelf(afterAll);
        assertTrue(varsSet.containsKey("qwer"));
        assertFalse(varsSet.containsKey("blah"));
        assertFalse(varsSet.containsKey("asdf"));
        assertFalse(varsSet.containsKey("neverset"));

        varsSet = PolicyVariableUtils.getVariablesSetByDescendantsAndSelf(root);
        assertTrue(varsSet.containsKey("qwer"));
        assertTrue(varsSet.containsKey("blah"));
        assertTrue(varsSet.containsKey("asdf"));
        assertFalse(varsSet.containsKey("neverset"));
    }

    @Test
    public void testGetVariablesUsedBySuccessors() throws Exception {
        Set<String> varsUsed = PolicyVariableUtils.getVariablesUsedBySuccessors(before);
        assertTrue(varsUsed.contains("blah"));
        assertTrue(varsUsed.contains("neverset"));
        assertFalse(varsUsed.contains("qwer"));
        assertFalse("Should not include variables used by self", varsUsed.contains("foo"));
        assertFalse(varsUsed.contains("neverused"));
    }

    @Test
    public void testGetVariablesUsedByDescendantsAndSelf() throws Exception {
        Set<String> varsUsed = new HashSet<String>(Arrays.asList(PolicyVariableUtils.getVariablesUsedByDescendantsAndSelf(root)));
        assertTrue(varsUsed.contains("blah"));
        assertTrue(varsUsed.contains("neverset"));
        assertTrue(varsUsed.contains("foo"));
        assertFalse(varsUsed.contains("qwer"));
        assertFalse(varsUsed.contains("neverused"));
    }

    @Test
    public void testUseVariables() {
        root = new SetVariableAssertion("variableTOSet", "${assertion.latency.ms}");
        assertTrue(PolicyVariableUtils.usesAnyVariable(root, "assertion.latency.ms"));
        assertFalse(PolicyVariableUtils.usesAnyVariable(root, "assertion.latency.ns"));
        assertTrue(PolicyVariableUtils.usesAnyVariable(root, "assertion.latency.ns", "assertion.latency.ms"));
        assertFalse(PolicyVariableUtils.usesAnyVariable(root, null));
    }

    @Test
    public void testGetVariablesUsedByPolicyButNotPreviouslySet() throws Exception {
        Set<String> varsUsed = new HashSet<String>(Arrays.asList(PolicyVariableUtils.getVariablesUsedByPolicyButNotPreviouslySet(root, null)));

        assertTrue(varsUsed.contains("neverset"));
        assertTrue(varsUsed.contains("accum"));
        assertEquals(2, varsUsed.size());
    }

    @Test
    public void testGetVariablesSetByPolicyButNotSubsequentlyUsed() throws Exception {
        Map<String, VariableMetadata> varsSet = PolicyVariableUtils.getVariablesSetByPolicyButNotSubsequentlyUsed(root, null);

        assertTrue(varsSet.containsKey("accum"));
        assertTrue(varsSet.containsKey("asdf"));
        assertTrue(varsSet.containsKey("i.exceededlimit"));
        assertTrue(varsSet.containsKey("i.iterations"));
        assertTrue(varsSet.containsKey("neverused"));
        assertTrue(varsSet.containsKey("qwer"));
        assertEquals(6, varsSet.size());
    }
}
