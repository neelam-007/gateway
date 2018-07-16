package com.l7tech.external.assertions.throughputquota;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;


/**
 * Test the ThroughputQuotaAssertion.
 */
public class ThroughputQuotaAssertionTest {

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new ThroughputQuotaAssertion());
    }

}
