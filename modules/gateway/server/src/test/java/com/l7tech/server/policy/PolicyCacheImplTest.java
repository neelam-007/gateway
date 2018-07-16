package com.l7tech.server.policy;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.EntitiesProcessedInBatch;
import com.l7tech.server.PlatformTransactionManagerStub;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.folder.FolderCache;
import com.l7tech.server.util.ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.*;
import org.mockito.stubbing.*;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.l7tech.policy.PolicyType.INCLUDE_FRAGMENT;
import static com.l7tech.policy.PolicyType.PRIVATE_SERVICE;
import static com.l7tech.server.event.EntityInvalidationEvent.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.ArrayUtils.toArray;
import static org.apache.commons.lang3.ArrayUtils.toPrimitive;
import static org.apache.commons.lang3.ObjectUtils.NULL;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for the behaviors of add/update/remove from policy cache.
 */
@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class PolicyCacheImplTest {

    private static final String POLICY_NAME_PREFIX = "POLICY_";
    private static final String POLICY_SIMPLE = "com/l7tech/server/policy/cache/POLICY_Simple";
    private static final String POLICY_FRAGMENT_SIMPLE = "com/l7tech/server/policy/cache/POLICY_PolicyFragment01";
    private static final String POLICY_FRAGMENT_SIMPLE_MODIFIED = "com/l7tech/server/policy/cache/POLICY_PolicyFragment01_Modified";
    private static final String POLICY_FRAGMENT_INCLUDING_FRAGMENT = "com/l7tech/server/policy/cache/POLICY_PolicyFragment02";
    private static final String POLICY_FRAGMENT_INCLUDING_FRAGMENT_MODIFIED = "com/l7tech/server/policy/cache/POLICY_PolicyFragment02_Modified";
    private static final String POLICY_INCLUDING_SIMPLE_FRAGMENT_01 = "com/l7tech/server/policy/cache/POLICY_ServiceIncludingFragment01_1";
    private static final String POLICY_INCLUDING_SIMPLE_FRAGMENT_02 = "com/l7tech/server/policy/cache/POLICY_ServiceIncludingFragment01_2";
    private static final String POLICY_INCLUDING_FRAGMENT_WITH_FRAGMENT = "com/l7tech/server/policy/cache/POLICY_ServiceIncludingFragment02_1";

    private Map<Goid, Policy> policyMapById = new HashMap<>();
    private Map<String, Policy> policyMapByGuid = new HashMap<>();

    @Mock
    private FolderCache folderCache;
    @Mock
    private PolicyManager policyManager;
    @Mock
    private PolicyVersionManager policyVersionManager;
    private PolicyCacheImpl policyCache;
    private ConfiguredGOIDGenerator goidGenerator;

    @Before
    public void before() throws FindException {
        ApplicationContext context = ApplicationContexts.getTestApplicationContext();
        PlatformTransactionManager platformTransactionManager = new PlatformTransactionManagerStub();
        ServerPolicyFactory serverPolicyFactory = context.getBean("policyFactory", ServerPolicyFactory.class);

        this.goidGenerator = new ConfiguredGOIDGenerator();
        this.policyCache = new PolicyCacheImpl(platformTransactionManager, serverPolicyFactory, this.folderCache, new EntitiesProcessedInBatch());
        this.policyCache.setPolicyManager(this.policyManager);
        this.policyCache.setPolicyVersionManager(this.policyVersionManager);
        this.policyCache.setApplicationEventPublisher(context);

        when(this.policyManager.findByPrimaryKey(any(Goid.class))).thenAnswer((Answer<Policy>) invocation -> this.policyMapById.get(invocation.getArgumentAt(0, Goid.class)));
        when(this.policyManager.findByGuid(any(String.class))).thenAnswer((Answer<Policy>) invocation -> this.policyMapByGuid.get(invocation.getArgumentAt(0, String.class)));
        when(this.policyVersionManager.findActiveVersionForPolicy(any(Goid.class))).thenAnswer((Answer<PolicyVersion>) invocation -> {
            PolicyVersion version = new PolicyVersion();
            version.setPolicyGoid(invocation.getArgumentAt(0, Goid.class));
            version.setOrdinal(this.policyMapById.get(version.getPolicyGoid()).getVersion());
            return version;
        });

        this.policyCache = spy(this.policyCache);
    }

    @Test
    public void testSinglePolicyCaching() throws Exception {
        Policy policy = this.createPolicy(PRIVATE_SERVICE, POLICY_SIMPLE);
        EntityInvalidationEvent event = new EntityInvalidationEvent(policy, Policy.class, new Goid[] { policy.getGoid() }, new char[] { CREATE });
        this.policyCache.onApplicationEvent(event);

        ServerPolicyHandle serverPolicy = this.policyCache.getServerPolicy(policy);
        assertNotNull(serverPolicy);

        // extra verification to ensure that the real update is called only the required number of times and for the correct objects
        verifyUpdatePolicyNumberOfCalls(1, policy);
    }

    @Test
    public void testPolicyRemove() throws Exception {
        Policy policy = this.createPolicy(PRIVATE_SERVICE, POLICY_SIMPLE);
        EntityInvalidationEvent event = new EntityInvalidationEvent(policy, Policy.class, new Goid[] { policy.getGoid() }, new char[] { DELETE });
        this.policyCache.onApplicationEvent(event);

        ServerPolicyHandle serverPolicy = this.policyCache.getServerPolicy(policy);
        assertNotNull(serverPolicy);
        this.removePolicy(policy);
        this.policyCache.onApplicationEvent(event);
        serverPolicy = this.policyCache.getServerPolicy(policy);
        assertNull(serverPolicy);
    }

    @Test
    public void testIncludedPolicyCaching() throws Exception {
        Policy fragment = this.createPolicy(INCLUDE_FRAGMENT, POLICY_FRAGMENT_SIMPLE);
        Policy includingFragment1 = this.createPolicy(PRIVATE_SERVICE, POLICY_INCLUDING_SIMPLE_FRAGMENT_01);
        Policy includingFragment2 = this.createPolicy(PRIVATE_SERVICE, POLICY_INCLUDING_SIMPLE_FRAGMENT_02);
        EntityInvalidationEvent event = new EntityInvalidationEvent(NULL,
                                                                    Policy.class,
                                                                    toArray(fragment.getGoid(), includingFragment1.getGoid(), includingFragment2.getGoid()),
                                                                    toPrimitive(toArray(CREATE, CREATE, CREATE)));
        this.policyCache.onApplicationEvent(event);

        ServerPolicyHandle fragmentServerPolicy = this.policyCache.getServerPolicy(fragment);
        assertNotNull(fragmentServerPolicy);

        // Check if the compiled policy has the correct assertions
        checkAssertions(fragmentServerPolicy, AuditDetailAssertion.class, HttpRoutingAssertion.class);

        testIncludingPolicyFragments(includingFragment2.getGoid(), singletonList(fragment.getGoid()), HttpBasic.class, Include.class);

        // extra verification to ensure that the real update is called only the required number of times
        verifyUpdatePolicyNumberOfCalls(1, fragment);
        verifyUpdatePolicyNumberOfCalls(1, includingFragment1);
        verifyUpdatePolicyNumberOfCalls(1, includingFragment2);
    }

    @Test
    public void testTwoLevelsIncludedPolicyCaching() throws Exception {
        Policy fragment1 = this.createPolicy(INCLUDE_FRAGMENT, POLICY_FRAGMENT_SIMPLE);
        Policy fragment2 = this.createPolicy(INCLUDE_FRAGMENT, POLICY_FRAGMENT_INCLUDING_FRAGMENT);
        Policy includingFragmentWithFragment = this.createPolicy(PRIVATE_SERVICE, POLICY_INCLUDING_FRAGMENT_WITH_FRAGMENT);
        EntityInvalidationEvent event = new EntityInvalidationEvent(NULL,
                Policy.class,
                toArray(fragment1.getGoid(), fragment2.getGoid(), includingFragmentWithFragment.getGoid()),
                toPrimitive(toArray(CREATE, CREATE, CREATE)));
        this.policyCache.onApplicationEvent(event);

        ServerPolicyHandle fragmentServerPolicy = this.policyCache.getServerPolicy(fragment1);
        assertNotNull(fragmentServerPolicy);

        // Check if the compiled policy has the correct assertions
        checkAssertions(fragmentServerPolicy, AuditDetailAssertion.class, HttpRoutingAssertion.class);

        testIncludingPolicyFragments(fragment2.getGoid(), singletonList(fragment1.getGoid()), AuditDetailAssertion.class, Include.class);
        testIncludingPolicyFragments(includingFragmentWithFragment.getGoid(), asList(fragment1.getGoid(), fragment2.getGoid()), HttpBasic.class, Include.class);

        // extra verification to ensure that the real update is called only the required number of times
        verifyUpdatePolicyNumberOfCalls(1, fragment1);
        verifyUpdatePolicyNumberOfCalls(1, fragment2);
        verifyUpdatePolicyNumberOfCalls(1, includingFragmentWithFragment);
    }

    @Test
    public void testIncludedPolicyCachingUpdate() throws Exception {
        Policy fragment = this.createPolicy(INCLUDE_FRAGMENT, POLICY_FRAGMENT_SIMPLE);
        Policy includingFragment1 = this.createPolicy(PRIVATE_SERVICE, POLICY_INCLUDING_SIMPLE_FRAGMENT_01);
        Policy includingFragment2 = this.createPolicy(PRIVATE_SERVICE, POLICY_INCLUDING_SIMPLE_FRAGMENT_02);
        EntityInvalidationEvent event = new EntityInvalidationEvent(NULL,
                Policy.class,
                toArray(fragment.getGoid(), includingFragment1.getGoid(), includingFragment2.getGoid()),
                toPrimitive(toArray(CREATE, CREATE, CREATE)));
        this.policyCache.onApplicationEvent(event);

        ServerPolicyHandle fragmentServerPolicy = this.policyCache.getServerPolicy(fragment);
        assertNotNull(fragmentServerPolicy);

        // Check if the compiled policy has the correct assertions
        checkAssertions(fragmentServerPolicy, AuditDetailAssertion.class, HttpRoutingAssertion.class);

        testIncludingPolicyFragments(includingFragment2.getGoid(), singletonList(fragment.getGoid()), HttpBasic.class, Include.class);

        // let's replace the fragment
        // the fragment should be modified, but the including policies needs to continue having the same content

        fragment = this.createPolicy(fragment.getGoid(), INCLUDE_FRAGMENT, POLICY_FRAGMENT_SIMPLE_MODIFIED, fragment.getVersion() + 1);
        event = new EntityInvalidationEvent(NULL, Policy.class, toArray(fragment.getGoid()), toPrimitive(toArray(UPDATE)));
        this.policyCache.onApplicationEvent(event);

        fragmentServerPolicy = this.policyCache.getServerPolicy(fragment);
        assertNotNull(fragmentServerPolicy);

        // Check if the compiled policy has the correct assertions
        checkAssertions(fragmentServerPolicy, HttpRoutingAssertion.class);

        testIncludingPolicyFragments(includingFragment2.getGoid(), singletonList(fragment.getGoid()), HttpBasic.class, Include.class);

        // extra verification to ensure that the real update is called only the required number of times
        verifyUpdatePolicyNumberOfCalls(2, fragment);
        verifyUpdatePolicyNumberOfCalls(2, includingFragment1);
        verifyUpdatePolicyNumberOfCalls(2, includingFragment2);
    }

    @Test
    public void testTwoLevelsIncludedPolicyCachingUpdate() throws Exception {
        Policy fragment1 = this.createPolicy(INCLUDE_FRAGMENT, POLICY_FRAGMENT_SIMPLE);
        Policy fragment2 = this.createPolicy(INCLUDE_FRAGMENT, POLICY_FRAGMENT_INCLUDING_FRAGMENT);
        Policy includingFragmentWithFragment = this.createPolicy(PRIVATE_SERVICE, POLICY_INCLUDING_FRAGMENT_WITH_FRAGMENT);
        EntityInvalidationEvent event = new EntityInvalidationEvent(NULL,
                Policy.class,
                toArray(fragment1.getGoid(), fragment2.getGoid(), includingFragmentWithFragment.getGoid()),
                toPrimitive(toArray(CREATE, CREATE, CREATE)));
        this.policyCache.onApplicationEvent(event);

        ServerPolicyHandle fragmentServerPolicy = this.policyCache.getServerPolicy(fragment1);
        assertNotNull(fragmentServerPolicy);

        // Check if the compiled policy has the correct assertions
        checkAssertions(fragmentServerPolicy, AuditDetailAssertion.class, HttpRoutingAssertion.class);

        testIncludingPolicyFragments(fragment2.getGoid(), singletonList(fragment1.getGoid()), AuditDetailAssertion.class, Include.class);
        testIncludingPolicyFragments(includingFragmentWithFragment.getGoid(), asList(fragment1.getGoid(), fragment2.getGoid()), HttpBasic.class, Include.class);

        // let's replace the fragments
        // the fragments should be modified, but the including policies needs to continue having the same content

        fragment1 = this.createPolicy(fragment1.getGoid(), INCLUDE_FRAGMENT, POLICY_FRAGMENT_SIMPLE_MODIFIED, fragment1.getVersion() + 1);
        event = new EntityInvalidationEvent(NULL, Policy.class, toArray(fragment1.getGoid()), toPrimitive(toArray(UPDATE)));
        this.policyCache.onApplicationEvent(event);

        fragmentServerPolicy = this.policyCache.getServerPolicy(fragment1);
        assertNotNull(fragmentServerPolicy);

        // Check if the compiled policy has the correct assertions
        checkAssertions(fragmentServerPolicy, HttpRoutingAssertion.class);

        testIncludingPolicyFragments(fragment2.getGoid(), singletonList(fragment1.getGoid()), AuditDetailAssertion.class, Include.class);
        testIncludingPolicyFragments(includingFragmentWithFragment.getGoid(), asList(fragment1.getGoid(), fragment2.getGoid()), HttpBasic.class, Include.class);

        fragment2 = this.createPolicy(fragment2.getGoid(), INCLUDE_FRAGMENT, POLICY_FRAGMENT_INCLUDING_FRAGMENT_MODIFIED, fragment2.getVersion() + 1);
        event = new EntityInvalidationEvent(NULL, Policy.class, toArray(fragment2.getGoid()), toPrimitive(toArray(UPDATE)));
        this.policyCache.onApplicationEvent(event);

        fragmentServerPolicy = this.policyCache.getServerPolicy(fragment2);
        assertNotNull(fragmentServerPolicy);

        testIncludingPolicyFragments(fragment2.getGoid(), singletonList(fragment1.getGoid()), Include.class);
        testIncludingPolicyFragments(includingFragmentWithFragment.getGoid(), asList(fragment1.getGoid(), fragment2.getGoid()), HttpBasic.class, Include.class);

        // extra verification to ensure that the real update is called only the required number of times
        verifyUpdatePolicyNumberOfCalls(2, fragment1);
        verifyUpdatePolicyNumberOfCalls(3, fragment2);
        verifyUpdatePolicyNumberOfCalls(3, includingFragmentWithFragment);
    }

    private void verifyUpdatePolicyNumberOfCalls(int expected, Policy policy) {
        verify(this.policyCache, times(expected)).buildPolicyCacheEntry(argThat(new ArgumentMatcher<Policy>() {
            @Override
            public boolean matches(Object arg) {
                return ((Policy) arg).getGoid().equals(policy.getGoid());
            }
        }), any(Policy.class), any(Set.class), any(Map.class), any(List.class));
    }

    private void testIncludingPolicyFragments(Goid policyGoid, List<Goid> includedGoids, Class<? extends Assertion>... expectedAssertions) throws Exception {
        ServerPolicyHandle includingFragmentServerPolicy = this.policyCache.getServerPolicy(policyGoid);
        assertNotNull(includingFragmentServerPolicy);

        Set<Goid> usedPolicyIds = includingFragmentServerPolicy.getMetadata().getUsedPolicyIds(false);
        assertNotNull(usedPolicyIds);
        assertEquals("Different number of included policies", includedGoids.size(), usedPolicyIds.size());

        includedGoids.forEach(goid -> assertTrue("Policy " + goid.toString() + " was not found as dependency for " + policyGoid.toString(), usedPolicyIds.contains(goid)));

        checkAssertions(includingFragmentServerPolicy, expectedAssertions);
    }

    private void checkAssertions(ServerPolicyHandle handle, Class<? extends Assertion>... expectedAssertions) throws Exception {
        Assertion assertion = handle.getPolicyAssertion();
        assertTrue(assertion instanceof AllAssertion);

        // Check if the compiled policy has the correct assertions
        AllAssertion allAssertion = (AllAssertion) assertion;
        List<Assertion> children = allAssertion.getChildren();
        assertNotNull(children);
        assertFalse(children.isEmpty());
        assertEquals(expectedAssertions.length, children.size());
        for (int i = 0; i < expectedAssertions.length; i++) {
            Class<? extends Assertion> assertionClass = expectedAssertions[i];
            Assertion child = children.get(i);

            assertEquals("Unexpected assertion: " + child.getClass().getSimpleName(), assertionClass, child.getClass());
        }
    }

    private Policy createPolicy(PolicyType policyType, String xmlPath) throws Exception {
        return this.createPolicy((Goid) this.goidGenerator.generate(null, null), policyType, xmlPath, 1);
    }

    private Policy createPolicy(Goid policyGoid, PolicyType policyType, String xmlPath, int version) throws Exception {
        String name = xmlPath.substring(xmlPath.lastIndexOf("/") + POLICY_NAME_PREFIX.length() + 1);
        if (name.endsWith("Modified")) {
            name = name.replace("_Modified", EMPTY);
        }

        Policy policy = new Policy(policyType, name, loadPolicyXml(xmlPath), false);
        policy.setGoid(policyGoid);
        policy.setGuid(name + "_guid");
        policy.setVersion(version);

        this.storePolicy(policy);
        return policy;
    }

    private void storePolicy(Policy policy) {
        this.policyMapById.put(policy.getGoid(), policy);
        this.policyMapByGuid.put(policy.getGuid(), policy);
    }

    private void removePolicy(Policy policy) {
        this.policyMapById.remove(policy.getGoid());
        this.policyMapByGuid.remove(policy.getGuid());
    }

    private static String loadPolicyXml(String policyFilePath) throws IOException {
        return IOUtils.toString(PolicyCacheImplTest.class.getClassLoader().getResourceAsStream(policyFilePath + ".xml"));
    }
}
