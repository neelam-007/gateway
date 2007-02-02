package com.l7tech.policy.assertion;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.logging.Logger;

/**
 * Test for Assertion#meta() and DefaultAssertionMetadata.
 */
public class AssertionMetadataTest extends TestCase {
    private static final Logger log = Logger.getLogger(AssertionMetadataTest.class.getName());

    public AssertionMetadataTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AssertionMetadataTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testDefaults() throws Exception {
        Assertion ass = new RateLimitAssertion();
        AssertionMetadata am = ass.meta();
        assertNotNull(am);

        assertEquals("RateLimit", am.get(AssertionMetadata.PROP_BASE_NAME));
        assertEquals("", am.get(AssertionMetadata.PROP_DESCRIPTION));
        assertEquals("Rate Limit Assertion", am.get(AssertionMetadata.PROP_LONG_NAME));
        assertEquals("Rate Limit", am.get(AssertionMetadata.PROP_SHORT_NAME));
        assertEquals(Boolean.FALSE, am.get(AssertionMetadata.PROP_USED_BY_CLIENT));
        assertEquals("com.l7tech.policy.wsp.RateLimitAssertionMapping", am.get(AssertionMetadata.PROP_WSP_TYPE_MAPPING));
        assertEquals("com.l7tech.console.action.RateLimitPropertiesAction", am.get(AssertionMetadata.PROP_PROPERTIES_ACTION));
        log.info("Generated defaults test OK");
    }
}
