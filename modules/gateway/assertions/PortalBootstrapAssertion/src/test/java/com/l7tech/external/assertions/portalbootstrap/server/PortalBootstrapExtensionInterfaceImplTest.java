package com.l7tech.external.assertions.portalbootstrap.server;

import com.l7tech.external.assertions.portalbootstrap.PortalBootstrapExtensionInterface;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.Goid;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author ALEE, 6/25/2015
 */
public class PortalBootstrapExtensionInterfaceImplTest {
    private PortalBootstrapExtensionInterface extInt;

    @Before
    public void setup() {
        extInt = new PortalBootstrapExtensionInterfaceImpl();
    }

    @Test(expected = IllegalArgumentException.class)
    public void enrollWithPortalNullConnectionName() throws Exception {
        testOtkConnectionName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void enrollWithPortalEmptyConnectionName() throws Exception {
        testOtkConnectionName(" ");
    }

    private void testOtkConnectionName(final String otkConnectionName) throws IOException {
        final JdbcConnection connection = new JdbcConnection();
        connection.setGoid(new Goid(0, 1));
        connection.setName(otkConnectionName);
        try {
            extInt.enrollWithPortal("fake", connection);
            fail("expected IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("OTK connection name must not be null or empty", e.getMessage());
            throw e;
        }
    }
}
