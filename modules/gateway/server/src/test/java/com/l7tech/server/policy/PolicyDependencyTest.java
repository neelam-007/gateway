/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.HeaderBasedEntityFinder;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.EntityFinderStub;
import com.l7tech.server.event.PolicyCacheEvent;
import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.folder.FolderCacheStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class PolicyDependencyTest {
    private static final Logger logger = Logger.getLogger(PolicyDependencyTest.class.getName());

    private PolicyManager simpleFinder;
    private HeaderBasedEntityFinder entityFinder;
    private PolicyManager cycleFinder;
    private PolicyManager complexCycleFinder;
    private PolicyManager clonedGrandchildFinder;
    private PolicyManager invalidGrandchildFinder;
    private PolicyManager unlicensedGrandchildFinder;
    private PolicyManager notSoSimpleFinder;
    private Set<Goid> policiesToSetAsUnlicensed = new HashSet<Goid>();
    private Set<Goid> policiesThatHaveBeenClosed = new HashSet<Goid>();

    @Before
    public void setUp() throws Exception {
        AssertionRegistry ar = new AssertionRegistry();
        ar.registerAssertion(Include.class);
        WspConstants.setTypeMappingFinder(ar);

        entityFinder = new EntityFinderStub();
        setupSimple();
        setupCycle();
        setupComplexCycle();
        setupClonedGrandchild();
        setupInvalidGrandchild();
        setupUnlicensedGrandchild();
        setupNotSoSimple();
    }

    private void setupSimple() {
        final Policy i2 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1002), "i2", new HttpBasic());
        i2.setGuid(UUID.nameUUIDFromBytes("i2".getBytes()).toString());
        final Policy i1 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1001), "i1", new Include(i2.getGuid(), "i2"));
        i1.setGuid(UUID.nameUUIDFromBytes("i1".getBytes()).toString());
        final Policy sp = newPolicy(PolicyType.PRIVATE_SERVICE, new Goid(0,2000), "test-sp", new Include(i1.getGuid(), "test-i1!"));
        sp.setGuid(UUID.nameUUIDFromBytes("test-sp".getBytes()).toString());

        simpleFinder = new PolicyManagerStub(new Policy[] { i1, i2, sp });
    }

    private void setupClonedGrandchild() {
        final Policy gc = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1234), "grandchild", new HttpBasic());
        gc.setGuid(UUID.nameUUIDFromBytes("grandchild".getBytes()).toString());
        final Policy i2 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1002), "child2", new Include(gc.getGuid(), "grandchild"));
        i2.setGuid(UUID.nameUUIDFromBytes("child2".getBytes()).toString());
        final Policy i1 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1001), "child1", new Include(i2.getGuid(), "grandchild"));
        i1.setGuid(UUID.nameUUIDFromBytes("child1".getBytes()).toString());
        final Policy sp1 = newPolicy(PolicyType.PRIVATE_SERVICE, new Goid(0,2000), "parent1", new Include(i1.getGuid(), "child1"), new Include(i2.getGuid(), "child2"));
        sp1.setGuid(UUID.nameUUIDFromBytes("parent1".getBytes()).toString());
        final Policy sp2 = newPolicy(PolicyType.PRIVATE_SERVICE, new Goid(0,2001), "parent2", new Include(i1.getGuid(), "child1"), new Include(i2.getGuid(), "child2"));
        sp2.setGuid(UUID.nameUUIDFromBytes("parent2".getBytes()).toString());

        clonedGrandchildFinder = new PolicyManagerStub(new Policy[] { gc, i1, i2, sp1, sp2 });
    }

    private void setupInvalidGrandchild() {
        final Policy gc1 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1001), "grandchild1", new HttpBasic());
        gc1.setGuid(UUID.nameUUIDFromBytes("grandchild1".getBytes()).toString());
        final Policy gc2 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1002), "grandchild2", "invalid policy xml");
        gc2.setGuid(UUID.nameUUIDFromBytes("grandchild2".getBytes()).toString());
        final Policy i1 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,2001), "child1", new Include(gc1.getGuid(), "grandchild1"));
        i1.setGuid(UUID.nameUUIDFromBytes("child1".getBytes()).toString());
        final Policy i2 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,2002), "child2", new Include(gc2.getGuid(), "grandchild2"));
        i2.setGuid(UUID.nameUUIDFromBytes("child2".getBytes()).toString());
        final Policy sp = newPolicy(PolicyType.PRIVATE_SERVICE, new Goid(0,3000), "parent", new Include(i1.getGuid(), "child1"), new Include(i2.getGuid(), "child2"));
        sp.setGuid(UUID.nameUUIDFromBytes("parent".getBytes()).toString());

        invalidGrandchildFinder = new PolicyManagerStub(new Policy[] { gc1, gc2, i1, i2, sp });
    }

    private void setupUnlicensedGrandchild() {
        policiesToSetAsUnlicensed.add( new Goid(0,11001L) );
        final Policy gc1 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,11001), "grandchild1", new HttpBasic());
        gc1.setGuid(UUID.nameUUIDFromBytes("grandchild1".getBytes()).toString());
        final Policy gc2 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,11002), "grandchild2", new HttpBasic());
        gc2.setGuid(UUID.nameUUIDFromBytes("grandchild2".getBytes()).toString());
        final Policy i1 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,12001), "child1", new Include(gc1.getGuid(), "grandchild1"));
        i1.setGuid(UUID.nameUUIDFromBytes("child1".getBytes()).toString());
        final Policy i2 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,12002), "child2", new Include(gc2.getGuid(), "grandchild2"));
        i2.setGuid(UUID.nameUUIDFromBytes("child2".getBytes()).toString());
        final Policy sp = newPolicy(PolicyType.PRIVATE_SERVICE, new Goid(0,13000), "parent", new Include(i1.getGuid(), "child1"), new Include(i2.getGuid(), "child2"));
        sp.setGuid(UUID.nameUUIDFromBytes("parent".getBytes()).toString());

        unlicensedGrandchildFinder = new PolicyManagerStub(new Policy[] { gc1, gc2, i1, i2, sp });
    }

    private void setupCycle() {
        UUID i1Uuid = UUID.nameUUIDFromBytes("test-i1".getBytes());
        UUID spUuid = UUID.nameUUIDFromBytes("test-sp".getBytes());
        final Policy i1 = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1001), "test-i1", new Include(spUuid.toString(), "test-i2!"));
        i1.setGuid(i1Uuid.toString());
        final Policy sp = newPolicy(PolicyType.PRIVATE_SERVICE, new Goid(0,2000), "test-sp", new Include(i1Uuid.toString(), "test-i1!"));
        sp.setGuid(spUuid.toString());

        cycleFinder = new PolicyManagerStub(new Policy[] { i1, sp });
    }

    private void setupComplexCycle() {
        int cycleLength = 100;
        List<Policy> policies = new ArrayList<Policy>(cycleLength);

        HashMap<Long, String> idToGuidMap = new HashMap<Long, String>();
        for ( int p=0; p<cycleLength; p++) {
            String name = "test-i" + p;
            idToGuidMap.put(1000L + p, UUID.nameUUIDFromBytes(name.getBytes()).toString());
        }

        for ( int p=0; p<cycleLength; p++) {
            long includeId = 1000 + ((1 + p) % cycleLength);
            Policy newPolicy = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1000 + p), "test-i" + (1000 + p), new Include(idToGuidMap.get(includeId), "test-i" + includeId + "!"));
            newPolicy.setGuid(idToGuidMap.get(1000L + p));
            policies.add( newPolicy );
        }

        complexCycleFinder = new PolicyManagerStub(policies.toArray( new Policy[policies.size()] ));
    }

    private Policy newPolicy(PolicyType type, Goid goid, String name, Assertion... asses) {
        final AllAssertion root;
        if (asses.length == 1 && asses[0] instanceof AllAssertion) {
            root = (AllAssertion) asses[0];
        } else {
            root = new AllAssertion(Arrays.asList(asses));
        }
        Policy p = new Policy(type, name, WspWriter.getPolicyXml(root), true);
        p.setGoid(goid);
        return p;
    }

    private Policy newPolicy(PolicyType type, Goid goid, String name, String policyXml) {
        Policy p = new Policy(type, name, policyXml, true);
        p.setGoid(goid);
        return p;
    }

    @Test
    public void testSimple() throws Exception {
        setupPolicyCache(simpleFinder, null);
    }

    @Test
    public void testPreorderIterator() throws Exception {
        Policy policy = simpleFinder.findByPrimaryKey(new Goid(0,2000));
        Iterator i = new NewPreorderIterator(policy.getAssertion(), new IncludeAssertionDereferenceTranslator(simpleFinder));

        assertTrue(i.hasNext() && i.next() instanceof AllAssertion); // sp
        assertTrue(i.hasNext() && i.next() instanceof AllAssertion); // i1
        assertTrue(i.hasNext() && i.next() instanceof AllAssertion); // i2
        assertTrue(i.hasNext() && i.next() instanceof HttpBasic);
        assertFalse(i.hasNext());
    }

    private void setupNotSoSimple() {
        final Policy audit = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1000), "audit", new AuditAssertion());
        audit.setGuid(UUID.nameUUIDFromBytes("audit".getBytes()).toString());
        final Policy includeAudit = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1001), "include(audit)", new Include(audit.getGuid(), "Include Audit"));

        final Policy basicAndUserOrGroup = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1002), "basicAndUserOrGroup", new AllAssertion(Arrays.asList(
                new HttpBasic(),
                new OneOrMoreAssertion(Arrays.asList(
                        new SpecificUser(),
                        new MemberOfGroup()
                )))));
        basicAndUserOrGroup.setGuid(UUID.nameUUIDFromBytes("basicAndUserOrGroup".getBytes()).toString());
        final Policy includeBasic = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1003), "include(basicAndUserOrGroup)", new Include(basicAndUserOrGroup.getGuid(), "Include Basic & User or Group"));
        includeBasic.setGuid(UUID.nameUUIDFromBytes("include(basicAndUserOrGroup".getBytes()).toString());
        final Policy servicePolicyWithTwoIncludes;
        try {
            servicePolicyWithTwoIncludes = newPolicy(PolicyType.PRIVATE_SERVICE, new Goid(0,1004L), "sp", includeAudit.getAssertion(), includeBasic.getAssertion());
            servicePolicyWithTwoIncludes.setGuid(UUID.nameUUIDFromBytes("sp".getBytes()).toString());
        } catch (IOException e) {
            throw new RuntimeException(e); // Can't happen
        }
        notSoSimpleFinder = new PolicyManagerStub(new Policy[] { audit, includeAudit, basicAndUserOrGroup, includeBasic, servicePolicyWithTwoIncludes });
    }

    @Test
    public void testNotSoSimple() throws Exception {
        Policy servicePolicy = notSoSimpleFinder.findByPrimaryKey(new Goid(0,1004));
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

        PolicyPathBuilder ppb = new PolicyPathBuilderFactory(notSoSimpleFinder, entityFinder).makePathBuilder();
        PolicyPathResult ppr = ppb.generate(servicePolicy.getAssertion());
        assertEquals(ppr.getPathCount(),2);
    }

    @Test
    public void testCycle() throws Exception {
        final ApplicationEvent[] holder = new ApplicationEvent[1];
        final boolean[] sawCircular = new boolean[1];
        setupPolicyCache(cycleFinder, new ApplicationEventPublisher(){
            @Override
            public void publishEvent( ApplicationEvent event ) {
                if ( event instanceof PolicyCacheEvent.Invalid ) {
                    holder[0] = event;
                    if ( ((PolicyCacheEvent.Invalid) event).getException() instanceof CircularPolicyException) {
                        sawCircular[0] = true;
                    }
                }
            }
        } );
        assertNotNull("Policy event published", holder[0]);
        assertTrue("Policy event correct type", holder[0] instanceof PolicyCacheEvent.Invalid);
        assertTrue("Policy event cause", sawCircular[0]);
    }

    @Test
    public void testComplexCycle() throws Exception {
        final ApplicationEvent[] holder = new ApplicationEvent[1];
        final boolean[] sawCircular = new boolean[1];
        setupPolicyCache(complexCycleFinder, new ApplicationEventPublisher(){
            @Override
            public void publishEvent( ApplicationEvent event ) {
                if ( event instanceof PolicyCacheEvent.Invalid ) {
                    holder[0] = event;
                    if ( ((PolicyCacheEvent.Invalid) event).getException() instanceof CircularPolicyException) {
                        sawCircular[0] = true;
                    }
                }
            }
        } );
        assertNotNull("Policy event published", holder[0]);
        assertTrue("Policy event correct type", holder[0] instanceof PolicyCacheEvent.Invalid);
        assertTrue("Policy event cause", sawCircular[0]);
    }

    @Test
    public void testGrandchild() throws Exception {
        PolicyCache cache = setupPolicyCache(clonedGrandchildFinder, null);
        Map<Goid, Integer> m = cache.getDependentVersions(new Goid(0,2000L));
        System.err.println( m );
        assertEquals( "Number of dependencies", 4, m.size());
        assertTrue(m.containsKey(new Goid(0,2000L)));
        assertTrue(m.containsKey(new Goid(0,1234L)));
        assertTrue(m.containsKey(new Goid(0,1001L)));
        assertTrue(m.containsKey(new Goid(0,1002L)));
    }

    @Test
    public void testFailedDeletion() throws Exception {
        PolicyCache cache = setupPolicyCache(clonedGrandchildFinder, null);
        try {
            cache.validateRemove(new Goid(0,1001));
            fail("Expected PolicyDeletionForbiddenException");
        } catch (PolicyDeletionForbiddenException e) {
            logger.log(Level.INFO, "Caught expected exception", e);

            // Should be able to remove, even though this invalidates other policies
            assertTrue("Removed from cache", cache.remove( new Goid(0,1001) ));          
        }
    }

    @Test
    public void testRemoveAndRecreate() throws Exception {
        PolicyCache cache = setupPolicyCache(clonedGrandchildFinder, null);

        assertNotNull( "2000 is valid", cache.getServerPolicy( new Goid(0,2000) ));
        System.err.println("Removing from cache");
        cache.remove( new Goid(0,1001) );
        assertNull( "2000 is invalid", cache.getServerPolicy( new Goid(0,2000) ));
        System.err.println("Replacing in cache");
        Policy newPolicy = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1001), "child1", new Include(UUID.nameUUIDFromBytes("grandchild".getBytes()).toString(), "grandchild"));
        newPolicy.setGuid(UUID.nameUUIDFromBytes("child1".getBytes()).toString());
        cache.update( newPolicy );
        assertNotNull( "2000 is valid", cache.getServerPolicy( new Goid(0,2000) ));
    }

    /**
     * Test that removal cascades to invalid entries that are no longer in use 
     */
    @Test
    public void testRemoveWithCascade() throws Exception {
        PolicyCacheImpl cache = setupPolicyCache(clonedGrandchildFinder, null);
        cache.setTraceLevel( Level.INFO );

        assertNotNull( "2000 is valid", cache.getServerPolicy( new Goid(0,2000) ));
        System.err.println("Removing 1001 from cache");
        cache.remove( new Goid(0,1001) );
        assertTrue( "1001 cached invalid", cache.isInCache( new Goid(0,1001L) ));
        assertTrue( "2000 cached invalid", cache.isInCache( new Goid(0,2000L) ));
        assertTrue( "2001 cached invalid", cache.isInCache( new Goid(0,2001L) ));
        System.err.println("Removing 2000 from cache");
        cache.remove( new Goid(0,2000) );
        assertTrue( "1001 cached invalid", cache.isInCache( new Goid(0,1001L) ));
        assertFalse( "2000 cached invalid", cache.isInCache( new Goid(0,2000L) ));
        assertTrue( "2001 cached invalid", cache.isInCache( new Goid(0,2001L) ));
        System.err.println("Removing 2001 from cache");
        cache.remove( new Goid(0,2001) );
        assertFalse( "1001 cached invalid", cache.isInCache( new Goid(0,1001L) ));
        assertFalse( "2000 cached invalid", cache.isInCache( new Goid(0,2000L) ));
        assertFalse( "2001 cached invalid", cache.isInCache( new Goid(0,2001L) ));
    }

    @Test
    public void testInvalidGrandchild() throws Exception {
        PolicyCache cache = setupPolicyCache(invalidGrandchildFinder, null);

        assertNotNull( "1001 is valid", cache.getServerPolicy( new Goid(0,1001) ));
        assertNotNull( "2001 is valid", cache.getServerPolicy( new Goid(0,2001) ));
        assertNull( "1002 is invalid", cache.getServerPolicy( new Goid(0,1002) ));
        assertNull( "2002 is invalid", cache.getServerPolicy( new Goid(0,2002) ));
        assertNull( "3000 is invalid", cache.getServerPolicy( new Goid(0,3000) ));

        Policy newPolicy = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1002), "grandchild2", new HttpBasic());
        newPolicy.setGuid(UUID.nameUUIDFromBytes("grandchild2".getBytes()).toString());
        cache.update( newPolicy );

        assertNotNull( "1001 is valid", cache.getServerPolicy( new Goid(0,1001) ));
        assertNotNull( "2001 is valid", cache.getServerPolicy( new Goid(0,2001) ));
        assertNotNull( "1002 is valid", cache.getServerPolicy( new Goid(0,1002) ));
        assertNotNull( "2002 is valid", cache.getServerPolicy( new Goid(0,2002) ));
        assertNotNull( "3000 is valid", cache.getServerPolicy( new Goid(0,3000) ));        
    }

    @Test
    public void testInvalidGrandchildEvents() throws Exception {
        final Set<Goid> validPolicies = new TreeSet<Goid>();
        final Set<Goid> invalidPolicies = new TreeSet<Goid>();
        PolicyCacheImpl cache = setupPolicyCache(invalidGrandchildFinder, new ApplicationEventPublisher(){
            @Override
            public void publishEvent( ApplicationEvent event ) {
                if ( event instanceof PolicyCacheEvent.Invalid ) {
                    invalidPolicies.add( ((PolicyCacheEvent.Invalid)event).getPolicyId() );
                } else if ( event instanceof PolicyCacheEvent.Updated ) {
                    validPolicies.add( ((PolicyCacheEvent.Updated)event).getPolicy().getGoid() );                    
                }
            }
        } );
        cache.setTraceLevel( Level.INFO );

        System.err.println("Valid policies  : " + validPolicies);
        System.err.println("Invalid policies: " + invalidPolicies);
        assertTrue( "1001 is valid", validPolicies.contains( new Goid(0,1001L) ));
        assertTrue( "2001 is valid", validPolicies.contains( new Goid(0,2001L) ));
        assertTrue( "1002 is invalid", invalidPolicies.contains( new Goid(0,1002L) ));
        assertTrue( "2002 is invalid", invalidPolicies.contains( new Goid(0,2002L) ));
        assertTrue( "3000 is invalid", invalidPolicies.contains( new Goid(0,3000L) ));

        validPolicies.clear();
        invalidPolicies.clear();
        System.err.println("Updating policy 1002");
        Policy newPolicy = newPolicy(PolicyType.INCLUDE_FRAGMENT, new Goid(0,1002), "grandchild2", new HttpBasic());
        newPolicy.setGuid(UUID.nameUUIDFromBytes("grandchild2".getBytes()).toString());
        cache.update( newPolicy );

        System.err.println("Valid policies  : " + validPolicies);
        System.err.println("Invalid policies: " + invalidPolicies);
        assertFalse( "1001 status unchanged", validPolicies.contains( new Goid(0,1001L) ));
        assertFalse( "2001 status unchanged", validPolicies.contains( new Goid(0,2001L) ));
        assertTrue( "1002 is valid", validPolicies.contains( new Goid(0,1002L) ));
        assertTrue( "2002 is valid", validPolicies.contains( new Goid(0,2002L) ));
        assertTrue( "3000 is valid", validPolicies.contains( new Goid(0,3000L) ));
    }

    @Test
    public void testUpdateDescendentForVersioning() throws Exception {
        PolicyCache cache = setupPolicyCache(clonedGrandchildFinder, null);

        String versionBefore = cache.getUniquePolicyVersionIdentifer( new Goid(0,2000L) );

        Policy child = clonedGrandchildFinder.findByPrimaryKey( new Goid(0,1234) );
        child.setVersion( 2 );
        cache.update( child );

        String versionAfter = cache.getUniquePolicyVersionIdentifer( new Goid(0,2000L) );

        assertNotNull("Version before update", versionBefore);
        assertNotNull("Version after update", versionAfter);

        System.err.println( "Version before update: " + versionBefore );
        System.err.println( "Version after update : " + versionAfter );

        assertFalse("Version updated", versionBefore.equals( versionAfter ));
    }

    @Test
    public void testSuccessfulDeletion() throws Exception {
        PolicyCache cache = setupPolicyCache(clonedGrandchildFinder, null);
        assertTrue("Policy deleted", cache.remove(new Goid(0,2000)));
    }

    @Test
    public void testResetUnlicensed() throws Exception {
        PolicyCacheImpl cache = setupPolicyCache(unlicensedGrandchildFinder, null);

        assertNotNull( "11001 is valid", cache.getServerPolicy( new Goid(0,11001) ));
        assertNotNull( "12001 is valid", cache.getServerPolicy( new Goid(0,12001) ));
        assertNotNull( "11002 is valid", cache.getServerPolicy( new Goid(0,11002) ));
        assertNotNull( "12002 is valid", cache.getServerPolicy( new Goid(0,12002) ));
        assertNotNull( "13000 is valid", cache.getServerPolicy( new Goid(0,13000) ));

        assertEquals( "Policy result", AssertionStatus.FAILED, cache.getServerPolicy( new Goid(0,11001L) ).checkRequest( null ) );

        policiesToSetAsUnlicensed.clear();
        cache.onApplicationEvent( new LicenseEvent(this, Level.INFO, "Updated", "New license") );

        assertNotNull( "11001 is valid", cache.getServerPolicy( new Goid(0,11001) ));
        assertNotNull( "12001 is valid", cache.getServerPolicy( new Goid(0,12001) ));
        assertNotNull( "11002 is valid", cache.getServerPolicy( new Goid(0,11002) ));
        assertNotNull( "12002 is valid", cache.getServerPolicy( new Goid(0,12002) ));
        assertNotNull( "13000 is valid", cache.getServerPolicy( new Goid(0,13000) ));

        assertEquals( "Policy result", AssertionStatus.NONE, cache.getServerPolicy( new Goid(0,11001L) ).checkRequest( null ) );
    }

    @Test
    public void testCloseWhenNotUsed() throws Exception {
        PolicyCache cache = setupPolicyCache(simpleFinder, null);

        policiesThatHaveBeenClosed.clear();
        ServerPolicyHandle handle = cache.getServerPolicy( new Goid(0,2000) );
        cache.remove( new Goid(0,2000) );

        assertTrue("2000 not closed", policiesThatHaveBeenClosed.isEmpty() );

        handle.close();

        assertTrue("2000 closed", policiesThatHaveBeenClosed.contains( new Goid(0,2000L) ) );
    }

    private PolicyCacheImpl setupPolicyCache(PolicyManager manager, ApplicationEventPublisher aep) throws Exception {
        if (aep == null) {
            aep = new ApplicationEventPublisher() {
                @Override
                public void publishEvent( ApplicationEvent event ) {
                }
            };
        }

        PolicyCacheImpl cache = new PolicyCacheImpl(null, null, new FolderCacheStub()){
            @Override
            protected ServerAssertion buildServerPolicy( final Policy policy ) throws InvalidPolicyException {
                try {
                    final Assertion assertion = policy.getAssertion();
                    final AssertionStatus status;
                    if ( policiesToSetAsUnlicensed.contains( policy.getGoid() )) {
                        setLicenseStatus( policy.getGoid(), false);
                        status = AssertionStatus.FAILED;
                    } else {
                        setLicenseStatus( policy.getGoid(), true);
                        status = AssertionStatus.NONE;
                    }
                    return new ServerAssertion(){
                        @Override
                        public AssertionStatus checkRequest( PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
                            return status;
                        }

                        @Override
                        public Assertion getAssertion() {
                            return assertion;
                        }

                        @Override
                        public void close() {
                            policiesThatHaveBeenClosed.add( policy.getGoid() );
                        }
                    };
                } catch (IOException ioe) {
                    throw new InvalidPolicyException("Invalid policy", ioe);
                }
            }
            @Override
            protected void logAndAudit( AuditDetailMessage message, String... params ) {
                System.err.println( MessageFormat.format(message.getMessage(), (Object[])params) );
            }
            @Override
            protected void logAndAudit( AuditDetailMessage message, String[] params, Exception ex ) {
                System.err.println( MessageFormat.format(message.getMessage(), (Object[])params) );
                if ( ex != null ) ex.printStackTrace();
            }
        };
        cache.setApplicationEventPublisher( aep );
        cache.setPolicyManager(manager);
        cache.onApplicationEvent(new Started(this, Component.GATEWAY, "Test"));
        return cache;
    }
}
