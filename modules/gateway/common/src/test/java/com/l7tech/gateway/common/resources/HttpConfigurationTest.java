package com.l7tech.gateway.common.resources;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HttpConfigurationTest {
    private HttpConfiguration config;
    private SecurityZone zone;

    @Before
    public void setup() {
        config = new HttpConfiguration();
        zone = new SecurityZone();
    }

    @Test
    public void copyConstructorSetsSecurityZone() {
        config.setSecurityZone(zone);
        final HttpConfiguration copy = new HttpConfiguration(config, false);
        assertEquals(zone, copy.getSecurityZone());
    }
}
