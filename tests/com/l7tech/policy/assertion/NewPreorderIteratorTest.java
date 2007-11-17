/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.policy.PolicyManagerStub;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.PolicyCacheImpl;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyType;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class NewPreorderIteratorTest extends TestCase {
    private static final Logger log = Logger.getLogger(NewPreorderIteratorTest.class.getName());

    public NewPreorderIteratorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(NewPreorderIteratorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * The test of the Includes version is in {@link com.l7tech.server.policy.PolicyDependencyTest}
     */
    public void testNoIncludes() throws Exception {
        final HttpBasic basic = new HttpBasic();
        final SpecificUser specificUser = new SpecificUser();
        final MemberOfGroup memberOfGroup = new MemberOfGroup();
        final OneOrMoreAssertion oneOrMore = new OneOrMoreAssertion(Arrays.asList(specificUser, memberOfGroup));
        AllAssertion all = new AllAssertion(Arrays.asList(basic, oneOrMore));

        NewPreorderIterator npi = new NewPreorderIterator(all, null);
        assertSame(npi.next(), all);
        assertSame(npi.next(), basic);
        assertSame(npi.next(), oneOrMore);
        assertSame(npi.next(), specificUser);
        assertSame(npi.next(), memberOfGroup);
        assertFalse(npi.hasNext());

        log.info(all.toString());
    }

    /**
     * The test of the Includes version is in {@link com.l7tech.server.policy.PolicyDependencyTest}
     */
    public void testRemoveNoIncludes() throws Exception {
        final HttpBasic basic = new HttpBasic();
        final SpecificUser specificUser = new SpecificUser();
        final MemberOfGroup memberOfGroup = new MemberOfGroup();
        final OneOrMoreAssertion oneOrMore = new OneOrMoreAssertion(Arrays.asList(specificUser, memberOfGroup));
        AllAssertion all = new AllAssertion(Arrays.asList(basic, oneOrMore));

        NewPreorderIterator npi = new NewPreorderIterator(all, null);
        assertSame(npi.next(), all);
        assertSame(npi.next(), basic);
        assertSame(npi.next(), oneOrMore);
        assertSame(npi.next(), specificUser);
        npi.remove();
        assertFalse(oneOrMore.getChildren().contains(specificUser));
        assertTrue(oneOrMore.getChildren().contains(memberOfGroup));
        assertSame(npi.next(), memberOfGroup);
        assertFalse(npi.hasNext());
        log.info(all.toString());
    }

    public void testCantRemoveRoot() throws Exception {
        final AllAssertion all = new AllAssertion(Arrays.asList(new HttpBasic()));
        try {
            final NewPreorderIterator npi = new NewPreorderIterator(all, null);
            assertSame(npi.next(), all);
            npi.remove();
            fail("Expected UOE");
        } catch (UnsupportedOperationException e) {
            // OK
        }
    }

}