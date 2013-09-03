package com.l7tech.gateway.common.log;

import com.l7tech.objectmodel.SecurityZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SinkConfigurationTest {
    private SinkConfiguration s1;
    private SinkConfiguration s2;
    private SecurityZone zone;

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setName("TestZone");
        s1 = new SinkConfiguration();
        s2 = new SinkConfiguration();
    }

    @Test
    public void equalsDifferentSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(null);
        assertFalse(s1.equals(s2));
        assertFalse(s2.equals(s1));
    }

    @Test
    public void equalsSameSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(zone);
        assertTrue(s1.equals(s2));
        assertTrue(s2.equals(s1));
    }

    @Test
    public void testHashCodeDifferentSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(null);
        assertFalse(s1.hashCode() == s2.hashCode());
    }

    @Test
    public void testHashCodeSameSecurityZone() {
        s1.setSecurityZone(zone);
        s2.setSecurityZone(zone);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void copyFromSetsSecurityZone() {
        final SecurityZone zone = new SecurityZone();
        s1.setSecurityZone(zone);
        final SinkConfiguration copy = new SinkConfiguration();
        copy.copyFrom(s1);
        assertEquals(zone, copy.getSecurityZone());
    }

    @Test
    public void copyFromSetsNullSecurityZone() {
        s1.setSecurityZone(null);
        final SinkConfiguration copy = new SinkConfiguration();
        copy.setSecurityZone(new SecurityZone());
        copy.copyFrom(s1);
        assertNull(copy.getSecurityZone());
    }

    @Test
    public void testLoadingProperties(){
        String xml = "<java version=\"1.6.0\" class=\"java.beans.XMLDecoder\"><object class=\"java.util.HashMap\"><void method=\"put\"><string>file.maxSize</string><string>20000</string></void><void method=\"put\"><string>file.format</string><string>STANDARD</string></void><void method=\"put\"><string>file.logCount</string><string>10</string></void></object></java>";
        SinkConfiguration sinkConfiguration = new SinkConfiguration();
        sinkConfiguration.setXmlProperties(xml);
        Assert.assertEquals("20000", sinkConfiguration.getProperty("file.maxSize"));
    }
}
