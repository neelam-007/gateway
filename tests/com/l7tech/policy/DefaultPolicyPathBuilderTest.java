/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id:$
 */

package com.l7tech.policy;

import com.l7tech.common.xml.TestDocuments;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.wsp.WspReader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.List;

/**
 * Test the default policy assertion path builder/analyzer class
 * functionality.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class DefaultPolicyPathBuilderTest extends TestCase {

    public DefaultPolicyPathBuilderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DefaultPolicyPathBuilderTest.class);
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
        DefaultPolicyPathBuilder builder = new DefaultPolicyPathBuilder();

        assertTrue(builder.generate(oom).getPathCount() == 3);
    }

    public void testAllAssertionSingleDepthWithConjunctionOr() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new TrueAssertion()
          });

        final List kids2 =
          Arrays.asList(new Assertion[]{
              new FalseAssertion()
          });

        Assertion one = new OneOrMoreAssertion(kids);
        Assertion two = new OneOrMoreAssertion(kids2);
        Assertion oom = new AllAssertion(Arrays.asList(new Assertion[]{one, two}));
        DefaultPolicyPathBuilder builder = new DefaultPolicyPathBuilder();

        final int pathCount = builder.generate(oom).getPathCount();
        assertTrue("The path count value received is " + pathCount, pathCount == 2);
    }


    public void testSingleDepthPolicyPathWithConjunctionAnd() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        Assertion oom = new AllAssertion(kids);
        DefaultPolicyPathBuilder builder = new DefaultPolicyPathBuilder();

        assertTrue(builder.generate(oom).getPathCount() == 1);
    }

    public void testSingleDepthPolicyPathWithConjunctionAnd2() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new OneOrMoreAssertion(),
              new TrueAssertion()
          });

        Assertion oom = new AllAssertion(kids);
        DefaultPolicyPathBuilder builder = new DefaultPolicyPathBuilder();

        assertTrue(builder.generate(oom).getPathCount() == 1);
    }

    public void testTwoDepthPolicyPathWithConjunctionOr() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new FalseAssertion()
          });

        final List kids2 =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        final List kids3 =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new FalseAssertion()
          });

        Assertion one = new OneOrMoreAssertion(kids);
        Assertion two = new OneOrMoreAssertion(kids2);
        Assertion three = new OneOrMoreAssertion(kids3);

        Assertion oom = new OneOrMoreAssertion(Arrays.asList(new Assertion[]{one, two, three}));
        DefaultPolicyPathBuilder builder = new DefaultPolicyPathBuilder();
        int count = builder.generate(oom).getPathCount();
        assertTrue("The value received is " + count, count == 63);
    }

    public void testTwoDepthPolicyPathWithConjunctionAnd() throws Exception {
        final List kids =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        final List kids2 =
          Arrays.asList(new Assertion[]{
              new TrueAssertion(),
              new FalseAssertion(),
              new TrueAssertion()
          });

        Assertion one = new OneOrMoreAssertion(kids);
        Assertion two = new OneOrMoreAssertion(kids2);
        Assertion three = new AllAssertion(Arrays.asList(new Assertion[]{new TrueAssertion()}));
        Assertion oom = new OneOrMoreAssertion(Arrays.asList(new Assertion[]{one, two, three}));
        DefaultPolicyPathBuilder builder = new DefaultPolicyPathBuilder();

        int count = builder.generate(oom).getPathCount();
        assertTrue("The value received is " + count, count == 31);
    }

    public void testBug763MonsterPolicy() throws Exception {
        Assertion policy = WspReader.parse(TestDocuments.getInputStream(TestDocuments.BUG_763_MONSTER_POLICY));
        DefaultPolicyPathBuilder builder = new DefaultPolicyPathBuilder();

        PolicyPathResult result = builder.generate(policy);
        int count = result.getPathCount();
        assertTrue("The value received is " + count, count == 23);
    }

    public void testPerOperationDecorationsTreeFollowedByIdTree() throws Exception {
        Assertion policy = WspReader.parse(TestDocuments.getInputStream(TestDocuments.WAREHOUSE_SECURED_POLICY));
        DefaultPolicyPathBuilder builder = new DefaultPolicyPathBuilder();

        PolicyPathResult result = builder.generate(policy);
        int count = result.getPathCount();
        assertTrue("The value received is " + count, count == 15);
    }
}