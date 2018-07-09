package com.l7tech.external.assertions.hazelcastembeddedprovider;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

/**
 * Test the HazelcastEmbeddedProviderAssertion.
 */
public class HazelcastEmbeddedProviderAssertionTest {

    @Test
    public void testCloneIsDeepCopy() {
        AllAssertionsTest.checkCloneIsDeepCopy(new HazelcastEmbeddedProviderAssertion());
    }

}
