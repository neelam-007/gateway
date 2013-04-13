package com.l7tech.identity;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class IdentityProviderConfigTest {
    private IdentityProviderConfig config;

    @Before
    public void setup() {
        config = new IdentityProviderConfig();
    }

    @Test
    public void copyFromSetsSecurityZone() {
        final SecurityZone zone = new SecurityZone();
        config.setSecurityZone(zone);
        final IdentityProviderConfig copy = new IdentityProviderConfig();
        copy.copyFrom(config);
        assertEquals(zone, copy.getSecurityZone());
    }

    @Test
    public void copyFromSetsNullSecurityZone() {
        config.setSecurityZone(null);
        final IdentityProviderConfig copy = new IdentityProviderConfig();
        copy.setSecurityZone(new SecurityZone());
        copy.copyFrom(config);
        assertNull(copy.getSecurityZone());
    }
}
