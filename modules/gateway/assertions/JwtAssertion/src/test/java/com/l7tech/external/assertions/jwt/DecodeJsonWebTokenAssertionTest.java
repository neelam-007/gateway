package com.l7tech.external.assertions.jwt;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DecodeJsonWebTokenAssertionTest {

    /**
     * Ensure that the policy name factory can handle a DecodeJsonWebTokenAssertion with all default values.
     */
    @Test
    public void getAssertionNameDefaultValues() {
        DecodeJsonWebTokenAssertion defaultAssertion = new DecodeJsonWebTokenAssertion();
        assertEquals("Decode Json Web Token", DecodeJsonWebTokenAssertion.policyNameFactory.getAssertionName(defaultAssertion, true));
    }
}
