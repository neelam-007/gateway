package com.l7tech.external.assertions.mqnative;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.util.EntityUseUtils;
import com.l7tech.test.BugNumber;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test MQ Native Route
 */
public class MqNativeRoutingAssertionTest {

    @Test
    @BugNumber(11951)
    public void testEntityUsageOverride() {
        assertEquals("Overridden entity type name", "MQ Native Queue", EntityUseUtils.getTypeName( new MqNativeRoutingAssertion(), EntityType.SSG_ACTIVE_CONNECTOR ));
        assertEquals("Default entity type name", "Listen Port", EntityUseUtils.getTypeName( new MqNativeRoutingAssertion(), EntityType.SSG_CONNECTOR ));
    }
}
