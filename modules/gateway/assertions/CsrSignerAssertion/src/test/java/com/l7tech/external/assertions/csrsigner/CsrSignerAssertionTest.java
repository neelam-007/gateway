package com.l7tech.external.assertions.csrsigner;

import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.policy.AssertionRegistry;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Test the CsrSignerAssertion.
 */
public class CsrSignerAssertionTest {

    private static final Logger log = Logger.getLogger(CsrSignerAssertionTest.class.getName());

    @BeforeClass
    public static void beforeClass() throws Exception {
        new AssertionRegistry().afterPropertiesSet();
    }

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new CsrSignerAssertion());
    }

    @Test
    public void testFeatureSetName() throws Exception {
        assertEquals("set:modularAssertions", new CsrSignerAssertion().getFeatureSetName());
    }

}
