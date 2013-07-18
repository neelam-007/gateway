package com.l7tech.external.assertions.siteminder;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the SiteMinderAuthenticateAssertion.
 */
public class SiteMinderAuthenticateAssertionTest {

    private static final Logger log = Logger.getLogger(SiteMinderAuthenticateAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new SiteMinderAuthenticateAssertion());
    }

}
