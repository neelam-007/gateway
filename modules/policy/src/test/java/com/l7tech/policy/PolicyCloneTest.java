/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.util.Arrays;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;

/**
 * Make sure that the clone() and getCopy() methods do the right thing, even for complex policies.
 * User: mike
 * Date: Aug 25, 2003
 * Time: 11:41:56 AM
 */
public class PolicyCloneTest extends TestCase {
    private static Logger log = Logger.getLogger(PolicyCloneTest.class.getName());

    public PolicyCloneTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(PolicyCloneTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testDeepCopy() throws Exception {
        SpecificUser u1 = new SpecificUser();
        u1.setUserLogin("Foo1");
        Assertion pol = new AllAssertion(Arrays.asList(new Assertion[] {
            new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                new MemberOfGroup(),
            })),
            new AllAssertion(Arrays.asList(new Assertion[] {
                new SpecificUser(),
            })),
            new ExactlyOneAssertion(Arrays.asList(new Assertion[] {
                u1,
            })),
        }));

        Assertion pol2 = pol.getCopy();
        ExactlyOneAssertion pol2eoa = (ExactlyOneAssertion) ((AllAssertion)pol2).getChildren().get(2);
        SpecificUser u2 = (SpecificUser) pol2eoa.getChildren().get(0);
        assertTrue(u1.getUserLogin().equals(u2.getUserLogin()));

        // Make sure we got a deep copy, not a reference copy
        u1.setUserLogin("bloof");
        assertFalse(u1.getUserLogin().equals(u2.getUserLogin()));

    }
}
