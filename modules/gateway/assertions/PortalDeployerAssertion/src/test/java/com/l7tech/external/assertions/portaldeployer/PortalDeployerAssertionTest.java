package com.l7tech.external.assertions.portaldeployer;

import static org.junit.Assert.*;
import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the PortalDeployerAssertion.
 */
public class PortalDeployerAssertionTest {

    private static final Logger log = Logger.getLogger(PortalDeployerAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new PortalDeployerAssertion());
    }

}
