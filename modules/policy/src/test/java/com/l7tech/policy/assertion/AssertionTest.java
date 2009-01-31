/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.util.Arrays;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;

import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.AllAssertions;
import com.l7tech.util.HexUtils;
import org.junit.Test;
import org.junit.Assert;

/**
 * Test Assertion/CompositeAssertion data structure management.
 */
@SuppressWarnings({"unchecked"})
public class AssertionTest {
    private static final String ASSERTION_PRE_50 = "rO0ABXNyADFjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uQ3VzdG9tQXNzZXJ0aW9uSG9sZGVyZtcreFwddTICAANMAAhjYXRlZ29yeXQAKkxjb20vbDd0ZWNoL3BvbGljeS9hc3NlcnRpb24vZXh0L0NhdGVnb3J5O0wAD2N1c3RvbUFzc2VydGlvbnQAMUxjb20vbDd0ZWNoL3BvbGljeS9hc3NlcnRpb24vZXh0L0N1c3RvbUFzc2VydGlvbjtMAA9kZXNjcmlwdGlvblRleHR0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAlY29tLmw3dGVjaC5wb2xpY3kuYXNzZXJ0aW9uLkFzc2VydGlvbttfY5k8vaKxAgAAeHBwcHA=";

    @Test
    public void testAddingChildToParent() throws Exception {
        CompositeAssertion root = new AllAssertion();
        Assert.assertEquals(1, root.getOrdinal());
        Assert.assertNull(root.getParent());

        Assertion t1 = new TrueAssertion();
        Assert.assertEquals(1, root.getOrdinal());
        Assert.assertNull(t1.getParent());

        root.getChildren().add(t1);
        root.treeChanged();
        Assert.assertEquals(1, root.getOrdinal());
        Assert.assertEquals(2, t1.getOrdinal());
        Assert.assertTrue(t1.getParent() == root);
        Assert.assertNull(root.getParent());
    }

    @Test
    public void testNormalPolicyBuilding() throws Exception {
        Assertion t1 = new TrueAssertion();
        Assertion t2 = new TrueAssertion();
        Assertion t3 = new TrueAssertion();
        Assertion t4 = new TrueAssertion();
        Assertion t5 = new TrueAssertion();
        CompositeAssertion root = new AllAssertion(Arrays.asList(t1, t2, t3, t4, t5));
        Assert.assertEquals(1, root.getOrdinal());
        Assert.assertEquals(2, t1.getOrdinal());
        Assert.assertEquals(5, t4.getOrdinal());
        Assert.assertEquals(6, t5.getOrdinal());
        Assert.assertEquals(root, t3.getParent());
        Assert.assertTrue(root.getChildren().contains(t2));

        Assertion ct1 = new TrueAssertion();
        Assertion ct2 = new TrueAssertion();
        CompositeAssertion c1 = new OneOrMoreAssertion(Arrays.asList(ct1, ct2));
        root.getChildren().add(c1);
        root.treeChanged();
        Assert.assertEquals(root, c1.getParent());
        Assert.assertTrue(root.getChildren().contains(c1));
        Assert.assertEquals(7, c1.getOrdinal());
        Assert.assertEquals(8, ct1.getOrdinal());
        Assert.assertEquals(9, ct2.getOrdinal());
        Assert.assertTrue(root.getAssertionWithOrdinal(7) == c1);

        final FalseAssertion f1 = new FalseAssertion();
        root.getChildren().add(4, f1);
        root.treeChanged();
        Assert.assertEquals(6, f1.getOrdinal());
        Assert.assertEquals(10, ct2.getOrdinal());
        Assert.assertTrue(root.getAssertionWithOrdinal(9) == ct1);
    }

    @Test
    public void testFindFeatureName() throws Exception {
        String compClassname = "com.l7tech.external.assertions.comparison.ComparisonAssertion";
        String defaultCompFs = Assertion.makeDefaultFeatureSetName(compClassname, "com.l7tech.external.assertions.comparison");
        Assert.assertEquals("assertion:Comparison", defaultCompFs);
    }

    @Test
    public void testAnnotationSanity() {
        for ( Assertion assertion : AllAssertions.GATEWAY_EVERYTHING ) {
            String name = assertion.getClass().getName();
            if (name.startsWith("Request")) {
                Assert.assertTrue("Has request annotation : " + name, Assertion.isRequest(assertion));

            } else if (name.startsWith("Response")) {
                Assert.assertTrue("Has response annotation : " + name, Assertion.isResponse(assertion));
            }

            Assert.assertFalse("Has both request and response annotations : " + name,
                    Assertion.isRequest(assertion) && Assertion.isResponse(assertion));                

            if (name.contains("Wss") && !name.contains("Wssp")) {
                Assert.assertTrue("Has WSS annotation : " + name, Assertion.isWSSecurity(assertion));
            }
        }
    }

    @Test
    public void testDeserializePre50IsEnabled() throws Exception {
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream(HexUtils.decodeBase64(ASSERTION_PRE_50)) );
        Assertion assertion = (Assertion) in.readObject();
        Assert.assertTrue("Assertion is enabled", assertion.isEnabled());
    }
}
