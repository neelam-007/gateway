/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.AssertionTraversalTest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Test the default policy validator class functionality.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0

 */
public class DefaultPolicyValidatorTest extends TestCase {

    public DefaultPolicyValidatorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DefaultPolicyValidatorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSingleDepthPolicyPathWithConjunctionOr() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        Assertion oom = new OneOrMoreAssertion(kids);
        DefaultPolicyValidator validator = new DefaultPolicyValidator();
        validator.validate(oom);
        assertTrue(validator.assertionPaths.size() == 3);
    }

    public void testSingleDepthPolicyPathWithConjunctionAnd() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        Assertion oom = new AllAssertion(kids);
        DefaultPolicyValidator validator = new DefaultPolicyValidator();
        validator.validate(oom);
        assertTrue(validator.assertionPaths.size() == 1);
    }

     public void testTwoDepthPolicyPathWithConjunctionOr() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        Assertion one = new OneOrMoreAssertion(kids);
        Assertion oom = new OneOrMoreAssertion(Arrays.asList(new Assertion[] {one, one, one}));
        DefaultPolicyValidator validator = new DefaultPolicyValidator();
        validator.validate(oom);
        assertTrue(validator.assertionPaths.size() == 81);
    }

    public void testTwoDepthPolicyPathWithConjunctionAnd() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        Assertion one = new OneOrMoreAssertion(kids);
        Assertion two = new AllAssertion(Arrays.asList(new Assertion[] {new TrueAssertion()}));
        Assertion oom = new OneOrMoreAssertion(Arrays.asList(new Assertion[] {one, one, two}));
        DefaultPolicyValidator validator = new DefaultPolicyValidator();
        validator.validate(oom);
        assertTrue(validator.assertionPaths.size() == 27);
    }
}
