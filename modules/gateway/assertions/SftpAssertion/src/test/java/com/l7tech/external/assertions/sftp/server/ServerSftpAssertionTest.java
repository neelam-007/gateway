package com.l7tech.external.assertions.sftp.server;

import com.l7tech.external.assertions.sftp.SftpAssertion;
import com.l7tech.policy.AssertionRegistry;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test the SftpAssertion.
 */
public class ServerSftpAssertionTest {

    @BeforeClass
    public static void setup() {
        AssertionRegistry.installEnhancedMetadataDefaults();
        System.setProperty("com.l7tech.logging.debug", "true");
    }

    @Test
    public void testFeatureSetName() throws Exception {
        assertEquals("assertion:Sftp", new SftpAssertion().getFeatureSetName());
    }

    @Test
    public void testToDo() throws Exception {
        // TODO
    }
}