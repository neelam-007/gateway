package com.l7tech.external.assertions.rawtcp.server;

import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion;
import com.l7tech.policy.AssertionRegistry;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Test the SimpleRawTransportAssertion.
 */
public class ServerSimpleRawTransportAssertionTest {
    private static final Logger logger = Logger.getLogger(ServerSimpleRawTransportAssertionTest.class.getName());

    @BeforeClass
    public static void setup() {
        AssertionRegistry.installEnhancedMetadataDefaults();
    }

    @Test
    public void testFeatureSetName() throws Exception {
        assertEquals("assertion:SimpleRawTransport", new SimpleRawTransportAssertion().getFeatureSetName());
    }
}
