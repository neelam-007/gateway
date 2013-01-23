package com.l7tech.gateway.common.transport;

import com.l7tech.test.BugNumber;
import org.junit.Test;

import static com.l7tech.gateway.common.transport.SsgConnector.CLIENT_AUTH_NEVER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class SsgConnectorTest {

    @Test
    public void testEquals() {
        SsgConnector a = new SsgConnector(1, "foo bar", 8080, "http", false, "MESSAGE_INPUT", CLIENT_AUTH_NEVER, null, null);
        a.putProperty("foo", "bar");
        SsgConnector b = new SsgConnector(1, "foo bar", 8080, "http", false, "MESSAGE_INPUT", CLIENT_AUTH_NEVER, null, null);
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
