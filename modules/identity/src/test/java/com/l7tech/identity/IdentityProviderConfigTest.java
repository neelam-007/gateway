package com.l7tech.identity;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Assert;
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

    @Test
    public void loadProperties(){
        String xml = "<java version=\"1.6.0_01\" class=\"java.beans.XMLDecoder\"><object class=\"java.util.HashMap\"><void method=\"put\"><string>adminEnabled</string><boolean>true</boolean></void></object></java>";
        IdentityProviderConfig alpha = new IdentityProviderConfig();
        alpha.setSerializedProps(xml);

        IdentityProviderConfig beta = new IdentityProviderConfig(alpha);

        Assert.assertEquals(alpha, beta);
    }
}
