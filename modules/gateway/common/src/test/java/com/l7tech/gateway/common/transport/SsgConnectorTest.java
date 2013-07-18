package com.l7tech.gateway.common.transport;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.test.BugNumber;
import org.junit.Before;
import org.junit.Test;

import static com.l7tech.gateway.common.transport.SsgConnector.CLIENT_AUTH_NEVER;
import static org.junit.Assert.*;

/**
 *
 */
public class SsgConnectorTest {
    private SsgConnector c1;
    private SsgConnector c2;
    private SecurityZone zone;

    @Before
    public void setup() {
        zone = new SecurityZone();
        zone.setName("TestZone");
        c1 = new SsgConnector();
        c2 = new SsgConnector();
    }

    @Test
    public void equalsDifferentSecurityZone() {
        c1.setSecurityZone(zone);
        c2.setSecurityZone(null);
        assertFalse(c1.equals(c2));
        assertFalse(c2.equals(c1));
    }

    @Test
    public void equalsSameSecurityZone() {
        c1.setSecurityZone(zone);
        c2.setSecurityZone(zone);
        assertTrue(c1.equals(c2));
        assertTrue(c2.equals(c1));
    }

    @Test
    public void testHashCodeDifferentSecurityZone() {
        c1.setSecurityZone(zone);
        c2.setSecurityZone(null);
        assertFalse(c1.hashCode() == c2.hashCode());
    }

    @Test
    public void testHashCodeSameSecurityZone() {
        c1.setSecurityZone(zone);
        c2.setSecurityZone(zone);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void testEquals() {
        SsgConnector a = new SsgConnector(new Goid(0,1), "foo bar", 8080, "http", false, "MESSAGE_INPUT", CLIENT_AUTH_NEVER, null, null);
        a.putProperty("foo", "bar");
        SsgConnector b = new SsgConnector(new Goid(0,1), "foo bar", 8080, "http", false, "MESSAGE_INPUT", CLIENT_AUTH_NEVER, null, null);
        b.putProperty("foo", "bar");
        eq(a, b);

        b.putProperty("foo", "rab");
        ne(a, b);

        b.putProperty("foo", "bar");
        eq(a, b);

        a.putProperty("foo", "asdf");
        b.putProperty("foo", "asdf");
        eq(a, b);
    }

    private void eq(Object a, Object b) {
        assertTrue(a.equals(b));
        assertTrue(b.equals(a));
        assertTrue(a.hashCode() == b.hashCode());
    }

    private void ne(Object a, Object b) {
        assertFalse(a.equals(b));
        assertFalse(b.equals(a));
    }

    @Test
    @BugNumber(13718)
    public void testEndpointSetIncludesChildEndpoints() {
        SsgConnector a = new SsgConnector();
        a.setEndpoints("MESSAGE_INPUT,ADMIN_REMOTE");

        assertTrue(a.endpointSet().contains(SsgConnector.Endpoint.MESSAGE_INPUT));
        assertTrue(a.endpointSet().contains(SsgConnector.Endpoint.ADMIN_REMOTE));
        assertTrue(a.endpointSet().contains(SsgConnector.Endpoint.ADMIN_REMOTE_ESM));
        assertTrue(a.endpointSet().contains(SsgConnector.Endpoint.ADMIN_REMOTE_SSM));
    }
}
