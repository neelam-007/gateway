package com.l7tech.external.assertions.circuitbreaker;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

/**
 * Test the CircuitBreakerAssertion.
 */
public class CircuitBreakerAssertionTest {

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new CircuitBreakerAssertion());
    }
}
