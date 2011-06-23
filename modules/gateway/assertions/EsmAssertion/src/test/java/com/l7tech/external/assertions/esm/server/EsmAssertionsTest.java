package com.l7tech.external.assertions.esm.server;

import com.l7tech.server.ServerConfigStub;
import com.l7tech.external.assertions.esm.EsmMetricsAssertion;
import com.l7tech.external.assertions.esm.EsmSubscriptionAssertion;
import org.junit.Test;
import static org.junit.Assert.*;

import org.springframework.context.ApplicationContext;

import java.util.logging.Logger;

/**
 * Test the EsmAssertion.
 */
public class EsmAssertionsTest {

    private static final Logger log = Logger.getLogger(EsmAssertionsTest.class.getName());
    private static ApplicationContext applicationContext;
    private static ServerConfigStub serverConfig;

    @Test
    public void testFeatureNames() throws Exception {
        assertEquals("assertion:EsmMetrics", new EsmMetricsAssertion().getFeatureSetName());
        assertEquals("assertion:EsmSubscription", new EsmSubscriptionAssertion().getFeatureSetName());
    }
}