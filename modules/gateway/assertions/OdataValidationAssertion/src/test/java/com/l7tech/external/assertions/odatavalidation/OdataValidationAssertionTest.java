package com.l7tech.external.assertions.odatavalidation;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

/**
 * Test the OdataValidationAssertion.
 */
public class OdataValidationAssertionTest {

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new OdataValidationAssertion());
    }

}
