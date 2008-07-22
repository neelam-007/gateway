package com.l7tech.external.assertions.ncesdeco.server;

import com.l7tech.external.assertions.ncesdeco.NcesDecoratorAssertion;
import com.l7tech.server.ServerConfigStub;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

/**
 * Test the NcesDecoratorAssertion.
 */
public class ServerNcesDecoratorAssertionTest extends TestCase {

    private static final Logger log = Logger.getLogger(ServerNcesDecoratorAssertionTest.class.getName());
    private static ApplicationContext applicationContext;
    private static ServerConfigStub serverConfig;

    public ServerNcesDecoratorAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerNcesDecoratorAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSomething() throws Exception {
        System.out.println(new NcesDecoratorAssertion().getFeatureSetName());
    }

}
