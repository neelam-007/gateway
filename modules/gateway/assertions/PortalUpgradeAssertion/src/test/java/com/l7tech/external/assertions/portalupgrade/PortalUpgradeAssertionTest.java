package com.l7tech.external.assertions.portalupgrade;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the PortalUpgradeAssertion.
 */
public class PortalUpgradeAssertionTest {

    private static final Logger log = Logger.getLogger(PortalUpgradeAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy( new PortalUpgradeAssertion() );
    }

}
