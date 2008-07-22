package com.l7tech.external.assertions.esm.server;

import com.l7tech.server.ServerConfigStub;
import com.l7tech.external.assertions.esm.EsmMetricsAssertion;
import com.l7tech.external.assertions.esm.EsmSubscriptionAssertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

/**
 * Test the EsmAssertion.
 */
public class EsmAssertionsTest extends TestCase {

    private static final Logger log = Logger.getLogger(EsmAssertionsTest.class.getName());
    private static ApplicationContext applicationContext;
    private static ServerConfigStub serverConfig;

    public EsmAssertionsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(EsmAssertionsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testFeatureNames() throws Exception {
        assertEquals("assertion:EsmMetrics", new EsmMetricsAssertion().getFeatureSetName());
        assertEquals("assertion:EsmSubscription", new EsmSubscriptionAssertion().getFeatureSetName());
    }
}