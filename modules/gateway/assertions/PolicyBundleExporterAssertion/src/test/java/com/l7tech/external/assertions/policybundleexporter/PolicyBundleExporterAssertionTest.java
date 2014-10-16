package com.l7tech.external.assertions.policybundleexporter;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

/**
 * Test the PolicyBundleExporterAssertion.
 */
public class PolicyBundleExporterAssertionTest {

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new PolicyBundleExporterAssertion());
    }

}
