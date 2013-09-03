package com.l7tech.gateway.common.jdbc;

import com.l7tech.objectmodel.SecurityZone;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class JdbcConnectionTest {
    private JdbcConnection connection;
    private SecurityZone zone;

    @Before
    public void setup() {
        zone = new SecurityZone();
        connection = new JdbcConnection();
    }

    @Test
    public void copyFromSetsSecurityZone() {
        connection.setSecurityZone(zone);
        final JdbcConnection copy = new JdbcConnection();
        copy.copyFrom(connection);
        assertEquals(zone, copy.getSecurityZone());
    }

    @Test
    public void testXmlDecoder(){
        JdbcConnection con = new JdbcConnection();
        con.setSerializedProps("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<java version=\"1.7.0\" class=\"java.beans.XMLDecoder\">\n" +
                " <object class=\"java.util.TreeMap\">\n" +
                "  <void method=\"put\">\n" +
                "   <string>EnableCancelTimeout</string>\n" +
                "   <boolean>true</boolean>\n" +
                "  </void>\n" +
                "  <void method=\"put\">\n" +
                "   <string>fg</string>\n" +
                "   <string>asd</string>\n" +
                "  </void>\n" +
                " </object>\n" +
                "</java>\n");
        Boolean b = (Boolean) con.getAdditionalProperties().get("EnableCancelTimeout");
        Assert.assertTrue(b != null);
        Assert.assertTrue(b.booleanValue());

    }
}
