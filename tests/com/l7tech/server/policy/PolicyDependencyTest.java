/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.policy.*;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.PolicyPathBuilder;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.PolicyPathResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AuditAssertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.NewPreorderIterator;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspWriter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PolicyDependencyTest extends TestCase {
    private static final Logger logger = Logger.getLogger(PolicyDependencyTest.class.getName());

    private PolicyManager simpleFinder;
    private PolicyManager cycleFinder;
    private PolicyManager clonedGrandchildFinder;
    private PolicyManager notSoSimpleFinder;

    public PolicyDependencyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(PolicyDependencyTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        AssertionRegistry ar = new AssertionRegistry();
        ar.registerAssertion(Include.class);
        WspConstants.setTypeMappingFinder(ar);

        setupSimple();
        setupCycle();
        setupClonedGrandchild();
        setupNotSoSimple();
    }

    private void setupSimple() {
        final Policy i2 = newPolicy(PolicyType.INCLUDE_FRAGMENT, 1002, "i2", new HttpBasic());
        final Policy i1 = newPolicy(PolicyType.INCLUDE_FRAGMENT, 1001, "i1", new Include(1002L, "i2"));
        final Policy sp = newPolicy(PolicyType.PRIVATE_SERVICE, 2000, "test-sp", new Include(1001L, "test-i1!"));

        simpleFinder = new PolicyManagerStub(new Policy[] { i1, i2, sp });
    }

    private void setupClonedGrandchild() {
        final Policy gc = newPolicy(PolicyType.INCLUDE_FRAGMENT, 1234, "grandchild", new HttpBasic());
        final Policy i2 = newPolicy(PolicyType.INCLUDE_FRAGMENT, 1002, "child2", new Include(1234L, "grandchild"));
        final Policy i1 = newPolicy(PolicyType.INCLUDE_FRAGMENT, 1001, "child1", new Include(1234L, "grandchild"));
        final Policy sp = newPolicy(PolicyType.PRIVATE_SERVICE, 2000, "parent", new Include(1001L, "child1"), new Include(1002L, "child2"));

        clonedGrandchildFinder = new PolicyManagerStub(new Policy[] { gc, i1, i2, sp });
    }

    private void setupCycle() {
        final Policy i1 = newPolicy(PolicyType.INCLUDE_FRAGMENT, 1001, "test-i1", new Include(2000L, "test-i2!"));
        final Policy sp = newPolicy(PolicyType.PRIVATE_SERVICE, 2000, "test-sp", new Include(1001L, "test-i1!"));

        cycleFinder = new PolicyManagerStub(new Policy[] { i1, sp });
    }

    private Policy newPolicy(PolicyType type, long oid, String name, Assertion... asses) {
        final AllAssertion root;
        if (asses.length == 1 && asses[0] instanceof AllAssertion) {
            root = (AllAssertion) asses[0];
        } else {
            root = new AllAssertion(Arrays.asList(asses));
        }
        Policy p = new Policy(type, name, WspWriter.getPolicyXml(root), true);
        p.setOid(oid);
        return p;
    }

    public void testSimple() throws Exception {
        setupPolicyCache(simpleFinder);
    }

    public void testPreorderIterator() throws Exception {
        Policy policy = simpleFinder.findByPrimaryKey(2000);
        Iterator i = new NewPreorderIterator(policy.getAssertion(), new IncludeAssertionDereferenceTranslator(simpleFinder));

        assertTrue(i.hasNext() && i.next() instanceof AllAssertion); // sp
        assertTrue(i.hasNext() && i.next() instanceof AllAssertion); // i1
        assertTrue(i.hasNext() && i.next() instanceof AllAssertion); // i2
        assertTrue(i.hasNext() && i.next() instanceof HttpBasic);
        assertFalse(i.hasNext());
    }

    private void setupNotSoSimple() {
        final Policy audit = newPolicy(PolicyType.INCLUDE_FRAGMENT, 1000, "audit", new AuditAssertion());
        final Policy includeAudit = newPolicy(PolicyType.INCLUDE_FRAGMENT, 1001, "include(audit)", new Include(1000L, "Include Audit"));

        final Policy basicAndUserOrGroup = newPolicy(PolicyType.INCLUDE_FRAGMENT, 1002, "basicAndUserOrGroup", new AllAssertion(Arrays.asList(
                new HttpBasic(),
                new OneOrMoreAssertion(Arrays.asList(
                        new SpecificUser(),
                        new MemberOfGroup()
                )))));
        final Policy includeBasic = newPolicy(PolicyType.INCLUDE_FRAGMENT, 1003, "include(basicAndUserOrGroup)", new Include(1002L, "Include Basic & User or Group"));
        final Policy servicePolicyWithTwoIncludes;
        try {
            servicePolicyWithTwoIncludes = newPolicy(PolicyType.PRIVATE_SERVICE, 1004L, "sp", includeAudit.getAssertion(), includeBasic.getAssertion());
        } catch (IOException e) {
            throw new RuntimeException(e); // Can't happen
        }
        notSoSimpleFinder = new PolicyManagerStub(new Policy[] { audit, includeAudit, basicAndUserOrGroup, includeBasic, servicePolicyWithTwoIncludes });
    }

    public void testNotSoSimple() throws Exception {
        Policy servicePolicy = notSoSimpleFinder.findByPrimaryKey(1004);
        Iterator i = new NewPreorderIterator(servicePolicy.getAssertion(), new IncludeAssertionDereferenceTranslator(notSoSimpleFinder));
        assertTrue(i.hasNext() && i.next() instanceof AllAssertion); // service
        assertTrue(i.hasNext() && i.next() instanceof AllAssertion); // include(audit)
        assertTrue(i.hasNext() && i.next() instanceof AllAssertion); // audit
        assertTrue(i.hasNext() && i.next() instanceof AuditAssertion);
        assertTrue(i.hasNext() && i.next() instanceof AllAssertion); // include(basicAndUserOrGroup)
        assertTrue(i.hasNext() && i.next() instanceof AllAssertion); // basicAndUserOrGroup
        assertTrue(i.hasNext() && i.next() instanceof HttpBasic);
        assertTrue(i.hasNext() && i.next() instanceof OneOrMoreAssertion);
        assertTrue(i.hasNext() && i.next() instanceof SpecificUser);
        assertTrue(i.hasNext() && i.next() instanceof MemberOfGroup);
        assertFalse(i.hasNext());

        PolicyPathBuilder ppb = new PolicyPathBuilderFactory(notSoSimpleFinder).makePathBuilder();
        PolicyPathResult ppr = ppb.generate(servicePolicy.getAssertion());
        assertEquals(ppr.getPathCount(),2);
    }

    public void testCycle() throws Exception {
        try {
            setupPolicyCache(cycleFinder);
            fail("Expected CircularPolicyException");
        } catch (CircularPolicyException e) {
            // Success
            logger.log(Level.INFO, "Caught expected exception", e);
        }
    }

    public void testGrandchild() throws Exception {
        setupPolicyCache(clonedGrandchildFinder);
    }

    public void testFailedDeletion() throws Exception {
        try {
            PolicyCache cache = setupPolicyCache(clonedGrandchildFinder);
            cache.remove(1001);
            fail("Expected PDFE");
        } catch (PolicyDeletionForbiddenException e) {
            logger.log(Level.INFO, "Caught expected exception", e);
        }
    }

    public void testSuccessfulDeletion() throws Exception {
        PolicyCache cache = setupPolicyCache(clonedGrandchildFinder);
        cache.remove(2000);
    }

    private PolicyCache setupPolicyCache(PolicyManager manager) throws Exception {
        PolicyCacheImpl cache = new PolicyCacheImpl(null);
        cache.setPolicyManager(manager);
        cache.afterPropertiesSet();
        return cache;
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }
}
