package com.l7tech.external.assertions.siteminder;

import static org.junit.Assert.*;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the SiteMinderAssertion.
 */
public class SiteMinderAssertionTest {

    private static final Logger log = Logger.getLogger(SiteMinderAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new SiteMinderAssertion());
    }

}
