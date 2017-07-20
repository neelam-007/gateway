package com.l7tech.external.assertions.circuitbreaker;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the CircuitBreakerAssertion.
 */
public class CircuitBreakerAssertionTest {

    private static final Logger log = Logger.getLogger(CircuitBreakerAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new CircuitBreakerAssertion());
    }

}
