package com.l7tech.external.assertions.rawtcp.server;

import com.l7tech.external.assertions.rawtcp.SimpleRawTransportAssertion;
import org.junit.Test;

import java.util.logging.Logger;

/**
 * Test the SimpleRawTransportAssertion.
 */
public class ServerSimpleRawTransportAssertionTest {
    private static final Logger logger = Logger.getLogger(ServerSimpleRawTransportAssertionTest.class.getName());

    @Test
    public void testSomething() throws Exception {
        ServerSimpleRawTransportAssertion ass = new ServerSimpleRawTransportAssertion(new SimpleRawTransportAssertion(), null, null);
        // TODO
    }
}
