package com.l7tech.server.util;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;

import com.l7tech.common.io.PortRange;
import com.l7tech.gateway.common.transport.SsgConnector;

/**
 *
 */
public class FirewallRulesTest extends TestCase {

    public FirewallRulesTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(FirewallRulesTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
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

    public static final String TEST_CONNECTOR_RULES
          = "[0:0] -A INPUT  -p tcp -m tcp --dport 8080 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 8443 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 9443 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 4747 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 4800:4815 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 4547 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 4550:4565 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 2124 -j ACCEPT\n";
}
