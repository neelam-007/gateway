package com.l7tech.external.assertions.portalbootstrap;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the PortalBootstrapAssertion.
 */
public class PortalBootstrapAssertionTest {

    private static final Logger log = Logger.getLogger(PortalBootstrapAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy( new PortalBootstrapAssertion() );
    }

}
