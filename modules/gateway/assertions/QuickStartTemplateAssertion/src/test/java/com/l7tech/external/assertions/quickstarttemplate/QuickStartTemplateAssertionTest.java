package com.l7tech.external.assertions.quickstarttemplate;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the QuickStartTemplateAssertion.
 */
public class QuickStartTemplateAssertionTest {

    private static final Logger log = Logger.getLogger(QuickStartTemplateAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy( new QuickStartTemplateAssertion() );
    }

}
