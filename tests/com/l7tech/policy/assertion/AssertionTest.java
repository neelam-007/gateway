/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.util.Arrays;

import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;

/**
 * Test Assertion/CompositeAssertion data structure management.
 */
public class AssertionTest extends TestCase {
    private static Logger log = Logger.getLogger(AssertionTest.class.getName());

    public AssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testAddingChildToParent() throws Exception {
        CompositeAssertion root = new AllAssertion();
        assertEquals(1, root.getOrdinal());
        assertNull(root.getParent());

        Assertion t1 = new TrueAssertion();
        assertEquals(1, root.getOrdinal());
        assertNull(t1.getParent());

        root.getChildren().add(t1);
        root.treeChanged();
        assertEquals(1, root.getOrdinal());
        assertEquals(2, t1.getOrdinal());
        assertTrue(t1.getParent() == root);
        assertNull(root.getParent());
    }

    public void testNormalPolicyBuilding() throws Exception {
        Assertion t1 = new TrueAssertion();
        Assertion t2 = new TrueAssertion();
        Assertion t3 = new TrueAssertion();
        Assertion t4 = new TrueAssertion();
        Assertion t5 = new TrueAssertion();
        CompositeAssertion root = new AllAssertion(Arrays.asList(new Assertion[] { t1, t2, t3, t4, t5 }));
        assertEquals(1, root.getOrdinal());
        assertEquals(2, t1.getOrdinal());
        assertEquals(5, t4.getOrdinal());
        assertEquals(6, t5.getOrdinal());
        assertEquals(root, t3.getParent());
        assertTrue(root.getChildren().contains(t2));

        Assertion ct1 = new TrueAssertion();
        Assertion ct2 = new TrueAssertion();
        CompositeAssertion c1 = new OneOrMoreAssertion(Arrays.asList(new Assertion[] { ct1, ct2 }));
        root.getChildren().add(c1);
        root.treeChanged();
        assertEquals(root, c1.getParent());
        assertTrue(root.getChildren().contains(c1));
        assertEquals(7, c1.getOrdinal());
        assertEquals(8, ct1.getOrdinal());
        assertEquals(9, ct2.getOrdinal());
        assertTrue(root.getAssertionWithOrdinal(7) == c1);

        final FalseAssertion f1 = new FalseAssertion();
        root.getChildren().add(4, f1);
        root.treeChanged();
        assertEquals(6, f1.getOrdinal());
        assertEquals(10, ct2.getOrdinal());
        assertTrue(root.getAssertionWithOrdinal(9) == ct1);
    }
}
