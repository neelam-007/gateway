package com.l7tech.external.assertions.messagecontext.server;

import com.l7tech.server.ServerConfigStub;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

/**
 * Test the MessageContextAssertion.
 */
public class ServerMessageContextAssertionTest extends TestCase {

    private static final Logger log = Logger.getLogger(ServerMessageContextAssertionTest.class.getName());
    private static ApplicationContext applicationContext;
    private static ServerConfigStub serverConfig;

    public ServerMessageContextAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerMessageContextAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSomething() throws Exception {
        fail("Templated test code, please write your own test here (or delete this class).");
    }

}
