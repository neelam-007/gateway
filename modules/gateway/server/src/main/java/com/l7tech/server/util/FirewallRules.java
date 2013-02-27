package com.l7tech.server.util;

import com.l7tech.common.io.PortRange;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.IpProtocol;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gathers information from firewall_rules files to build a map of ports in use cluster-wide.
 */
public class FirewallRules {
    protected static final Logger logger = Logger.getLogger(FirewallRules.class.getName());

    /**
     * Write the firewall rules to the specified path using the specified source data.
     *
     * @param pathToWrite  the path to the firewall_rules file to create or overwrite. Required.
     * @param connectors  all SsgConnector instances to include in the written-out firewall rules.  May be empty but mustn't be null.
     * @param ipProtocol  determines which connectors' bind addresses will be used to write the firewall rules, based on their IP protocol (IPv4 or IPv6)
     * @throws java.io.IOException if there is a problem writing out the firewall rules file.
     */
    public static void writeFirewallDropfile(String pathToWrite, final Collection<SsgConnector> connectors, final IpProtocol ipProtocol) throws IOException {
        FileUtils.saveFileSafely(pathToWrite,  new FileUtils.Saver() {
            @Override
            public void doSave(FileOutputStream fos) throws IOException {
                final Map<String, List<String>> mappedRules = createFirewallRuleForConnector(connectors, ipProtocol);
                writeFirewallRules(fos, mappedRules, ipProtocol);
            }
        });
    }

    public static void writeFirewallDropfileForRules(String pathToWrite, final Collection<SsgFirewallRule> rules, final IpProtocol ipProtocol) throws IOException {
        FileUtils.saveFileSafely(pathToWrite,  new FileUtils.Saver() {
            @Override
            public void doSave(FileOutputStream fos) throws IOException {
                final Map<String, List<String>> mappedRules = createFirewallRules(rules, ipProtocol);
                writeFirewallRules(fos, mappedRules, ipProtocol);
            }
        });
    }

    /**
     * [0:0] -A INPUT -i INTERFACE -p tcp -m tcp --dport 22:23 -j ACCEPT
     */
    static void writeFirewallRules(OutputStream fos, Map<String, List<String>> mappedRules, final IpProtocol ipProtocol) throws IOException
    {
        if(mappedRules == null || mappedRules.isEmpty()) return;
        PrintStream ps = new PrintStream(fos);
        try {
            for(final Map.Entry<String, List<String>> e : mappedRules.entrySet()){
                final List<String> rules = e.getValue();
                if(rules == null || rules.isEmpty()) continue;
                //IPv6 does not support NAT so we should skip it
                if(ipProtocol.equals(IpProtocol.IPv6) && "NAT".equalsIgnoreCase(e.getKey())) continue;
                //print table header
                ps.println("*" + e.getKey().toLowerCase());
                for(final String rule : rules){
                    ps.println(rule);
                }
                ps.println("COMMIT");
                ps.println();
            }
            ps.flush();
            if (ps.checkError()) throw new IOException("Error while writing firewall rules");
        } finally {
            ps.flush();
        }
    }

