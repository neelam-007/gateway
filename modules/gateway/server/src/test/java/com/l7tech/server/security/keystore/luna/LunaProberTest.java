package com.l7tech.server.security.keystore.luna;

import org.junit.*;

/**
 *
 */
public class LunaProberTest {
    @Test
    public void testProberDoesNotThrow() throws Exception {
        // The test might run in an environment with or without a configured Luna token,
        // so all we care about here is that it does not throw an exception.
        LunaProber.isLunaClientLibraryAvailable();
    }
}
