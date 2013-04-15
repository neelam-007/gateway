package com.l7tech.security.cert;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TrustedCertTest {
    private SecurityZone zone;
    private TrustedCert cert1;
    private TrustedCert cert2;

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setName("TestZone");
        cert1 = new TrustedCert();
        cert2 = new TrustedCert();
    }

    @Test
    public void copyFromSetsSecurityZone() {
        cert1.setSecurityZone(zone);
        final TrustedCert copy = new TrustedCert();
        copy.copyFrom(cert1);
        assertEquals(zone, copy.getSecurityZone());
    }

    @Test
    public void equalsDifferentSecurityZone() {
        cert1.setSecurityZone(zone);
        cert2.setSecurityZone(null);
        assertFalse(cert1.equals(cert2));
        assertFalse(cert2.equals(cert1));
    }

    @Test
    public void equalsSameSecurityZone() {
        cert1.setSecurityZone(zone);
        cert2.setSecurityZone(zone);
        assertTrue(cert1.equals(cert2));
        assertTrue(cert2.equals(cert1));
    }

    @Test
    public void testHashCodeDifferentSecurityZone() {
        cert1.setSecurityZone(zone);
        cert2.setSecurityZone(null);
        assertFalse(cert1.hashCode() == cert2.hashCode());
    }

    @Test
    public void testHashCodeSameSecurityZone() {
        cert1.setSecurityZone(zone);
        cert2.setSecurityZone(zone);
        assertEquals(cert1.hashCode(), cert2.hashCode());
    }

}