    static Map<String, List<String>> createFirewallRuleForConnector(final Collection<SsgConnector> connectors, final IpProtocol ipProtocol){
        Map<String, List<String>> iptables = new LinkedHashMap<String, List<String>>();
        for (SsgConnector connector : connectors) {
            String table = connector.getProperty("table");
            if(table == null || table.isEmpty()) table = "filter";
            List<String> rules = iptables.get(table);
            if(rules == null) rules = new ArrayList<String>();

            String bindAddress = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
            String interfaceName = null;
            if ( bindAddress != null && ipProtocol.validateAddress(bindAddress).isEmpty() ) {
                interfaceName = getInterfaceForIP(bindAddress);
                if ( interfaceName == null ) {
                    logger.log( Level.WARNING, "Could not determine interface for IP address ''{0}'', this connector will be inaccessible.", bindAddress);
                    continue;
                }
            }

            List<PortRange> ranges = connector.getTcpPortsUsed();
            for (PortRange range : ranges) {
                final StringBuilder builder = new StringBuilder();
                int portStart = range.getPortStart();
                int portEnd = range.getPortEnd();

                builder.append("[0:0] -A INPUT ");
                if ( interfaceName != null ) {
                    builder.append(" -i ");
                    builder.append(interfaceName);
                }
                builder.append(" -p tcp -m tcp --dport ");
                if (portStart == portEnd)
                    builder.append(portStart);
                else
                    builder.append(portStart).append(":").append(portEnd);
                builder.append(" -j ACCEPT");
                rules.add(builder.toString());
            }
            iptables.put(table, rules);
        }
        return iptables;
    }

    private static final List<String> MATCH_GENERIC;
    private static final List<String> MATCH_IMPLICIT_TCP;
    private static final List<String> MATCH_IMPLICIT_UDP;
    private static final List<String> MATCH_IMPLICIT_ICMP;
    private static final Map<String, List<String>> JUMP_TARGET;
    private static final Map<String, List<String>> PROTOCOL_MATCH;

    static {
        List<String> g = new ArrayList<String>();
        g.add("p");
        g.add("protocol");
        g.add("s");
        g.add("src");
        g.add("source");
        g.add("d");
        g.add("dst");
        g.add("destination");
        g.add("i");
        g.add("in-interface");
        g.add("o");
        g.add("out-interface");
        g.add("f");
        g.add("fragment");
        MATCH_GENERIC = Collections.unmodifiableList(g);

        List<String> t = new ArrayList<String>();
        t.add("sport");
        t.add("source-port");
        t.add("dport");
        t.add("destination-port");
        t.add("tcp-flags");
        t.add("syn");
        t.add("tcp-option");
        MATCH_IMPLICIT_TCP = Collections.unmodifiableList(t);

        List<String> u = new ArrayList<String>();
        u.add("sport");
        u.add("source-port");
        u.add("dport");
        u.add("destination-port");
        MATCH_IMPLICIT_UDP = Collections.unmodifiableList(u);

        List<String> i = new ArrayList<String>();
        i.add("icmp-type");
        MATCH_IMPLICIT_ICMP = Collections.unmodifiableList(i);

        List<String> redirect = new ArrayList<String>();
        redirect.add("to-ports");

        List<String> dnat = new ArrayList<String>();
        dnat.add("to-destination");

        Map<String, List<String>> m = new HashMap<String, List<String>>();
        m.put("REDIRECT", redirect);
        m.put("DNAT", dnat);
        m.put("ACCEPT", new ArrayList<String>());
        JUMP_TARGET = Collections.unmodifiableMap(m);

        Map<String, List<String>> n = new HashMap<String, List<String>>();
        n.put("tcp", MATCH_IMPLICIT_TCP);
        n.put("udp", MATCH_IMPLICIT_UDP);
        n.put("icmp", MATCH_IMPLICIT_ICMP);
        PROTOCOL_MATCH = Collections.unmodifiableMap(n);
    }

