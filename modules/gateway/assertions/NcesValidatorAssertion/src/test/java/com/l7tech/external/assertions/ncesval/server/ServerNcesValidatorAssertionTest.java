package com.l7tech.external.assertions.ncesval.server;

import com.l7tech.external.assertions.ncesval.NcesValidatorAssertion;
import com.l7tech.server.ServerConfigStub;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

/**
 * Test the NcesValidatorAssertion.
 */
public class ServerNcesValidatorAssertionTest extends TestCase {

    private static final Logger log = Logger.getLogger(ServerNcesValidatorAssertionTest.class.getName());
    private static ApplicationContext applicationContext;
    private static ServerConfigStub serverConfig;

    public ServerNcesValidatorAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerNcesValidatorAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSomething() throws Exception {
        System.out.println(new NcesValidatorAssertion().getFeatureSetName());
    }

}
