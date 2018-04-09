package com.l7tech.external.assertions.pbsmel;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

/**
 * Test the ServiceMetricsEventListenerAssertion.
 */
public class ServiceMetricsEventListenerAssertionTest {

    @Test
    public void testCloneIsDeepCopy() {
        AllAssertionsTest.checkCloneIsDeepCopy(new ServiceMetricsEventListenerAssertion());
    }

}
