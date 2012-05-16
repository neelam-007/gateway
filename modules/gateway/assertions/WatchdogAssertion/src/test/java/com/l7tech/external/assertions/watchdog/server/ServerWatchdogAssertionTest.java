package com.l7tech.external.assertions.watchdog.server;

import com.l7tech.external.assertions.watchdog.WatchdogAssertion;
import com.l7tech.message.Message;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.SyspropUtil;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Test the WatchdogAssertion.
 */
public class ServerWatchdogAssertionTest {
    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.watchdog.interruptRequest",
            "com.l7tech.watchdog.logStackTrace"
        );
    }

    @Test
    public void testWatchdogExpiry() throws Exception {
        SyspropUtil.setProperty("com.l7tech.watchdog.interruptRequest", "true");
        SyspropUtil.setProperty("com.l7tech.watchdog.logStackTrace", "false");
        WatchdogAssertion ass = new WatchdogAssertion();
        ass.setMilliseconds(50);
        ServerWatchdogAssertion sass = new ServerWatchdogAssertion(ass);

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        try {
            sass.checkRequest(context);
            Thread.sleep(200);
            fail("Request thread was not interrupted by watchdog");
        } catch (InterruptedException e) {
            // Ok
        } finally {
            context.close();
        }
    }
}
