/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id: CompositeAssertionTest.java,v 1.5 2003/06/25 19:59:43 mike Exp $
 */

package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Test the assertion traversal functionality.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0

 */
public class AssertionTraversalTest extends TestCase {

    public AssertionTraversalTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AssertionTraversalTest.class);
    }


    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSingleDepthTraversal() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        Assertion oom = new OneOrMoreAssertion(kids);
        int counter = 0;
        for (Iterator i = oom.preorderIterator(); i.hasNext();) {
            i.next();
            ++counter;
        }
        assertTrue(counter == kids.size() + 1);
    }

    public void testDeepTraversal() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        Assertion oom = new OneOrMoreAssertion(kids);
        for (int i = 0; i < 50; i++) {
            oom = new OneOrMoreAssertion(Arrays.asList(new Assertion[]{oom}));
        }
        int counter = 0;
        for (Iterator i = oom.preorderIterator(); i.hasNext();) {
            i.next();
            ++counter;
        }
        assertTrue(counter == kids.size() + 51);
    }

    public void testSingleDepthTraversalWithRemove() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        CompositeAssertion oom = new OneOrMoreAssertion(kids);

        Iterator i = oom.preorderIterator();
        try {
            i.remove();
            fail("IllegalStateException should have been thrown");
        } catch (IllegalStateException e) {
            // expected
        }

        i.next(); //skip root, ugly but not much can do
        i.next();
        i.remove(); // pass

        assertTrue(oom.getChildren().size() == 2);

        try {
            i.remove();
            fail("IllegalStateException should have been thrown");
        } catch (IllegalStateException e) {
            // expected
        }
    }

     public void testDeepTraversalPruneEmptyComposites() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        Assertion oom = new OneOrMoreAssertion(kids);
        for (int i = 0; i < 50; i++) {
            List children = new ArrayList(Arrays.asList(new Assertion[]{oom}));
            children.add(new ExactlyOneAssertion(new ArrayList(0)));
            oom = new OneOrMoreAssertion(children);
        }


        for (Iterator i = oom.preorderIterator(); i.hasNext();) {
            Assertion a = (Assertion)i.next();
            if (a instanceof CompositeAssertion) {
                CompositeAssertion ca = (CompositeAssertion)a;
                if (ca.getChildren().size() == 0) {
                    i.remove();
                }
            }
        }

         for (Iterator i = oom.preorderIterator(); i.hasNext();) {
             Assertion a = (Assertion)i.next();
             if (a instanceof CompositeAssertion) {
                 CompositeAssertion ca = (CompositeAssertion)a;
                 if (ca.getChildren().size() == 0) {
                     fail("Unexpected CompositeAssertion with no children");
                 }
             }
         }
    }

}