    static Map<String, List<String>> createFirewallRules(@NotNull final Collection<SsgFirewallRule> rules, @NotNull IpProtocol ipProtocol){
        Map<String, List<String>> iptables = new LinkedHashMap<String, List<String>>();
        for(SsgFirewallRule rule : rules){
            String table = rule.getProperty("table");
            if(table == null || table.trim().isEmpty()) table = "filter";
            List<String> currentRules = iptables.get(table);
            if(currentRules == null) currentRules = new ArrayList<String>();

            final StringBuilder builder = new StringBuilder("[0:0] -A ").append(rule.getProperty("chain"));

            //generic match
            final String generic = getOptions(rule, MATCH_GENERIC);
            if(!generic.isEmpty()){
                builder.append(" ").append(generic);
            }

            //implicit match - based on protocol
            final String protocol = getProtocol(rule);
            final String implicit = getOptions(rule, PROTOCOL_MATCH.get(protocol));
            if(!implicit.isEmpty()){
                builder.append(" ").append(implicit);
            }
            //explicit ?

            //jump target
            final String jump = rule.getProperty("jump");
            if(jump != null && !jump.isEmpty()){
                builder.append(" ").append("-j ").append(jump);
                final String targetOptions = getOptions(rule, JUMP_TARGET.get(jump));
                if(targetOptions != null && !targetOptions.isEmpty()){
                    builder.append(" ").append(targetOptions);
                }
            }
            final String source = getIpAddress(rule, "source");
            final String destination = getIpAddress(rule, "destination");
            //if we are using ipv4 and we got ipv6 address in the source or destination, don't write it to the ip6tables
            //and vice-versa
            if(ipProtocol.equals(IpProtocol.IPv4)){
                if(source != null && !source.isEmpty() && !InetAddressUtil.isValidIpv4Address(source)){
                    continue;
                }
                if(destination != null && !destination.isEmpty() && !InetAddressUtil.isValidIpv4Address(destination)){
                    continue;
                }
            }
            else if(ipProtocol.equals(IpProtocol.IPv6)){
                if(source != null && !source.isEmpty() && !InetAddressUtil.isValidIpv6Address(source)){
                    continue;
                }
                if(destination != null && !destination.isEmpty() && !InetAddressUtil.isValidIpv6Address(destination)){
                    continue;
                }
            }
            currentRules.add(builder.toString());
            iptables.put(table, currentRules);
        }
        return iptables;
    }

    private static final Pattern IP_ADDRESS = Pattern.compile("(?:!\\s+)?(.+?)(?:/(.+))?");

    private static String getIpAddress(@NotNull final SsgFirewallRule rule, final String propertyName){
        final String source = rule.getProperty(propertyName);
        if(source != null){
            final Matcher matcher = IP_ADDRESS.matcher(source);
            if(matcher.matches()){
                return matcher.group(1);
            }
        }
        return null;
    }

    private static String getOptions(@NotNull final SsgFirewallRule rule, final List<String> matchers){
        final StringBuilder sb = new StringBuilder();
        if(matchers != null){
            for(String m : matchers){
                final String dash = m.length() == 1 ? "-" : "--";
                String value = rule.getProperty(m);
                if(m.equals("in-interface") || m.equals("out-interface")){
                    String bindAddress = value;
                    if ( bindAddress != null) {
                        value = getInterfaceForIP(bindAddress);
                        if ( value == null ) {
                            logger.log( Level.WARNING, "Could not determine interface for IP address ''{0}'', omitting '" + m + " value from rule.", bindAddress);
                            continue;
                        }
                    }
                }
                if(value != null && !value.trim().isEmpty()){
                    sb.append(dash).append(m).append(" ").append(value).append(" ");
                }
            }
        }
        return sb.toString().trim();
    }

    private static String getProtocol(@NotNull final SsgFirewallRule rule){
        String protocol = rule.getProperty("protocol");
        if(protocol == null){
            protocol = rule.getProperty("p");
        }
        return protocol;
    }

    private static String getInterfaceForIP( final String address ) {
        String name = null;

        try {
            NetworkInterface ni;
            if ( InetAddress.getByName(address).isLoopbackAddress() ) {
                ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            } else {
                ni = NetworkInterface.getByInetAddress(InetAddress.getByName(address));
            }

            if ( ni != null ) {
                while ( ni.isVirtual() ) {
                    ni = ni.getParent();
                }

                name = ni.getName();
            }
        } catch ( IOException ioe ) {
            logger.log( Level.FINE, "Unable to determine network interface for ip '"+address+"'." , ExceptionUtils.getDebugException(ioe));
        }

        return name;
    }
}
