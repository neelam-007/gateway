package com.l7tech.server.partition;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.logging.Logger;
import java.util.Set;
import java.util.List;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;

import com.l7tech.common.io.PortRange;

/**
 *
 */
public class FirewallRulesParserTest extends TestCase {
    private static final Logger log = Logger.getLogger(FirewallRulesParserTest.class.getName());

    public FirewallRulesParserTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(FirewallRulesParserTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void EXAMPLE_testGetFromAllPartitions() throws Exception {
        FirewallRulesParser.PortInfo portInfo = FirewallRulesParser.getAllInfo();        
        log.info("Got all info: " + portInfo);
    }

    public void testParseRules() throws Exception {
        List<PortRange> ranges = FirewallRulesParser.parseFirewallRules(new ByteArrayInputStream(SAMPLE_FILE.getBytes()));
        assertNotNull(ranges);
        assertFalse(ranges.isEmpty());
        assertEquals(ranges.toString(), "[[PortRange TCP 8080-8080], [PortRange TCP 8443-8443], [PortRange TCP 9443-9443], [PortRange TCP /192.168.217.1 9889-9889], [PortRange TCP /192.168.217.1 5666-5720], [PortRange TCP 2122-2122]]");

        FirewallRulesParser.PartitionPortInfo ppi = new FirewallRulesParser.PartitionPortInfo("testpartition", ranges);
        assertFalse(ppi.isPortUsed(8080, true, null));
        assertFalse(ppi.isPortUsed(9889, false, InetAddress.getByName("2.3.4.5")));
        assertFalse(ppi.isPortUsed(9889, false, InetAddress.getLocalHost()));
        assertTrue(ppi.isPortUsed(8080, false, null));
        assertTrue(ppi.isPortUsed(5668, false, null));
        assertTrue(ppi.isPortUsed(5668, false, InetAddress.getByName("192.168.217.1")));
    }

    public static final String SAMPLE_FILE
            = "[0:0] -I INPUT $Rule_Insert_Point  -p tcp -m tcp --dport 8080 -j ACCEPT\n" +
              "[0:0] -I INPUT $Rule_Insert_Point  -p tcp -m tcp --dport 8443 -j ACCEPT\n" +
              "[0:0] -I INPUT $Rule_Insert_Point  -p tcp -m tcp --dport 9443 -j ACCEPT\n" +
              "[0:0] -I INPUT $Rule_Insert_Point  -d 192.168.217.1 -p tcp -m tcp --dport 9889 -j ACCEPT\n" +
              "[0:0] -I INPUT $Rule_Insert_Point  -d 192.168.217.1 -p tcp -m tcp --dport 5666:5720 -j ACCEPT\n" +
              "[0:0] -I INPUT $Rule_Insert_Point  -p tcp -m tcp --dport 2122 -j ACCEPT";
}
