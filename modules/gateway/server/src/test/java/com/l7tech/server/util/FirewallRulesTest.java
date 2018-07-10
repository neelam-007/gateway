package com.l7tech.server.util;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.util.IpProtocol;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class FirewallRulesTest {

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

    @Test
    public void testGenerateRules() throws Exception {
        List<SsgConnector> lc = createTestConnectors();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Map<String, List<String>> iptables = FirewallRules.createFirewallRuleForConnector(lc, IpProtocol.IPv4);
        FirewallRules.writeFirewallRules(os, iptables, IpProtocol.IPv4);

        String got = os.toString();
        got = got.replaceAll("\r\n", "\n");
        got = got.replaceAll("\n\r", "\n");
        got = got.replaceAll("\r", "\n");
        assertEquals(TEST_CONNECTOR_RULES, got);
    }

    public static final String TEST_CONNECTOR_RULES
          = "*filter\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 8080 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 8443 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 9443 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 4747 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 4800:4814 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 4547 -j ACCEPT\n" +
            "[0:0] -A INPUT  -p tcp -m tcp --dport 4550:4564 -j ACCEPT\n" +
            "COMMIT\n\n";

    @Test
    public void testGenerateFirewallRules() throws Exception {
        List<SsgFirewallRule> lc = new ArrayList<SsgFirewallRule>();

        SsgFirewallRule r = new SsgFirewallRule();
        r.setName("Redirect port 80 to 8080");
        r.setOrdinal(1);
        r.putProperty("protocol", "tcp");
        r.putProperty("destination-port", "80");
        r.putProperty("table", "NAT");
        r.putProperty("chain", "PREROUTING");
        r.putProperty("jump", "REDIRECT");
        r.putProperty("to-ports", "8080");
        lc.add(r);

        SsgFirewallRule s = new SsgFirewallRule();
        s.setName("Open port 4547");
        s.setOrdinal(2);
        s.putProperty("destination-port", "4547");
        s.putProperty("protocol", "tcp");
        s.putProperty("chain", "INPUT");
        s.putProperty("jump", "ACCEPT");
        s.setEnabled(true);
        lc.add(s);

        SsgFirewallRule u = new SsgFirewallRule();
        u.setName("ip6 with ip6 source");
        u.setOrdinal(3);
        u.putProperty("destination-port", "4547");
        u.putProperty("protocol", "tcp");
        u.putProperty("chain", "INPUT");
        u.putProperty("jump", "ACCEPT");
        u.putProperty("source", "fe80::20c:29ff:feb0:8692/64");
        u.setEnabled(true);
        lc.add(u);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Map<String, List<String>> iptables = FirewallRules.createFirewallRules(lc, IpProtocol.IPv4);
        FirewallRules.writeFirewallRules(os, iptables, IpProtocol.IPv4);

        String got = os.toString();
        got = got.replaceAll("\r\n", "\n");
        got = got.replaceAll("\n\r", "\n");
        got = got.replaceAll("\r", "\n");

        assertEquals(TEST_CONNECTOR_FIREWALL_RULES, got);
    }

    @Test
    public void testGenerateFirewall6Rules() throws Exception {
        List<SsgFirewallRule> lc = new ArrayList<SsgFirewallRule>();

        SsgFirewallRule r = new SsgFirewallRule();
        r.setName("Redirect port 80 to 8080");
        r.setOrdinal(1);
        r.putProperty("protocol", "tcp");
        r.putProperty("destination-port", "80");
        r.putProperty("table", "NAT");
        r.putProperty("chain", "PREROUTING");
        r.putProperty("jump", "REDIRECT");
        r.putProperty("to-ports", "8080");
        lc.add(r);

        SsgFirewallRule redirect = new SsgFirewallRule();
        redirect.setName("Redirect port 80 to 8080");
        redirect.setOrdinal(2);
        redirect.putProperty("protocol", "tcp");
        redirect.putProperty("destination-port", "80");
        redirect.putProperty("table", "NAT");
        redirect.putProperty("chain", "PREROUTING");
        redirect.putProperty("jump", "REDIRECT");
        redirect.putProperty("to-ports", "8080");
        redirect.putProperty("bindAddress", "127.0.0.1");

        redirect.setEnabled(true);
        lc.add(redirect);

        SsgFirewallRule s = new SsgFirewallRule();
        s.setName("Open port 4547");
        s.setOrdinal(3);
        s.putProperty("destination-port", "4547");
        s.putProperty("protocol", "tcp");
        s.putProperty("chain", "INPUT");
        s.putProperty("jump", "ACCEPT");
        s.setEnabled(true);
        lc.add(s);

        //this should not be added
        SsgFirewallRule t = new SsgFirewallRule();
        t.setName("ip6 with ip4 source");
        t.setOrdinal(4);
        t.putProperty("destination-port", "4547");
        t.putProperty("protocol", "tcp");
        t.putProperty("chain", "INPUT");
        t.putProperty("jump", "ACCEPT");
        t.putProperty("source", "! 192.168.1.1/24");
        t.setEnabled(true);
        lc.add(t);

        //this should be added
        SsgFirewallRule u = new SsgFirewallRule();
        u.setName("ip6 with ip6 source");
        u.setOrdinal(5);
        u.putProperty("destination-port", "4547");
        u.putProperty("protocol", "tcp");
        u.putProperty("chain", "INPUT");
        u.putProperty("jump", "ACCEPT");
        u.putProperty("source", "fe80::20c:29ff:feb0:8692/64");
        u.setEnabled(true);
        lc.add(u);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Map<String, List<String>> iptables = FirewallRules.createFirewallRules(lc, IpProtocol.IPv6);
        FirewallRules.writeFirewallRules(os, iptables, IpProtocol.IPv6);

        String got = os.toString();
        got = got.replaceAll("\r\n", "\n");
        got = got.replaceAll("\n\r", "\n");
        got = got.replaceAll("\r", "\n");

        assertEquals(TEST_CONNECTOR_FIREWALL6_RULES, got);
    }

    private static final String TEST_CONNECTOR_FIREWALL_RULES =
            "*nat\n" +
            "[0:0] -A PREROUTING --protocol tcp --destination-port 80 -j REDIRECT --to-ports 8080\n" +
            "COMMIT\n\n" +
            "*filter\n" +
            "[0:0] -A INPUT --protocol tcp --destination-port 4547 -j ACCEPT\n" +
            "COMMIT\n\n";

    private static final String TEST_CONNECTOR_FIREWALL6_RULES =
                    "*filter\n" +
                    "[0:0] -A INPUT --protocol tcp --destination-port 4547 -j ACCEPT\n" +
                    "[0:0] -A INPUT --protocol tcp --source fe80::20c:29ff:feb0:8692/64 --destination-port 4547 -j ACCEPT\n" +
                    "COMMIT\n\n";
}
