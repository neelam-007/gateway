package com.l7tech.external.assertions.pbkvs;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the PolicyBackedKeyValueStoreAssertion.
 */
public class PolicyBackedKeyValueStoreAssertionTest {

    private static final Logger log = Logger.getLogger(PolicyBackedKeyValueStoreAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy( new PolicyBackedKeyValueStoreAssertion() );
    }

}
