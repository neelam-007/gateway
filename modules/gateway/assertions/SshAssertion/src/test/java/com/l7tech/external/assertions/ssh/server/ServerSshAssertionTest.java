package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.policy.AssertionRegistry;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test the SftpAssertion.
 */
public class ServerSshAssertionTest {

    @BeforeClass
    public static void setup() {
        AssertionRegistry.installEnhancedMetadataDefaults();
        System.setProperty("com.l7tech.logging.debug", "true");
    }

    @Test
    public void testFeatureSetName() throws Exception {
        assertEquals("set:modularAssertions", new SshRouteAssertion().getFeatureSetName());
    }

    @Test
    public void testToDo() throws Exception {
        // TODO
    }
}