package com.l7tech.server.partition;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

import com.l7tech.common.io.PortRange;
import com.l7tech.common.transport.SsgConnector;

/**
 *
 */
public class FirewallRulesTest extends TestCase {
    private static final Logger log = Logger.getLogger(FirewallRulesTest.class.getName());

    public FirewallRulesTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(FirewallRulesTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void EXAMPLE_testGetFromAllPartitions() throws Exception {
        FirewallRules.PortInfo portInfo = FirewallRules.getAllInfo();
        log.info("Got all info: " + portInfo);
    }

    private List<SsgConnector> createTestConnectors() {
        List<SsgConnector> lc = new ArrayList<SsgConnector>();

        SsgConnector http = new SsgConnector();
        http.setName("Default HTTP (8080)");
        http.setScheme(SsgConnector.SCHEME_HTTP);
        http.setEndpoints("MESSAGE_INPUT,ADMIN_REMOTE,ADMIN_APPLET,OTHER_SERVLETS");
        http.setPort(8080);
        http.setEnabled(true);
        lc.add(http);

        SsgConnector https = new SsgConnector();
        https.setName("Default HTTPS (8443)");
        https.setScheme(SsgConnector.SCHEME_HTTPS);
        https.setEndpoints("MESSAGE_INPUT,ADMIN_REMOTE,ADMIN_APPLET,OTHER_SERVLETS");
        https.setPort(8443);
        https.setKeyAlias("SSL");
        https.setSecure(true);
        https.setClientAuth(SsgConnector.CLIENT_AUTH_OPTIONAL);
        https.setEnabled(true);
        lc.add(https);

        SsgConnector httpsNocc = new SsgConnector();
        httpsNocc.setName("Default HTTPS (9443)");
        httpsNocc.setScheme(SsgConnector.SCHEME_HTTPS);
        httpsNocc.setEndpoints("MESSAGE_INPUT,ADMIN_REMOTE,ADMIN_APPLET,OTHER_SERVLETS");
        httpsNocc.setPort(9443);
        httpsNocc.setKeyAlias("SSL");
        httpsNocc.setSecure(true);
        httpsNocc.setClientAuth(SsgConnector.CLIENT_AUTH_NEVER);
        httpsNocc.setEnabled(true);
        lc.add(httpsNocc);

        SsgConnector ftp = new SsgConnector();
        ftp.setName("Test FTP (4747,4800-4815)");
        ftp.setScheme(SsgConnector.SCHEME_FTP);
        ftp.setEndpoints("MESSAGE_INPUT");
        ftp.setPort(4747);
        ftp.putProperty(SsgConnector.PROP_PORT_RANGE_START, "4800");
        ftp.putProperty(SsgConnector.PROP_PORT_RANGE_COUNT, "15");
        ftp.setEnabled(true);
        lc.add(ftp);

        SsgConnector ftps = new SsgConnector();
        ftps.setName("Test FTPS (4547,4550-4565)");
        ftps.setScheme(SsgConnector.SCHEME_FTP);
        ftps.setEndpoints("MESSAGE_INPUT");
        ftps.setPort(4547);
        ftps.putProperty(SsgConnector.PROP_PORT_RANGE_START, "4550");
        ftps.putProperty(SsgConnector.PROP_PORT_RANGE_COUNT, "15");
        ftps.setEnabled(true);
        lc.add(ftps);

        return lc;
    }

    public void testGenerateRules() throws Exception {
        List<SsgConnector> lc = createTestConnectors();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FirewallRules.writeFirewallRules(os, 2124, lc);

        String got = os.toString();
        got = got.replaceAll("\r\n", "\n");
        got = got.replaceAll("\n\r", "\n");
        got = got.replaceAll("\r", "\n");
        assertEquals(TEST_CONNECTOR_RULES, got);
    }

    public void testParseRules() throws Exception {
        List<PortRange> ranges = FirewallRules.parseFirewallRules(new ByteArrayInputStream(SAMPLE_FILE.getBytes()));
        assertNotNull(ranges);
        assertFalse(ranges.isEmpty());
        assertEquals(ranges.toString(), "[[PortRange TCP 8080-8080], [PortRange TCP 8443-8443], [PortRange TCP 9443-9443], [PortRange TCP /192.168.217.1 9889-9889], [PortRange TCP /192.168.217.1 5666-5720], [PortRange TCP 2122-2122]]");

        FirewallRules.PartitionPortInfo ppi = new FirewallRules.PartitionPortInfo("testpartition", ranges);
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

    public static final String TEST_CONNECTOR_RULES
            = "[0:0] -I INPUT $Rule_Insert_Point  -p tcp -m tcp --dport 8080 -j ACCEPT\n" +
              "[0:0] -I INPUT $Rule_Insert_Point  -p tcp -m tcp --dport 8443 -j ACCEPT\n" +
              "[0:0] -I INPUT $Rule_Insert_Point  -p tcp -m tcp --dport 9443 -j ACCEPT\n" +
              "[0:0] -I INPUT $Rule_Insert_Point  -p tcp -m tcp --dport 4747 -j ACCEPT\n" +
              "[0:0] -I INPUT $Rule_Insert_Point  -p tcp -m tcp --dport 4800:4815 -j ACCEPT\n" +
              "[0:0] -I INPUT $Rule_Insert_Point  -p tcp -m tcp --dport 4547 -j ACCEPT\n" +
              "[0:0] -I INPUT $Rule_Insert_Point  -p tcp -m tcp --dport 4550:4565 -j ACCEPT\n" +
              "[0:0] -I INPUT $Rule_Insert_Point  -p tcp -m tcp --dport 2124 -j ACCEPT\n";
}
