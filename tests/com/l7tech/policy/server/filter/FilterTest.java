/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.server.filter;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Test of some policy filter scenarios.
 * User: mike
 * Date: Aug 15, 2003
 * Time: 11:45:21 AM
 */
public class FilterTest extends TestCase {
    private static Logger log = Logger.getLogger(FilterTest.class.getName());

    public FilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(FilterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSimpleFilter() throws Exception {
        Assertion pol = new AllAssertion(Arrays.asList(new Assertion[] {
            new SslAssertion(),
            new HttpBasic(),
            new ExactlyOneAssertion(Arrays.asList(new Assertion[] {
                new SpecificUser(0, "alice"),
                new SpecificUser(0, "bob"),
                new MemberOfGroup(0, "sales")
            })),
            new RoutingAssertion()
        }));

        User alice = new User();
        alice.setLogin("alice");

        Assertion filtered = FilterManager.getInstance().applyAllFilters(alice, pol);
        //log.info("Policy filtered for alice: " + WspWriter.getPolicyXml(filtered));

        assertTrue(filtered instanceof AllAssertion);
        List kids = ((CompositeAssertion) filtered).getChildren();
        assertTrue(kids != null);
        assertTrue(kids.size() == 2);
        assertTrue(kids.get(0) instanceof SslAssertion);
        assertTrue(kids.get(1) instanceof HttpBasic);
    }
}
